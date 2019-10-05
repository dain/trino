/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.prestosql.orc;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import io.prestosql.metadata.Metadata;
import io.prestosql.metadata.MetadataManager;
import io.prestosql.spi.Page;
import io.prestosql.spi.block.ArrayBlock;
import io.prestosql.spi.block.Block;
import io.prestosql.spi.block.LazyBlock;
import io.prestosql.spi.block.MapBlock;
import io.prestosql.spi.block.RowBlock;
import io.prestosql.spi.type.ArrayType;
import io.prestosql.spi.type.RowType;
import io.prestosql.spi.type.StandardTypes;
import io.prestosql.spi.type.Type;
import io.prestosql.spi.type.TypeSignature;
import io.prestosql.spi.type.TypeSignatureParameter;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.List;
import java.util.stream.Stream;

import static com.google.common.collect.Iterables.cycle;
import static com.google.common.collect.Iterables.limit;
import static com.google.common.collect.Lists.newArrayList;
import static io.airlift.testing.Assertions.assertInstanceOf;
import static io.prestosql.memory.context.AggregatedMemoryContext.newSimpleAggregatedMemoryContext;
import static io.prestosql.orc.OrcReader.MAX_BATCH_SIZE;
import static io.prestosql.orc.OrcTester.Format.ORC_12;
import static io.prestosql.orc.OrcTester.HIVE_STORAGE_TIME_ZONE;
import static io.prestosql.orc.OrcTester.READER_OPTIONS;
import static io.prestosql.orc.OrcTester.writeOrcColumnHive;
import static io.prestosql.orc.metadata.CompressionKind.LZ4;
import static io.prestosql.spi.type.BigintType.BIGINT;
import static io.prestosql.spi.type.RowType.field;
import static io.prestosql.spi.type.VarcharType.VARCHAR;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class TestOrcLazy
{
    private static final Metadata METADATA = MetadataManager.createTestMetadataManager();
    private static final Type MAP_VARCHAR_BIGINT = METADATA.getType(new TypeSignature(
            StandardTypes.MAP,
            TypeSignatureParameter.typeParameter(VARCHAR.getTypeSignature()),
            TypeSignatureParameter.typeParameter(BIGINT.getTypeSignature())));
    private static final Type MAP_VARCHAR_ARRAY = METADATA.getType(new TypeSignature(
            StandardTypes.MAP,
            TypeSignatureParameter.typeParameter(VARCHAR.getTypeSignature()),
            TypeSignatureParameter.typeParameter(new ArrayType(VARCHAR).getTypeSignature())));
    private static final Type MAP_VARCHAR_MAP = METADATA.getType(new TypeSignature(
            StandardTypes.MAP,
            TypeSignatureParameter.typeParameter(VARCHAR.getTypeSignature()),
            TypeSignatureParameter.typeParameter(MAP_VARCHAR_BIGINT.getTypeSignature())));
    private static final Type MAP_VARCHAR_ROW = METADATA.getType(new TypeSignature(
            StandardTypes.MAP,
            TypeSignatureParameter.typeParameter(VARCHAR.getTypeSignature()),
            TypeSignatureParameter.typeParameter(RowType.from(ImmutableList.of(field("a", VARCHAR), field("b", BIGINT))).getTypeSignature())));

    @Test
    public void testLazy()
            throws Exception
    {
        testType(VARCHAR, ImmutableList.of("a", "o", "z"));

        testType(new ArrayType(VARCHAR), ImmutableList.of(
                ImmutableList.of("a", "b", "c"),
                ImmutableList.of("o"),
                ImmutableList.of("1", "2", "3")));
        testType(new ArrayType(new ArrayType(VARCHAR)), ImmutableList.of(
                ImmutableList.of(ImmutableList.of("a", "b"), ImmutableList.of("c")),
                ImmutableList.of(ImmutableList.of("one"), ImmutableList.of()),
                ImmutableList.of(ImmutableList.of("1", "2", "3"))));
        testType(new ArrayType(RowType.from(ImmutableList.of(field("a", VARCHAR), field("b", BIGINT)))), ImmutableList.of(
                ImmutableList.of(ImmutableList.of("a", 1), ImmutableList.of("b", 2), ImmutableList.of("c", 3)),
                ImmutableList.of(ImmutableList.of("o", 0)),
                ImmutableList.of(ImmutableList.of("1", 10), ImmutableList.of("2", 20), ImmutableList.of("3", 30))));
        testType(new ArrayType(MAP_VARCHAR_BIGINT), ImmutableList.of(
                ImmutableList.of(ImmutableMap.of("a", 1), ImmutableMap.of("b", 2), ImmutableMap.of("c", 3)),
                ImmutableList.of(ImmutableMap.of("o", 0)),
                ImmutableList.of(ImmutableMap.of("1", 10), ImmutableMap.of("2", 20), ImmutableMap.of("3", 30))));

        testType(MAP_VARCHAR_BIGINT, ImmutableList.of(
                ImmutableMap.of("a", 1L, "b", 2L, "c", 3L),
                ImmutableMap.of("o", 0L),
                ImmutableMap.of("1", 10L, "2", 20L, "3", 30L)));
        testType(MAP_VARCHAR_ARRAY, ImmutableList.of(
                ImmutableMap.of("a", ImmutableList.of("1", "2"), "b", ImmutableList.of("3", "4", "5")),
                ImmutableMap.of("o", ImmutableList.of("zero")),
                ImmutableMap.of("1", ImmutableList.of("10"), "2", ImmutableList.of("20"))));
        testType(MAP_VARCHAR_MAP, ImmutableList.of(
                ImmutableMap.of("a", ImmutableMap.of("1", 2), "b", ImmutableMap.of("3", 4, "5", 6)),
                ImmutableMap.of("o", ImmutableMap.of("zero", 0)),
                ImmutableMap.of("1", ImmutableMap.of("10", 10), "2", ImmutableMap.of("20", 20))));
        testType(MAP_VARCHAR_ROW, ImmutableList.of(
                ImmutableMap.of("a", ImmutableList.of("1", 2), "b", ImmutableList.of("3", 4)),
                ImmutableMap.of("o", ImmutableList.of("zero", 0)),
                ImmutableMap.of("1", ImmutableList.of("10", 10), "2", ImmutableList.of("20", 20))));

        testType(
                RowType.from(ImmutableList.of(field("a", VARCHAR), field("b", BIGINT), field("c", new ArrayType(VARCHAR)), field("d", MAP_VARCHAR_BIGINT))),
                ImmutableList.of(
                        ImmutableList.of("x", 1, ImmutableList.of("a", "b", "c"), ImmutableMap.of("a", 1L, "b", 2L, "c", 3L)),
                        ImmutableList.of("y", 2, ImmutableList.of("o"), ImmutableMap.of("o", 0L)),
                        ImmutableList.of("z", 3, ImmutableList.of("1", "2", "3"), ImmutableMap.of("1", 10L, "2", 20L, "3", 30L))));
    }

    private static <T> void testType(Type type, List<T> uniqueValues)
            throws Exception
    {
        Stream<T> writeValues = newArrayList(limit(cycle(uniqueValues), 1000)).stream();

        try (TempFile tempFile = new TempFile()) {
            writeOrcColumnHive(tempFile.getFile(), ORC_12, LZ4, type, writeValues.iterator());

            try (OrcRecordReader recordReader = createCustomOrcRecordReader(tempFile, OrcPredicate.TRUE, type, MAX_BATCH_SIZE)) {
                for (Page page = recordReader.nextPage(); page != null; page = recordReader.nextPage()) {
                    for (int i = 0; i < page.getChannelCount(); i++) {
                        assertLazy(page.getBlock(i), recordReader, page.getPositionCount());
                    }
                }
            }
        }
    }

    private static void assertLazy(Block block, OrcRecordReader recordReader, int pagePositionCount)
    {
        long expectedBytesPerRow = recordReader.getMaxCombinedBytesPerRow();
        assertInstanceOf(block, LazyBlock.class);
        assertFalse(block.isLoaded());
        assertEquals(block.getSizeInBytes(), 0);
        block = ((LazyBlock) block).getBlock();
        assertFalse(block instanceof LazyBlock);
        expectedBytesPerRow += block.getSizeInBytes() / pagePositionCount;
        assertEquals(recordReader.getMaxCombinedBytesPerRow(), expectedBytesPerRow, 1.0);

        if (block instanceof ArrayBlock) {
            long expectedSize = (Integer.BYTES + Byte.BYTES) * block.getPositionCount();
            assertEquals(block.getSizeInBytes(), expectedSize);

            Block elementsBlock = block.getChildren().get(0);
            assertLazy(elementsBlock, recordReader, pagePositionCount);

            long elementBlockSize = elementsBlock.getSizeInBytes();
            expectedSize += elementBlockSize;
            assertEquals(block.getSizeInBytes(), expectedSize);

            expectedBytesPerRow += elementBlockSize / pagePositionCount;
            assertEquals(recordReader.getMaxCombinedBytesPerRow(), expectedBytesPerRow, 1.0);
        }
        if (block instanceof MapBlock) {
            Block keysBlock = block.getChildren().get(0);
            assertFalse(keysBlock instanceof LazyBlock);
            assertEquals(keysBlock.getSizeInBytes(), keysBlock.getSizeInBytes());

            assertTrue(block.getSizeInBytes() > (Integer.BYTES + Integer.BYTES + Byte.BYTES) * block.getPositionCount() + keysBlock.getSizeInBytes());
            long expectedSize = block.getSizeInBytes();

            Block valuesBlock = block.getChildren().get(1);
            assertLazy(valuesBlock, recordReader, pagePositionCount);

            long valuesBlockSize = valuesBlock.getSizeInBytes();
            expectedSize += valuesBlockSize;
            assertEquals(block.getSizeInBytes(), expectedSize);

            expectedBytesPerRow += valuesBlockSize / pagePositionCount;
            assertEquals(recordReader.getMaxCombinedBytesPerRow(), expectedBytesPerRow, 1.0);
        }
        if (block instanceof RowBlock) {
            long expectedSize = (Integer.BYTES + Byte.BYTES) * block.getPositionCount();
            assertEquals(block.getSizeInBytes(), expectedSize);

            for (Block fieldBlock : block.getChildren()) {
                assertLazy(fieldBlock, recordReader, pagePositionCount);

                long fieldBlockSize = fieldBlock.getSizeInBytes();
                expectedSize += fieldBlockSize;
                assertEquals(block.getSizeInBytes(), expectedSize);

                expectedBytesPerRow += fieldBlockSize / pagePositionCount;
                assertEquals(recordReader.getMaxCombinedBytesPerRow(), expectedBytesPerRow, 1.0);
            }
        }

        assertEquals(block.getSizeInBytes(), block.getSizeInBytes());
    }

    private static OrcRecordReader createCustomOrcRecordReader(TempFile tempFile, OrcPredicate predicate, Type type, int initialBatchSize)
            throws IOException
    {
        OrcDataSource orcDataSource = new FileOrcDataSource(tempFile.getFile(), READER_OPTIONS);
        OrcReader orcReader = new OrcReader(orcDataSource, READER_OPTIONS);

        assertEquals(orcReader.getColumnNames(), ImmutableList.of("test"));
        assertEquals(orcReader.getFooter().getRowsInRowGroup(), 10_000);

        return orcReader.createRecordReader(
                orcReader.getRootColumn().getNestedColumns(),
                ImmutableList.of(type),
                predicate,
                HIVE_STORAGE_TIME_ZONE,
                newSimpleAggregatedMemoryContext(),
                initialBatchSize,
                RuntimeException::new);
    }
}

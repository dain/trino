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
package io.trino.plugin.hive;

import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Writable;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkArgument;
import static org.assertj.core.api.Assertions.assertThat;

public class TestFooterAwareHiveRecordReader
{
    @Test
    public void test()
            throws IOException
    {
        assertThat(readAllValues(new TestingHiveRecordReader(10))).containsExactly(10, 9, 8, 7, 6, 5, 4, 3, 2, 1);
        testFooterRead(10, 1, 10, 9, 8, 7, 6, 5, 4, 3, 2);
        testFooterRead(10, 2, 10, 9, 8, 7, 6, 5, 4, 3);
        testFooterRead(10, 8, 10, 9);
        testFooterRead(10, 9, 10);
        testFooterRead(10, 10);
        testFooterRead(10, 11);
        testFooterRead(10, 100);

        testFooterRead(0, 1);
        testFooterRead(0, 2);
    }

    private static void testFooterRead(int fileLines, int footerCount, Integer... expectedValues)
            throws IOException
    {
        FooterAwareHiveRecordReader reader = new FooterAwareHiveRecordReader(new TestingHiveRecordReader(fileLines), footerCount);
        assertThat(readAllValues(reader)).containsExactly(expectedValues);
        assertThat(reader.next()).isEmpty();
    }

    private static List<Integer> readAllValues(HiveRecordReader reader)
            throws IOException
    {
        List<Integer> values = new ArrayList<>();
        while (true) {
            Optional<Writable> value = reader.next();
            if (value.isEmpty()) {
                return values;
            }
            values.add(((IntWritable) value.get()).get());
        }
    }

    private static class TestingHiveRecordReader
            implements HiveRecordReader
    {
        private int linesRemaining;
        private final IntWritable value = new IntWritable();

        public TestingHiveRecordReader(int fileLines)
        {
            checkArgument(fileLines >= 0, "fileLines is negative");
            this.linesRemaining = fileLines;
        }

        @Override
        public Class<? extends Writable> getType()
        {
            return IntWritable.class;
        }

        @Override
        public long getCompletedBytes()
        {
            return 1000 - linesRemaining;
        }

        @Override
        public Optional<Writable> next()
                throws IOException
        {
            if (linesRemaining <= 0) {
                return Optional.empty();
            }
            value.set(linesRemaining);
            linesRemaining--;
            return Optional.of(value);
        }

        @Override
        public void close()
                throws IOException
        {
            linesRemaining = 0;
            value.set(-99);
        }
    }
}

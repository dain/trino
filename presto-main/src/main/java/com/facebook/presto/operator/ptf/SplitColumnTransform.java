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
package com.facebook.presto.operator.ptf;

import com.facebook.presto.spi.Page;
import com.facebook.presto.spi.PageBuilder;
import com.facebook.presto.spi.block.Block;
import com.facebook.presto.spi.block.BlockBuilder;
import com.facebook.presto.spi.type.Type;
import com.google.common.collect.ImmutableList;
import io.airlift.slice.Slice;

import java.lang.invoke.MethodHandle;
import java.util.List;
import java.util.function.Function;

import static com.facebook.presto.spi.type.VarcharType.VARCHAR;
import static com.facebook.presto.util.Failures.internalError;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.collect.ObjectArrays.concat;
import static java.util.Objects.requireNonNull;

public class SplitColumnTransform
        implements Function<Page, Page>
{
    private final int splitColumn;
    private final Slice delimiter;
    private final List<Type> types;
    private final List<MethodHandle> casts;
    private final PageBuilder pageBuilder;

    public SplitColumnTransform(int splitColumn, Slice delimiter, List<Type> types, List<MethodHandle> casts)
    {
        checkArgument(splitColumn >= 0, "splitColumn is negative");
        this.splitColumn = splitColumn;
        this.delimiter = requireNonNull(delimiter, "delimiter is null");
        this.types = ImmutableList.copyOf(requireNonNull(types, "types is null"));
        this.casts = ImmutableList.copyOf(requireNonNull(casts, "casts is null"));
        pageBuilder = new PageBuilder(types);
    }

    @Override
    public Page apply(Page input)
    {
        Block block = input.getBlock(splitColumn);

        for (int position = 0; position < block.getPositionCount(); position++) {
            pageBuilder.declarePosition();

            int partNumber = 0;
            if (!block.isNull(position)) {
                Slice string = VARCHAR.getSlice(block, position);

                int index = 0;
                while (index < string.length() && partNumber < types.size()) {
                    int splitIndex = string.indexOf(delimiter, index);

                    // last part of string does not end with delimiter
                    if (splitIndex < 0) {
                        splitIndex = string.length();
                    }

                    // Add the part from current index to found split
                    Slice part = string.slice(index, splitIndex - index);
                    writePart(partNumber, part);

                    partNumber++;

                    // Continue searching after delimiter
                    index = splitIndex + delimiter.length();
                }
            }

            // write null to the unmatched columns
            while (partNumber < types.size()) {
                pageBuilder.getBlockBuilder(partNumber).appendNull();
                partNumber++;
            }
        }

        Page splitColumns = pageBuilder.build();
        pageBuilder.reset();

        return new Page(input.getPositionCount(), concat(input.getBlocks(), splitColumns.getBlocks(), Block.class));
    }

    private void writePart(int partNumber, Slice part)
    {
        try {
            BlockBuilder blockBuilder = pageBuilder.getBlockBuilder(partNumber);
            Type type = types.get(partNumber);
            MethodHandle cast = casts.get(partNumber);
            if (type.getJavaType() == boolean.class) {
                type.writeBoolean(blockBuilder, (boolean) cast.invokeExact(part));
            }
            else if (type.getJavaType() == long.class) {
                type.writeLong(blockBuilder, (long) cast.invokeExact(part));
            }
            else if (type.getJavaType() == double.class) {
                type.writeDouble(blockBuilder, (double) cast.invokeExact(part));
            }
            else if (type.getJavaType() == Slice.class) {
                type.writeSlice(blockBuilder, (Slice) cast.invokeExact(part));
            }
            else {
                type.writeObject(blockBuilder, cast.invoke(part));
            }
        }
        catch (Throwable throwable) {
            throw internalError(throwable);
        }
    }
}

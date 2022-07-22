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

import io.airlift.slice.DynamicSliceOutput;
import org.apache.hadoop.io.Writable;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Optional;
import java.util.Queue;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;

public class FooterAwareHiveRecordReader
        implements HiveRecordReader
{
    private final HiveRecordReader delegate;
    // the value of the next footerCount rows
    private final Queue<Writable> buffer;
    // the buffer that will be used to read the next row
    private Writable nextBufferValue;

    private final DynamicSliceOutput copyBuffer = new DynamicSliceOutput(128);

    public FooterAwareHiveRecordReader(HiveRecordReader delegate, int footerCount)
            throws IOException
    {
        this.delegate = requireNonNull(delegate, "delegate is null");
        this.buffer = new ArrayDeque<>(footerCount);

        checkArgument(footerCount > 0, "footerCount must be at least 1");
        nextBufferValue = createWritable(delegate.getType());

        // load the first footerCount rows into the buffer
        for (int i = 0; i < footerCount; i++) {
            bufferValue(createWritable(delegate.getType()));
        }
    }

    @Override
    public Class<? extends Writable> getType()
    {
        return delegate.getType();
    }

    @Override
    public long getCompletedBytes()
    {
        return delegate.getCompletedBytes();
    }

    @Override
    public Optional<Writable> next()
            throws IOException
    {
        if (nextBufferValue != null) {
            bufferValue(nextBufferValue);
        }

        Writable value = buffer.poll();
        // the next buffered row uses the last returned value
        nextBufferValue = value;
        return Optional.ofNullable(value);
    }

    @Override
    public void close()
            throws IOException
    {
        nextBufferValue = null;
        buffer.clear();
        delegate.close();
    }

    private void bufferValue(Writable valueToBeBuffered)
            throws IOException
    {
        Optional<Writable> next = delegate.next();
        // if there is not a next value, the values in the buffer are from the footer and are ignored
        if (next.isEmpty()) {
            close();
            return;
        }
        // the value must be copied from the reader's buffer to our buffer, because the
        // reader will reuse this buffer for the next read
        copyValue(next.get(), valueToBeBuffered);
        buffer.add(valueToBeBuffered);
    }

    private void copyValue(Writable source, Writable destination)
            throws IOException
    {
        copyBuffer.reset();
        source.write(copyBuffer);
        destination.readFields(copyBuffer.slice().getInput());
    }

    private static Writable createWritable(Class<? extends Writable> type)
    {
        try {
            return type.getDeclaredConstructor().newInstance();
        }
        catch (ReflectiveOperationException e) {
            throw new RuntimeException("Error creating writable type: " + type.getCanonicalName(), e);
        }
    }
}

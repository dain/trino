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

import org.apache.hadoop.io.Writable;
import org.apache.hadoop.mapred.RecordReader;

import java.io.IOException;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.util.Objects.requireNonNull;

public class GenericHiveRecordReader<K, V extends Writable>
        implements HiveRecordReader
{
    private final RecordReader<K, V> recordReader;
    private final K key;
    private final V value;
    private final long totalBytes;

    private long completedBytes;
    private boolean closed;

    public GenericHiveRecordReader(RecordReader<K, V> recordReader, long totalBytes)
    {
        this.recordReader = requireNonNull(recordReader, "recordReader is null");
        this.key = recordReader.createKey();
        this.value = recordReader.createValue();
        this.totalBytes = totalBytes;
        checkArgument(totalBytes >= 0, "totalBytes is negative");
    }

    @Override
    public Class<? extends Writable> getType()
    {
        return value.getClass();
    }

    @Override
    public long getCompletedBytes()
    {
        if (!closed) {
            updateCompletedBytes();
        }
        return completedBytes;
    }

    private void updateCompletedBytes()
    {
        try {
            @SuppressWarnings("NumericCastThatLosesPrecision")
            long newCompletedBytes = (long) (totalBytes * recordReader.getProgress());
            completedBytes = min(totalBytes, max(completedBytes, newCompletedBytes));
        }
        catch (IOException ignored) {
        }
    }

    @Override
    public Optional<Writable> next()
            throws IOException
    {
        checkState(!closed);
        if (!recordReader.next(key, value)) {
            return Optional.empty();
        }
        return Optional.of(value);
    }

    @Override
    public void close()
            throws IOException
    {
        // some hive input formats are broken and bad things can happen if you close them multiple times
        if (closed) {
            return;
        }
        closed = true;

        updateCompletedBytes();

        recordReader.close();
    }
}

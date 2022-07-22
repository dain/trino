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
package io.trino.plugin.hive.text;

import io.trino.plugin.hive.HiveRecordReader;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.Writable;

import java.io.IOException;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkState;
import static java.util.Objects.requireNonNull;

public class TextHiveRecordReader
        implements HiveRecordReader
{
    private final LineRecordReader recordReader;
    private final Text value;

    private boolean closed;

    public TextHiveRecordReader(LineRecordReader recordReader)
    {
        this.recordReader = requireNonNull(recordReader, "recordReader is null");
        this.value = new Text();
    }

    @Override
    public Class<? extends Writable> getType()
    {
        return Text.class;
    }

    @Override
    public long getCompletedBytes()
    {
        return recordReader.getCompletedBytes();
    }

    @Override
    public Optional<Writable> next()
            throws IOException
    {
        checkState(!closed);
        if (!recordReader.next(value)) {
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

        recordReader.close();
    }
}

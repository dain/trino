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

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.lib.input.SplitLineReader;

import java.io.IOException;
import java.io.InputStream;

// Note: this code was forked from Apache Hadoop
public class UncompressedSplitLineReader
        extends SplitLineReader
{
    private boolean needAdditionalRecord;
    private final long splitLength;
    private long totalBytesRead;
    private boolean finished;
    private final boolean usingCRLF;

    public UncompressedSplitLineReader(
            InputStream in,
            Configuration conf,
            byte[] recordDelimiterBytes,
            long splitLength)
            throws IOException
    {
        super(in, conf, recordDelimiterBytes);
        this.splitLength = splitLength;
        this.usingCRLF = (recordDelimiterBytes == null);
    }

    @Override
    protected int fillBuffer(InputStream in, byte[] buffer, boolean inDelimiter)
            throws IOException
    {
        int maxBytesToRead = buffer.length;
        if (totalBytesRead < splitLength) {
            long bytesLeftInSplit = splitLength - totalBytesRead;

            if (bytesLeftInSplit < maxBytesToRead) {
                maxBytesToRead = (int) bytesLeftInSplit;
            }
        }
        int bytesRead = in.read(buffer, 0, maxBytesToRead);

        // If the split ended in the middle of a record delimiter then we need
        // to read one additional record, as the consumer of the next split will
        // not recognize the partial delimiter as a record.
        // However, if using the default delimiter and the next character is a
        // linefeed then next split will treat it as a delimiter all by itself
        // and the additional record read should not be performed.
        if (totalBytesRead == splitLength && inDelimiter && bytesRead > 0) {
            if (usingCRLF) {
                needAdditionalRecord = (buffer[0] != '\n');
            }
            else {
                needAdditionalRecord = true;
            }
        }
        if (bytesRead > 0) {
            totalBytesRead += bytesRead;
        }
        return bytesRead;
    }

    @Override
    public int readLine(Text text, int maxLineLength)
            throws IOException
    {
        int bytesRead = 0;
        if (!finished) {
            // only allow at most one more record to be read after the stream
            // reports the split ended
            if (totalBytesRead > splitLength) {
                finished = true;
            }

            int maxBytesToConsume = Math.max((int) Math.min(Integer.MAX_VALUE, splitLength - totalBytesRead), maxLineLength);
            bytesRead = super.readLine(text, maxLineLength, maxBytesToConsume);
        }
        return bytesRead;
    }

    @Override
    public int readLine(Text text, int maxLineLength, int maxBytesToConsume)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public int readLine(Text text)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean needAdditionalRecordAfterSplit()
    {
        return !finished && needAdditionalRecord;
    }

    @Override
    protected void unsetNeedAdditionalRecordAfterSplit()
    {
        needAdditionalRecord = false;
    }
}

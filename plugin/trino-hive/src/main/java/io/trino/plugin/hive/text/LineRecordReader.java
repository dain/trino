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

import com.google.common.io.Closer;
import io.airlift.log.Logger;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.lib.input.SplitLineReader;

import java.io.Closeable;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static java.lang.Math.max;
import static java.util.Objects.requireNonNull;

// Note: this code was forked from Apache Hadoop
public class LineRecordReader
        implements Closeable
{
    private static final Logger LOG = Logger.get(LineRecordReader.class);

    private final LineDataSource lineDataSource;
    private final SplitLineReader in;
    private final int maxLineLength;
    private final Closer closer = Closer.create();

    private long pos;

    public LineRecordReader(Configuration job, LineDataSource lineDataSource)
            throws IOException
    {
        try {
            requireNonNull(job, "job is null");
            this.lineDataSource = requireNonNull(lineDataSource, "lineDataSource is null");
            closer.register(lineDataSource);

            this.maxLineLength = job.getInt(org.apache.hadoop.mapreduce.lib.input.LineRecordReader.MAX_LINE_LENGTH, Integer.MAX_VALUE);

            byte[] recordDelimiter = getRecordDelimiter(job);
            if (lineDataSource.isCompressed()) {
                in = new SplitLineReader(lineDataSource.getInputStream(), job, recordDelimiter);
            }
            else {
                long splitLength = lineDataSource.getFileEnd() - lineDataSource.getFileStart();
                in = new UncompressedSplitLineReader(lineDataSource.getInputStream(), job, recordDelimiter, splitLength);
            }
            closer.register(in);
        }
        catch (IOException e) {
            throw LineDataSource.closeAndRethrow(e, closer);
        }
    }

    public long getCompletedBytes()
    {
        try {
            return max(lineDataSource.getFilePosition() - lineDataSource.getFileStart(), 0);
        }
        catch (IOException ignored) {
            return lineDataSource.getFileStart();
        }
    }

    private int skipUtfByteOrderMark(Text value)
            throws IOException
    {
        // Strip BOM(Byte Order Mark)
        // Text only support UTF-8, we only need to check UTF-8 BOM
        // (0xEF,0xBB,0xBF) at the start of the text stream.
        int newMaxLineLength = (int) Math.min(3L + (long) maxLineLength, Integer.MAX_VALUE);
        int newSize = in.readLine(value, newMaxLineLength);
        // Even we read 3 extra bytes for the first line,
        // we won't alter existing behavior (no backwards incompatible issue).
        // Because the newSize is less than maxLineLength and
        // the number of bytes copied to Text is always no more than newSize.
        // If the return size from readLine is not less than maxLineLength,
        // we will discard the current line and read the next line.
        pos += newSize;
        int textLength = value.getLength();
        byte[] textBytes = value.getBytes();
        if ((textLength >= 3) && (textBytes[0] == (byte) 0xEF) &&
                (textBytes[1] == (byte) 0xBB) && (textBytes[2] == (byte) 0xBF)) {
            // find UTF-8 BOM, strip it.
            LOG.info("Found UTF-8 BOM and skipped it");
            textLength -= 3;
            newSize -= 3;
            if (textLength > 0) {
                // It may work to use the same buffer and not do the copyBytes
                textBytes = value.copyBytes();
                value.set(textBytes, 3, textLength);
            }
            else {
                value.clear();
            }
        }
        return newSize;
    }

    public boolean next(Text value)
            throws IOException
    {
        // We always read one extra line, which lies outside the upper
        // split limit i.e. (end - 1)
        while (lineDataSource.getFilePosition() <= lineDataSource.getFileEnd() || in.needAdditionalRecordAfterSplit()) {
            int newSize;
            if (pos == 0) {
                newSize = skipUtfByteOrderMark(value);
            }
            else {
                newSize = in.readLine(value, maxLineLength);
                pos += newSize;
            }

            if (newSize == 0) {
                return false;
            }
            if (newSize < maxLineLength) {
                return true;
            }

            // line too long. try again
            LOG.info("Skipped line of size " + newSize + " at pos " + (pos - newSize));
        }

        return false;
    }

    @Override
    public void close()
            throws IOException
    {
        closer.close();
    }

    private static byte[] getRecordDelimiter(Configuration job)
    {
        String delimiter = job.get("textinputformat.record.delimiter");
        if (delimiter == null) {
            return null;
        }
        return delimiter.getBytes(StandardCharsets.UTF_8);
    }
}

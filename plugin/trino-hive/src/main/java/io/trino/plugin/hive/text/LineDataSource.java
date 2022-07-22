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
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.Seekable;
import org.apache.hadoop.io.compress.CodecPool;
import org.apache.hadoop.io.compress.CompressionCodec;
import org.apache.hadoop.io.compress.CompressionCodecFactory;
import org.apache.hadoop.io.compress.Decompressor;
import org.apache.hadoop.mapred.FileSplit;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;

import static java.util.Objects.requireNonNull;

// Note: this code was forked from Apache Hadoop
public class LineDataSource
        implements Closeable
{
    private final long fileStart;
    private final long fileEnd;
    private final boolean compressed;

    private final InputStream inputStream;
    private final Seekable filePosition;
    private final Closer closer = Closer.create();

    public LineDataSource(Configuration job, FileSplit split)
            throws IOException
    {
        requireNonNull(job, "job is null");
        requireNonNull(split, "split is null");

        fileStart = split.getStart();
        fileEnd = fileStart + split.getLength();

        Path file = split.getPath();
        CompressionCodecFactory compressionCodecs = new CompressionCodecFactory(job);
        CompressionCodec codec = compressionCodecs.getCodec(file);
        compressed = (codec != null);

        // open the file and seek to the start of the split
        FSDataInputStream in = file.getFileSystem(job).open(file);
        try {
            this.filePosition = in;
            if (compressed) {
                if (fileStart != 0) {
                    // So we have a split that is part of a file stored using
                    // a Compression codec that cannot be split.
                    throw new IOException("Cannot seek in " + codec.getClass().getSimpleName() + " compressed stream");
                }
                Decompressor decompressor = CodecPool.getDecompressor(codec);
                closer.register(() -> CodecPool.returnDecompressor(decompressor));
                this.inputStream = codec.createInputStream(in, decompressor);
            }
            else {
                in.seek(fileStart);
                this.inputStream = in;
            }
            closer.register(inputStream);
        }
        catch (Throwable e) {
            closer.register(in);
            throw closeAndRethrow(e, closer);
        }
    }

    public boolean isCompressed()
    {
        return compressed;
    }

    public InputStream getInputStream()
    {
        return inputStream;
    }

    public long getFileStart()
    {
        return fileStart;
    }

    public long getFileEnd()
    {
        return fileEnd;
    }

    public long getFilePosition()
            throws IOException
    {
        return filePosition.getPos();
    }

    @Override
    public void close()
            throws IOException
    {
        closer.close();
    }

    static RuntimeException closeAndRethrow(Throwable e, Closer closer)
            throws IOException
    {
        try (closer) {
            throw closer.rethrow(e);
        }
    }
}

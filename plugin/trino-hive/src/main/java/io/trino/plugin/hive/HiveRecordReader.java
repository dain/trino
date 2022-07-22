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

import java.io.Closeable;
import java.io.IOException;
import java.util.Optional;

public interface HiveRecordReader
        extends Closeable
{
    /**
     * The type of the writable that will be returned from this reader.
     */
    Class<? extends Writable> getType();

    /**
     * The bytes that have been read so far.
     */
    long getCompletedBytes();

    /**
     * Gets the next row if there are more rows. If there are no more rows empty
     * is returned. The writable returned from this method will be reused by
     * subsequent reads, so data must be copied out of the writable before
     * calling next again.
     */
    Optional<Writable> next()
            throws IOException;
}

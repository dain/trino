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
package com.facebook.presto.operator.ptf.spacesaving;

import static com.google.common.base.MoreObjects.toStringHelper;
import static java.util.Objects.requireNonNull;

class Counter<T>
{
    private T item;
    private long count;
    private long error;

    private Bucket<T> bucket;

    Counter(Bucket<T> bucket)
    {
        this.bucket = requireNonNull(bucket, "bucket is null");
    }

    public long getCount()
    {
        return count;
    }

    public void increment()
    {
        count++;
    }

    public long getError()
    {
        return error;
    }

    public void setError(long error)
    {
        this.error = error;
    }

    public Bucket<T> getBucket()
    {
        return bucket;
    }

    public void setBucket(Bucket<T> bucket)
    {
        this.bucket = bucket;
    }

    public T getItem()
    {
        return item;
    }

    public void setItem(T item)
    {
        this.item = item;
    }

    @Override
    public String toString()
    {
        return toStringHelper(this)
                .add("item", item)
                .add("count", count)
                .add("error", error)
                .toString();
    }
}

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
package io.trino.spi.connector;

import io.trino.spi.Page;
import io.trino.spi.block.Block;

import java.util.function.ObjLongConsumer;

import static java.util.Objects.requireNonNull;

final class StaticSourcePage
        implements SourcePage
{
    private Page page;

    StaticSourcePage(Page page)
    {
        requireNonNull(page, "page is null");
        this.page = page;
    }

    @Override
    public int getPositionCount()
    {
        if (page == null) {
            throw new IllegalStateException("page is destroyed");
        }
        return page.getPositionCount();
    }

    @Override
    public long getSizeInBytes()
    {
        if (page == null) {
            return 0;
        }
        return page.getSizeInBytes();
    }

    @Override
    public long getRetainedSizeInBytes()
    {
        if (page == null) {
            return 0;
        }
        return page.getRetainedSizeInBytes();
    }

    @Override
    public void retainedBytesForEachPart(ObjLongConsumer<Object> consumer)
    {
        if (page == null) {
            return;
        }
        for (int i = 0; i < page.getChannelCount(); i++) {
            page.getBlock(i).retainedBytesForEachPart(consumer);
        }
    }

    @Override
    public int getChannelCount()
    {
        if (page == null) {
            throw new IllegalStateException("page is destroyed");
        }
        return page.getChannelCount();
    }

    @Override
    public Block getBlock(int channel)
    {
        if (page == null) {
            throw new IllegalStateException("page is destroyed");
        }
        return page.getBlock(channel);
    }

    @Override
    public Page getPage()
    {
        if (page == null) {
            throw new IllegalStateException("page is destroyed");
        }
        return page;
    }

    @Override
    public Page getColumns(int[] channels)
    {
        if (page == null) {
            throw new IllegalStateException("page is destroyed");
        }
        return page.getColumns(channels);
    }

    @Override
    public void selectPositions(int[] positions, int offset, int size)
    {
        if (page == null) {
            throw new IllegalStateException("page is destroyed");
        }
        page = page.getPositions(positions, offset, size);
    }

    @Override
    public void destroy()
    {
        page = null;
    }

    @Override
    public boolean isDestroyed()
    {
        return page == null;
    }
}

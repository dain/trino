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

/**
 * A page of data from a connector.
 * <p>
 * A page has a fixed number of positions and a fixed set of channels.
 * <p>
 * This interface is not thread safe.
 */
public interface SourcePage
{
    /**
     * Creates a new SourcePage from the specified block.
     */
    static SourcePage create(int positionCount)
    {
        return new NoChannelsSourcePage(positionCount);
    }

    /**
     * Creates a new SourcePage from the specified block.
     */
    static SourcePage create(Block block)
    {
        return new StaticSourcePage(new Page(block.getPositionCount(), block));
    }

    /**
     * Creates a new SourcePage from the specified page.
     */
    static SourcePage create(Page page)
    {
        return new StaticSourcePage(page);
    }

    /**
     * Gets the number of positions in the page.
     */
    int getPositionCount();

    /**
     * Gets the current loaded size of the page in bytes.
     * <p>
     * This method will return 0 if the page has been destroyed.
     */
    long getSizeInBytes();

    /**
     * Gets the current retained size of the page in bytes.
     * <p>
     * This method is callable even if the page has been destroyed.
     */
    long getRetainedSizeInBytes();

    /**
     * Calls retainedBytesForEachPart on all loaded blocks;
     * <p>
     * If the page has been destroyed, this method is a no-op.
     */
    void retainedBytesForEachPart(ObjLongConsumer<Object> consumer);

    /**
     * Gets the number of channels in the page.
     */
    int getChannelCount();

    /**
     * Gets the block for the specified channel.
     */
    Block getBlock(int channel);

    /**
     * Gets all data.
     */
    Page getPage();

    /**
     * Gets a projection of the page containing only the specified channels.
     */
    default Page getColumns(int[] channels)
    {
        Block[] blocks = new Block[channels.length];
        for (int i = 0; i < channels.length; i++) {
            blocks[i] = getBlock(channels[i]);
        }
        return new Page(getPositionCount(), blocks);
    }

    /**
     * Modify this page to mask data internally. After this method is called
     * this page and all returned blocks and pages will have the specified size.
     * <p>
     * This method should be preferred to {@link Block#getPositions(int[], int, int)}
     * and {@link Page#getPositions(int[], int, int)} where possible, as this allows
     * the underlying reader to filter positions on subsequent reads.
     */
    void selectPositions(int[] positions, int offset, int size);

    /**
     * Destroy this page and release any resources it holds.
     * <p>
     * This method should be called when the page is no longer needed.
     * Once this method is called, the page should not be used, and all
     * method calls on the page should throw an exception, unless noted otherwise.
     * This method is idempotent and can be called multiple times.
     */
    void destroy();

    /**
     * Returns true if this page has been destroyed.
     */
    boolean isDestroyed();
}

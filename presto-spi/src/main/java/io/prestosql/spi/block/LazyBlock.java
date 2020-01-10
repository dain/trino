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
package io.prestosql.spi.block;

import io.airlift.slice.Slice;
import org.openjdk.jol.info.ClassLayout;

import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static io.prestosql.spi.block.BlockUtil.checkArrayRange;
import static io.prestosql.spi.block.BlockUtil.checkValidRegion;
import static java.lang.Integer.max;
import static java.lang.Math.min;
import static java.lang.String.format;
import static java.util.Collections.singletonList;
import static java.util.Objects.requireNonNull;

@NotThreadSafe
public class LazyBlock
        implements Block
{
    private static final int INSTANCE_SIZE = ClassLayout.parseClass(LazyBlock.class).instanceSize() + ClassLayout.parseClass(LazyLoadListeners.class).instanceSize();

    @Nullable
    private final int[] positions;
    private final int offset;
    private final int size;

    @Nullable
    private PartialLazyBlockLoader loader;
    @Nullable
    private Block block;

    private final LazyLoadListeners listeners;

    public LazyBlock(int positionCount, LazyBlockLoader loader)
    {
        this(positionCount, new PartialLazyBlockLoaderAdapter(positionCount, loader));
    }

    public LazyBlock(int positionCount, PartialLazyBlockLoader loader)
    {
        this(null, 0, positionCount, loader, new LazyLoadListeners());
    }

    private LazyBlock(@Nullable int[] positions, int offset, int size, PartialLazyBlockLoader loader, LazyLoadListeners listeners)
    {
        this.positions = positions;
        this.offset = offset;
        this.size = size;
        this.loader = requireNonNull(loader, "loader is null");
        this.listeners = requireNonNull(listeners, "listeners is null");
    }

    @Override
    public int getPositionCount()
    {
        return size;
    }

    @Override
    public int getSliceLength(int position)
    {
        return getBlock().getSliceLength(position);
    }

    @Override
    public byte getByte(int position, int offset)
    {
        return getBlock().getByte(position, offset);
    }

    @Override
    public short getShort(int position, int offset)
    {
        return getBlock().getShort(position, offset);
    }

    @Override
    public int getInt(int position, int offset)
    {
        return getBlock().getInt(position, offset);
    }

    @Override
    public long getLong(int position, int offset)
    {
        return getBlock().getLong(position, offset);
    }

    @Override
    public Slice getSlice(int position, int offset, int length)
    {
        return getBlock().getSlice(position, offset, length);
    }

    @Override
    public <T> T getObject(int position, Class<T> clazz)
    {
        return getBlock().getObject(position, clazz);
    }

    @Override
    public boolean bytesEqual(int position, int offset, Slice otherSlice, int otherOffset, int length)
    {
        return getBlock().bytesEqual(position, offset, otherSlice, otherOffset, length);
    }

    @Override
    public int bytesCompare(int position, int offset, int length, Slice otherSlice, int otherOffset, int otherLength)
    {
        return getBlock().bytesCompare(
                position,
                offset,
                length,
                otherSlice,
                otherOffset,
                otherLength);
    }

    @Override
    public void writeBytesTo(int position, int offset, int length, BlockBuilder blockBuilder)
    {
        getBlock().writeBytesTo(position, offset, length, blockBuilder);
    }

    @Override
    public void writePositionTo(int position, BlockBuilder blockBuilder)
    {
        getBlock().writePositionTo(position, blockBuilder);
    }

    @Override
    public boolean equals(int position, int offset, Block otherBlock, int otherPosition, int otherOffset, int length)
    {
        return getBlock().equals(
                position,
                offset,
                otherBlock,
                otherPosition,
                otherOffset,
                length);
    }

    @Override
    public long hash(int position, int offset, int length)
    {
        return getBlock().hash(position, offset, length);
    }

    @Override
    public int compareTo(int leftPosition, int leftOffset, int leftLength, Block rightBlock, int rightPosition, int rightOffset, int rightLength)
    {
        return getBlock().compareTo(
                leftPosition,
                leftOffset,
                leftLength,
                rightBlock,
                rightPosition,
                rightOffset,
                rightLength);
    }

    @Override
    public Block getSingleValueBlock(int position)
    {
        return getBlock().getSingleValueBlock(position);
    }

    @Override
    public long getSizeInBytes()
    {
        if (!isLoaded()) {
            return 0;
        }
        return getBlock().getSizeInBytes();
    }

    @Override
    public long getRegionSizeInBytes(int position, int length)
    {
        if (!isLoaded()) {
            return 0;
        }
        return getBlock().getRegionSizeInBytes(position, length);
    }

    @Override
    public long getPositionsSizeInBytes(boolean[] positions)
    {
        if (!isLoaded()) {
            return 0;
        }
        return getBlock().getPositionsSizeInBytes(positions);
    }

    @Override
    public long getRetainedSizeInBytes()
    {
        if (!isLoaded()) {
            return INSTANCE_SIZE;
        }
        return INSTANCE_SIZE + getBlock().getRetainedSizeInBytes();
    }

    @Override
    public long getEstimatedDataSizeForStats(int position)
    {
        return getBlock().getEstimatedDataSizeForStats(position);
    }

    @Override
    public void retainedBytesForEachPart(BiConsumer<Object, Long> consumer)
    {
        getBlock().retainedBytesForEachPart(consumer);
        consumer.accept(this, (long) INSTANCE_SIZE);
    }

    @Override
    public String getEncodingName()
    {
        return LazyBlockEncoding.NAME;
    }

    @Override
    public Block getPositions(int[] selectedPositions, int offset, int length)
    {
        if (isLoaded()) {
            return getBlock().getPositions(selectedPositions, offset, length);
        }
        checkArrayRange(selectedPositions, offset, length);

        int[] newPositions = new int[length];
        if (positions != null) {
            for (int i = 0; i < length; i++) {
                newPositions[i] = positions[selectedPositions[offset + i]] + this.offset;
            }
        }
        else {
            for (int i = 0; i < length; i++) {
                newPositions[i] = selectedPositions[offset + i] + this.offset;
            }
        }
        for (int newPosition : newPositions) {
            if (newPosition < 0 || newPosition >= size) {
                throw new IllegalArgumentException("position is not valid");
            }
        }
        return new LazyBlock(newPositions, 0, newPositions.length, loader, listeners);
    }

    @Override
    public Block copyPositions(int[] positions, int offset, int length)
    {
        return getBlock().copyPositions(positions, offset, length);
    }

    @Override
    public Block getRegion(int positionOffset, int length)
    {
        if (isLoaded()) {
            return getBlock().getRegion(positionOffset, length);
        }
        checkValidRegion(getPositionCount(), positionOffset, length);
        return new LazyBlock(positions, offset + positionOffset, length, loader, listeners);
    }

    @Override
    public Block copyRegion(int position, int length)
    {
        return getBlock().copyRegion(position, length);
    }

    @Override
    public boolean isNull(int position)
    {
        return getBlock().isNull(position);
    }

    @Override
    public final List<Block> getChildren()
    {
        return singletonList(getBlock());
    }

    public Block getBlock()
    {
        load(false);
        return block;
    }

    @Override
    public boolean isLoaded()
    {
        return block != null && block.isLoaded();
    }

    @Override
    public Block getLoadedBlock()
    {
        load(true);
        return block;
    }

    public static void listenForLoads(Block block, Consumer<Block> listener)
    {
        requireNonNull(block, "block is null");
        requireNonNull(listener, "listener is null");

        LazyLoadListeners.addListenersRecursive(block, singletonList(listener));
    }

    private void load(boolean recursive)
    {
        if (loader == null) {
            return;
        }

        if (positions == null) {
            block = requireNonNull(loader.load(offset, size), "loader returned null");
        }
        else {
            block = requireNonNull(loader.load(positions, offset, size), "loader returned null");
        }
        if (block.getPositionCount() != size) {
            throw new IllegalStateException(format("Loaded block positions count (%s) doesn't match lazy block positions count (%s)", block.getPositionCount(), size));
        }

        if (recursive) {
            block = block.getLoadedBlock();
        }
        else {
            // load and remove directly nested lazy blocks
            while (block instanceof LazyBlock) {
                block = ((LazyBlock) block).getBlock();
            }
        }

        // clear reference to loader to free resources, since load was successful
        loader = null;

        // notify listeners
        listeners.blockLoaded(block, recursive);
    }

    private static class LazyLoadListeners
    {
        @Nullable
        private List<Consumer<Block>> listeners;
        private boolean hasFired;

        private void addListeners(List<Consumer<Block>> listeners)
        {
            if (hasFired) {
                throw new IllegalStateException("Block is already loaded");
            }
            if (this.listeners == null) {
                this.listeners = new ArrayList<>();
            }
            this.listeners.addAll(listeners);
        }

        private void blockLoaded(Block block, boolean recursive)
        {
            // notify listeners
            List<Consumer<Block>> listeners = this.listeners;
            this.listeners = null;
            if (listeners != null) {
                hasFired = true;
                listeners.forEach(listener -> listener.accept(block));

                // add listeners to unloaded child blocks
                if (!recursive) {
                    addListenersRecursive(block, listeners);
                }
            }
        }

        /**
         * If block is unloaded, add the listeners; otherwise call this method on child blocks
         */
        @SuppressWarnings("AccessingNonPublicFieldOfAnotherObject")
        private static void addListenersRecursive(Block block, List<Consumer<Block>> listeners)
        {
            if (block instanceof LazyBlock && !block.isLoaded()) {
                LazyLoadListeners lazyData = ((LazyBlock) block).listeners;
                lazyData.addListeners(listeners);
                return;
            }

            for (Block child : block.getChildren()) {
                addListenersRecursive(child, listeners);
            }
        }
    }

    private static class PartialLazyBlockLoaderAdapter
            implements PartialLazyBlockLoader
    {
        private final int positionsCount;
        @Nullable
        private LazyBlockLoader loader;
        @Nullable
        private Block block;

        private int currentPosition;

        public PartialLazyBlockLoaderAdapter(int positionsCount, LazyBlockLoader loader)
        {
            this.positionsCount = positionsCount;
            this.loader = requireNonNull(loader, "loader is null");
        }

        @Override
        public Block load(int positionOffset, int length)
        {
            if (currentPosition > positionOffset) {
                throw new IllegalStateException(format("LazyBlock already advanced beyond position %s", positionOffset));
            }

            Block blockRegion = getLoadedBlock().getRegion(positionOffset, length);
            currentPosition = positionOffset + length;
            return blockRegion;
        }

        @Override
        public Block load(int[] positions, int offset, int length)
        {
            if (length == 0) {
                return getLoadedBlock().getPositions(positions, offset, length);
            }

            int min = Integer.MAX_VALUE;
            int max = 0;
            for (int i = 0; i < length; i++) {
                int position = positions[i + offset];
                min = min(min, position);
                max = max(max, position);
            }
            if (currentPosition > min) {
                throw new IllegalStateException(format("LazyBlock already advanced beyond position %s", min));
            }

            Block blockPositions = getLoadedBlock().getPositions(positions, offset, length);
            currentPosition = max;
            return blockPositions;
        }

        private Block getLoadedBlock()
        {
            if (loader == null) {
                return block;
            }

            block = requireNonNull(loader.load(), "loader returned null");
            if (block.getPositionCount() != positionsCount) {
                throw new IllegalStateException(format("Loaded block positions count (%s) doesn't match lazy block positions count (%s)", block.getPositionCount(), positionsCount));
            }
            loader = null;
            return block;
        }
    }
}

package org.example;

import java.nio.ByteBuffer;

class ArenaAllocator {
    private ByteBuffer buffer;
    private int lastOffset = 0;

    public ArenaAllocator(int size) {
        buffer = ByteBuffer.allocate(size);
    }

    public int allocate(int size) {
        if (buffer.remaining() < size) {
            throw new OutOfMemoryError("Arena out of memory");
        }
        int currentOffset = lastOffset;
        buffer.position(currentOffset + size);
        return currentOffset;
    }

    public ByteBuffer getBuffer() {
        return buffer;
    }
}
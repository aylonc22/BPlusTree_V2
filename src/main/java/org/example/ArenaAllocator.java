package org.example;

import java.nio.ByteBuffer;

class ArenaAllocator {
    private ByteBuffer buffer;

    public ArenaAllocator(int size) {
        buffer = ByteBuffer.allocate(size);
    }

    public int allocate(int size) {
        if (buffer.remaining() < size) {
            throw new OutOfMemoryError("Arena out of memory");
        }
        int currentOffset = buffer.position();
        buffer.position(currentOffset + size);
        return currentOffset;
    }

    public ByteBuffer getBuffer() {
        return buffer;
    }
}
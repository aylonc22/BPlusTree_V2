package org.example;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

class BPlusTreeNode {
    private static final int INT_SIZE = Integer.BYTES;
    private static final int BOOLEAN_SIZE = Byte.BYTES;

    private int order;
    private ArenaAllocator allocator;
    private int offset;

    // Constructor for new nodes
    public BPlusTreeNode(int order, ArenaAllocator allocator, boolean isLeaf) {
        this.order = order;
        this.allocator = allocator;
        this.offset = allocator.allocate(nodeSize(isLeaf));
        initializeNode(isLeaf);
    }

    // Constructor for existing nodes
    public BPlusTreeNode(int order, ArenaAllocator allocator, int offset) {
        this.order = order;
        this.allocator = allocator;
        this.offset = offset;
    }

    private int nodeSize(boolean isLeaf) {
        if (isLeaf) {
            return INT_SIZE + BOOLEAN_SIZE + INT_SIZE * (order - 1) + INT_SIZE * (order - 1) + 100 * (order - 1);
        } else {
            return INT_SIZE + BOOLEAN_SIZE + INT_SIZE * order + INT_SIZE * (order + 1);
        }
    }

    private void initializeNode(boolean isLeaf) {
        ByteBuffer buffer = allocator.getBuffer();
        buffer.putInt(offset, 0); // Key count
        buffer.put(offset + INT_SIZE, (byte) (isLeaf ? 1 : 0)); // Is leaf
    }

    public int getKeyCount() {
        return allocator.getBuffer().getInt(offset);
    }

    public boolean isLeaf() {
        return allocator.getBuffer().get(offset + INT_SIZE) == 1;
    }

    public int getKey(int index) {
        return allocator.getBuffer().getInt(offset + INT_SIZE * (index + 2));
    }

    public void setKey(int index, int key) {
        allocator.getBuffer().putInt(offset + INT_SIZE * (index + 2), key);
    }

    public int getChild(int index) {
        return allocator.getBuffer().getInt(offset + INT_SIZE * (order + 1) + INT_SIZE * index);
    }

    public void setChild(int index, int childOffset) {
        allocator.getBuffer().putInt(offset + INT_SIZE * (order + 1) + INT_SIZE * index, childOffset);
    }

    // Only leaf nodes have values
    public int getValue(int index) {
        if (!isLeaf()) {
            throw new UnsupportedOperationException("Values can only be retrieved from leaf nodes.");
        }
        ByteBuffer buffer = allocator.getBuffer();
        int start = offset + INT_SIZE * (order + 1) + INT_SIZE * index;
        return buffer.getInt(start); // Retrieve the integer directly
    }

    public void setValue(int index, int value) {
        if (!isLeaf()) {
            throw new UnsupportedOperationException("Values can only be set for leaf nodes.");
        }
        ByteBuffer buffer = allocator.getBuffer();
        int start = offset + INT_SIZE * (order + 1) + INT_SIZE * index;
        buffer.putInt(start, value); // Store the integer directly
    }

    public int getOffset() {
        return offset;
    }

    public void incrementKeyCount() {
        allocator.getBuffer().putInt(offset, getKeyCount() + 1);
    }

    public void decrementKeyCount() {
        allocator.getBuffer().putInt(offset, getKeyCount() - 1);
    }

    public void printNode(int level) {
        System.out.print("Level " + level + ": ");
        for (int i = 0; i < getKeyCount(); i++) {
            System.out.print(getKey(i));
            if (isLeaf()) {
                System.out.print("(" + getValue(i) + ")");
            }
            if (i < getKeyCount() - 1) {
                System.out.print(", ");
            }
        }
        System.out.println();
    }
}

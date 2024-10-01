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
    public BPlusTreeNode(int order, ArenaAllocator allocator) {
        this.order = order;
        this.allocator = allocator;
        this.offset = allocator.allocate(nodeSize());
        initializeNode();
    }

    // Constructor for existing nodes
    public BPlusTreeNode(int order, ArenaAllocator allocator, int offset) {
        this.order = order;
        this.allocator = allocator;
        this.offset = offset;
    }

    private int nodeSize() {
        return INT_SIZE + BOOLEAN_SIZE + INT_SIZE * (order - 1) + INT_SIZE * order + 10 * (order - 1);
    }

    private void initializeNode() {
        ByteBuffer buffer = allocator.getBuffer();
        buffer.putInt(offset, 0); // Key count
        buffer.put(offset + INT_SIZE, (byte) 1); // Is leaf
    }

    public int getKeyCount() {
        return allocator.getBuffer().getInt(offset);
    }

    public boolean isLeaf() {
        return allocator.getBuffer().get(offset + INT_SIZE) == 1;
    }

    public void setLeaf(boolean leaf) {
        allocator.getBuffer().put(offset + INT_SIZE, (byte) (leaf ? 1 : 0));
    }

    public int getKey(int index) {
        return allocator.getBuffer().getInt(offset + INT_SIZE * (index + 1));
    }

    public void setKey(int index, int key) {
        allocator.getBuffer().putInt(offset + INT_SIZE * (index + 1), key);
    }

    public int getChild(int index) {
        return allocator.getBuffer().getInt(offset + INT_SIZE * (order - 1 + index));
    }

    public void setChild(int index, int childOffset) {
        allocator.getBuffer().putInt(offset + INT_SIZE * (order - 1 + index), childOffset);
    }

    public String getValue(int index) {
        ByteBuffer buffer = allocator.getBuffer();
        int start = offset + INT_SIZE * (order - 1) + INT_SIZE * index;
        int length = buffer.getInt(start);
        byte[] valueBytes = new byte[length];
        buffer.position(start + INT_SIZE);
        buffer.get(valueBytes);
        return new String(valueBytes, StandardCharsets.UTF_8);
    }

    public void setValue(int index, String value) {
        ByteBuffer buffer = allocator.getBuffer();
        int start = offset + INT_SIZE * (order - 1) + INT_SIZE * index;
        byte[] valueBytes = value.getBytes(StandardCharsets.UTF_8);
        buffer.putInt(start, valueBytes.length);
        buffer.position(start + INT_SIZE);
        buffer.put(valueBytes);
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
            System.out.print(getKey(i) + "(" + getValue(i) + ") ");
        }
        System.out.println();
    }
}

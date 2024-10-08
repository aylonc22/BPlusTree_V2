package org.example;

import java.nio.ByteBuffer;

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
            // Leaf node: Key Count + Is Leaf + (order - 1) Keys + (order - 1) Values + Parent Offset
            return INT_SIZE + BOOLEAN_SIZE + INT_SIZE * (order - 1) + INT_SIZE * (order - 1) + INT_SIZE;
        } else {
            // Internal node: Key Count + Is Leaf + order Keys + (order + 1) Children + Parent Offset
            return INT_SIZE + BOOLEAN_SIZE + INT_SIZE * order + INT_SIZE * (order + 1) + INT_SIZE;
        }
    }

    private void initializeNode(boolean isLeaf) {
        ByteBuffer buffer = allocator.getBuffer();
        buffer.putInt(offset, 0); // Key count
        buffer.put(offset + INT_SIZE, (byte) (isLeaf ? 1 : 0)); // Is leaf
        setParentOffset(-1);
    }

    public int getKeyCount() {
        return allocator.getBuffer().getInt(offset);
    }

    public boolean isLeaf() {
        return allocator.getBuffer().get(offset + INT_SIZE) == 1;
    }

    public int getKey(int index) {
        return allocator.getBuffer().getInt(offset + INT_SIZE * (index + 2) + BOOLEAN_SIZE);
    }

    public void setKey(int index, int key) {
        allocator.getBuffer().putInt(offset + INT_SIZE * (index + 2) + BOOLEAN_SIZE, key);
    }

    public int getChild(int index) {
        return allocator.getBuffer().getInt(offset + INT_SIZE * (order + 1) + BOOLEAN_SIZE + INT_SIZE * index);
    }

    public void setChild(int index, int childOffset) {
        allocator.getBuffer().putInt(offset + INT_SIZE * (order + 1) + BOOLEAN_SIZE + INT_SIZE * index, childOffset);
    }

    // Only leaf nodes have values
    public int getValue(int index) {
        if (!isLeaf()) {
            throw new UnsupportedOperationException("Values can only be retrieved from leaf nodes.");
        }
        ByteBuffer buffer = allocator.getBuffer();
        int start = offset + INT_SIZE * order + BOOLEAN_SIZE + INT_SIZE * index;
        return buffer.getInt(start); // Retrieve the integer directly
    }

    public void setValue(int index, int value) {
        if (!isLeaf()) {
            throw new UnsupportedOperationException("Values can only be set for leaf nodes.");
        }
        ByteBuffer buffer = allocator.getBuffer();
        int start = offset + INT_SIZE * order + BOOLEAN_SIZE + INT_SIZE * index;
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

    public int getParentOffset() {
        int start = offset + nodeSize(isLeaf()) - INT_SIZE;
        return allocator.getBuffer().getInt(start);
    }

    public void setParentOffset(int parentOffset) {
        allocator.getBuffer().putInt(offset + nodeSize(isLeaf()) - INT_SIZE, parentOffset);
    }

    public int getParentIndex() {
        int parentOffset = getParentOffset();
        if (parentOffset == -1) {
            return -1; // No parent
        }

        BPlusTreeNode parentNode = new BPlusTreeNode(order, allocator, parentOffset);
        for (int i = 0; i <= parentNode.getKeyCount(); i++) {
            if (parentNode.getChild(i) == this.offset) {
                return i; // Found the index of this child in its parent
            }
        }
        return -1; // Should not happen if the node is correctly linked
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
    public void printNodeContents() {
        ByteBuffer buffer = allocator.getBuffer();
        System.out.println("Node contents at offset " + offset + ":");

        // Print Key Count
        System.out.printf("Key Count: %d%n", buffer.getInt(offset));

        // Print Is Leaf
        System.out.printf("Is Leaf: %d%n", buffer.get(offset + INT_SIZE));

        int keyCount = getKeyCount();
        int currentPosition = offset + INT_SIZE + BOOLEAN_SIZE; // Start after key count and isLeaf

        // Print Keys
        System.out.println("Keys:");
        for (int i = 0; i < keyCount; i++) {
            System.out.printf("Key[%d]: %d%n", i, buffer.getInt(currentPosition));
            currentPosition += INT_SIZE; // Move to next key
        }

        // Print Values if it's a leaf node
        if (isLeaf()) {
            System.out.println("Values:");
            for (int i = 0; i < keyCount; i++) {
                System.out.printf("Value[%d]: %d%n", i, buffer.getInt(currentPosition));
                currentPosition += INT_SIZE; // Move to next value
            }
        }

        // Print Children (if it's not a leaf node)
        if (!isLeaf()) {
            System.out.println("Children:");
            for (int i = 0; i <= keyCount; i++) { // Include one extra for the children
                System.out.printf("Child[%d]: %d%n", i, buffer.getInt(currentPosition));
                currentPosition += INT_SIZE; // Move to next child
            }
        }

        // Print Parent Offset
        System.out.printf("Parent Offset: %d%n", buffer.getInt(currentPosition));
    }
}
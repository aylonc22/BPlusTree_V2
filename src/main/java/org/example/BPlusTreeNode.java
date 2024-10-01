package org.example;

import java.nio.ByteBuffer;

public class BPlusTreeNode {
    private static final int INT_SIZE = Integer.BYTES;
    private static final int BOOLEAN_SIZE = Byte.BYTES;

    private int order;
    private ArenaAllocator allocator;
    private int offset;

    public BPlusTreeNode(int order, ArenaAllocator allocator) {
        this.order = order;
        this.allocator = allocator;
        this.offset = allocator.allocate(nodeSize());
        initializeNode();
    }

    private int nodeSize() {
        return INT_SIZE + INT_SIZE * (order - 1) + INT_SIZE * order + BOOLEAN_SIZE; // Keys + children + isLeaf
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

    // Leaf Node Specific Methods
    public int getValue(int index) {
        return allocator.getBuffer().getInt(offset + INT_SIZE * (order - 1) + INT_SIZE * index);
    }

    public void setValue(int index, int value) {
        allocator.getBuffer().putInt(offset + INT_SIZE * (order - 1) + INT_SIZE * index, value);
    }

    public void insertNonFull(int key, int value) {
        ByteBuffer buffer = allocator.getBuffer();
        int count = getKeyCount();
        int i = count - 1;

        if (isLeaf()) {
            while (i >= 0 && getKey(i) > key) {
                setKey(i + 1, getKey(i));
                setValue(i + 1, getValue(i));
                i--;
            }
            setKey(i + 1, key);
            setValue(i + 1, value);
            buffer.putInt(offset, count + 1);
        } else {
            while (i >= 0 && getKey(i) > key) {
                i--;
            }
            i++;
            int finalI = i;
            BPlusTreeNode child = new BPlusTreeNode(order, allocator) {
                {
                    offset = getChild(finalI);
                }
            };
            if (child.getKeyCount() == order - 1) {
                splitChild(i);
                if (getKey(i) < key) {
                    i++;
                }
            }
            int finalI1 = i;
            new BPlusTreeNode(order, allocator) {
                {
                    offset = getChild(finalI1);
                }
            }.insertNonFull(key, value);
        }
    }

    private void splitChild(int index) {
        BPlusTreeNode child = new BPlusTreeNode(order, allocator) {
            {
                offset = getChild(index);
            }
        };

        BPlusTreeNode newNode = new BPlusTreeNode(order, allocator);
        newNode.setLeaf(child.isLeaf());

        int splitIndex = (order - 1) / 2;
        for (int j = 0; j < splitIndex; j++) {
            newNode.setKey(j, child.getKey(j + splitIndex + 1));
            newNode.setValue(j, child.getValue(j + splitIndex + 1));
        }
        if (!child.isLeaf()) {
            for (int j = 0; j <= splitIndex; j++) {
                newNode.setChild(j, child.getChild(j + splitIndex + 1));
            }
        }

        child.setLeaf(false);
        for (int j = getKeyCount(); j > index; j--) {
            setChild(j + 1, getChild(j));
            setKey(j, getKey(j - 1));
            setValue(j, getValue(j - 1));
        }
        setChild(index + 1, newNode.offset);
        setKey(index, child.getKey(splitIndex));
        allocator.getBuffer().putInt(offset, getKeyCount() + 1);
    }

}

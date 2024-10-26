package org.example;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class BPlusTree {
    private final int order;
    private final ArenaAllocator allocator;
    private BPlusTreeNode root;

    public BPlusTree(int order, ArenaAllocator allocator) {
        this.order = order;
        this.allocator = allocator;
        this.root = new BPlusTreeNode(order, allocator, true); // Start with a single leaf node
    }

    public void insert(int key, int value) {
        BPlusTreeNode node = root;
        if (node.getKeyCount() == order - 1) {
            BPlusTreeNode newRoot = new BPlusTreeNode(order, allocator, false);
            newRoot.setChild(0, root.getOffset());
            splitChild(newRoot, 0);
            root.setParentOffset(newRoot.getOffset());
            root = newRoot;
            insertNonFull(root, key, value);
        } else {
            insertNonFull(node, key, value);
        }
    }

    private void insertNonFull(BPlusTreeNode node, int key, int value) {
        if (node.isLeaf()) {
            int i = node.getKeyCount() - 1;
            while (i >= 0 && key < node.getKey(i)) {
                node.setKey(i + 1, node.getKey(i));
                node.setValue(i + 1, node.getValue(i));
                i--;
            }
            node.setKey(i + 1, key);
            node.setValue(i + 1, value);
            node.incrementKeyCount();
        } else {
            int i = node.getKeyCount() - 1;
            while (i >= 0 && key < node.getKey(i)) {
                i--;
            }
            i++; // Find the child to insert into
            BPlusTreeNode child = new BPlusTreeNode(order, allocator, node.getChild(i));
            if (child.getKeyCount() == order - 1) {
                splitChild(node, i);
                // Determine which of the two children to insert into
                if (key > node.getKey(i)) {
                    i++;
                }
            }
            insertNonFull(new BPlusTreeNode(order, allocator, node.getChild(i)), key, value);
        }
    }

    private void splitChild(BPlusTreeNode parent, int index) {
        BPlusTreeNode child = new BPlusTreeNode(order, allocator, parent.getChild(index));
        BPlusTreeNode newChild = new BPlusTreeNode(order, allocator, true); // New leaf node

        int midIndex = order / 2; // For order 3, midIndex will be 1

        // Move the last key and value from child to newChild
        for (int j = midIndex; j < child.getKeyCount(); j++) {
            newChild.setKey(j - midIndex, child.getKey(j));
            newChild.setValue(j - midIndex, child.getValue(j));
        }

        // Update the key count for the children
        newChild.incrementKeyCount(child.getKeyCount() - midIndex);
        child.incrementKeyCount(-(child.getKeyCount() - midIndex));

        // Update parent
        for (int j = parent.getKeyCount(); j > index; j--) {
            parent.setKey(j, parent.getKey(j - 1));
            parent.setChild(j + 1, parent.getChild(j));
        }

        // Promote the middle key to the parent
        parent.setKey(index, child.getKey(midIndex - 1)); // Key to promote
        parent.setChild(index + 1, newChild.getOffset()); // Link to the new child
        parent.incrementKeyCount(); // Increase parent's key count

        // Update the parent offset in the new child
        newChild.setParentOffset(parent.getOffset());
    }
    public Integer search(int key) {
        return search(root, key);
    }

    private Integer search(BPlusTreeNode node, int key) {
        int i = 0;
        while (i < node.getKeyCount() && key > node.getKey(i)) {
            i++;
        }

        if (i < node.getKeyCount() && key == node.getKey(i)) {
            return node.isLeaf() ? node.getValue(i) : null; // Return value if leaf
        }

        if (node.isLeaf()) {
            return null; // Not found
        }

        return search(new BPlusTreeNode(order, allocator, node.getChild(i)), key);
    }

    // Print the B+ Tree
    public void printTree() {
        printNode(root, 0);
    }

    private void printNode(BPlusTreeNode node, int level) {
        System.out.print("Level " + level + ": ");
        for (int i = 0; i < node.getKeyCount(); i++) {
            System.out.print(node.getKey(i));
            if (node.isLeaf()) {
                System.out.print("(" + node.getValue(i) + ")");
            }
            if (i < node.getKeyCount() - 1) {
                System.out.print(", ");
            }
        }
        System.out.println();

        if (!node.isLeaf()) {
            for (int i = 0; i <= node.getKeyCount(); i++) {
                printNode(new BPlusTreeNode(order, allocator, node.getChild(i)), level + 1);
            }
        }
    }

    // Testing the BPlusTree implementation
    public static void main(String[] args) {
        ArenaAllocator allocator = new ArenaAllocator(1024); // Example size
        BPlusTree tree = new BPlusTree(3, allocator);

        // Insert key-value pairs
        System.out.println("Inserting key-value pairs:");
        tree.insert(10, 100);
        tree.insert(20, 200);
        tree.insert(5, 50);
        tree.insert(15, 150);
        tree.insert(30, 300);
        //tree.insert(25, 250); // Adding more to trigger splits

        // Print the tree structure
        System.out.println("B+ Tree structure:");
        tree.printTree();

        // Search for values
//        System.out.println("Search for key 10: " + (tree.search(10) != null ? "Found" : "Not Found"));
//        System.out.println("Search for key 20: " + (tree.search(20) != null ? "Found" : "Not Found"));
//        System.out.println("Search for key 15: " + (tree.search(15) != null ? "Found" : "Not Found"));
//        System.out.println("Search for key 25: " + (tree.search(25) != null ? "Found" : "Not Found"));
//        System.out.println("Search for key 40: " + (tree.search(40) != null ? "Found" : "Not Found"));
    }
}
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
            // Insert into leaf node
            int i = node.getKeyCount() - 1;
            while (i >= 0 && key < node.getKey(i)) {
                node.setKey(i + 1, node.getKey(i)); // Shift keys right
                node.setValue(i + 1, node.getValue(i)); // Shift values right
                i--;
            }
            node.setKey(i + 1, key);
            node.setValue(i + 1, value);
            node.incrementKeyCount(); // Update key count
        } else {
            // It's an internal node
            int i = node.getKeyCount() - 1;

            // Check if there's only one child by checking the right sibling
            if (node.getChild(i + 1) == -1) {
                // Directly insert into the single child
                int childOffset = node.getChild(i);
                insertNonFull(new BPlusTreeNode(order, allocator, childOffset), key, value);
                return; // Exit after handling insertion
            }

            // If there are multiple keys, find the correct child
            while (i >= 0 && key < node.getKey(i)) {
                i--;
            }
            i++; // Move to the child that should be traversed

            // Check if the child needs to be split
            int childOffset = node.getChild(i);
            BPlusTreeNode child = new BPlusTreeNode(order, allocator, childOffset);
            if (child.getKeyCount() == order - 1) {
                // Child is full, split it
                splitChild(node, i);
                // After splitting, check which child to go to
                if (key > node.getKey(i)) {
                    childOffset = node.getChild(i + 1); // Go to the right child
                }
            }

            // Recur to the appropriate child
            insertNonFull(new BPlusTreeNode(order, allocator, childOffset), key, value);
        }
    }

    private void splitChild(BPlusTreeNode parent, int index) {
        BPlusTreeNode child = new BPlusTreeNode(order, allocator, parent.getChild(index));
        BPlusTreeNode newChild;

        int midIndex = order / 2; // Midpoint for splitting

        if (child.isLeaf()) {
            // Splitting a leaf node
            newChild = new BPlusTreeNode(order, allocator, true); // Create a new leaf node

            // Move the last keys and values to the new child
            for (int j = midIndex; j < child.getKeyCount(); j++) {
                newChild.setKey(j - midIndex, child.getKey(j));
                newChild.setValue(j - midIndex, child.getValue(j));
            }

            // Update key counts
            newChild.incrementKeyCount(child.getKeyCount() - midIndex);
            child.incrementKeyCount(-(child.getKeyCount() - midIndex));

            // Adjust the parent node
            parent.setKey(index, child.getKey(midIndex - 1)); // Promote middle key to parent
            parent.setChild(index + 1, newChild.getOffset()); // Link new child
        } else {
            // Splitting an internal node
            newChild = new BPlusTreeNode(order, allocator, false); // Create a new internal node

            // Move keys to the new child
            for (int j = midIndex; j < child.getKeyCount(); j++) {
                newChild.setKey(j - midIndex, child.getKey(j));
            }

            // Move child pointers to the new child
            for (int j = midIndex + 1; j <= child.getKeyCount(); j++) {
                int childOffset = child.getChild(j);
                if (childOffset != -1) { // Only move valid child pointers
                    newChild.setChild(j - midIndex - 1, childOffset);
                }
            }

            // Update key counts
            newChild.incrementKeyCount(child.getKeyCount() - midIndex);
            child.incrementKeyCount(-(child.getKeyCount() - midIndex));
            if(!newChild.isLeaf()){
                child.printNodeContents();
                newChild.printNodeContents();
            }
            // Promote the middle key to the parent
            parent.setKey(index, child.getKey(midIndex - 1)); // Promote key
            parent.setChild(index + 1, newChild.getOffset()); // Link new child
        }

        // Shift parent keys and children to make room for the new key
        for (int j = parent.getKeyCount(); j > index; j--) {
            parent.setKey(j, parent.getKey(j - 1));
            parent.setChild(j + 1, parent.getChild(j));
        }

        // Update the parent offset in the new child
        newChild.setParentOffset(parent.getOffset());
        parent.incrementKeyCount(); // Increase parent's key count
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
            return node.isLeaf() ? node.getValue(i) : null; // Return value if it's a leaf node
        }

        // If it's a leaf node and key wasn't found
        if (node.isLeaf()) {
            return null; // Key not found
        }

        // Traverse to the child node
        int childOffset = node.getChild(i);
        if (childOffset != -1) { // Only proceed if the child exists
            BPlusTreeNode childNode = new BPlusTreeNode(order, allocator, childOffset);
            return search(childNode, key); // Recursively search in the child
        }

        return null; // Key not found
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
                int childOffset = node.getChild(i);
                if (childOffset != -1) { // Only print valid child nodes
                    printNode(new BPlusTreeNode(order, allocator, childOffset), level + 1);
                }
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
        tree.insert(25, 250); // Adding more to trigger splits

        // Print the tree structure
        System.out.println("B+ Tree structure:");
       tree.printTree();

        // Search for values
//       System.out.println("Search for key 10: " + (tree.search(10) != null ? "Found" : "Not Found"));
//        System.out.println("Search for key 20: " + (tree.search(20) != null ? "Found" : "Not Found"));
//        System.out.println("Search for key 15: " + (tree.search(15) != null ? "Found" : "Not Found"));
//        System.out.println("Search for key 25: " + (tree.search(25) != null ? "Found" : "Not Found"));
//        System.out.println("Search for key 40: " + (tree.search(40) != null ? "Found" : "Not Found"));
    }
}
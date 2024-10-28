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

            // Move the keys to the new child
            for (int j = midIndex; j < child.getKeyCount(); j++) {
                newChild.setKey(j - midIndex, child.getKey(j));
            }

            // Move the children pointers to the new child
            for (int j = midIndex + 1; j <= child.getKeyCount(); j++) {
                newChild.setChild(j - midIndex - 1, child.getChild(j));
            }

            // Update key counts
            newChild.incrementKeyCount(child.getKeyCount() - midIndex);
            child.incrementKeyCount(-(child.getKeyCount() - midIndex));

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
       node.printNodeContents();
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
        Set<Integer> printedLeafOffsets = new HashSet<>();
        printNode(root, 0, printedLeafOffsets);
    }

    private void printNode(BPlusTreeNode node, int level, Set<Integer> printedLeafOffsets) {
        // Print internal nodes
        if (!node.isLeaf()) {
            System.out.print("Level " + level + ": ");

            for (int i = 0; i < node.getKeyCount(); i++) {
                System.out.print(node.getKey(i));
                if (i < node.getKeyCount() - 1) {
                    System.out.print(", ");
                }
            }
            System.out.println();

            // Recursively print child nodes
            for (int i = 0; i <= node.getKeyCount(); i++) {
                int childOffset = node.getChild(i);
                printNode(new BPlusTreeNode(order, allocator, childOffset), level + 1, printedLeafOffsets);
            }
        } else {
            // Print leaves only if they haven't been printed
            if (!printedLeafOffsets.contains(node.getOffset())) {
                System.out.print("Leaf: ");
                for (int i = 0; i < node.getKeyCount(); i++) {
                    System.out.print(node.getKey(i) + "(" + node.getValue(i) + ")");
                    if (i < node.getKeyCount() - 1) {
                        System.out.print(", ");
                    }
                }
                System.out.println();
                printedLeafOffsets.add(node.getOffset()); // Mark this leaf as printed
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
//       System.out.println("Search for key 10: " + (tree.search(10) != null ? "Found" : "Not Found"));
//        System.out.println("Search for key 20: " + (tree.search(20) != null ? "Found" : "Not Found"));
//        System.out.println("Search for key 15: " + (tree.search(15) != null ? "Found" : "Not Found"));
//        System.out.println("Search for key 25: " + (tree.search(25) != null ? "Found" : "Not Found"));
//        System.out.println("Search for key 40: " + (tree.search(40) != null ? "Found" : "Not Found"));
    }
}
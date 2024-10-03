package org.example;

public class BPlusTree {
    private int order;
    private ArenaAllocator allocator;
    private BPlusTreeNode root;

    public BPlusTree(int order, ArenaAllocator allocator) {
        this.order = order;
        this.allocator = allocator;
        this.root = new BPlusTreeNode(order, allocator, true); // Root starts as a leaf node
    }

    public void insert(int key, int value) {
        // If tree is empty, create a new root
        if (root == null) {
            root = new BPlusTreeNode(order, allocator, true); // Create a new leaf node as root
            root.setKey(0, key);
            root.setValue(0, value);
            root.incrementKeyCount();
            return;
        }

        // If root is full, split it and create a new root
        if (root.getKeyCount() == order - 1) {
            BPlusTreeNode newRoot = new BPlusTreeNode(order, allocator, false);
            newRoot.setChild(0, root.getOffset());
            splitChild(newRoot, 0);

            int childIndex = newRoot.getKeyCount() > 0 && key > newRoot.getKey(0) ? 1 : 0;

            // Create a new BPlusTreeNode from the child offset
            BPlusTreeNode childNode = new BPlusTreeNode(order, allocator, newRoot.getChild(childIndex));
            root = newRoot; // Update the root reference
            insertNonFull(childNode, key, value);
        } else {
            // Insert into the non-full root
            insertNonFull(root, key, value);
        }

        // Update parent keys after insertion
        updateParentKeys(root, key);
    }

    private void insertNonFull(BPlusTreeNode node, int key, int value) {
        int i = node.getKeyCount() - 1;

        if (node.isLeaf()) {
            // Find the position to insert the new key
            while (i >= 0 && key < node.getKey(i)) {
                i--;
            }
            // Shift keys and values to the right
            node.incrementKeyCount();
            for (int j = node.getKeyCount() - 1; j > i + 1; j--) {
                node.setKey(j, node.getKey(j - 1));
                node.setValue(j, node.getValue(j - 1)); // Move values as well
            }

            // Insert the new key and value
            node.setKey(i + 1, key);
            node.setValue(i + 1, value);
        } else {
            // Find the child to recurse into
            while (i >= 0 && key < node.getKey(i)) {
                i--;
            }
            i++;
            BPlusTreeNode childNode = new BPlusTreeNode(order, allocator, node.getChild(i));
            // Recur on the child node
            if (childNode.getKeyCount() == order - 1) {
                splitChild(node, i); // Split the child if it's full
                // After splitting, determine which of the two children to insert into
                if (key > node.getKey(i)) {
                    childNode = new BPlusTreeNode(order, allocator, node.getChild(i + 1));
                }
            }
            insertNonFull(childNode, key, value);
        }
    }

    // Recursive method to update parent keys
    private void updateParentKeys(BPlusTreeNode node, int newKey) {
        if (node.getParentOffset() == -1) {
            return; // No parent to update
        }

        BPlusTreeNode parentNode = new BPlusTreeNode(order, allocator, node.getParentOffset());
        int index = node.getParentIndex();

        // Update the parent's key at the index position
        if (index >= 0) {
            parentNode.setKey(index, newKey);
        }

        // Recur upwards to update parent keys if necessary
        updateParentKeys(parentNode, newKey);
    }

    private void splitChild(BPlusTreeNode parent, int index) {
        BPlusTreeNode fullChild = new BPlusTreeNode(order, allocator, parent.getChild(index));
        BPlusTreeNode newChild = new BPlusTreeNode(order, allocator, fullChild.isLeaf());

        // Calculate the index for the median key
        int midIndex = (order - 1) / 2;
        int midKey = fullChild.getKey(midIndex);

        // Insert the median key into the parent
        parent.setKey(index, midKey);
        parent.incrementKeyCount();

        // Move keys and values to the new child
        for (int i = parent.getKeyCount() - 1; i > index; i--) {
            parent.setKey(i, parent.getKey(i - 1));
            parent.setChild(i + 1, parent.getChild(i));
        }
        parent.setChild(index + 1, newChild.getOffset()); // Link the new child
        if (fullChild.isLeaf()) {
            // Leaf node: move (midIndex) keys and values to new child
            for (int i = 0; i < midIndex; i++) {
                newChild.setKey(i, fullChild.getKey(i + midIndex));
                newChild.setValue(i, fullChild.getValue(i + midIndex));
            }

            // Adjust key counts
            for (int i = 0; i < midIndex; i++) {
                fullChild.decrementKeyCount(); // Remove keys from full child
            }
            newChild.incrementKeyCount(); // New child has midIndex keys
        } else {
            // Internal node: move (midIndex) keys to new child
            for (int i = 0; i < midIndex; i++) {
                newChild.setKey(i, fullChild.getKey(i + midIndex + 1));
            }
            // Move children pointers as well
            for (int i = 0; i <= midIndex; i++) {
                newChild.setChild(i, fullChild.getChild(i + midIndex + 1));
            }
            // Adjust key counts
            for (int i = 0; i < midIndex; i++) {
                fullChild.decrementKeyCount(); // Remove keys from full child
            }
            newChild.incrementKeyCount(); // New child has midIndex keys
        }
    }

    public int search(int key) {
        return search(root, key);
    }

    private int search(BPlusTreeNode node, int key) {
        int i = 0;
        while (i < node.getKeyCount() && key > node.getKey(i)) {
            i++;
        }
        if (i < node.getKeyCount() && key == node.getKey(i) && node.isLeaf()) {
            return node.getValue(i);
        } else if (node.isLeaf()) {
            return -1; // Not found
        } else {
            return search(new BPlusTreeNode(order, allocator, node.getChild(i)), key);
        }
    }

    public void printTree() {
        printTree(root, 0);
    }

    private void printTree(BPlusTreeNode node, int level) {
        if (node != null) {
            node.printNode(level);
            if (!node.isLeaf()) {
                for (int i = 0; i <= node.getKeyCount(); i++) {
                    printTree(new BPlusTreeNode(order, allocator, node.getChild(i)), level + 1);
                }
            }
        }
    }
    public static void main(String[] args) {
        // Initialize the B+ tree with order 3 and an arena allocator
        ArenaAllocator allocator = new ArenaAllocator(1024); // Adjust size as needed
        BPlusTree tree = new BPlusTree(3, allocator);

        // Test Insertions
        System.out.println("Inserting values:");
        tree.insert(10, 10);
        tree.insert(20, 20);
        //tree.insert(5, 5);
        //tree.insert(6, "Value6");
        //tree.insert(12, "Value12");
        //tree.insert(30, "Value30");

        // Print the tree structure after insertions
        //System.out.println("\nTree structure after insertions:");
       //tree.printTree();

        // Test Search
        //System.out.println("\nSearching for values:");
        //System.out.println("Key 10: " + tree.search(10)); // Should return "Value10"
        //System.out.println("Key 5: " + tree.search(5));   // Should return "Value5"
        //System.out.println("Key 30: " + tree.search(30)); // Should return "Value30"
        //System.out.println("Key 15: " + tree.search(15)); // Should return null

        // Additional Test: Edge cases
        //System.out.println("Searching for non-existing key:");
        //System.out.println("Key 100: " + tree.search(100)); // Should return null
    }
}

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
       if(value == -1){
           throw new IllegalArgumentException("Value cannot be -1");
       }
        BPlusTreeNode rootNode = root;
        if (rootNode.getKeyCount() == order - 1) {
            // Root is full, need to split
            BPlusTreeNode newRoot = new BPlusTreeNode(order, allocator, false); // New root is internal
            newRoot.setChild(0, rootNode.getOffset());
            splitChild(newRoot, 0);
            int i = 0;
            if (newRoot.getKey(0) < key) {
                i++;
            }
            insertNonFull(newRoot, key, value);
            root = newRoot;
        } else {
            insertNonFull(rootNode, key, value);
        }
    }

    private void insertNonFull(BPlusTreeNode node, int key, int value) {
        int count = node.getKeyCount();
        int i = count - 1;

        if (node.isLeaf()) {
            // Insert into the leaf node
            while (i >= 0 && node.getKey(i) > key) {
                node.setKey(i + 1, node.getKey(i));
                node.setValue(i + 1, node.getValue(i)); // Shift values as well
                i--;
            }
            node.setKey(i + 1, key);
            node.setValue(i + 1, value);
            node.incrementKeyCount();
        } else {
            // Navigate to the correct child
            while (i >= 0 && node.getKey(i) > key) {
                i--;
            }
            i++;
            BPlusTreeNode childNode = new BPlusTreeNode(order, allocator, node.getChild(i));
            if (childNode.getKeyCount() == order - 1) {
                splitChild(node, i);
                if (node.getKey(i) < key) {
                    i++;
                }
            }
            insertNonFull(new BPlusTreeNode(order, allocator, node.getChild(i)), key, value);
        }
    }

    private void splitChild(BPlusTreeNode parent, int index) {
        BPlusTreeNode fullChild = new BPlusTreeNode(order, allocator, parent.getChild(index));
        BPlusTreeNode newChild = new BPlusTreeNode(order, allocator, fullChild.isLeaf());

        // Transfer keys and children to the new child
        int midIndex = (order - 1) / 2;
        int midKey = fullChild.getKey(midIndex);

        // Insert the median key into the parent node
        parent.setChild(index + 1, newChild.getOffset());
        parent.setKey(index, midKey);
        parent.incrementKeyCount();

        for (int i = parent.getKeyCount() - 1; i > index; i--) {
            parent.setKey(i, parent.getKey(i - 1));
            parent.setChild(i + 1, parent.getChild(i));
        }

        // Move the keys and values to the new child
        for (int i = 0; i < midIndex; i++) {
            newChild.setKey(i, fullChild.getKey(i + midIndex + 1));
            newChild.setValue(i, fullChild.getValue(i + midIndex + 1));
        }
        if (!fullChild.isLeaf()) {
            for (int i = 0; i <= midIndex; i++) {
                newChild.setChild(i, fullChild.getChild(i + midIndex + 1));
            }
        }
        fullChild.decrementKeyCount();
        newChild.incrementKeyCount();
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
        tree.insert(10, 100);
        tree.insert(20, 200);
        //tree.insert(5, 50);
        //tree.insert(6, 60);
        //tree.insert(12, 120);
        //tree.insert(30, 300);

        // Print the tree structure after insertions
        System.out.println("\nTree structure after insertions:");
        tree.printTree();

        // Test Search
        System.out.println("\nSearching for values:");
        System.out.println("Key 10: " + tree.search(10)); // Should return 100
        System.out.println("Key 5: " + tree.search(5));   // Should return 50
        System.out.println("Key 30: " + tree.search(30)); // Should return 300
        System.out.println("Key 15: " + tree.search(15)); // Should return null

        // Additional Test: Edge cases
        System.out.println("Searching for non-existing key:");
        System.out.println("Key 100: " + tree.search(100)); // Should return null
    }
}

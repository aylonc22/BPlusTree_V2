package org.example;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class BPlusTree {
    private int order;
    private ArenaAllocator allocator;
    private BPlusTreeNode root;

    public BPlusTree(int order, ArenaAllocator allocator) {
        this.order = order;
        this.allocator = allocator;
    }
public void insertMany(HashMap<Integer,Integer> items){
        for (var item:items.entrySet()){
            insert(item.getKey(),item.getValue());
        }
}
    public void insert(int key, int value) {
        // If the tree is empty, create a new root
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
            // Check if the key already exists
            if (search(key) != -1) {
                // Update the value if the key exists
                updateValue(root, key, value);
                return;
            }
            // Insert into the non-full root
            insertNonFull(root, key, value);
        }

        // Update parent keys after insertion
        updateParentKeys(root, key);
    }

    private void updateValue(BPlusTreeNode node, int key, int value) {
        while (node != null) {
            for (int i = 0; i < node.getKeyCount(); i++) {
                if (node.getKey(i) == key) {
                    node.setValue(i, value);
                    return; // Value updated
                }
            }
            // If it's not a leaf, move to the correct child
            if (!node.isLeaf()) {
                int childIndex = 0;
                while (childIndex < node.getKeyCount() && key > node.getKey(childIndex)) {
                    childIndex++;
                }
                node = new BPlusTreeNode(order, allocator, node.getChild(childIndex));
            } else {
                break; // Reached a leaf without finding the key
            }
        }
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
            //printTree(node,0);
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
    public void delete(int key) {
        if (root == null) {
            return; // Tree is empty
        }

        // Start the deletion process
        delete(root, key);

        // If the root node is empty after deletion, make the first child the new root
        if (root.getKeyCount() == 0 && !root.isLeaf()) {
            root = new BPlusTreeNode(order, allocator, root.getChild(0));
        }
    }

    private void delete(BPlusTreeNode node, int key) {
        int index = 0;
        System.out.println("t");
        node.printNodeContents();
        // Find the index of the key in the current node
        while (index < node.getKeyCount() && key > node.getKey(index)) {
            index++;
        }

        if (index < node.getKeyCount() && key == node.getKey(index)) {
            if (node.isLeaf()) {
                // Key found in leaf node; remove it
                removeFromLeaf(node, index);
            } else {
                // Key found in internal node; find the appropriate leaf
                BPlusTreeNode leafNode = new BPlusTreeNode(order, allocator, node.getChild(index));
                // Recur to find and delete the key from the leaf
                delete(leafNode, key);
            }
        } else {
            // If not found and it's a leaf node, do nothing
            if (node.isLeaf()) {
                return; // Key not found
            }

            // Determine the child to recurse into
            boolean isLastChild = (index == node.getKeyCount());
            BPlusTreeNode childNode = new BPlusTreeNode(order, allocator, node.getChild(isLastChild ? index - 1 : index));

            // Recur on the child node
            delete(childNode, key);
        }

        // After the recursive call, check if we need to update the parent's key
        updateParentKey(node, key);
    }

    private void removeFromLeaf(BPlusTreeNode node, int index) {
        // Shift keys and values to the left to fill the gap
        for (int i = index + 1; i < node.getKeyCount(); i++) {
            node.setKey(i - 1, node.getKey(i));
            node.setValue(i - 1, node.getValue(i));
        }
        node.decrementKeyCount(); // Decrease the key count
    }

    private void updateParentKey(BPlusTreeNode node, int key) {
        if (node.getParentOffset() == -1) {
            return; // No parent to update
        }

        BPlusTreeNode parentNode = new BPlusTreeNode(order, allocator, node.getParentOffset());
        int index = node.getParentIndex();

        // Update the parent's key if necessary
        if (index >= 0 && node.getKeyCount() > 0) {
            // Update with the first key of the current node if necessary
            parentNode.setKey(index, node.getKey(0));
        }
    }

    private int getPredecessor(BPlusTreeNode node, int index) {
        BPlusTreeNode current = new BPlusTreeNode(order, allocator, node.getChild(index));
        while (!current.isLeaf()) {
            current = new BPlusTreeNode(order, allocator, current.getChild(current.getKeyCount()));
        }
        return current.getKey(current.getKeyCount() - 1);
    }

    private void fill(BPlusTreeNode node, int index) {
        BPlusTreeNode leftChild = new BPlusTreeNode(order, allocator, node.getChild(index));
        BPlusTreeNode rightChild = new BPlusTreeNode(order, allocator, node.getChild(index + 1));

        // If the left child has enough keys
        if (leftChild.getKeyCount() >= (order - 1) / 2) {
            // Move a key from the parent to the left child
            for (int i = leftChild.getKeyCount(); i > 0; i--) {
                leftChild.setKey(i, leftChild.getKey(i - 1));
            }
            leftChild.setKey(0, node.getKey(index - 1));
            node.setKey(index - 1, leftChild.getKey(1)); // Move the first key of the right child up
            leftChild.incrementKeyCount();
            rightChild.decrementKeyCount();
        }
        // If the right child has enough keys
        else if (rightChild.getKeyCount() >= (order - 1) / 2) {
            leftChild.setKey(leftChild.getKeyCount(), node.getKey(index)); // Move the parent key down
            node.setKey(index, rightChild.getKey(0)); // Move the first key of the right child up
            for (int i = 1; i < rightChild.getKeyCount(); i++) {
                rightChild.setKey(i - 1, rightChild.getKey(i));
            }
            rightChild.decrementKeyCount();
            leftChild.incrementKeyCount();
        }
        // If both children have the minimum number of keys, merge them
        else {
            merge(node, index);
            leftChild = new BPlusTreeNode(order, allocator, node.getChild(index)); // Update leftChild reference
            leftChild.decrementKeyCount(); // Remove the merged key
        }
    }

    private void merge(BPlusTreeNode parent, int index) {
        BPlusTreeNode leftChild = new BPlusTreeNode(order, allocator, parent.getChild(index));
        BPlusTreeNode rightChild = new BPlusTreeNode(order, allocator, parent.getChild(index + 1));

        // Move the key from the parent to the merged child
        leftChild.setKey(leftChild.getKeyCount(), parent.getKey(index)); // Move the parent key down
        leftChild.incrementKeyCount();

        // Copy keys from the right child to the left child
        for (int i = 0; i < rightChild.getKeyCount(); i++) {
            leftChild.setKey(leftChild.getKeyCount(), rightChild.getKey(i));
            leftChild.incrementKeyCount();
        }

        // Update parent
        for (int i = index + 1; i < parent.getKeyCount(); i++) {
            parent.setKey(i - 1, parent.getKey(i));
        }
        parent.decrementKeyCount();
    }
    public void printTree() {
        Set<Integer> printedOffsets = new HashSet<>();
        printTree(root, 0, printedOffsets);
    }

    private void printTree(BPlusTreeNode node, int level, Set<Integer> printedOffsets) {
        if (node != null) {
            // Check if this node has already been printed
            if (!printedOffsets.contains(node.getOffset())) {
                node.printNode(level); // Print the node contents
                printedOffsets.add(node.getOffset()); // Mark this node as printed

                // If the node is not a leaf, print its children
                if (!node.isLeaf()) {
                    for (int i = 0; i <= node.getKeyCount(); i++) {
                        printTree(new BPlusTreeNode(order, allocator, node.getChild(i)), level + 1, printedOffsets);
                    }
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

        tree.insert(1, 5);
        tree.insert(2, 30);
         tree.insert(3, 5);
        tree.insert(6, 6);
        tree.insert(7, 8);
        //tree.insert(30, "Value30");

        // Print the tree structure after insertions
        //System.out.println("\nTree structure after insertions:");
        tree.printTree();

        // Test Search
        //System.out.println("\nSearching for values:");
        //System.out.println("Key 10: " + tree.search(10)); // Should return "Value10"
        //System.out.println("Key 5: " + tree.search(5));   // Should return "Value5"
        //System.out.println("Key 30: " + tree.search(30)); // Should return "Value30"
        //System.out.println("Key 15: " + tree.search(15)); // Should return null
        // Additional Test: Edge cases
    }
}

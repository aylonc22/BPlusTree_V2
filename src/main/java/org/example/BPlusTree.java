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

        delete(root, key);

        // If the root node is empty after deletion, make the first child the new root
        if (root.getKeyCount() == 0 && !root.isLeaf()) {
            root = new BPlusTreeNode(order, allocator, root.getChild(0));
        }
    }

    private void delete(BPlusTreeNode node, int key) {
        int index = 0;
        while (index < node.getKeyCount() && key > node.getKey(index)) {
            index++;
        }

        // If found in this node
        if (index < node.getKeyCount() && key == node.getKey(index)) {
            if (node.isLeaf()) {
                // Remove the key and value from the leaf node
                removeFromLeaf(node, index);
            } else {
                // Remove from internal node
                removeFromInternal(node, index);
            }
        } else {
            // If not found and it's a leaf node, do nothing
            if (node.isLeaf()) {
                return;
            }

            boolean isLastChild = (index == node.getKeyCount());

            // Get the child node to recurse into
            BPlusTreeNode childNode = new BPlusTreeNode(order, allocator, node.getChild(isLastChild ? index - 1 : index));

            // If the child is underflowed, fix it
            if (childNode.getKeyCount() < (order - 1) / 2) {
                fill(node, index); // This method needs to be defined
                childNode = new BPlusTreeNode(order, allocator, node.getChild(isLastChild ? index - 1 : index));
            }

            // Recur on the child
            delete(childNode, key);
        }
    }

    private void removeFromLeaf(BPlusTreeNode node, int index) {
        for (int i = index + 1; i < node.getKeyCount(); i++) {
            node.setKey(i - 1, node.getKey(i));
            node.setValue(i - 1, node.getValue(i));
        }
        node.decrementKeyCount(); // Decrease the key count
    }

    private void removeFromInternal(BPlusTreeNode node, int index) {
        int keyToDelete = node.getKey(index);

        BPlusTreeNode leftChild = new BPlusTreeNode(order, allocator, node.getChild(index));
        if (leftChild.getKeyCount() >= (order - 1) / 2) {
            // Find the predecessor
            int predecessor = getPredecessor(node, index);
            node.setKey(index, predecessor);
            delete(leftChild, predecessor);
        } else {
            BPlusTreeNode rightChild = new BPlusTreeNode(order, allocator, node.getChild(index + 1));
            if (rightChild.getKeyCount() >= (order - 1) / 2) {
                // Find the successor
                int successor = getSuccessor(node, index);
                node.setKey(index, successor);
                delete(rightChild, successor);
            } else {
                // Merge
                merge(node, index);
                leftChild = new BPlusTreeNode(order, allocator, node.getChild(index)); // Update leftChild reference
                delete(leftChild, keyToDelete);
            }
        }
    }

    private int getPredecessor(BPlusTreeNode node, int index) {
        BPlusTreeNode current = new BPlusTreeNode(order, allocator, node.getChild(index));
        while (!current.isLeaf()) {
            current = new BPlusTreeNode(order, allocator, current.getChild(current.getKeyCount()));
        }
        return current.getKey(current.getKeyCount() - 1);
    }

    private int getSuccessor(BPlusTreeNode node, int index) {
        BPlusTreeNode current = new BPlusTreeNode(order, allocator, node.getChild(index + 1));
        while (!current.isLeaf()) {
            current = new BPlusTreeNode(order, allocator, current.getChild(0));
        }
        return current.getKey(0);
    }

    private void fill(BPlusTreeNode node, int index) {
        // This method should handle redistributing keys from sibling nodes or merging nodes.
        // Example logic might include checking left and right siblings to see if keys can be borrowed.
        // Implement the fill logic based on your B+ tree structure and requirements.
    }

    private void merge(BPlusTreeNode node, int index) {
        BPlusTreeNode leftChild = new BPlusTreeNode(order, allocator, node.getChild(index));
        BPlusTreeNode rightChild = new BPlusTreeNode(order, allocator, node.getChild(index + 1));

        // Move the middle key to the left child
        leftChild.setKey(leftChild.getKeyCount(), node.getKey(index));
        for (int i = 0; i < rightChild.getKeyCount(); i++) {
            leftChild.setKey(leftChild.getKeyCount() + i + 1, rightChild.getKey(i));
            leftChild.setValue(leftChild.getKeyCount() + i + 1, rightChild.getValue(i));
        }

        // Link children
        if (!leftChild.isLeaf()) {
            for (int i = 0; i <= rightChild.getKeyCount(); i++) {
                leftChild.setChild(leftChild.getKeyCount() + i + 1, rightChild.getChild(i));
            }
        }

        // Update key counts
        leftChild.incrementKeyCount(); // Include the moved middle key
        int rightChildKeyCount = rightChild.getKeyCount();
        for(int i =0;i<rightChildKeyCount;i++) {
            leftChild.incrementKeyCount();// Add the right child keys
        }
        node.decrementKeyCount(); // Remove the key from the parent
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

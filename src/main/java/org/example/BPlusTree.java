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

        // Find the index of the key in the current node
        while (index < node.getKeyCount() && key > node.getKey(index)) {
            index++;
        }

        // If found in this node
        if (index < node.getKeyCount() && key == node.getKey(index)) {
            if (node.isLeaf()) {
                // Key found in leaf; remove it
                removeFromLeaf(node, index);
            } else {
                // Key found in internal node; find the appropriate child to recurse into
                BPlusTreeNode leftChild = new BPlusTreeNode(order, allocator, node.getChild(index));
                if (leftChild.getKeyCount() > 0 && leftChild.containsKey(key)) {
                    // Key is in the left child, delete from it
                    delete(leftChild, key);
                } else {
                    // If key is not found in left child, check the right child
                    BPlusTreeNode rightChild = new BPlusTreeNode(order, allocator, node.getChild(index + 1));
                    if (rightChild.getKeyCount() > 0 && rightChild.containsKey(key)) {
                        // Key is in the right child, delete from it
                        delete(rightChild, key);
                    } else {
                        // Key not found in either child, no action needed
                        return;
                    }
                }
            }
        } else {
            // If not found and it's a leaf node, do nothing
            if (node.isLeaf()) {
                return; // Key not found
            }

            // Move to the appropriate child based on the index
            BPlusTreeNode childNode = new BPlusTreeNode(order, allocator, node.getChild(index));
            // Recur on the child node
            delete(childNode, key);
        }

        // After deleting, check if we need to update the internal node's key
        if (!node.isLeaf() && index < node.getKeyCount() && node.getKey(index) == key) {
            // Key was found in internal node; check if it needs to be replaced or removed
            BPlusTreeNode leftChild = new BPlusTreeNode(order, allocator, node.getChild(index));
            if (leftChild.getKeyCount() > 0) {
                // Find a new key to replace the internal key if the left child still has keys
                int newKey = leftChild.getKey(leftChild.getKeyCount() - 1); // Use the largest key from left child
                node.setKey(index, newKey);
            } else {
                // No more keys in left child, remove the internal key
                removeInternalKey(node,index); // Assume a method to remove a key at the given index
            }

            // Check for underflow and handle if necessary
            if (node.getKeyCount() < (order - 1) / 2) {
                // Handle underflow: merge or borrow keys from siblings
                handleUnderflow(node, index);
            }
        }
    }

    private void removeFromLeaf(BPlusTreeNode node, int index) {
        // Shift keys and values to the left to fill the gap
        for (int i = index + 1; i < node.getKeyCount(); i++) {
            node.setKey(i - 1, node.getKey(i));
            node.setValue(i - 1, node.getValue(i));
        }
        node.decrementKeyCount(); // Decrease the key count
    }
    private void removeInternalKey(BPlusTreeNode node, int index) {
        // Get the child nodes for the index
        BPlusTreeNode leftChild = new BPlusTreeNode(order, allocator, node.getChild(index));
        BPlusTreeNode rightChild = new BPlusTreeNode(order, allocator, node.getChild(index + 1));

        if (leftChild.getKeyCount() >= (order - 1) / 2) {
            // If the left child has enough keys, find the predecessor
            int predecessor = getPredecessor(node, index);
            node.setKey(index, predecessor); // Replace the internal key with the predecessor
            delete(leftChild, predecessor); // Remove the predecessor from the left child
        } else if (rightChild.getKeyCount() >= (order - 1) / 2) {
            // If the right child has enough keys, find the successor
            int successor = getSuccessor(node, index);
            node.setKey(index, successor); // Replace the internal key with the successor
            delete(rightChild, successor); // Remove the successor from the right child
        } else {
            // Merge left and right children
            merge(node, index);
            leftChild = new BPlusTreeNode(order, allocator, node.getChild(index)); // Update leftChild reference
            // Now delete the key from the merged child
            int keyToRemove = node.getKey(index); // Key to remove from the merged child
            delete(leftChild, keyToRemove);
        }
    }
    private int getPredecessor(BPlusTreeNode node, int index) {
        BPlusTreeNode current = new BPlusTreeNode(order, allocator, node.getChild(index));
        while (!current.isLeaf()) {
            current = new BPlusTreeNode(order, allocator, current.getChild(current.getKeyCount()));
        }
        return current.getKey(current.getKeyCount() - 1); // Return the last key in the leaf
    }
    private int getSuccessor(BPlusTreeNode node, int index) {
        BPlusTreeNode current = new BPlusTreeNode(order, allocator, node.getChild(index + 1));
        while (!current.isLeaf()) {
            current = new BPlusTreeNode(order, allocator, current.getChild(0));
        }
        return current.getKey(0); // Return the first key in the leaf
    }
    private void handleUnderflow(BPlusTreeNode node, int index) {
        BPlusTreeNode leftSibling = null;
        BPlusTreeNode rightSibling = null;
        int parentOffset = node.getParentOffset();
        if(parentOffset == -1){
            return; //node does not have parent
        }

        int parentKeyCount = new BPlusTreeNode(order,allocator,parentOffset).getKeyCount(); // Assuming this method exists to get parent key count

        // Get the left sibling if it exists
        if (index > 0) {
            leftSibling = new BPlusTreeNode(order, allocator, node.getParentOffset());
            leftSibling = new BPlusTreeNode(order, allocator, leftSibling.getChild(index - 1));
        }

        // Get the right sibling if it exists
        if (index < parentKeyCount) {
            rightSibling = new BPlusTreeNode(order, allocator, node.getParentOffset());
            rightSibling = new BPlusTreeNode(order, allocator, rightSibling.getChild(index + 1));
        }

        // Check if we can borrow from the left sibling
        if (leftSibling != null && leftSibling.getKeyCount() > (order - 1) / 2) {
            // Borrow the last key from left sibling
            int borrowedKey = leftSibling.getKey(leftSibling.getKeyCount() - 1);
            // Shift keys in the current node to make room for the borrowed key
            for (int i = node.getKeyCount(); i > 0; i--) {
                node.setKey(i, node.getKey(i - 1));
            }
            node.setKey(0, borrowedKey); // Set the borrowed key in the current node

            // Update the parent key
            int parentKey = leftSibling.getKey(leftSibling.getKeyCount() - 1); // Assuming the last key of left sibling is the parent key
            node.setKey(0, parentKey); // Set the parent key to the current node

            leftSibling.decrementKeyCount(); // Decrease the key count in the left sibling
        }
        // Check if we can borrow from the right sibling
        else if (rightSibling != null && rightSibling.getKeyCount() > (order - 1) / 2) {
            // Borrow the first key from right sibling
            int borrowedKey = rightSibling.getKey(0);
            node.setKey(node.getKeyCount(), borrowedKey); // Insert at the end

            // Update the parent key
            node.setKey(node.getKeyCount() - 1, rightSibling.getKey(0)); // Update the parent key

            // Shift keys in the right sibling
            for (int i = 1; i < rightSibling.getKeyCount(); i++) {
                rightSibling.setKey(i - 1, rightSibling.getKey(i)); // Shift left
            }
            rightSibling.decrementKeyCount(); // Decrease the key count in the right sibling
        }
        // If both siblings are not sufficient, merge with one of them
        else {
            if (leftSibling != null) {
                // Merge with the left sibling
                int totalKeys = leftSibling.getKeyCount() + node.getKeyCount();
                for (int i = 0; i < node.getKeyCount(); i++) {
                    leftSibling.setKey(leftSibling.getKeyCount() + i, node.getKey(i)); // Append node keys to left sibling
                }
                leftSibling.incrementKeyCount(node.getKeyCount()); // Increase key count in left sibling

                // Update parent
                removeKeyFromParent(node, parentKeyCount, index); // Use a helper method to remove the key from the parent
            } else if (rightSibling != null) {
                // Merge with the right sibling
                int totalKeys = rightSibling.getKeyCount() + node.getKeyCount();
                for (int i = 0; i < node.getKeyCount(); i++) {
                    rightSibling.setKey(rightSibling.getKeyCount() + i, node.getKey(i)); // Append node keys to right sibling
                }
                rightSibling.incrementKeyCount(node.getKeyCount()); // Increase key count in right sibling

                // Update parent
                removeKeyFromParent(node, parentKeyCount, index + 1); // Use a helper method to remove the key from the parent
            }
        }
    }

    // Helper method to remove a key from the parent
    private void removeKeyFromParent(BPlusTreeNode node, int parentKeyCount, int index) {
        // Shift keys in the parent node to remove the specified key
        for (int i = index; i < parentKeyCount - 1; i++) {
            node.setKey(i, node.getKey(i + 1));
        }
        node.decrementKeyCount(); // Decrease the key count in the parent
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

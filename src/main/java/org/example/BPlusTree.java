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
            insertNonFull(root, key, value);
            root.printNodeContents();
//            BPlusTreeNode newRoot = new BPlusTreeNode(order, allocator, false);
//            newRoot.setChild(0, root.getOffset());
//            splitChild(newRoot, 0,true);
//            root.setParentOffset(newRoot.getOffset());
//            root = newRoot;
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

            // If the last child is the only child and is not full
            if (node.getChild(i + 1) == -1) {
                int childOffset = node.getChild(i);
                BPlusTreeNode childNode = new BPlusTreeNode(order, allocator, childOffset);

                // Check if the single child is full
                if (childNode.getKeyCount() == order - 1) {
                    // Split the full child
                    splitChild(node, i,false);
                    // After splitting, we need to determine the correct child offset again
                    if (key > node.getKey(i)) {
                        childOffset = node.getChild(i + 1); // Go to the right child
                    } else {
                        childOffset = node.getChild(i); // Stay on the left child
                    }
                }
                // Insert into the (possibly split) child
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
                splitChild(node, i,true);
                // After splitting, check which child to go to
                if (key > node.getKey(i)) {
                    childOffset = node.getChild(i + 1); // Go to the right child
                }
            }

            // Recur to the appropriate child
            insertNonFull(new BPlusTreeNode(order, allocator, childOffset), key, value);
        }
    }
    /**
     * Splits a child node of the B+ tree when it exceeds its maximum capacity of keys.
     * This method handles both leaf and internal nodes and ensures that the tree maintains
     * its structural properties after the split.
     *
     * @param parent The parent node that contains the child to be split.
     * @param index The index of the child node within the parent that is being split.
     * @param flag A boolean flag indicating whether to increment the parent's key count.
     * for special cases where there is 1 key and 1 child, and we want to add another child without another key
     */
    private void splitChild(BPlusTreeNode parent, int index, boolean flag) {
        // Create a reference to the child node that will be split
        BPlusTreeNode child = new BPlusTreeNode(order, allocator, parent.getChild(index));
        BPlusTreeNode newChild; // This will be the new child created after the split

        // Determine the midpoint index for splitting
        int midIndex = order / 2; // Midpoint for splitting

        if (child.isLeaf()) {
            // Splitting a leaf node
            newChild = new BPlusTreeNode(order, allocator, true); // Create a new leaf node

            // Move the last keys and values from the original child to the new child
            for (int j = midIndex; j < child.getKeyCount(); j++) {
                newChild.setKey(j - midIndex, child.getKey(j)); // Set keys in the new child
                newChild.setValue(j - midIndex, child.getValue(j)); // Set values in the new child
            }

            // Update key counts for both child nodes
            newChild.incrementKeyCount(child.getKeyCount() - midIndex); // Key count for new child
            child.incrementKeyCount(-(child.getKeyCount() - midIndex)); // Key count for original child

            // Adjust the parent node
            parent.setKey(index, child.getKey(midIndex - 1)); // Promote the middle key to parent
            parent.setChild(index + 1, newChild.getOffset()); // Link the new child to the parent

        } else {
            // Splitting an internal node
            newChild = new BPlusTreeNode(order, allocator, false); // Create a new internal node

            // Move keys from the original child to the new child
            for (int j = midIndex; j < child.getKeyCount(); j++) {
                newChild.setKey(j - midIndex, child.getKey(j)); // Set keys in the new child
            }

            // Move child pointers (links to other nodes) from the original child to the new child
            for (int j = midIndex + 1; j <= child.getKeyCount(); j++) {
                int childOffset = child.getChild(j);
                if (childOffset != -1) { // Only move valid child pointers
                    newChild.setChild(j - midIndex - 1, childOffset); // Set child pointers in the new child
                }
            }

            // Update key counts for both child nodes
            newChild.incrementKeyCount(child.getKeyCount() - midIndex); // Key count for new child
            child.incrementKeyCount(-(child.getKeyCount() - midIndex)); // Key count for original child

            // Reset the child pointers of the original child to -1 after the split
            for (int j = midIndex + 1; j <= child.getKeyCount(); j++) {
                child.setChild(j, -1); // Set invalid pointers to -1
            }

            // Promote the middle key from the original child to the parent
            parent.setKey(index, child.getKey(midIndex - 1)); // Promote key
            parent.setChild(index + 1, newChild.getOffset()); // Link the new child to the parent
        }

        // Shift parent keys and child links to make room for the new key
        for (int j = parent.getKeyCount(); j > index; j--) {
            parent.setKey(j, parent.getKey(j - 1)); // Shift keys to the right
            parent.setChild(j + 1, parent.getChild(j)); // Shift child links to the right
        }

        // Update the parent offset in the new child
        newChild.setParentOffset(parent.getOffset()); // Set the new child's parent offset

        // Increment the parent's key count if the flag is true
        if (flag) {
            parent.incrementKeyCount(); // Increase parent's key count to reflect the new key
        }
    }


    public Integer search(int key) {
        return search(root, key);
    }

    /**
     * Searches for a key in the B+ tree starting from the given node.
     *
     * @param node The current node to search in.
     * @param key The key to search for.
     * @return The value associated with the key if found; null otherwise.
     */
    private Integer search(BPlusTreeNode node, int key) {
        int i = 0;

        // Traverse keys in the current node to find the appropriate child
        while (i < node.getKeyCount() && key > node.getKey(i)) {
            i++;
        }
        node.printNodeContents();
        // If the current node is a leaf node, check for the key
        if (node.isLeaf()) {
            // If the key matches, return the corresponding value
            if (i < node.getKeyCount() && key == node.getKey(i)) {
                return node.getValue(i); // Return value if found in the leaf
            }
            return null; // Key not found in the leaf
        }

        // If we're at an internal node and the key matches, go to the appropriate child
        if (i < node.getKeyCount() && key == node.getKey(i)) {
            int childOffset = node.getChild(i);
            if (childOffset != -1) { // Only proceed if the child exists
                BPlusTreeNode childNode = new BPlusTreeNode(order, allocator, childOffset);
                return search(childNode, key); // Recursively search in the child
            }
        }

        // Traverse to the appropriate child node
        int childOffset = node.getChild(i);
        if (childOffset != -1) { // Only proceed if the child exists
            BPlusTreeNode childNode = new BPlusTreeNode(order, allocator, childOffset);
            return search(childNode, key); // Recursively search in the child
        }

        return null; // Key not found if child does not exist
    }

    // Print the B+ Tree
    public void printTree() {
        printNode(root, 0);
    }

    private void printNode(BPlusTreeNode node, int level) {
        System.out.print( "Offset "+node.getOffset()+" Level " + level + ": ");
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
        //tree.insert(15, 150);
       // tree.insert(30, 300);
        //tree.insert(25, 250); // Adding more to trigger splits
       //tree.insert(35, 300);
        //tree.insert(37, 300);
        // Print the tree structure
        System.out.println("B+ Tree structure:");
       tree.printTree();

        // Search for values
      // System.out.println("Search for key 10: " + (tree.search(10) != null ? "Found" : "Not Found"));
        //System.out.println("Search for key 20: " + (tree.search(20) != null ? "Found" : "Not Found"));
        //System.out.println("Search for key 15: " + (tree.search(15) != null ? "Found" : "Not Found"));
        //System.out.println("Search for key 25: " + (tree.search(25) != null ? "Found" : "Not Found"));
        //System.out.println("Search for key 40: " + (tree.search(40) != null ? "Found" : "Not Found"));
    }
}
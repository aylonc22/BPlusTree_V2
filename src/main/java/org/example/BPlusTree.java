package org.example;

class BPlusTree {
    private BPlusTreeNode root;
    private final int order;
    private final ArenaAllocator allocator;

    public BPlusTree(int order, int arenaSize) {
        this.order = order;
        this.allocator = new ArenaAllocator(arenaSize);
        this.root = new BPlusTreeNode(order, allocator);
    }

    public void insert(int key, String value) {
        if (root.getKeyCount() == order - 1) {
            BPlusTreeNode newRoot = new BPlusTreeNode(order, allocator);
            newRoot.setLeaf(false);
            newRoot.setChild(0, root.getOffset());
            splitChild(newRoot, 0);
            root = newRoot;
        }
        insertNonFull(root, key, value);
    }

    private void insertNonFull(BPlusTreeNode node, int key, String value) {
        int count = node.getKeyCount();
        int i = count - 1;

        if (node.isLeaf()) {
            while (i >= 0 && node.getKey(i) > key) {
                node.setKey(i + 1, node.getKey(i));
                node.setValue(i + 1, node.getValue(i));
                i--;
            }
            node.setKey(i + 1, key);
            node.setValue(i + 1, value);
            node.incrementKeyCount();
        } else {
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
        BPlusTreeNode child = new BPlusTreeNode(order, allocator, parent.getChild(index));
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

        child.decrementKeyCount();
        for (int j = parent.getKeyCount(); j > index; j--) {
            parent.setChild(j + 1, parent.getChild(j));
            parent.setKey(j, parent.getKey(j - 1));
        }
        parent.setChild(index + 1, newNode.getOffset());
        parent.setKey(index, child.getKey(splitIndex));
        parent.incrementKeyCount();
    }

    public String search(int key) {
        return search(root, key);
    }

    private String search(BPlusTreeNode node, int key) {
        int i = 0;
        while (i < node.getKeyCount() && node.getKey(i) < key) {
            i++;
        }
        if (i < node.getKeyCount() && node.getKey(i) == key) {
            return node.getValue(i);
        }
        if (node.isLeaf()) {
            return null;
        }
        return search(new BPlusTreeNode(order, allocator, node.getChild(i)), key);
    }

    public void printTree() {
        printTree(root, 0);
    }

    private void printTree(BPlusTreeNode node, int level) {
        node.printNode(level);
        if (!node.isLeaf()) {
            for (int i = 0; i <= node.getKeyCount(); i++) {
                printTree(new BPlusTreeNode(order, allocator, node.getChild(i)), level + 1);
            }
        }
    }

    public static void main(String[] args) {
        BPlusTree bPlusTree = new BPlusTree(4, 1024);

        bPlusTree.insert(10, "Ten");
        bPlusTree.insert(20, "Twenty");
        bPlusTree.insert(5, "Five");
        bPlusTree.insert(6, "Six");
        bPlusTree.insert(12, "Twelve");
        bPlusTree.insert(30, "Thirty");

        System.out.println("Searching for key 10: " + bPlusTree.search(10)); // Ten
        System.out.println("Searching for key 15: " + bPlusTree.search(15)); // null

        System.out.println("Tree structure:");
        bPlusTree.printTree(); // Print the tree structure
    }
}

import org.example.ArenaAllocator;
import org.example.BPlusTree;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class BPlusTreeTest {
    @Test
    public void should_create_tree(){
        var tree = new BPlusTree(3,new ArenaAllocator(1024));
        tree.insert(3,4);
        tree.printTree();
    }
    @Test
    public void should_find_insertion(){
        var tree = new BPlusTree(3,new ArenaAllocator(1024));
        tree.insert(3,4);
        assertEquals(4,tree.search(3));
    }
    @Test
    public void should_notFind_insertion(){
        var tree = new BPlusTree(3,new ArenaAllocator(1024));
        tree.insert(3,4);
        assertEquals(-1,tree.search(3));
    }
}

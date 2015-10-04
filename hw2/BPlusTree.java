import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Map.Entry;
import java.util.Stack;

/**
 * BPlusTree Class Assumptions: 1. No duplicate keys inserted 2. Order D:
 * D<=number of keys in a node <=2*D 3. All keys are non-negative
 * TODO: Rename to BPlusTree
 */
public class BPlusTree<K extends Comparable<K>, T> {

    public Node<K,T> root;
    public static final int D = 2;

    /**
     * TODO Search the value for a specific key
     *
     * @param key
     * @return value
     */
    public T search(K key) {
        if (root == null)
            return null;
        if (root.isLeafNode) {
            for (int i = 0; i < root.keys.size(); i++) {
                if (root.keys.get(i).equals(key)) {
                    return ((LeafNode<K,T>) root).values.get(i);
                }
            }
        } else {
            Node<K, T> node = root;
            ArrayList<K> keys = root.keys;

            //This while used to find the matched leafNode
            while (!node.isLeafNode) {
                IndexNode<K, T> index = (IndexNode<K, T>) node;
                int i = 0;
                for (; i < keys.size(); i++) {
                    K k = keys.get(i);
                    if (k.compareTo(key) > 0) {
                        node = index.children.get(i);
                        keys = node.keys;
                        break;
                    }
                }
                if (i == keys.size()) {
                    node = index.children.get(i);
                    keys = node.keys;
                }
            }
            for (int i = 0; i < node.keys.size(); i++) {
                if (node.keys.get(i).equals(key)) {
                    return ((LeafNode<K,T>) node).values.get(i);
                }
            }
        }
        return null;
    }

    /**
     * TODO Insert a key/value pair into the BPlusTree
     *
     * @param key
     * @param value
     */
    public void insert(K key, T value) {
        if (root == null) {
            root = new LeafNode<K, T>(key, value);
            return;
        }
        if (root.isLeafNode) {
            ((LeafNode<K,T>) root).insertSorted(key, value);
            if (root.isOverflowed()) {
                Entry<K, Node<K, T>> entry= splitLeafNode((LeafNode<K,T>) root);
                Node<K,T> rightChild = entry.getValue();
                ((LeafNode<K,T>)root).nextLeaf = (LeafNode<K,T>)rightChild;
                ((LeafNode<K,T>)rightChild).previousLeaf = (LeafNode<K,T>)root;
                Node<K, T> newRoot = new IndexNode<K, T>(entry.getKey(), root, rightChild);
                root = newRoot;
            }
        } else {
            //stack save the path nodes;
            Stack<IndexNode<K, T>> path = new Stack<IndexNode<K, T>>();
            Node<K, T> node = root;
            ArrayList<K> keys = root.keys;

            //This while used to find the matched leafNode
            while (!node.isLeafNode) {
                IndexNode<K, T> index = (IndexNode<K,T>) node;
                path.push(index);
                int i = 0;
                for (; i < keys.size(); i++) {
                    K k = keys.get(i);
                    if (k.compareTo(key) > 0) {
                        node = index.children.get(i);
                        keys = node.keys;
                        break;
                    }
                }
                if (i == keys.size()) {
                    node = index.children.get(i);
                    keys = node.keys;
                }
            }
            ((LeafNode<K,T>) node).insertSorted(key, value);

            //
            if (node.isOverflowed()) {
                Entry<K, Node<K, T>> entry= splitLeafNode((LeafNode<K,T>) node);
                K newKey = entry.getKey();
                boolean splitRoot = false;
                while (!path.empty()) {
                    IndexNode<K, T> index = path.pop();

                    int idx = 0;
                    for (K k : index.keys) {
                        if (k.compareTo(newKey) > 0) {
                            index.insertSorted(entry, idx);
                            break;
                        }
                        idx++;
                    }
                    if (idx == index.keys.size()) {
                        index.insertSorted(entry, idx);
                    }
                    if (index.isOverflowed()) {
                        entry= splitIndexNode((IndexNode<K,T>) index);
                        newKey = entry.getKey();
                        if (path.empty())
                            splitRoot = true;
                    } else {
                        break;
                    }
                }
                if (splitRoot) {
                    Node<K, T> newRoot = new IndexNode<K, T>(newKey, root, entry.getValue());
                    root = newRoot;
                }
            }
        }
    }

    /**
     * TODO Split a leaf node and return the new right node and the splitting
     * key as an Entry<slitingKey, RightNode>
     *
     * @param leaf, any other relevant data
     * @return the key/node pair as an Entry
     */
    public Entry<K, Node<K,T>> splitLeafNode(LeafNode<K,T> leaf) {
        int len = leaf.keys.size();
        int mid = len / 2;
        K key = leaf.keys.get(mid);
        LeafNode<K, T> rightNode = new LeafNode<K, T>(leaf.keys.subList(mid, len),
                leaf.values.subList(mid, len));

        leaf.keys = new ArrayList<K>(leaf.keys.subList(0, mid));
        leaf.values = new ArrayList<T>(leaf.values.subList(0, mid));

        rightNode.nextLeaf = leaf.nextLeaf;
        rightNode.previousLeaf = leaf;
        leaf.nextLeaf = rightNode;

        return new AbstractMap.SimpleEntry<K, Node<K, T>>(key, rightNode);
    }

    /**
     * TODO split an indexNode and return the new right node and the splitting
     * key as an Entry<slitingKey, RightNode>
     *
     * @param index, any other relevant data
     * @return new key/node pair as an Entry
     */
    public Entry<K, Node<K,T>> splitIndexNode(IndexNode<K,T> index) {
        int len = index.keys.size();
        int mid = len / 2;
        K key = index.keys.get(mid);
        IndexNode<K, T> rightNode = new IndexNode<K, T>(index.keys.subList(mid + 1, len),
                index.children.subList(mid + 1, len + 1));
        index.keys = new ArrayList<K>(index.keys.subList(0, mid));
        index.children = new ArrayList<Node<K, T>>(index.children.subList(0, mid + 1));

        return new AbstractMap.SimpleEntry<K, Node<K, T>>(key, rightNode);
    }

    /**
     * TODO
     *
     * @param key
     * @param node
     * @return the leafNode of key
     */
    private LeafNode<K,T> searchAndDelete(K key, Node<K,T> node, Stack<Entry<Node<K,T>, Integer>> stack) {
        if (node == null) {
            return null;
        }
        else {
            if (node.isLeafNode) {
                LeafNode<K,T> leafNode = (LeafNode<K,T>)node;
                ArrayList<K> keys = leafNode.keys;
                ArrayList<T> values = leafNode.values;
                for (int i = 0; i < keys.size(); i++) {
                    K k = keys.get(i);
                    if (k.equals(key)) {
                        values.remove(i);
                        keys.remove(i);
                        return leafNode;
                    }
                }
                return null;
            }
            else {
                IndexNode<K,T> indexNode = (IndexNode<K,T>)node;
                ArrayList<K> keys = indexNode.keys;
                ArrayList<Node<K,T>> children = indexNode.children;
                int i;
                for (i = 0; i < keys.size(); i++) {
                    K k = keys.get(i);
                    if (key.compareTo(k) < 0) {
                        break;
                    }
                }
                Entry<Node<K,T>, Integer> entry = new AbstractMap.SimpleEntry<>(node, i);
                stack.push(entry);
                node = children.get(i);
                return searchAndDelete(key, node, stack);
            }
        }
    }

    /**
     * TODO Delete a key/value pair from this B+Tree
     *
     * @param key
     */
    public void delete(K key) {
        Stack<Entry<Node<K,T>, Integer>> stack = new Stack<>();
        LeafNode<K,T> leafNode = searchAndDelete(key, root, stack);
        if (leafNode != null && leafNode.isUnderflowed()) {
            Entry<Node<K,T>, Integer> entry = stack.pop();
            IndexNode<K,T> parent = (IndexNode<K,T>)entry.getKey();
            int index = entry.getValue();
            LeafNode<K,T> left = (index==0)?null:leafNode.previousLeaf;
            LeafNode<K,T> right = leafNode.nextLeaf;
            handleLeafNodeUnderflow(left, right, parent, index);
            while (parent.isUnderflowed()) {
            	if (stack.isEmpty()) return;
                entry = stack.pop();
                parent = (IndexNode<K,T>)entry.getKey();
                index = entry.getValue();
                IndexNode<K,T> leftIndex = (index==0)?null:(IndexNode<K,T>)parent.children.get(index-1);
                IndexNode<K,T> rightIndex = (index==parent.children.size())?null:(IndexNode<K,T>)parent.children.get(index+1);
                handleIndexNodeUnderflow(leftIndex, rightIndex, parent, index);
            }
        }
    }

    /**
     * TODO Handle LeafNode Underflow (merge or redistribution)
     *
     * @param left
     *            : the smaller node
     * @param right
     *            : the bigger node
     * @param parent
     *            : their parent index node
     * @param index
     * 			  : the splitkey position in parent
     * @return the splitkey position in parent if merged so that parent can
     *         delete the splitkey later on. -1 otherwise
     */
    public void handleLeafNodeUnderflow(LeafNode<K,T> left, LeafNode<K,T> right,
                                        IndexNode<K,T> parent, int index) {
        if (left == null) {
            LeafNode<K,T> current = right.previousLeaf;
            int rsize = right.keys.size();
            int csize = current.keys.size();
            if (rsize >= (2*D-csize)) {
                int shift = (rsize+csize) / 2 - csize;
                for (int i = 0; i < shift; i++) {
                    K key = right.keys.remove(i);
                    T value = right.values.remove(i);
                    current.insertSorted(key, value);
                }
                parent.keys.set(index, right.keys.get(0));
                return;
            }
        }
        else {
            LeafNode<K,T> current = left.nextLeaf;
            int lsize = left.keys.size();
            int csize = current.keys.size();
            if (lsize >= (2*D-csize)) {
            	int size = lsize+csize;
                int shift = (size%2==0)?(size/2-csize):(size/2-csize+1);
                for (int i = lsize - 1; i >= lsize - shift; i-- ) {
                    K key = left.keys.remove(i);
                    T value = left.values.remove(i);
                    current.insertSorted(key, value);
                }
                parent.keys.set(index-1, current.keys.get(0));
                return;
            }
            else {
                if (right != null) {
                    int rsize = right.keys.size();
                    if (rsize >= (2*D-csize)) {
                        int shift = (rsize+csize) / 2 - csize;
                        for (int i = 0; i < shift; i++) {
                            K key = right.keys.remove(i);
                            T value = right.values.remove(i);
                            current.insertSorted(key, value);
                        }
                        parent.keys.set(index, right.keys.get(0));
                        return;
                    }
                }
            }
        }
        if (left != null) {
            parent.keys.remove(index-1);
            LeafNode<K,T> child = (LeafNode<K,T>)parent.children.remove(index);
            for (int i = 0; i < child.keys.size(); i++) {
                left.insertSorted(child.keys.get(i), child.values.get(i));
            }
            left.nextLeaf = child.nextLeaf;
            return;
        }
        else {
            parent.keys.remove(index);
            LeafNode<K,T> child = (LeafNode<K,T>)parent.children.remove(index);
            for (int i = 0; i < child.keys.size(); i++) {
                right.insertSorted(child.keys.get(i), child.values.get(i));
            }
            right.previousLeaf = child.previousLeaf;
            return;
        }
    }

    /**
     * TODO Handle IndexNode Underflow (merge or redistribution)
     *
     * @param leftIndex
     *            : the smaller node
     * @param rightIndex
     *            : the bigger node
     * @param parent
     *            : their parent index node
     * @return the splitkey position in parent if merged so that parent can
     *         delete the splitkey later on. -1 otherwise
     */
    public void handleIndexNodeUnderflow(IndexNode<K,T> leftIndex,
                                        IndexNode<K,T> rightIndex, IndexNode<K,T> parent, int index) {
    	IndexNode<K, T> current = (IndexNode<K,T>)parent.children.get(index);
    	if (leftIndex == null) {
    		int rsize = rightIndex.keys.size();
    		int csize = current.keys.size();
    		if (rsize >= (2*D-csize)) {
                int shift = (rsize+csize) / 2 - csize;
                for (int i = 0; i < shift; i++) {
                    K key = rightIndex.keys.remove(i);
                    K parentKey = parent.keys.get(index);
                    parent.keys.set(index, key);
                    Node<K, T> child = rightIndex.children.remove(i);
                    Entry<K,Node<K,T>> entry = new AbstractMap.SimpleEntry<>(parentKey, child);
                    current.insertSorted(entry, current.keys.size());
                }
                return;
            }
    	}
    	else {
    		int lsize = leftIndex.keys.size();
            int csize = current.keys.size();
            if (lsize >= (2*D-csize)) {
            	int size = lsize+csize;
                int shift = (size%2==0)?(size/2-csize):(size/2-csize+1);
                for (int i = lsize - 1; i >= lsize - shift; i-- ) {
                    K key = leftIndex.keys.remove(i);
                    K parentKey = parent.keys.get(index);
                    parent.keys.set(index-1, key);
                    Node<K, T> child = leftIndex.children.remove(i+1);
                    Entry<K,Node<K,T>> entry = new AbstractMap.SimpleEntry<>(parentKey, child);
                    current.insertSorted(entry, current.keys.size());
                }
                return;
            }
            else {
            	if (rightIndex != null) {
            		int rsize = rightIndex.keys.size();
            		if (rsize >= (2*D-csize)) {
                        int shift = (rsize+csize) / 2 - csize;
                        for (int i = 0; i < shift; i++) {
                        	K key = rightIndex.keys.remove(i);
                            K parentKey = parent.keys.get(index);
                            parent.keys.set(index, key);
                            Node<K, T> child = rightIndex.children.remove(i);
                            Entry<K,Node<K,T>> entry = new AbstractMap.SimpleEntry<>(parentKey, child);
                            current.insertSorted(entry, current.keys.size());
                        }
                        return;
                    }
            	}
            }
    	}
    	if (leftIndex != null) {
    		K key = parent.keys.remove(index-1);
    		leftIndex.keys.add(key);
    		Node<K,T> child = parent.children.remove(index);
    		leftIndex.keys.addAll(child.keys);
    		leftIndex.children.addAll(((IndexNode<K,T>)child).children);
            if (parent.keys.size() == 0) {
            	root = leftIndex;
            }
            return;
    	}
    	else {
    		K key = parent.keys.remove(index);
    		rightIndex.keys.add(0, key);
    		Node<K,T> child = parent.children.remove(index);
    		rightIndex.keys.addAll(0, child.keys);
    		rightIndex.children.addAll(0, ((IndexNode<K,T>)child).children);
            if (parent.keys.size() == 0) {
            	root = rightIndex;
            }
            return;
    	}
    }

}


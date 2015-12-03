package kuvaldis.algorithm.structure;

/**
 * Sorted set based on B-tree implementation
 */
public class BTreeSet {

    // even and greater than 2
    private static final int M = 4;

    private Node root;
    private int height;
    private int size;

    // for the purpose of simplicity I use one additional cell for children and one for values
    private static final class Node {
        private final int[] values = new int[M + 1];
        private int length;
        private Node[] children = new Node[M + 2];
        public Node parent;
    }

    public BTreeSet() {
        root = new Node();
    }

    public void add(final int value) {
        if (contains(value)) {
            return;
        }
        if (root.length == 0) height++;
        final Node newRoot = insert(root, value);
        size++;
        if (newRoot == null) return;
        root = newRoot;
        height++;
    }


    // attempt to insert value into the node.
    // if level is equal to tree height, then we couldn't manage to insert it to existing nodes
    // returns non null value as an additional child node
    private Node insert(final Node insertIntoNode, final int value) {
        Node childNewRoot = null;
        // assume M = 4 and the node is like []1[]8[]16[]18[]
        // there are 4 compares (with 1, 8, 16, 18) and the last case when it's higher than 18,
        // means we should go to the last child
        int insertPosition;
        for (insertPosition = 0; insertPosition <= insertIntoNode.length; insertPosition++) {
            if (insertPosition == insertIntoNode.length || value < insertIntoNode.values[insertPosition]) {
                final Node child = insertIntoNode.children[insertPosition];
                if (child != null) {
                    childNewRoot = insert(child, value);
                    if (childNewRoot == null) {
                        return null;
                    }
                }
                break;
            }
        }

        // if we are here, then either there is childNewRoot to merge into current or the value should be added into current
        if (childNewRoot != null) {
            insertChildrenRoot(insertIntoNode, childNewRoot, insertPosition);
            if (insertIntoNode.length > M) {
                return split(insertIntoNode);
            }
        } else {
            insertValue(insertIntoNode, value, insertPosition);
            if (insertIntoNode.length > M) {
                return split(insertIntoNode);
            }
        }
        return null;
    }

    // runs when children node has been split on two and new root should be inserted to the current node
    private void insertChildrenRoot(final Node insertIntoNode, final Node childrenRoot, final int position) {
        for (int i = childrenRoot.length; i > position; i--) {
            childrenRoot.values[i] = childrenRoot.values[i - 1];
            childrenRoot.children[i + 1] = childrenRoot.children[i];
        }
        insertIntoNode.values[position] = childrenRoot.values[0];
        insertIntoNode.children[position] = childrenRoot.children[0];
        insertIntoNode.children[position + 1] = childrenRoot.children[1];
        insertIntoNode.length++;
    }

    // if we insert value, then there is not children for current node
    private void insertValue(final Node node, final int value, final int position) {
        //noinspection ManualArrayCopy
        for (int i = node.length; i > position; i--) {
            node.values[i] = node.values[i - 1];
        }
        node.values[position] = value;
        node.length++;
    }

    // splits on two halves and returns new root node
    /*
                                              []3[]
                                             /     \
           []1[]2[]3[]4[]5[] --------> []1[]2[] | []4[]5[]

     */
    private Node split(final Node node) {
        final Node secondHalf = new Node();
        secondHalf.length = M / 2;
        node.length = M / 2;
        //noinspection ManualArrayCopy
        for (int i = secondHalf.length; i > 0; i--) {
            secondHalf.children[i] = node.children[M / 2 + i + 1];
            secondHalf.values[i - 1] = node.values[M / 2 + i];
        }
        secondHalf.children[0] = node.children[M / 2 + 1];
        final Node newParent = new Node();
        newParent.length = 1;
        newParent.children[0] = node;
        newParent.children[1] = secondHalf;
        newParent.values[0] = node.values[M / 2];
        node.parent = newParent;
        secondHalf.parent = newParent;
        return newParent;
    }

    public int size() {
        return size;
    }

    public int height() {
        return height;
    }

    public boolean contains(final int value) {
        Node node = root;
        int i = 0;
        while (true) {
            if (i == node.length || value < node.values[i]) {
                if (node.children[i] == null) {
                    break;
                } else {
                    node = node.children[i];
                    i = 0;
                }
            } else if (value == node.values[i]) {
                return true;
            } else {
                i++;
            }
        }
        return false;
    }

    public void delete(final int value) {
        if (root == null) {
            return;
        }
        Node node = root;
        int deletePosition = 0;
        int level = 0;
        while (true) {
            if (deletePosition == node.length || value < node.values[deletePosition]) {
                if (node.children[deletePosition] == null) {
                    return;
                } else {
                    node = node.children[deletePosition];
                    deletePosition = 0;
                    level++;
                }
            } else if (value == node.values[deletePosition]) {
                break;
            } else {
                deletePosition++;
            }
        }
        doDelete(node, level, deletePosition);
    }

    // 3. The node is not leaf
    //     a. Left child has more then M/2 values. Swap node's value and left's child right most value. Remove new left most value:
    /*
                 delete
                 |
            []2[]6[]                           []2[]5[]
          /    |       \                     /    |       \
        ... []3[]4[]5[] []7[]8[]   --->    ... []3[]4[]6[] []7[]8[]
                                                       ^
                                                       delete
     */
    //     b. Right child has more then M/2 values. Swap node's value and right's child left most value. Remove:
    /*
                 delete
                 |
            []2[]5[]                           []2[]6[]
          /    |     \                        /    |    \
        ... []3[]4[]  []6[]7[]8[]   --->    ... []3[]4[]  []5[]7[]8[]
                                                            ^
                                                            delete
     */
    //     c. No such children. Merge left child, value, right child to one node and remove the value:
    /*
                 delete
                 |
            []2[]5[]                           []2[]
          /    |     \                        /       \
        ... []3[]4[]  []6[]7[]   --->    ...    []3[]4[]5[]6[]7[]
                                                        ^
                                                        delete
     */
    // 3 cases
    private void doDelete(final Node node, final int level, final int deletePosition) {
        deleteCase1(node, level, deletePosition);
    }

    // 1. The node is leaf and it has more then M/2 values. Just delete position.
    private void deleteCase1(final Node node, final int level, final int deletePosition) {
        if (level == height - 1 && node.length > M / 2) {
            deleteLeafPosition(node, deletePosition);
        } else {
            deleteCase2(node, level, deletePosition);
        }
    }

    // 2. The node is leaf and it has less equal to M/2 values:
    private void deleteCase2(final Node node, final int level, final int deletePosition) {
        if (level == height - 1 && node.length <= M / 2) {
            deleteCase2a(node, level, deletePosition);
        } else {
            deleteCase3(node, level, deletePosition);
        }
    }

    // 2.a. There is a sibling with more then M/2 values. Do the following (the sibling is right).
    //      Move parent value to new value in the current node, replace parent value with right node's left most value
    //      and remove it from sibling. It would be case number one then:
    /*
             []2[]5[]                      []2[]6[]
           /    |    \                   /     |      \
         ... []3[]4[] []6[]7[]8[] ---> ... []3[]4[]5[] []7[]8[]
                  ^                             ^
                  delete                        delete (now it's case number 1)
     */
    private void deleteCase2a(final Node node, final int level, final int deletePosition) {
        final int nodeParentIndex = nodeParentIndex(node);
        final Node leftSibling = leftSibling(node, nodeParentIndex);
        final Node rightSibling = rightSibling(node, nodeParentIndex);
        if (leftSibling != null && leftSibling.length > M / 2) {
            insertValue(node, node.parent.values[nodeParentIndex - 1], 0);
            node.parent.values[nodeParentIndex - 1] = leftSibling.values[leftSibling.length - 1];
            deleteLeafPosition(leftSibling, leftSibling.length - 1);
        } else if (rightSibling != null && rightSibling.length > M / 2) {
            insertValue(node, node.parent.values[nodeParentIndex], node.length);
            node.parent.values[nodeParentIndex] = rightSibling.values[0];
            deleteLeafPosition(rightSibling, 0);
        } else {
            deleteCase2b(node, level, deletePosition);
        }
    }

    // 2.b. There is no sibling with more then M/2 values. Merge shared parent, the node and right or left sibling,
    //      like so (there is right sibling):
    /*
             []2[]5[]                     []2[]
           /    |    \                  /      \
         ... []3[]4[] []6[]7[] --->   ...  []3[]4[]5[]6[]7[]
                  ^                             ^
                  delete                        delete
     */
    private void deleteCase2b(final Node node, final int level, final int deletePosition) {

    }


    private void deleteCase3(final Node node, final int level, final int deletePosition) {

    }

    // only for leaves, so no children
    private void deleteLeafPosition(final Node node, final int deletePosition) {
        //noinspection ManualArrayCopy
        for (int i = deletePosition; i <= node.length; i++) {
            node.values[i] = node.values[i + 1];
        }
        node.length--;
    }

    private int nodeParentIndex(final Node node) {
        if (node.parent == null) {
            return -1;
        }
        final Node parent = node.parent;
        for (int i = 0; i <= parent.length; i++) {
            if (parent.children[i] == node) {
                return i;
            }
        }
        return -1;
    }

    private Node leftSibling(final Node node, final int parentIndex) {
        if (parentIndex > 0) {
            return node.parent.children[parentIndex - 1];
        }
        return null;
    }

    private Node rightSibling(final Node node, final int parentIndex) {
        if (parentIndex >= 0 && parentIndex < node.parent.length) {
            return node.parent.children[parentIndex + 1];
        }
        return null;
    }
}

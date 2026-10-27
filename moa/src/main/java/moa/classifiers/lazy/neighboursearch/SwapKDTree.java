package moa.classifiers.lazy.neighboursearch;

import com.yahoo.labs.samoa.instances.Instance;
import com.yahoo.labs.samoa.instances.Instances;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeSet;

public class SwapKDTree extends NearestNeighbourSearch {

    public static class Node {
        /** Index point **/
        int index;
        /** Value split **/
        double value;
        /** The left child of this node. */
        Node left;
        /** The right child of this node. */
        Node right;
        /** The axis used to split the data at this node. */
        int axis;
        /** The total number of nodes in the subtree. */
        int treeSize;
        /** The number of invalid nodes in the subtree. */
        int invalidnum;
        /** Indicates whether this node has been deleted. */
        Boolean deleted;
        /** Indicates whether the entire tree has been deleted. */
        Boolean treeDeleted;

        /**
         * Initializes a node with the specified instance and axis.
         */
        Node(int index, int axis, double value) {
            this.index = index;
            this.axis = axis;
            this.value = value;

            left = null;
            right = null;

            treeSize = 0;
            invalidnum = 0;

            treeDeleted = false;
            deleted = false;
        }

        /**
         * Returns the value of the point corresponding to the specified axis.
         *
         * @return the point's value on the corresponding axis
         */
        double getValue() { return value; }

        /**
         * Returns the index of instance in queue.
         * @return the index
         */
        int getIndex() { return index; }

        /**
         * Checks if the node is a leaf.
         *
         * @return true if the node has no left or right child, false otherwise
         */
        boolean isLeaf() { return left == null && right == null; }

    }

    public static class CircularQueue {
        // fixed-size array
        private final Instance[] arr;
        // index of last element
        private int last;
        // index of front element
        private int front;
        // current number of elements
        private int size;
        // maximum capacity
        private final int capacity;

        public CircularQueue(int cap) {
            capacity = cap;
            arr = new Instance[capacity];
            front = 0;
            size = 0;
        }

        // Insert an element at the rear
        public void enqueue(Instance x) {
            if (size == capacity) {
                System.out.println("Queue is full!");
                return;
            }
            int rear = (front + size) % capacity;
            arr[rear] = x;
            last = rear;
            size++;
        }

        // Remove an element from the front
        public Instance dequeue() {
            if (size == 0) {
                System.out.println("Queue is empty!");
                return null;
            }
            Instance res = arr[front];
            front = (front + 1) % capacity;
            size--;
            return res;
        }

        public int getIndex() {
            return last;
        }

        public Instance getInstance(int index){
            return arr[index];
        }

    }

    private static class Neighbor implements Comparable<Neighbor> {
        int index;
        double distance;

        Neighbor(int index, double distance) {
            this.index = index;
            this.distance = distance;
        }

        @Override
        public int compareTo(Neighbor other) {
            int cmp = Double.compare(this.distance, other.distance);

            if (cmp == 0) {
                cmp = Integer.compare(this.index, other.index);
            }

            return cmp;
        }
    }

    public static class MetricsTree {
        long treeSize = 0;
        long leftTreeSize = 0;
        long rightTreeSize = 0;
        long heightTree = 0;
        long numDeletedNodes = 0;

        public String toString() {
            return treeSize + "," + leftTreeSize + "," + rightTreeSize + "," + heightTree + "," + numDeletedNodes;
        }

        public Map<String, Object> getMetrics() {
            Map<String, Object> metrics = new LinkedHashMap<>();
            metrics.put("tree_size", treeSize);
            metrics.put("left_tree_Size", leftTreeSize);
            metrics.put("right_tree_size", rightTreeSize);
            metrics.put("height_tree", heightTree);
            metrics.put("num_deleted_nodes", numDeletedNodes);
            return metrics;
        }
    }

    /** Metrics collected from the tree. **/
    public MetricsTree metricsInitTree = new MetricsTree();
    public MetricsTree metricsNewTree = new MetricsTree();

    /** The root of the tree **/
    private Node treeInit;
    /** The root of the tree **/
    private Node treeNew;
    /** The root of the tree **/
    private Node root;
    /** Window size **/
    private final int windowSize;
    /** Index instances **/
    private final CircularQueue queue;
    /** Number of dimensions **/
    private int numDim;
    /** Distances kNN **/
    private double[] m_DistanceList;
    /** Neighbors **/
    TreeSet<Neighbor> neighbors;

    /**
     * Default construtor.
     */
    public SwapKDTree() {
        this(1000);
    }

    /**
     * Set window size constructor.
     * @param windowSize window size
     */
    public SwapKDTree(int windowSize) {
        this.windowSize = windowSize;
        this.queue = new CircularQueue(windowSize);
        this.neighbors = new TreeSet<>();
    }

    @Override
    public Instance nearestNeighbour(Instance target) throws Exception {
        return (kNearestNeighbours(target, 1)).instance(0);
    }

    @Override
    public Instances kNearestNeighbours(Instance target, int k) throws Exception {

        // Essa busca ainda não trata direito valores repetidos, não sei se a ordem importa.
        // kNN search
        MyHeap heapInit = new MyHeap(k);
        findNearestNeighbours(target, treeInit, k, heapInit, 0);
        MyHeapElement h;
        while (heapInit.size() > 0) {
            h = heapInit.get();
            neighbors.add(new Neighbor(h.index, h.distance));
        }

        if (treeNew != null) {
            MyHeap heapNew = new MyHeap(k);
            findNearestNeighbours(target, treeNew, k, heapNew, 0);
            while (heapNew.size() > 0) {
                h = heapNew.get();
                neighbors.add(new Neighbor(h.index, h.distance));
            }
        }

        // Merge instances
        int i = 0;
        Instances result = new Instances(m_Instances, k);
        m_DistanceList = new double[k];
        for (Neighbor neighbor : neighbors) {
            if (i >= k) break;
            result.add(queue.getInstance(neighbor.index));
            m_DistanceList[i++] = neighbor.distance;
        }
        neighbors.clear();

        return result;
    }

    private void findNearestNeighbours(Instance target, Node node, int k, MyHeap heap, int depth) throws Exception {

        // Se a subárvore já está apagada nem ve.
        if (node == null || node.treeDeleted) {
            return;
        }

        Node best, other;
        if (target.value(node.axis) <= node.value) {
            best = node.left;
            other = node.right;
        } else {
            best = node.right;
            other = node.left;
        }

        findNearestNeighbours(target, best, k, heap, depth + 1);

        // Computes the distance and adds the node to the heap if it is closer than the current neighbors.
        if (!node.deleted) {
            double distNode;
            if (heap.size() < k) {
                distNode = m_DistanceFunction.distance(
                        queue.getInstance(node.getIndex()),
                        target,
                        Double.POSITIVE_INFINITY);
                heap.put(node.getIndex(), distNode);
            } else {
                MyHeapElement worst = heap.peek();
                distNode = m_DistanceFunction.distance(
                        queue.getInstance(node.getIndex()),
                        target,
                        worst.distance);
                if (distNode < worst.distance) {
                    heap.putBySubstitute(node.getIndex(), distNode);
                } else if (distNode == worst.distance) {
                    heap.putKthNearest(node.getIndex(), distNode);
                }
            }
        }

        // Computes the squared distance to the splitting hyperplane.
        double planeDist = m_DistanceFunction.sqDifference(
                node.axis,
                target.value(node.axis),
                node.value);

        // If the distance is within the search radius, checks the other subtree.
        if (heap.size() < k || planeDist <= heap.peek().distance) {
            findNearestNeighbours(target, other, k, heap, depth + 1);
        }
    }


    @Override
    public double[] getDistances() throws Exception {
        if (m_Instances == null || m_DistanceList == null)
            throw new Exception("The tree has not been supplied with a set of "
                    + "instances or getDistances() has been called "
                    + "before calling kNearestNeighbours().");
        return m_DistanceList;
    }

    /**
     * SwapTree update
     * @param inst		the instance to add
     * @throws Exception
     */
    @Override
    public void update(Instance inst) throws Exception {
        checkMissing(inst);
        // Init number of dimensions
        if (numDim == 0) numDim = inst.numAttributes() - 1;

        if (treeInit == null || treeInit.treeSize < windowSize) {
            queue.enqueue(inst);
            treeInit = insert(treeInit, inst, queue.getIndex(), -1, 0, metricsInitTree);
            updateMetrics(treeInit, treeNew); // Collect metrics
            return;
        }

        // Sliding window
        Instance removed = queue.dequeue();
        if (removed != null)
            if (!lazyDelete(treeInit, removed))
                throw new Exception("Failed to delete the instance from the tree.");

        queue.enqueue(inst);
        treeNew = insert(treeNew, inst, queue.getIndex(), -1, 0, metricsNewTree);

        if (treeInit.treeDeleted) {
            // Swap tree
            treeInit = treeNew;
            treeNew = null;

            metricsInitTree.heightTree = metricsNewTree.heightTree;
            metricsNewTree.heightTree = 0;
        }

        updateMetrics(treeInit, treeNew); // Collect metrics
    }

    /**
     * Inserts an instance recursively into the KD-tree.
     *
     * @param node       current node
     * @param inst       instance to insert
     * @param fatherAxis axis used by the parent node
     * @return the updated node
     */
    private Node insert(Node node, Instance inst, int index, int fatherAxis, int depth, MetricsTree metricsTree) {
        // Case 0.
        if (node == null) {
            int axis = (fatherAxis + 1) % numDim;
            // node = new Node(inst, axis);
            node = new Node(index, axis, inst.value(axis));
            updateTree(node);
            // Metric
            if (metricsTree.heightTree < depth)
                metricsTree.heightTree = depth;
            return node;
        }
        // Decide which subtree to follow
        if (inst.value(node.axis) <= node.value)
            node.left = insert(node.left, inst, index, node.axis, depth + 1, metricsTree);
        else
            node.right = insert(node.right, inst, index, node.axis, depth + 1, metricsTree);

        // Update stats new node
        updateTree(node);
        // Nota: deixar isso comentado... Pode ser util depois...
        // if (criterionCheck(node)) {
        //   node = rebuildTree(node);
        // }
        return node;
    }

    /**
     * Updates the size and number of inactive nodes in the subtree.
     *
     * @param node root node of the subtree
     */
    private void updateTree(Node node) {
        if (node == null)
            return;

        int leftSize = 0;
        int rightSize = 0;

        int leftInvalid = 0;
        int rightInvalid = 0;

        if (node.left != null) {
            leftSize = node.left.treeSize;
            leftInvalid = node.left.invalidnum;
        }

        if (node.right != null) {
            rightSize = node.right.treeSize;
            rightInvalid = node.right.invalidnum;
        }

        // Total number of nodes in the subtree.
        node.treeSize = leftSize + rightSize + 1;

        // Total number of inactive nodes in the subtree,
        // including the current node if it is inactive.
        node.invalidnum = leftInvalid + rightInvalid;

        if (node.deleted)
            node.invalidnum++;

        boolean leftDeleted =
                node.left == null || node.left.treeDeleted;

        boolean rightDeleted =
                node.right == null || node.right.treeDeleted;

        node.treeDeleted =
                node.deleted && leftDeleted && rightDeleted;
    }

    private void updateMetrics(Node treeInit, Node treeNew) {
        // Metrics init
        metricsInitTree.numDeletedNodes = treeInit.invalidnum;
        metricsInitTree.treeSize = treeInit.treeSize;
        metricsInitTree.leftTreeSize =
                treeInit.left != null
                        ? treeInit.left.treeSize
                        : 0;

        metricsInitTree.rightTreeSize =
                treeInit.right != null
                        ? treeInit.right.treeSize
                        : 0;
        // Metrics
        if (treeNew != null) {
            metricsNewTree.numDeletedNodes = treeNew.invalidnum;
            metricsNewTree.treeSize = treeNew.treeSize;
            metricsNewTree.leftTreeSize =
                    treeNew.left != null
                            ? treeNew.left.treeSize
                            : 0;

            metricsNewTree.rightTreeSize =
                    treeNew.right != null
                            ? treeNew.right.treeSize
                            : 0;
        } else {
            metricsNewTree.numDeletedNodes = 0;
            metricsNewTree.treeSize = 0;
            metricsNewTree.leftTreeSize = 0;
            metricsNewTree.rightTreeSize = 0;
        }
    }

    private boolean lazyDelete(Node node, Instance inst) {
        if (node == null)
            return false;

        // Ignore inactive nodes
        if (!node.deleted) {
            if (checkEqualInstance(queue.getInstance(node.getIndex()), inst)) {
                // Node founded in tree
                node.deleted = true;
                updateTree(node);
                return true;
            }
        }

        boolean deleted;
        if (inst.value(node.axis) <= node.value) {
            deleted = lazyDelete(node.left, inst);
        } else {
            deleted = lazyDelete(node.right, inst);
        }

        if (deleted)
            updateTree(node);

        return deleted;
    }

    public Instance search(Instance target) {
        Node node = search(target, treeInit);

        if (node == null) {
            node = search(target, treeNew);
        }

        return node == null ? null : queue.getInstance(node.getIndex());
    }

    private Node search(Instance target, Node node) {
        // Nó inexistente ou subárvore inteira apagada
        if (node == null || node.treeDeleted) {
            return null;
        }

        // Verifica o nó somente se ele não estiver apagado
        if (!node.deleted &&
                checkEqualInstance(target, queue.getInstance(node.getIndex()))) {
            return node;
        }

        if (target.value(node.axis) <= node.value) {
            return search(target, node.left);
        } else {
            return search(target, node.right);
        }
    }

    @Override
    public void setInstances(Instances insts) throws Exception {
        super.setInstances(insts);
        m_DistanceFunction.setDontNormalize(true);
        m_DistanceFunction.setInstances(insts);

    }

    /// //////// Auxiliar Functions

    /**
     * Prints the tree in Graphviz DOT format.
     */
    public void printTree() {
        System.out.println("digraph KDTree {");
        System.out.println("    node [shape=box];");
        System.out.println("    compound=true;");
        System.out.println("    rankdir=TB;");

        System.out.println("    subgraph cluster_init {");
        System.out.println("        label=\"Árvore Inicial\";");
        printInorder(treeInit); // primeira árvore
        System.out.println("    }");

        if (treeNew != null) {
            System.out.println("    subgraph cluster_new {");
            System.out.println("        label=\"Árvore Nova\";");
            printInorder(treeNew); // segunda árvore
            System.out.println("    }");
        }

        System.out.println("}");
    }

    /**
     * Prints the nodes and edges in DOT format using inorder traversal.
     *
     * @param node the current node
     */
    private void printInorder(Node node) {
        if (node == null) {
            return;
        }

        printInorder(node.left);

        String color = node.deleted ? "red" : "black";

        System.out.printf(
                "    n%d [label=\"axis: %d\\nsplit: %s\", color=\"%s\"];%n",
                System.identityHashCode(node),
                node.axis,
                node.getValue(),
                color
        );

        if (node.left != null) {
            System.out.printf(
                    "    n%d -> n%d [label=\"left\"];%n",
                    System.identityHashCode(node),
                    System.identityHashCode(node.left)
            );
        }

        if (node.right != null) {
            System.out.printf(
                    "    n%d -> n%d [label=\"right\"];%n",
                    System.identityHashCode(node),
                    System.identityHashCode(node.right)
            );
        }

        printInorder(node.right);
    }

    /**
     * Checks if there is any missing value in the given
     * instance.
     *
     * @param ins The instance to check missing values in.
     * @throws Exception If there is a missing value in the
     *                   instance.
     */
    private void checkMissing(Instance ins) throws Exception {
        for (int j = 0; j < ins.numValues(); j++) {
            if (ins.index(j) != ins.classIndex())
                if (ins.isMissingSparse(j)) {
                    System.out.println(ins);
                    throw new Exception("ERROR: KDTree can not deal with missing "
                            + "values. Please run ReplaceMissingValues filter "
                            + "on the dataset before passing it on to the KDTree.");
                }
        }
    }

    /**
     * Checks if instance A is equal B instance.
     *
     * @param a The instance A.
     * @param b The instance B.
     */
    private boolean checkEqualInstance(Instance a, Instance b){
        if (a.numAttributes() != b.numAttributes()) return false;
        for (int i = 0; i < numDim; i++) {
            if (a.value(i) != b.value(i)) return false;
        }
        return true;
    }
}

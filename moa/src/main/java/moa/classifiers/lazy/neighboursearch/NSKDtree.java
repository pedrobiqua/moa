package moa.classifiers.lazy.neighboursearch;

import com.yahoo.labs.samoa.instances.Instance;
import com.yahoo.labs.samoa.instances.Instances;

import moa.classifiers.lazy.neighboursearch.kdtrees.KDTreeNode;
import moa.tasks.EvaluateNKDTree;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeSet;

public class NSKDtree extends NearestNeighbourSearch {

    public class MetricsTree {
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

        public long getChildTreeSize() {
            if (leftTreeSize != 0)
                return leftTreeSize;
            else if (rightTreeSize != 0)
                return rightTreeSize;
            else
                return 0;
        }

        public long getTreeSize() {
            return treeSize;
        }

        public long getHeightTree() {
            return heightTree;
        }

        public long getNumDeletedNodes() {
            return numDeletedNodes;
        }
    }

    /** Metrics collected from the tree. **/
    public MetricsTree metricsTree = new MetricsTree();

    /** Search metrics **/
    private EvaluateNKDTree.SearchMetrics searchMetrics = null;

    /** Identifiers of tree nodes marked as deleted. **/
    protected TreeSet<Integer> m_InstDeleted = new TreeSet<>();

    /** Distance function used for nearest-neighbor searches. **/
    protected DistanceFunction m_DistanceFunction = new EuclideanDistance();

    private double[] m_DistanceList;

    /**
     * Index list of the instances of this kdtree. Instances get sorted according
     * to the splits. the nodes of the KDTree just hold their start and end
     * indices
     */
    // protected int[] m_InstList;


    /** Number of dimensions in the data. **/
    private int m_numDim;

    /** Root node of the k-d tree. **/
    private KDTreeNode m_Root;

    @Override
    public Instance nearestNeighbour(Instance target) throws Exception {
        return (kNearestNeighbours(target, 1)).instance(0);
    }

    @Override
    public Instances kNearestNeighbours(Instance target, int k) throws Exception {
        if (metricsTree.treeSize == 0) {
            throw new Exception("The K-d tree was not initialized. Please use the method setInstances(Instances)");
        }

        // Reset metrics
        if (searchMetrics != null) {
            searchMetrics.reset();
        }

        MyHeap heap = new MyHeap(k);
        findNearestNeighbours(target, m_Root, k, heap, 0);

        // Pega as distancias encontradas e os pontos
        Instances neighbours = new Instances(m_Instances, (heap.size() + heap
                .noOfKthNearest()));
        m_DistanceList = new double[heap.size() + heap.noOfKthNearest()];
        int[] indices = new int[heap.size() + heap.noOfKthNearest()];
        int i = indices.length - 1;
        MyHeapElement h;
        while (heap.noOfKthNearest() > 0) {
            h = heap.getKthNearest();
            indices[i] = h.index;
            m_DistanceList[i] = h.distance;
            i--;
        }
        while (heap.size() > 0) {
            h = heap.get();
            indices[i] = h.index;
            m_DistanceList[i] = h.distance;
            i--;
        }
        m_DistanceFunction.postProcessDistances(m_DistanceList);

        for (int idx = 0; idx < indices.length; idx++) {
            neighbours.add(m_Instances.instance(indices[idx]));
        }

        return neighbours;
    }

    private void findNearestNeighbours(Instance target, KDTreeNode node, int k,
                                       MyHeap heap, int depth) throws Exception {

        if (node == null) {
            return;
        }

        /** Collect node visited **/
        if (searchMetrics != null) {
            searchMetrics.nodeVisited();
        }

        KDTreeNode best, other;
        if (target.value(node.m_SplitDim) <= node.m_SplitValue) {
            best = node.m_Left;
            other = node.m_Right;
        } else {
            best = node.m_Right;
            other = node.m_Left;
        }

        findNearestNeighbours(target, best, k, heap, depth + 1);

        // Computes the distance and adds the node to the heap if it is closer than the current neighbors.
        if (!m_InstDeleted.contains(node.m_NodeNumber)) {
            double distNode;
            if (heap.size() < k) {
                distNode = m_DistanceFunction.distance(
                        m_Instances.instance(node.m_NodeNumber),
                        target,
                        Double.POSITIVE_INFINITY);
                heap.put(node.m_NodeNumber, distNode);
            } else {
                MyHeapElement worst = heap.peek();
                distNode = m_DistanceFunction.distance(
                        m_Instances.instance(node.m_NodeNumber),
                        target,
                        worst.distance);
                if (distNode < worst.distance) {
                    heap.putBySubstitute(node.m_NodeNumber, distNode);
                } else if (distNode == worst.distance) {
                    heap.putKthNearest(node.m_NodeNumber, distNode);
                }
            }
        }

        // Computes the squared distance to the splitting hyperplane.
        double planeDist = m_DistanceFunction.sqDifference(
                node.m_SplitDim,
                target.value(node.m_SplitDim),
                node.m_SplitValue);

        // If the distance is within the search radius, checks the other subtree.
        if (heap.size() < k || planeDist <= heap.peek().distance) {
//          backtrack++;
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

    @Override
    public void update(Instance ins) throws Exception {
        checkMissing(ins);
        addInstanceToTree(ins);
        metricsTree.treeSize++;
        updateSubTreeSizes();
    }

    private void addInstanceToTree(Instance inst) {
        int depth = 0;
        KDTreeNode p = m_Root;
        KDTreeNode prev = null;

        while (p != null) {
            prev = p;
            p.m_TreeSize++;
            int axis = depth % m_numDim;
            if (inst.value(axis) <= m_Instances.instance(p.m_NodeNumber).value(axis)) {
                p = p.m_Left;
            } else {
                p = p.m_Right;
            }

            depth++;
        }

//        stats.depthInsert = depth;

        m_Instances.add(inst);
        m_DistanceFunction.update(inst);

        if (m_Root == null) {
            m_Root = new KDTreeNode();
            m_Root.m_NodeNumber = 0;
            m_Root.m_Left = null;
            m_Root.m_Right = null;
            m_Root.m_SplitDim = 0;
            m_Root.m_SplitValue = inst.value(0);
            m_Root.m_TreeSize = 1;
            return;
        }

        int axis = (depth - 1) % this.m_numDim;

        assert prev != null;
        if (inst.value(axis) <= m_Instances.instance(prev.m_NodeNumber).value(axis)) {
            prev.m_Left = new KDTreeNode();
            prev.m_Left.m_NodeNumber = m_Instances.numInstances() - 1;
            prev.m_Left.m_SplitDim = depth % m_numDim;
            prev.m_Left.m_SplitValue = inst.value(prev.m_Left.m_SplitDim);
            prev.m_Left.m_TreeSize = 1;
        }
        else {
            prev.m_Right = new KDTreeNode();
            prev.m_Right.m_NodeNumber = m_Instances.numInstances() - 1;
            prev.m_Right.m_SplitDim = depth % m_numDim;
            prev.m_Right.m_SplitValue = inst.value(prev.m_Right.m_SplitDim);
            prev.m_Right.m_TreeSize = 1;
        }

        if (metricsTree.heightTree < depth) {
            metricsTree.heightTree = depth;
        }
    }

    public void delete(int idx) {
        m_InstDeleted.add(idx);
        metricsTree.numDeletedNodes = m_InstDeleted.size();
    }

    public void buildTree(Instances insts) {
        m_Instances = insts;
        int[] instList = new int[insts.size()];
        for (int idx = 0; idx < instList.length; idx++) {
            instList[idx] = idx;
        }

        m_InstDeleted.clear();
        metricsTree.heightTree = 0;
        m_Root = splitInstances(instList, 0, 0, instList.length -1);
        updateMetrics();
    }

    private KDTreeNode splitInstances(int[] insts, int depth, int left, int right) {
        if (left > right)
            return null;
        int splitDim = depth % m_numDim;

        // Pega o indice da mediana e organiza o insts para que os que são menores vão para esquerda e o resto para direita
        int median = (left + (right - left) / 2);
        int medianIdx = select(splitDim, insts, left, right, median); // Select retorna qual instancia é a mediana
        double medianValue = m_Instances.instance(medianIdx).value(splitDim);

        KDTreeNode node = new KDTreeNode();
        node.m_SplitDim = splitDim;
        node.m_SplitValue = medianValue;
        node.m_NodeNumber = medianIdx;

        // Tamanho da árvore/subárvore
        node.m_TreeSize = insts.length;

        // Separa entre esquerda e direita
        int countLeft = 0;
        int countRight = 0;
        for (int inst : insts) {
            if (inst != medianIdx) {
                if (m_Instances.instance(inst).value(splitDim) <= medianValue) {
                    countLeft++;
                } else {
                    countRight++;
                }
            }
        }
        int[] left_insts = new int[countLeft];
        int[] right_insts = new int[countRight];
        int i = 0;
        int j = 0;
        for (int index : insts) {
            if (index != medianIdx) {
                if (m_Instances.instance(index).value(splitDim) <= medianValue) {
                    left_insts[i] = index;
                    i++;
                } else {
                    right_insts[j] = index;
                    j++;
                }
            }
        }

        if (metricsTree.heightTree < depth) {
            metricsTree.heightTree = depth;
        }

        node.m_Left = splitInstances(left_insts, depth + 1, 0, left_insts.length - 1);
        node.m_Right = splitInstances(right_insts, depth + 1,0, right_insts.length - 1);

        return node;
    }

    ///  Getters and Setters

    @Override
    public void setInstances(Instances insts) throws Exception {
        super.setInstances(insts);
        m_numDim = m_Instances.numAttributes() - 1;
        m_DistanceFunction.setDontNormalize(true);
        m_DistanceFunction.setInstances(insts);
    }

    public Instances getInstances() {
        return super.getInstances();
    }

    public int getNumInstances() {
        return super.getInstances().size();
    }

    @Override
    public DistanceFunction getDistanceFunction() {
        return this.m_DistanceFunction;
    }

    @Override
    public void setDistanceFunction(DistanceFunction df) throws Exception {
        df.setDontNormalize(true);
        m_DistanceFunction = df;
        super.setDistanceFunction(df);
    }

    public void setSearchMetrics(EvaluateNKDTree.SearchMetrics searchMetrics) {
        this.searchMetrics = searchMetrics;
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

    private void updateMetrics() {
        metricsTree.treeSize = m_Instances.size();
        metricsTree.numDeletedNodes = 0;
        updateSubTreeSizes();
    }

    private void updateSubTreeSizes() {
        if (m_Root == null) {
            metricsTree.leftTreeSize = 0;
            metricsTree.rightTreeSize = 0;
            return;
        }

        metricsTree.leftTreeSize =
                m_Root.m_Left != null
                        ? m_Root.m_Left.m_TreeSize
                        : 0;

        metricsTree.rightTreeSize =
                m_Root.m_Right != null
                        ? m_Root.m_Right.m_TreeSize
                        : 0;
    }

    /**
     * Partitions the instances around a pivot. Used by quicksort and
     * kthSmallestValue.
     *
     * @param attIdx The attribution/dimension based on which the
     * instances should be partitioned.
     * @param index The master index array containing indices of the
     * instances.
     * @param l The begining index of the portion of master index
     * array that should be partitioned.
     * @param r The end index of the portion of master index array
     * that should be partitioned.
     * @return the index of the middle element
     */
    private int partition(int attIdx, int[] index, int l, int r) {

        double pivot = m_Instances.instance(index[(l + r) / 2]).value(attIdx);
        int help;

        while (l < r) {
            while ((m_Instances.instance(index[l]).value(attIdx) < pivot) && (l < r)) {
                l++;
            }
            while ((m_Instances.instance(index[r]).value(attIdx) > pivot) && (l < r)) {
                r--;
            }
            if (l < r) {
                help = index[l];
                index[l] = index[r];
                index[r] = help;
                l++;
                r--;
            }
        }
        if ((l == r) && (m_Instances.instance(index[r]).value(attIdx) > pivot)) {
            r--;
        }

        return r;
    }

    /**
     * Implements computation of the kth-smallest element according
     * to Manber's "Introduction to Algorithms".
     *
     * @param attIdx The dimension/attribute of the instances in
     * which to find the kth-smallest element.
     * @param indices The master index array containing indices of
     * the instances.
     * @param left The begining index of the portion of the master
     * index array in which to find the kth-smallest element.
     * @param right The end index of the portion of the master index
     * array in which to find the kth-smallest element.
     * @param k The value of k
     * @return The index of the kth-smallest element
     */
    private int select(int attIdx, int[] indices, int left, int right, int k) {

        if (left <= right) {
            int pivotindex = partition(attIdx, indices, left, right);
            if (pivotindex == k) return indices[pivotindex];
            else if (pivotindex > k) return select(attIdx, indices, left, pivotindex - 1, k);
            else return select(attIdx, indices, pivotindex + 1, right, k);
        }

        return -1;
    }
}

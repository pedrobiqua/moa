package moa.classifiers.lazy.neighboursearch;

import com.yahoo.labs.samoa.instances.Instance;
import com.yahoo.labs.samoa.instances.Instances;
import com.yahoo.labs.samoa.instances.InstancesHeader;

import java.util.Arrays;

// TODO: Aqui lembrar de retirar o build pelo maximal range, quero usar a profundidade da árvore

/**
 * Partial Naive KDTree.
 */
public class PNSKDTree extends NearestNeighbourSearch {

    public static class Node {
        // Params default kdtree
        Instance point;
        Node left, right;
        int axis;

        // Params to calculate alpha and beta
        int treeSize, invalidnum;
        Boolean deleted, treeDeleted;

        Node(int d, Instance inst, int axis) {
            this.point = inst;
            this.axis = axis;

            left = null;
            right = null;

            treeSize = 0;
            invalidnum = 0;

            treeDeleted = false;
            deleted = false;
        }
        double getValue() {
            return point.value(axis);
        }
    }

    InstancesHeader header;
    Node root;
    int numDim;

    private final double alphaBal = 0.6;
    private final double alphaDel = 0.5;
    private final int windowSize = 10;

    @Override
    public Instance nearestNeighbour(Instance target) throws Exception {
        return null;
    }

    @Override
    public Instances kNearestNeighbours(Instance target, int k) throws Exception {
        return null;
    }

    @Override
    public double[] getDistances() throws Exception {
        return new double[0];
    }

    @Override
    public void update(Instance ins) throws Exception {
        checkMissing(ins);
        // Init header and number of dimensions
        if (header == null) header = new InstancesHeader(ins.dataset());
        if (numDim == 0) numDim = ins.numAttributes() - 1;
        root = insert(root, ins, -1);
    }

    /**
     * Inserts an instance recursively into the KD-tree.
     *
     * @param node       current node
     * @param inst       instance to insert
     * @param fatherAxis axis used by the parent node
     * @return the updated node
     */
    private Node insert(Node node, Instance inst, int fatherAxis) {
        // Case 0.
        if (node == null) {
            int axis = (fatherAxis + 1) % numDim;
            node = new Node(numDim, inst, axis);
            updateTree(node);
            return node;
        }
        // Decide which subtree to follow
        if (inst.value(node.axis) <= node.point.value(node.axis))
            node.left = insert(node.left, inst, node.axis);
        else
            node.right = insert(node.right, inst, node.axis);

        // Update new node
        updateTree(node);
        if (criterionCheck(node)) {
            node = rebuildTree(node);
        }
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

    // TODO: Trocar isso amanhã para o novo metodo, alpha e beta. Para fazer o teste do ablation.
    /**
     * Checks whether the subtree rooted at the given node
     * satisfies the rebuild criteria.
     *
     * @param t root node of the subtree
     * @return true if the subtree should be rebuilt
     */
    private boolean criterionCheck(Node t) {
        if (root.treeSize <= windowSize)
            return false;

        // Alpha del
        double deleteEvaluation = (double) t.invalidnum / t.treeSize;
        if (deleteEvaluation > alphaDel) return true;

        // Alpha bal
        Node son = t.left;
        if (son == null) son = t.right;
        double balanceEvaluation = (double) son.treeSize / (t.treeSize - 1);
        return balanceEvaluation > alphaBal || balanceEvaluation < 1.0 - alphaBal;
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
     *
     *
     * @param node
     */
    private Node rebuildTree(Node node) {
        // Flatten para instances, cria Instances do tamanho que vai ser preciso, depois no build faz o processo de split e afins
        Instances insts = new Instances(header, (node.treeSize - node.invalidnum));
        flatten(node, insts);
        return buildTree(insts);
    }

    private void flatten(Node node, Instances insts) {
        if (node == null)
            return;
        if (!node.deleted)
            insts.add(node.point);
        flatten(node.left, insts);
        flatten(node.right, insts);
    }

    /**
     *
     * @param insts
     */
    private Node buildTree(Instances insts) {
        return splitTree(insts);
    }

    /**
     *
     *
     * @param instances
     * @return
     */
    private Node splitTree(Instances instances) {
        if (instances.size() == 0 ) return null;

        // Lista de indices
        int[] insts = new int[instances.size()];
        for (int i = 0; i < insts.length; i++) insts[i] = i;
        int left = 0;
        int right = insts.length - 1;
        int mid = (left + right) / 2;

        int axis = maxRangeAxis(instances, insts);
        int midIdx = select(instances, axis, insts, left, right, mid);

        Instance instanceMid = instances.instance(midIdx);
        Instances leftInsts = new Instances(header);
        Instances rightInsts = new Instances(header);

        for (int i = left; i <= right; i++) {
            int idx = insts[i];
            if (idx == midIdx)
                continue;
            Instance instance = instances.instance(idx);
            if (instance.value(axis) <= instanceMid.value(axis)) {
                leftInsts.add(instance);
            } else {
                rightInsts.add(instance);
            }
        }

        Node t = new Node(numDim, instanceMid, axis);

        t.left = splitTree(leftInsts);
        t.right = splitTree(rightInsts);

        updateTree(t);

        return t;
    }

    /**
     *
     *
     * @return
     */
    private int maxRangeAxis(Instances instances, int[] insts) {

        double[] min = new double[numDim];
        double[] max = new double[numDim];

        Arrays.fill(min, Double.POSITIVE_INFINITY);
        Arrays.fill(max, Double.NEGATIVE_INFINITY);

        for (int i : insts) {
            Instance instance = instances.instance(i);
            for (int j = 0; j < numDim; j++) {
                double value = instance.value(j);

                if (value < min[j])
                    min[j] = value;

                if (value > max[j])
                    max[j] = value;
            }
        }

        int axis = 0;
        double maxRange = max[0] - min[0];

        for (int j = 1; j < numDim; j++) {
            double range = max[j] - min[j];

            if (range > maxRange) {
                maxRange = range;
                axis = j;
            }
        }

        return axis;
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
    private int partition(Instances insts, int attIdx, int[] index, int l, int r) {

        double pivot = insts.instance(index[(l + r) / 2]).value(attIdx);
        int help;

        while (l < r) {
            while ((insts.instance(index[l]).value(attIdx) < pivot) && (l < r)) {
                l++;
            }
            while ((insts.instance(index[r]).value(attIdx) > pivot) && (l < r)) {
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
        if ((l == r) && (insts.instance(index[r]).value(attIdx) > pivot)) {
            r--;
        }

        return r;
    }

    /**
     * Implements computation of the kth-smallest element according
     * to Manber's "Introduction to Algorithms".
     *
     * @param insts Instances
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
    private int select(Instances insts, int attIdx, int[] indices, int left, int right, int k) {
        if (left <= right) {
            int pivotindex = partition(insts, attIdx, indices, left, right);
            if (pivotindex == k) return indices[pivotindex];
            else if (pivotindex > k) return select(insts, attIdx, indices, left, pivotindex - 1, k);
            else return select(insts, attIdx, indices, pivotindex + 1, right, k);
        }
        return -1;
    }


    /**
     *
     *
     * @param inst
     * @return
     */
    public void delete(Instance inst) throws Exception {
        boolean deleted = delete(root, inst);
        if (!deleted)
            throw new Exception("ERROR: can not delete instance");
    }

    /**
     *
     *
     * @param node
     * @param inst
     * @return
     */
    private boolean delete(Node node, Instance inst) {
        if (node == null)
            return false;

        // Found the node
        if (checkEqualInstance(node.point, inst)) {
            node.deleted = true;
            updateTree(node);
            return true;
        }

        boolean deleted;
        if (inst.value(node.axis) <= node.point.value(node.axis)) {
            deleted = delete(node.left, inst);
        } else {
            deleted = delete(node.right, inst);
        }

        if (deleted)
            updateTree(node);

        return deleted;
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

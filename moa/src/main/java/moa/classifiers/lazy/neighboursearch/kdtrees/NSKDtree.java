package moa.classifiers.lazy.neighboursearch.kdtrees;

import com.yahoo.labs.samoa.instances.Instance;
import com.yahoo.labs.samoa.instances.Instances;
import moa.classifiers.lazy.neighboursearch.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.TreeSet;

public class NSKDtree extends NearestNeighbourSearch {

    /// Atributos da classe
    private RebuildPolicy m_RebuildPolicies = new DeletedRatioPolicy();

    private int m_WindowSize = 1000;
    private Window m_Window;

    /** Tree nodes deleted **/
    protected TreeSet<Integer> m_InstDeleted = new TreeSet<>();

    protected DistanceFunction m_DistanceFunction = new EuclideanDistance();

    private double[] m_DistanceList;

    /**
     * Index list of the instances of this kdtree. Instances get sorted according
     * to the splits. the nodes of the KDTree just hold their start and end
     * indices
     */
    protected int[] m_InstList;

    /** Atributos da árvore **/
    private int m_numDim;
    // private int m_numNodes, m_numDim, m_heightTree;
    public StatsTree stats = new StatsTree();

    /** Árvore root node **/
    private KDTreeNode m_Root;


    @Override
    public Instance nearestNeighbour(Instance target) throws Exception {
        return (kNearestNeighbours(target, 1)).instance(0);
    }

    @Override
    public Instances kNearestNeighbours(Instance target, int k) throws Exception {
        if (stats.m_numNodes == 0) {
            throw new Exception("The K-d tree was not initialized. Please use the method setInstances(Instances)");
        }

        stats.backtrack = 0; // Reinicia a variavel que conta o número de backtracks
        // Eu acredito que a inserção do jeito que é hoje ajuda a
        // montar uma árvore que gera muitos backtracks
        stats.depthSearch = 0;

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

        if (depth > stats.depthSearch) {
            stats.depthSearch = depth;
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

        double planeDist = m_DistanceFunction.sqDifference(
                node.m_SplitDim,
                target.value(node.m_SplitDim),
                node.m_SplitValue);

        if (heap.size() < k || planeDist <= heap.peek().distance) {
            stats.backtrack++;
            findNearestNeighbours(target, other, k, heap, depth + 1);
        }
    }

    @Override
    public double[] getDistances() throws Exception {
        return new double[0];
    }

    @Override
    public void update(Instance ins) throws Exception {
        checkMissing(ins);
        ///  Controle da janela
        int idx_remove = m_Window.update();
        if (idx_remove != -1)
            delete(idx_remove);

        addInstanceToTree(ins);
        stats.m_numNodes++;

        if (m_RebuildPolicies.checkRebuild(stats)) {
//            System.out.println("// Reconstruindo a árvore!!");
//            System.out.println("// log: " + Math.log(stats.m_numNodes) / Math.log(1.0 / 0.6));
//            System.out.println("// num_nodes: " + stats.m_numNodes);
//            System.out.println("// height: " + stats.m_heightTree);
//            System.out.println("// Antes rebuild");
//            this.printTree();
            buildTree(m_Window.getInstancesWindow());
            stats.m_numNodes = m_Instances.size();
            stats.countRebuild++;
//            System.out.println("// log: " + Math.log(stats.m_numNodes) / Math.log(1.0 / 0.6));
//            System.out.println("// num_nodes: " + stats.m_numNodes);
//            System.out.println("// height: " + stats.m_heightTree);
//            System.out.println("// Depois rebuild");
//            this.printTree();
        }

    }

    private void addInstanceToTree(Instance inst) {
        int depth = 0;
        KDTreeNode p = m_Root;
        KDTreeNode prev = null;
        double[] newInstance = inst.toDoubleArray();

        while (p != null) {
            prev = p;
            int axis = depth % m_numDim;
            if (newInstance[axis] <= m_Instances.instance(p.m_NodeNumber).value(axis)) {
                p = p.m_Left;
            } else {
                p = p.m_Right;
            }

            depth++;
        }

        stats.depthInsert = depth;

        m_Instances.add(inst);

        if (m_Root == null) {
            m_Root = new KDTreeNode();
            m_Root.m_NodeNumber = 0;
            m_Root.m_Left = null;
            m_Root.m_Right = null;
            m_Root.m_SplitDim = 0;
            m_Root.m_SplitValue = inst.value(0);
            return;
        }

        // Profundidade de prev
        int axis = (depth - 1) % this.m_numDim;

        assert prev != null;
        if (newInstance[axis] <= m_Instances.instance(prev.m_NodeNumber).value(axis)) {
            prev.m_Left = new KDTreeNode();
            prev.m_Left.m_NodeNumber = m_Instances.numInstances() - 1;
            prev.m_Left.m_SplitDim = depth % m_numDim;
            prev.m_Left.m_SplitValue = inst.value(prev.m_Left.m_SplitDim);
        }
        else {
            prev.m_Right = new KDTreeNode();
            prev.m_Right.m_NodeNumber = m_Instances.numInstances() - 1;
            prev.m_Right.m_SplitDim = depth % m_numDim;
            prev.m_Right.m_SplitValue = inst.value(prev.m_Right.m_SplitDim);
        }

        if (stats.m_heightTree < depth) {
            stats.m_heightTree = depth;
        }
    }

    private void delete(int idx) {
        m_InstDeleted.add(idx);
        stats.m_numNodesDeleted = m_InstDeleted.size();
    }

    public int exactSearch(Instance inst) {
        return search(m_Root, inst);
    }

    private int search(KDTreeNode node, Instance inst) {
        if (node == null)
            return -1;

        if (instanceIsEqual(m_Instances.instance(node.m_NodeNumber), inst))
            return node.m_NodeNumber;

        double[] d_inst = inst.toDoubleArray();
        if (d_inst[node.m_SplitDim] <= node.m_SplitValue)
            return search(node.m_Left, inst);
        else
            return search(node.m_Right, inst);
    }

    public void buildTree(Instances insts) {
        m_Instances = insts;
        m_Window.setInstances(m_Instances);
        // Monta a árvore inteira
        int[] instList = new int[insts.size()];
        for (int idx = 0; idx < instList.length; idx++) {
            instList[idx] = idx;
        }
        m_InstDeleted.clear();

        stats.m_heightTree = 0;
        m_Root = splitInstances(instList, 0, 0, instList.length -1);
    }

    private KDTreeNode splitInstances(int[] insts, int depth, int left, int right) {
        if (left > right)
            return null;
        int splitDim = depth % m_numDim;

        // Pega o indice da mediana e organiza o insts para que os que são menores vão para esquerda e o resto para direita
        int median = (left + (right - left) / 2);
        int medianIdx = select(splitDim, insts, left, right, median); // Select retorna qual instancia é a do meio
        double medianValue = m_Instances.instance(medianIdx).value(splitDim);

        KDTreeNode node = new KDTreeNode();
        node.m_SplitDim = splitDim;
        node.m_SplitValue = medianValue;
        node.m_NodeNumber = medianIdx;

        if (stats.m_heightTree < depth) {
            stats.m_heightTree = depth;
        }

        // Recursão left e right
        node.m_Left = splitInstances(insts, depth + 1, left, median - 1);
        node.m_Right = splitInstances(insts, depth + 1,median + 1, right);

        return node;
    }

    ///  Geters and Setters

    @Override
    public void setInstances(Instances insts) throws Exception {
        super.setInstances(insts);
        m_numDim = m_Instances.numAttributes() - 1;
        if (m_Window == null) { // Cria o ponteiro da janela deslizante
            m_Window = new Window(this.m_WindowSize);
            m_Window.setInstances(m_Instances);
        }

        m_DistanceFunction.setDontNormalize(true);
        m_DistanceFunction.setInstances(insts);
    }

    public void setRebuildPolicies(RebuildPolicy rebuildPolicies) {
        this.m_RebuildPolicies = rebuildPolicies;
    }

    public void setWindowSize(int m_WindowSize) {
        this.m_WindowSize = m_WindowSize;
    }

    @Override
    public void setDistanceFunction(DistanceFunction df) throws Exception {
        df.setDontNormalize(true);
        m_DistanceFunction = df;
        super.setDistanceFunction(df);
    }

    /// Funções auxiliares
    private boolean instanceIsEqual(Instance instance1, Instance instance2) {
        if (instance1.numValues() != instance2.numValues())
            return false;

        for (int i = 0; i < instance1.toDoubleArray().length; i++) {
            if (instance1.value(i) != instance2.value(i))
                return false;
        }

        return true;
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

    public void printTree() {
        System.out.println("digraph KDTree {");
        System.out.println("node [shape=circle];");
        inorder(m_Root);
        System.out.println("}\n");
    }

    public void printPoints() {
        System.out.println("x,y,num,status");
        preorder(m_Root);

    }

    private void preorder(KDTreeNode node) {
        if (node == null) {
            return;
        }

        Instance node_instance = m_Instances.instance(node.m_NodeNumber);
        System.out.println(node_instance.value(0) + ","
                + node_instance.value(1) + ","
                + node.m_NodeNumber + ","
                + (m_InstDeleted.contains(node.m_NodeNumber) ? "apagado" : "ok"));

        preorder(node.m_Left);
        preorder(node.m_Right);
    }


    private void inorder(KDTreeNode node) {
        if (node == null) {
            return;
        }

        inorder(node.m_Left);

        // imprime o nó | mostra em sublinhado o valor usado no corte
        String nodeId = Integer.toString(node.m_NodeNumber);
        StringBuilder label = new StringBuilder("<(");
        for (int i = 0; i < m_Instances.instance(node.m_NodeNumber).numAttributes() - 1; i++){
            if (i < m_Instances.instance(node.m_NodeNumber).numAttributes() - 2) {
                if (i != node.m_SplitDim)
                    label.append(m_Instances.instance(node.m_NodeNumber).value(i)).append(" ");
                else
                    label.append("<u>").append(m_Instances.instance(node.m_NodeNumber).value(i)).append("</u>").append(" ");
            } else {
                if (i != node.m_SplitDim)
                    label.append(m_Instances.instance(node.m_NodeNumber).value(i));
                else
                    label.append("<u>").append(m_Instances.instance(node.m_NodeNumber).value(i)).append("</u>");
            }
        }
        label.append(")>");

        if ( m_InstDeleted.contains(node.m_NodeNumber)) {
            System.out.println(
                    nodeId + " [label=" + label + ", style=filled, fillcolor=red];");
        } else {
            System.out.println(
                    nodeId + " [label=" + label + "];");
        }

        // aresta esquerda
        if (node.m_Left != null) {
            System.out.println(
                    nodeId + " -> " + Integer.toString(node.m_Left.m_NodeNumber) + " [label=\"L\"];");
        }

        // aresta direita
        if (node.m_Right != null) {
            System.out.println(
                    nodeId + " -> " + Integer.toString(node.m_Right.m_NodeNumber) + " [label=\"R\"];");
        }

        // visita direita
        inorder(node.m_Right);
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

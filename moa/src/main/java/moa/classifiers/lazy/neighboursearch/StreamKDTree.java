/*
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

/*
 * StreamKDtree.java
 * Copyright (C) 2007-2012 Federal University of Paraná, Curitiba, Brazil
 */
package moa.classifiers.lazy.neighboursearch;

import moa.classifiers.lazy.neighboursearch.kdtrees.*;

import com.yahoo.labs.samoa.instances.Instance;
import com.yahoo.labs.samoa.instances.Instances;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

/**
 * Stream KDTree
 * <br/>
 *	Stream KDtree is an incremental kdtree.
 *
 *  <p> Depois colocar aqui as minhas publicações.</p>
 *
 * References:
 * <br/>
 * Jerome H. Friedman, Jon Luis Bentley, Raphael Ari Finkel (1977). An Algorithm for Finding Best Matches in Logarithmic Expected Time. ACM Transactions on Mathematics Software. 3(3):209-226.<br/>
 * <br/>
 * Andrew Moore (1991). A tutorial on kd-trees.
 *
 * @author Pedro Bianchini de Quadros (pedro.bianchini@ufpr.br)
 * @version $Revision: 1 $
 */
public class StreamKDTree extends NearestNeighbourSearch {

    // TODO:  - Arrumar a busca para não usar os nós deletados - Arrumar os splits com defeito para testar - Adicionar critérios de rebuild
    //         - Já adicionado, falta validar                   - Preciso ver isso com mais calma
    //
    // Anotações:
    // Já está sendo realizado as lazy deletion das instancias nos nós folha. Falta validar isso com um exemplo controlado!
    // Testar o build junto com o modo stream dessa classe, para avaliar as diferenças das métricas
    // Testes: Árvore ingenua, Arvore bucket (SlidingMidPoint)/(Median)/(MidPoint)
    //         Testar a Janela / Testar a inserção e parametros
    // Criar as politicas de rebuilding
    // O Split MedianOfWidestDimension() está inconsistente, na função de search isso fica claro, talvez é por isso que
    // não usam kkkkk

    /** For serialization. */
    private static final long serialVersionUID = 1505717283763272533L;

    // Posso testar uma composição de políticas de rebuild, e ver qual mantém a árvore mais estável por mais tempo.
    private final List<RebuildPolicy> rebuildPolicies;
    {
        rebuildPolicies = new ArrayList<>();
    }

    /**
     * Array holding the distances of the nearest neighbours. It is filled up both
     * by nearestNeighbour() and kNearestNeighbours().
     */
    protected double[] m_DistanceList;

    /**
     * Index list of the instances of this kdtree. Instances get sorted according
     * to the splits. the nodes of the KDTree just hold their start and end
     * indices
     */
    protected int[] m_InstList;

    /** Tree nodes deleted **/
    protected TreeSet<Integer> m_InstDeleted= new TreeSet<>();

    /** The root node of the tree. */ // LEMBRAR DE COLOCAR PROTECTED
    public KDTreeNode m_Root;

    /** The node splitter. */
    protected KDTreeNodeSplitter m_Splitter = new SlidingMidPointOfWidestSide();

    /** The max instances in leaf */
    protected int m_MaxInstInLeaf = 40;

    protected double m_MinBoxRelWidth = 1.0E-2;

    /**  */
    boolean m_NormalizeNodeWidth = false;

    /** Tree stats. */
    // public int m_NumNodes, m_NumLeaves, m_MaxDepth, m_NumNodesDeleted;

    /** Tree stats control **/
    public KDTreeStats m_Stats = new KDTreeStats();

    // Constants
    /** The index of WIDTH (MAX-MIN) value in attributes' range array. */
    public static final int WIDTH = EuclideanDistance.R_WIDTH;

    public StreamKDTree() {
        // Garante que a função de distância não vai fazer normalizações
        EuclideanDistance dist = new EuclideanDistance();
        dist.setDontNormalize(true);
        m_DistanceFunction = dist;
    }

    //// INTERFACE METHODS

    /**
     * Returns the nearest neighbour of the supplied target
     * instance.
     *
     * @param target The instance to find the nearest neighbour for.
     * @return The nearest neighbour from among the previously
     *         supplied training instances.
     * @throws Exception if the neighbours could not be found.
     */
    public Instance nearestNeighbour(Instance target) throws Exception {
        return (kNearestNeighbours(target, 1)).instance(0);
    }

    /**
     * Returns the k nearest neighbours of the supplied instance.
     * &gt;k neighbours are returned if there are more than one
     * neighbours at the kth boundary.
     *
     * @param target The instance to find the nearest neighbours for.
     * @param k      The number of neighbours to find.
     * @return The k nearest neighbours (or &gt;k if more there are than
     *         one neighbours at the kth boundary).
     * @throws Exception if the nearest neighbour could not be found.
     */
    public Instances kNearestNeighbours(Instance target, int k) throws Exception {
        checkMissing(target);

        MyHeap heap = new MyHeap(k);
        findNearestNeighbours(target, m_Root, k, heap, 0.0);

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

        for (int index : indices) {
            neighbours.add(m_Instances.instance(index));
        }

        return neighbours;
    }

    /**
     * Returns the distances to the kNearest or 1 nearest neighbour currently
     * found with either the kNearestNeighbours or the nearestNeighbour method.
     *
     * @return array containing the distances of the
     *         nearestNeighbours. The length and ordering of the array
     *         is the same as that of the instances returned by
     *         nearestNeighbour functions.
     * @throws Exception if called before calling kNearestNeighbours or
     *                   nearestNeighbours.
     */
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

        // TODO: VER OUTRAS FORMAS MAIS FACEIS DE RECRIAÇÃO
        // Verify rebuild
        if (rebuildPolicies.isEmpty())
            throw new Exception("Not add rebuild policy");
        for (RebuildPolicy policy : rebuildPolicies){
            if (policy.checkRebuild(m_Stats)){
                // rebuildKDTree();
                System.out.println("Recriando a árvore");

                for (int idx = 0; idx < m_Instances.numInstances(); idx++){
                    if (m_InstDeleted.isEmpty())
                        break;
                    if (!m_InstDeleted.remove(idx)){
                        // Se não for removido, adicionar no vetor de cópia
                        System.out.println("Add: " + idx);
                    }
                }
            }
        }
    }

    /**
     * Builds the KDTree on the given set of instances.
     *
     * @param instances The insts on which the KDTree is to be
     *                  built.
     * @throws Exception If some error occurs while
     *                   building the KDTree
     */
    public void setInstances(Instances instances) throws Exception {
        super.setInstances(instances);
        buildKDTree(instances);
    }

    public void setMaxInstInLeaf(int m_MaxInstInLeaf) {
        this.m_MaxInstInLeaf = m_MaxInstInLeaf;
    }

    public void setSplitter(KDTreeNodeSplitter splitter) {this.m_Splitter = splitter; }

    @Override
    public Instances getInstances() {
        return super.getInstances();
    }

    /**
     *
     */
    protected void buildKDTree(Instances instances) throws Exception {
        // throw new UnsupportedOperationException("Unimplemented method
        // 'setInstances'");
        checkMissing(instances);
        if (m_DistanceFunction == null)
            throw new Exception("ERROR: Missing distance function");
        else
            m_DistanceFunction.setInstances(instances);

        m_Instances = instances;
        int numInst = m_Instances.numInstances();

        // Make the global index list
        m_InstList = new int[numInst];

        for (int i = 0; i < numInst; i++) {
            m_InstList[i] = i;
        }

        double[][] universe = m_DistanceFunction.getRanges();

        // initializing internal fields of KDTreeSplitter
        m_Splitter.setInstances(m_Instances);
        m_Splitter.setInstanceList(m_InstList);
        if (m_DistanceFunction instanceof EuclideanDistance)
            m_Splitter.setEuclideanDistanceFunction((EuclideanDistance) m_DistanceFunction);
        else {
            throw new UnsupportedOperationException("Unimplemented method ''");
        }
        m_Splitter.setNodeWidthNormalization(m_NormalizeNodeWidth);

        // building tree
        m_Stats.m_NumNodes = m_Stats.m_NumLeaves = 1;
        m_Stats.m_MaxDepth = 0;
        m_Root = new KDTreeNode(m_Stats.m_NumNodes, 0, m_Instances.numInstances() - 1,
                universe);

        splitNodes(m_Root, universe, m_Stats.m_MaxDepth + 1);
    }

    /**
     * Essa função rebuilda a árvore, porem preciso ver se não preciso reiniciar alguma métrica da árvore.
     *
     * @throws Exception If there is some problem
     *                   on rebuilding.
     */
    void rebuildKDTree() throws Exception {
        buildKDTree(m_Instances);
    }

    /**
     * Recursively splits nodes of a tree starting from the supplied node.
     * The splitting stops for any node for which the number of instances/points
     * falls below a given threshold (given by m_MaxInstInLeaf), or if the
     * maximum relative width/range of the instances/points
     * (i.e. max_i(max(att_i) - min(att_i)) ) falls below a given threshold
     * (given by m_MinBoxRelWidth).
     *
     * @param node     The node to start splitting from.
     * @param universe The attribute ranges of the whole dataset.
     * @param depth    The depth of the supplied node.
     * @throws Exception If there is some problem
     *                   splitting.
     */
    protected void splitNodes(KDTreeNode node, double[][] universe,
            int depth) throws Exception {
        double[][] nodeRanges = m_DistanceFunction.initializeRanges(m_InstList,
                node.m_Start, node.m_End);
        if (node.numInstances() <= m_MaxInstInLeaf
                || getMaxRelativeNodeWidth(nodeRanges, universe) <= m_MinBoxRelWidth)
            return;

        // splitting a node so it is no longer a leaf
        m_Stats.m_NumLeaves--;

        if (depth > m_Stats.m_MaxDepth)
            m_Stats.m_MaxDepth = depth;

        m_Splitter.splitNode(node, m_Stats.m_NumNodes, nodeRanges, universe);
        m_Stats.m_NumNodes += 2;
        m_Stats.m_NumLeaves += 2;

        splitNodes(node.m_Left, universe, depth + 1);
        splitNodes(node.m_Right, universe, depth + 1);
    }

    /**
     * Returns the maximum attribute width of instances/points
     * in a KDTreeNode relative to the whole dataset.
     *
     * @param nodeRanges The attribute ranges of the
     *                   KDTreeNode whose maximum relative width is to be
     *                   determined.
     * @param universe   The attribute ranges of the whole
     *                   dataset (training instances + test instances so
     *                   far encountered).
     * @return The maximum relative width
     */
    protected double getMaxRelativeNodeWidth(double[][] nodeRanges,
            double[][] universe) {
        int widest = widestDim(nodeRanges, universe);
        if (widest < 0)
            return 0.0;
        else
            return nodeRanges[widest][WIDTH] / universe[widest][WIDTH];
    }

    /**
     * Returns the widest dimension/attribute in a
     * KDTreeNode (widest after normalizing).
     *
     * @param nodeRanges The attribute ranges of
     *                   the KDTreeNode.
     * @param universe   The attribute ranges of the
     *                   whole dataset (training instances + test
     *                   instances so far encountered).
     * @return The index of the widest
     *         dimension/attribute.
     */
    protected int widestDim(double[][] nodeRanges, double[][] universe) {
        final int classIdx = m_Instances.classIndex();
        double widest = 0.0;
        int w = -1;
        if (m_NormalizeNodeWidth) {
            for (int i = 0; i < nodeRanges.length; i++) {
                double newWidest = nodeRanges[i][WIDTH] / universe[i][WIDTH];
                if (newWidest > widest) {
                    if (i == classIdx)
                        continue;
                    widest = newWidest;
                    w = i;
                }
            }
        } else {
            for (int i = 0; i < nodeRanges.length; i++) {
                if (nodeRanges[i][WIDTH] > widest) {
                    if (i == classIdx)
                        continue;
                    widest = nodeRanges[i][WIDTH];
                    w = i;
                }
            }
        }
        return w;
    }

    //// FUNÇÕES AUXILIARES

    /**
     * Checks if there is any instance with missing values. Throws an exception if
     * there is, as KDTree does not handle missing values.
     *
     * @param instances the instances to check
     * @throws Exception if missing values are encountered
     */
    protected void checkMissing(Instances instances) throws Exception {
        for (int i = 0; i < instances.numInstances(); i++) {
            Instance ins = instances.instance(i);
            for (int j = 0; j < ins.numValues(); j++) {
                if (ins.index(j) != ins.classIndex())
                    if (ins.isMissingSparse(j)) {
                        throw new Exception("ERROR: KDTree can not deal with missing "
                                + "values. Please run ReplaceMissingValues filter "
                                + "on the dataset before passing it on to the KDTree.");
                    }
            }
        }
    }

    /**
     * Checks if there is any missing value in the given
     * instance.
     *
     * @param ins The instance to check missing values in.
     * @throws Exception If there is a missing value in the
     *                   instance.
     */
    protected void checkMissing(Instance ins) throws Exception {
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
     * Returns (in the supplied heap object) the k nearest
     * neighbours of the given instance starting from the give
     * tree node. &gt;k neighbours are returned if there are more than
     * one neighbours at the kth boundary. NOTE: This method should
     * not be used from outside this class. Outside classes should
     * call kNearestNeighbours(Instance, int).
     *
     * @param target            The instance to find the nearest neighbours for.
     * @param node              The KDTreeNode to start the search from.
     * @param k                 The number of neighbours to find.
     * @param heap              The MyHeap object to store/update the kNNs found
     *                          during the search.
     * @param distanceToParents The distance of the supplied target
     *                          to the parents of the supplied tree node.
     * @throws Exception if the nearest neighbour could not be found.
     */
    protected void findNearestNeighbours(Instance target, KDTreeNode node, int k,
            MyHeap heap, double distanceToParents) throws Exception {
        if (node.isALeaf()) {
            double distance;
            // look at all the instances in this leaf
            for (int idx = node.m_Start; idx <= node.m_End; idx++) {
                // Ignore deleted points
                if (m_InstDeleted.contains(m_InstList[idx]))
                    continue;

                if (target == m_Instances.instance(m_InstList[idx])) // for
                    // hold-one-out
                    // cross-validation
                    continue;
                if (heap.size() < k) {
                    distance = m_DistanceFunction.distance(target, m_Instances
                            .instance(m_InstList[idx]), Double.POSITIVE_INFINITY);
                    heap.put(m_InstList[idx], distance);
                } else {
                    MyHeapElement temp = heap.peek();
                    distance = m_DistanceFunction.distance(target, m_Instances
                            .instance(m_InstList[idx]), temp.distance);
                    if (distance < temp.distance) {
                        heap.putBySubstitute(m_InstList[idx], distance);
                    } else if (distance == temp.distance) {
                        heap.putKthNearest(m_InstList[idx], distance);
                    }
                } // end else heap.size==k
            } // end for

        } else {
            KDTreeNode nearer, further;
            boolean targetInLeft = target.value(node.m_SplitDim) <= node.m_SplitValue;

            if (targetInLeft) {
                nearer = node.m_Left;
                further = node.m_Right;
            } else {
                nearer = node.m_Right;
                further = node.m_Left;
            }
            findNearestNeighbours(target, nearer, k, heap, distanceToParents);

            // ... now look in further half if maxDist reaches into it
            if (heap.size() < k) { // if I haven't found the first k
                double distanceToSplitPlane = distanceToParents
                        + m_DistanceFunction.sqDifference(node.m_SplitDim, target
                                .value(node.m_SplitDim), node.m_SplitValue);
                findNearestNeighbours(target, further, k, heap, distanceToSplitPlane);
            } else { // else see if ball centered at query intersects with the other
                // side.
                double distanceToSplitPlane = distanceToParents
                        + m_DistanceFunction.sqDifference(node.m_SplitDim, target
                                .value(node.m_SplitDim), node.m_SplitValue);
                if (heap.peek().distance >= distanceToSplitPlane) {
                    findNearestNeighbours(target, further, k, heap, distanceToSplitPlane);
                }
            } // end else
        } // end else_if an internal node
    }

    private void addInstanceToTree(Instance inst) throws Exception {
        // Inicializa a estrutura
        if (m_Root == null) {
            m_Instances = new Instances(inst.dataset(), 0);
            m_Instances.add(inst);// Inicializa e adiciona a instancia no conj de instancias
            m_Stats.m_NumInstancias = m_Instances.size();

            m_InstList = new int[m_Instances.size()]; // Cria os indices

            m_DistanceFunction.setInstances(m_Instances);
            double[][] universe = m_DistanceFunction.getRanges();

            // inicializa campos internos da KDTreeSplitter
            m_Splitter.setInstances(m_Instances);
            m_Splitter.setInstanceList(m_InstList);
            if (m_DistanceFunction instanceof EuclideanDistance)
                m_Splitter.setEuclideanDistanceFunction((EuclideanDistance) m_DistanceFunction);
            else {
                throw new UnsupportedOperationException("Unimplemented method ''");
            }
            m_Splitter.setNodeWidthNormalization(m_NormalizeNodeWidth);

            m_Root = new KDTreeNode(0, 0, 0, universe);

            // Adiciona e retorna
            m_InstList[m_Instances.size() - 1] = m_Instances.size() - 1;

            m_Stats.m_NumNodes = m_Stats.m_NumLeaves = 1;
            m_Stats.m_MaxDepth = 0;
            return;
        }

        // Add instance to tree
        m_Instances.add(inst);
        addInstanceInfo(inst);
        addInstance(inst, m_Root, 0);
        m_Stats.m_NumInstancias = m_Instances.numInstances();
    }

    private void addInstance(Instance inst, KDTreeNode node, int depth) throws Exception {
        if (depth > m_Stats.m_MaxDepth)
            m_Stats.m_MaxDepth = depth;

        if (node.isALeaf()) {
            int[] instList = new int[m_Instances.numInstances()];
            try {
                System.arraycopy(m_InstList, 0, instList, 0, node.m_End + 1);
                if (node.m_End < m_InstList.length - 1) {
                    System.arraycopy(m_InstList, node.m_End + 1, instList, node.m_End + 2,
                            m_InstList.length - node.m_End - 1);
                }
                instList[node.m_End + 1] = m_Instances.numInstances() - 1;

            } catch (ArrayIndexOutOfBoundsException ex) {
                System.err.println("m_InstList.length: " + m_InstList.length
                        + " instList.length: " + instList.length + " node.m_End+1: "
                        + (node.m_End + 1) + " m_InstList.length-node.m_End+1: "
                        + (m_InstList.length - node.m_End - 1));
                throw ex;
            }

            m_InstList = instList;
            node.m_End++;
            node.m_NodeRanges = m_DistanceFunction.updateRanges(inst, node.m_NodeRanges);

            // faz o split se necessário
            m_Splitter.setInstanceList(m_InstList);
            double[][] universe = m_DistanceFunction.getRanges();
            if (node.numInstances() > m_MaxInstInLeaf
                    && getMaxRelativeNodeWidth(node.m_NodeRanges, universe) > m_MinBoxRelWidth) {
                m_Stats.m_NumLeaves--;
                m_Splitter.splitNode(node, m_Stats.m_NumNodes, node.m_NodeRanges, universe);
                m_Stats.m_NumNodes += 2;
                m_Stats.m_NumLeaves += 2;
                // Se for nó folha mais profundo incrementa o valor
                if (depth + 1 > m_Stats.m_MaxDepth) {
                    m_Stats.m_MaxDepth++; // Acrescenta 1 na altura da árvore.
                }
            }
        } else {
                // Esquerda
            if (inst.value(node.m_SplitDim) <= node.m_SplitValue) {
//                if (depth > m_Stats.m_MaxDepth)
//                    m_Stats.m_MaxDepth = depth;
                addInstance(inst, node.m_Left, depth + 1);
                afterAddInstance(node.m_Right);
                // Direita
            } else {
//                if (depth > m_Stats.m_MaxDepth)
//                    m_Stats.m_MaxDepth = depth;
                addInstance(inst, node.m_Right, depth + 1);
            }

            node.m_End++;
            node.m_NodeRanges = m_DistanceFunction.updateRanges(inst, node.m_NodeRanges);
        }
    }

    public void delete(Instance inst) throws Exception {
        // Buscar o Nó, idx dele
        int idx = search(m_Root, inst);
        // Adicionar na lista de apagados
        if (idx != -1) {
            m_InstDeleted.add(idx);
            // Atualizar o m_NumInstancesDeleted
            m_Stats.m_NumInstancesDeleted = m_InstDeleted.size();
        } else {
            throw new Exception("Not found instance: " + inst);
        }
    }

    private boolean instanceIsEqual(Instance instance1, Instance instance2) {
        if (instance1.numValues() != instance2.numValues())
            return false;

        for (int i = 0; i < instance1.toDoubleArray().length; i++){
            if (instance1.value(i) != instance2.value(i))
                return false;
        }

        return true;
    }

    private int search(KDTreeNode node, Instance inst) {
        int idx_Deleted = -1;
        if (node.isALeaf()){
            for (int idx = node.m_Start; idx <= node.m_End; ++idx) {
                // Se for igual e não foi deletado
                if (instanceIsEqual(inst, m_Instances.instance(m_InstList[idx])) && !m_InstDeleted.contains(m_InstList[idx]))
                    return m_InstList[idx]; // Retorna o indice
            }
        } else {
            boolean targetInLeft = inst.value(node.m_SplitDim) <= node.m_SplitValue;
            if (targetInLeft)
                idx_Deleted = search(node.m_Left, inst);
            else
                idx_Deleted = search(node.m_Right, inst);
        }

        return idx_Deleted;
    }

    /**
     * Adds one instance to KDTree loosely. It only changes the ranges in
     * EuclideanDistance, and does not affect the structure of the KDTree.
     *
     * @param instance the new instance. Usually this is the test instance
     *                 supplied to update the range of attributes in the distance
     *                 function.
     */
    public void addInstanceInfo(Instance instance) {
        m_DistanceFunction.updateRanges(instance);
    }

    private void afterAddInstance(KDTreeNode node) {
        node.m_Start++;
        node.m_End++;
        if (!node.isALeaf()) {
            afterAddInstance(node.m_Left);
            afterAddInstance(node.m_Right);

        }
    }

    public void print() {
        System.out.println("digraph KDTree {");
        System.out.println("node [shape=box];");

        printNode(m_Root);

        System.out.println("}");
    }

    private void printNode(KDTreeNode node) {
        if (node == null)
            return;

        StringBuilder label = new StringBuilder();

        if (node.isALeaf()) {
            label.append("Leaf\\n[").append(node.m_Start).append(",")
                    .append(node.m_End).append("]\\n");

            for (int idx = node.m_Start; idx <= node.m_End; ++idx) {
                if (m_InstDeleted.contains(m_InstList[idx])) {
                    label.append("X ").append(idx).append("\\n"); // deletado
                } else {
                    label.append(idx).append("\\n");
                }
            }

        } else {
            label.append("Node ").append(node.m_NodeNumber)
                    .append("\\nDim: ").append(node.m_SplitDim)
                    .append("\\nVal: ").append(node.m_SplitValue);
        }

        System.out.println("node" + node.m_NodeNumber +
                " [label=\"" + label.toString() + "\"];");

        if (node.m_Left != null) {
            System.out.println("node" + node.m_NodeNumber +
                    " -> node" + node.m_Left.m_NodeNumber + ";");
            printNode(node.m_Left);
        }

        if (node.m_Right != null) {
            System.out.println("node" + node.m_NodeNumber +
                    " -> node" + node.m_Right.m_NodeNumber + ";");
            printNode(node.m_Right);
        }
    }

    public void setRebuildPolicies(RebuildPolicy rebuildPolicies) {
        this.rebuildPolicies.add(rebuildPolicies);
    }

    //// END INTERFACE

}

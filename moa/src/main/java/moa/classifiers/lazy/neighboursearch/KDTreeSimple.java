package moa.classifiers.lazy.neighboursearch;

import java.io.PrintStream;
import java.util.Arrays;

import com.yahoo.labs.samoa.instances.Instance;
import com.yahoo.labs.samoa.instances.Instances;

public class KDTreeSimple extends NearestNeighbourSearch {

    public class Node {
        Node left, right, parent;
        int splitDim;
        int node_index;

        public Node(int node_index, Node left, Node right, int splitDim) {
            this.node_index = node_index;
            this.left = left;
            this.right = right;
            this.splitDim = splitDim;
        }

        public boolean isLeaf() {
            return (this.left == null) && (this.right == null);
        }
    }

    public class NodeDist {
        private Node node;
        private double distance;

        public NodeDist(Node node, double distance) {
            this.node = node;
            this.distance = distance;
        }
    }

    private Node root;
    private int numDim;

    private int height_tree;
    private int numNodes;

    private double[] m_DistanceList; // Usar isso no getDistances();

    public int backtrackCount = 0; // DEBUG DO BACKTRACK

    private EuclideanDistance distance_fn = new EuclideanDistance();

    public KDTreeSimple(int numDim) {
        this.numDim = numDim;
    }

    @Override
    public void setInstances(Instances insts) throws Exception {
        super.setInstances(insts);
    }

    @Override
    public void setDistanceFunction(DistanceFunction df) throws Exception {
        if (!(df instanceof EuclideanDistance))
            throw new Exception("KDTree currently only works with "
                    + "EuclideanDistanceFunction.");
        m_DistanceFunction = distance_fn = (EuclideanDistance) df;
    }

    @Override
    public Instances kNearestNeighbours(Instance target, int k) throws Exception {
        checkMissing(target);

        MyHeap heap = new MyHeap(k);
        findNearestNeighbours(root, target, k, heap, 0, 0.0);

        Instances neighbours = new Instances(m_Instances, (heap.size() + heap.noOfKthNearest()));
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

    @Override
    public Instance nearestNeighbour(Instance target) throws Exception {
        NodeDist bestDist = new NodeDist(null, Double.MAX_VALUE);
        NodeDist searchInstance = searchNearestNeighbor(root, target, 0, 0.0, bestDist);
        return searchInstance.node.node_index >= 0 ? m_Instances.instance(searchInstance.node.node_index) : null;
    }

    // KD-Tree Nearest Neighbor — versão correta com accumulatedDist
    private NodeDist searchNearestNeighbor(Node root, Instance target, int depth,
            double accumulatedDist, NodeDist bestDist) {

        if (root == null) {
            return bestDist;
        }

        // Caso folha
        if (root.isLeaf()) {
            double dist = distance_fn.distance(
                    m_Instances.instance(root.node_index),
                    target,
                    Double.POSITIVE_INFINITY);

            if (dist < bestDist.distance) {
                return new NodeDist(root, dist);
            }
            return bestDist;
        }

        int axis = depth % this.numDim;

        Node nextBranch, otherBranch;

        double rootValue = m_Instances.instance(root.node_index).value(axis);
        double targetValue = target.value(axis);

        if (targetValue < rootValue) {
            nextBranch = root.left;
            otherBranch = root.right;
        } else {
            nextBranch = root.right;
            otherBranch = root.left;
        }

        // Desce no branch mais provável
        bestDist = searchNearestNeighbor(nextBranch, target, depth + 1, accumulatedDist, bestDist);

        // Avalia o valor do nó atual
        double distance = distance_fn.distance(
                m_Instances.instance(root.node_index),
                target,
                Double.POSITIVE_INFINITY);

        if (distance < bestDist.distance) {
            bestDist = new NodeDist(root, distance);
        }

        // Distância mínima possível ao outro subespaço
        double diff = distance_fn.sqDifference(axis, targetValue, rootValue);
        double accumulatedPossible = accumulatedDist + diff;

        if (accumulatedPossible < bestDist.distance && otherBranch != null) {
            backtrackCount++;

            NodeDist novo = searchNearestNeighbor(
                    otherBranch,
                    target,
                    depth + 1,
                    accumulatedPossible,
                    bestDist);

            if (novo.distance < bestDist.distance) {
                return novo;
            }
        }

        return bestDist;
    }

    private void findNearestNeighbours(Node root, Instance target, int k, MyHeap heap,
            int depth,
            double accumulatedDist) throws Exception {

        if (root == null) {
            return;
        }

        // ------------------------------
        // 1) Processa o nó atual (POIS sua árvore tem instância em todo nó)
        // ------------------------------
        double dist;

        if (heap.size() < k) {
            dist = distance_fn.distance(
                    m_Instances.instance(root.node_index),
                    target,
                    Double.POSITIVE_INFINITY);
            heap.put(root.node_index, dist);

        } else {
            MyHeapElement worst = heap.peek(); // pior entre os k melhores
            dist = distance_fn.distance(
                    m_Instances.instance(root.node_index),
                    target,
                    worst.distance);

            if (dist < worst.distance) {
                heap.putBySubstitute(root.node_index, dist);
            } else if (dist == worst.distance) {
                heap.putKthNearest(root.node_index, dist);
            }
        }

        // ------------------------------
        // 2) Se for folha, acabou
        // ------------------------------
        if (root.isLeaf()) {
            return;
        }

        // ------------------------------
        // 3) Decide branches
        // ------------------------------
        int axis = depth % this.numDim;

        double rootValue = m_Instances.instance(root.node_index).value(axis);
        double targetValue = target.value(axis);

        Node nextBranch, otherBranch;

        if (targetValue < rootValue) {
            nextBranch = root.left;
            otherBranch = root.right;
        } else {
            nextBranch = root.right;
            otherBranch = root.left;
        }

        // ------------------------------
        // 4) Desce no branch mais provável
        // ------------------------------
        findNearestNeighbours(nextBranch, target, k, heap, depth + 1, accumulatedDist);

        // ------------------------------
        // 5) Poda (igual ao MOA e igual ao seu validador)
        // ------------------------------
        double diff = distance_fn.sqDifference(axis, targetValue, rootValue);
        double possibleDist = accumulatedDist + diff;

        if (heap.size() < k) {
            // ainda precisa visitar tudo até encher o heap
            findNearestNeighbours(otherBranch, target, k, heap, depth + 1, possibleDist);
            return;
        }

        // heap cheio → compara com o pior entre os k melhores
        if (heap.peek().distance >= possibleDist) {
            findNearestNeighbours(otherBranch, target, k, heap, depth + 1, possibleDist);
        }
    }

    protected void checkMissing(Instance ins) throws Exception {
        for (int j = 0; j < ins.numValues(); j++) {
            if (ins.index(j) != ins.classIndex())
                if (ins.isMissingSparse(j)) {
                    throw new Exception("ERROR: KDTree can not deal with missing "
                            + "values. Please run ReplaceMissingValues filter "
                            + "on the dataset before passing it on to the KDTree.");
                }
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
        insert(ins);
        // Preciso disso para poder normalizar durante o cálculo das distancias, se não
        // o backtrack fica errado
        distance_fn.setDontNormalize(false);
        distance_fn.setInstances(m_Instances); // Atualiza a normalização
    }

    private void insert(Instance ins) {
        int depth = 0;
        Node p = this.root;
        Node prev = null;
        double[] new_instance = ins.toDoubleArray();

        while (p != null) {
            prev = p;
            int axis = depth % numDim;
            if (new_instance[axis] < m_Instances.get(p.node_index).value(axis)) {
                p = p.left;
            } else {
                p = p.right;
            }

            depth++;
        }

        numNodes++;
        m_Instances.add(ins);
        if (root == null) {
            root = new Node(0, null, null, 0);
            return;
        }

        // Profundidade de prev
        int axis = (depth - 1) % numDim;
        int new_index = m_Instances.size() - 1;

        if (new_instance[axis] < m_Instances.get(prev.node_index).value(axis)) {
            prev.left = new Node(new_index, null, null, (depth % numDim));
        } else {
            prev.right = new Node(new_index, null, null, (depth % numDim));
        }

        if (this.height_tree < depth) {
            this.height_tree = depth;
        }
    }

    public void buildTree(Instances ins) {
        m_Instances = new Instances(ins);
        int[] indices = new int[ins.size()];
        for (int i = 0; i < ins.size(); i++)
            indices[i] = i;

        this.root = buildBalancedTree(indices, 0, ins.size(), 0);
    }

    private Node buildBalancedTree(int[] idx, int start, int end, int depth) {
        int n = end - start;
        if (n <= 0)
            return null;

        int axis = depth % this.numDim;

        // ---------- CORREÇÃO: precisamos de Integer[] para ordenar ----------
        Integer[] boxed = new Integer[n];
        for (int i = 0; i < n; i++)
            boxed[i] = idx[start + i];

        // Ordena apenas o intervalo usando a dimensão "axis"
        Arrays.sort(boxed, (a, b) -> Double.compare(
                m_Instances.instance(a).value(axis),
                m_Instances.instance(b).value(axis)));

        // Copia de volta para o int[] original
        for (int i = 0; i < n; i++)
            idx[start + i] = boxed[i];
        // -------------------------------------------------------------------

        int median = start + n / 2;
        int medianIndex = idx[median];

        return new Node(
                medianIndex,
                buildBalancedTree(idx, start, median, depth + 1),
                buildBalancedTree(idx, median + 1, end, depth + 1),
                axis);
    }

    public boolean isBalanced() {
        // Função que mostra tamanho minimo da árvore
        // Math: \lfloor \log_2 n \rfloor
        int height_min = (int) (logBase(2, (numNodes)));
        // poderia colocar isso:
        // height_limit = 1.44 * height_min; // Porem não sei se os valores estão certos
        return height_tree <= (height_min);
    }

    private double logBase(int base, int number) {
        return Math.log(number) / Math.log(base);
    }

    public double balancedFactor() {
        if (numNodes <= 1)
            return 1.0;

        int height_min = (int) Math.floor(logBase(2, numNodes + 1) - 1);
        int height_max = numNodes - 1;

        if (height_max == height_min)
            return 1.0;

        // Math: \frac{h_{max} - h}{h_{max}-h_{min}}
        // double factor = (double) (height_max - height_tree) / (height_max -
        // height_min);
        // Math: \frac{\log_2 n+1}{h + 1}
        double factor = logBase(2, numNodes + 1) / (height_tree + 1); // APENAS TESTE

        return factor;
    }

    public int getNumNodes() {
        return numNodes;
    }

    public int getHeightTree() {
        return height_tree;
    }

    public int getExpectedHeightTree() {
        // Math: \lfloor \log_2 n \rfloor
        return (int) logBase(2, numNodes);
    }

    ////////////// FUNÇÕES DE VALIDAÇÃO ////////////
    public void print(PrintStream out) {
        printKDTree(root, 0, out);
    }

    public void printKDTree(Node node, int depth, PrintStream out) {
        if (node == null) {
            return;
        }

        // Print current node
        out.println("Depth: " + depth + ", Split Dim: " + node.splitDim + ", Value: "
                + (m_Instances.get(node.node_index)).toString());

        // Recursively print left and right branches
        printKDTree(node.left, depth + 1, out);
        printKDTree(node.right, depth + 1, out);
    }

    // public Instance findInstanceInTree(Instance inst) {
    // int depth = 0;
    // Node p = this.root;
    // double[] instance = inst.toDoubleArray();

    // while (p != null) {
    // int axis = depth % numDim;
    // // Verifica se já achou a instancia naquele nó
    // if (inst == p.instance) {
    // return p.instance;
    // }

    // if (instance[axis] < p.instance.toDoubleArray()[axis]) {
    // p = p.left;
    // } else {
    // p = p.right;
    // }
    // depth++;
    // }

    // return null;
    // }

}

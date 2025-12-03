package moa.classifiers.lazy.neighboursearch;

import java.io.PrintStream;
import java.util.ArrayList;

import com.yahoo.labs.samoa.instances.Instance;
import com.yahoo.labs.samoa.instances.Instances;

public class KDTreeSimple extends NearestNeighbourSearch {

    public class Node {
        Node left, right, parent;
        Instance instance;
        int splitDim;

        public Node(Instance inst, Node parent, int splitDim) {
            this.instance = inst;
            this.parent = parent;
            this.splitDim = splitDim;
        }

        public Node(Instance value, Node left, Node right, int splitDim) {
            this.instance = value;
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

    public int backtrackCount = 0; // DEBUG DO BACKTRACK

    private NormalizableDistance distance_fn = new EuclideanDistance();

    public KDTreeSimple(int numDim) {
        this.numDim = numDim;
    }

    @Override
    public Instances kNearestNeighbours(Instance target, int k) throws Exception {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'kNearestNeighbours'");
    }

    @Override
    public Instance nearestNeighbour(Instance target) throws Exception {
        if (distance_fn.getInstances() == null) {
            distance_fn.setDontNormalize(true); // PARA NÃO NORMALIZAR!
            distance_fn.setInstances(new Instances(target.dataset(), 0));
        }

        Node searchInstance = searchNearestNeighbor(root, target, 0);
        return searchInstance.instance;

    }

    // AMANHA RESCREVER ISSO USANDO O HEAP E VENDO O ALGORITMO DO PROFESSOR
    private Node searchNearestNeighbor(Node root, Instance target, int depth) {

        if (root == null)
            return null;

        Node nextBranch = null;
        Node otherBranch = null;

        int axis = depth % this.numDim;

        // compare the property appropriate for the current depth
        if (target.value(axis) < root.instance.value(axis)) {
            nextBranch = root.left;
            otherBranch = root.right;
        } else {
            nextBranch = root.right;
            otherBranch = root.left;
        }

        Node temp = searchNearestNeighbor(nextBranch, target, depth + 1);

        NodeDist bestNodeDist = closest(temp, root, target);

        Node best = bestNodeDist.node;
        double bestDist = bestNodeDist.distance; // r
        double dist = Math.abs(target.value(axis) - root.instance.value(axis)); // r'

        if (dist < bestDist) {
            backtrackCount++;
            temp = searchNearestNeighbor(otherBranch, target, depth + 1);
            best = closest(temp, best, target).node;
        }

        return best;
    }

    private NodeDist closest(Node n0, Node n1, Instance target) {
        if (n0 == null) {
            return new NodeDist(n1, distance_fn.distance(n1.instance, target));
        }

        if (n1 == null)
            return new NodeDist(n0, distance_fn.distance(n0.instance, target));

        double d1 = distance_fn.distance(n0.instance, target);
        double d2 = distance_fn.distance(n1.instance, target);

        if (d1 < d2)
            return new NodeDist(n0, d1);
        else
            return new NodeDist(n1, d2);
    }

    @Override
    public double[] getDistances() throws Exception {
        throw new UnsupportedOperationException("Unimplemented method 'getDistances'");
    }

    @Override
    public void update(Instance ins) throws Exception {
        // long start = TimingUtils.getNanoCPUTimeOfCurrentThread();
        insert(ins);
        // long end = TimingUtils.getNanoCPUTimeOfCurrentThread();

        // double time = TimingUtils.nanoTimeToSeconds(end - start);

        // if (outputFile != null) {
        // outputFile.println("insert, " + time);
        // }
    }

    private void insert(Instance ins) {
        int depth = 0;
        Node p = this.root;
        Node prev = null;
        double[] new_instance = ins.toDoubleArray();

        while (p != null) {
            prev = p;
            int axis = depth % numDim;
            if (new_instance[axis] < p.instance.toDoubleArray()[axis]) {
                p = p.left;
            } else {
                p = p.right;
            }

            depth++;
        }

        numNodes++;

        if (root == null) {
            root = new Node(ins, null, 0);
            return;
        }

        // Profundidade de prev
        int axis = (depth - 1) % numDim;

        if (new_instance[axis] < prev.instance.toDoubleArray()[axis]) {
            prev.left = new Node(ins, prev, (depth % numDim));
        } else {
            prev.right = new Node(ins, prev, (depth % numDim));
        }

        if (this.height_tree < depth) {
            this.height_tree = depth;
        }
    }

    public void buildTree(Instances ins) {
        this.root = buildBalancedTree(ins, 0);
    }

    private Node buildBalancedTree(Instances insts, int depth) {
        if (insts.size() == 0) {
            return null;
        }

        int axis = depth % this.numDim;

        // Copia as instâncias para um array temporário ordenável
        ArrayList<Instance> sorted = new ArrayList<>(insts.size());
        for (int i = 0; i < insts.size(); i++) {
            sorted.add(insts.get(i));
        }

        sorted.sort((a, b) -> Double.compare(a.value(axis), b.value(axis)));

        int median = sorted.size() / 2;
        Instance medianInstance = sorted.get(median);

        // Cria conjuntos vazios com o mesmo header
        Instances left = new Instances(insts, 0);
        Instances right = new Instances(insts, 0);

        for (int i = 0; i < sorted.size(); i++) {
            if (i < median)
                left.add(sorted.get(i));
            else if (i > median)
                right.add(sorted.get(i));
        }

        return new Node(
                medianInstance,
                buildBalancedTree(left, depth + 1),
                buildBalancedTree(right, depth + 1),
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
        out.println("Depth: " + depth + ", Split Dim: " + node.splitDim + ", Value: " + node.instance);

        // Recursively print left and right branches
        printKDTree(node.left, depth + 1, out);
        printKDTree(node.right, depth + 1, out);
    }

    public Instance findInstanceInTree(Instance inst) {
        int depth = 0;
        Node p = this.root;
        double[] instance = inst.toDoubleArray();

        while (p != null) {
            int axis = depth % numDim;
            // Verifica se já achou a instancia naquele nó
            if (inst == p.instance) {
                return p.instance;
            }

            if (instance[axis] < p.instance.toDoubleArray()[axis]) {
                p = p.left;
            } else {
                p = p.right;
            }
            depth++;
        }

        return null;
    }

}

package moa.classifiers.lazy.neighboursearch;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;

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

    private Node root;
    private int numDim;

    private int height_tree;
    private int numNodes;

    private EuclideanDistance euclidian_distance = new EuclideanDistance();

    public KDTreeSimple(int numDim) {
        this.numDim = numDim;
    }

    @Override
    public Instance nearestNeighbour(Instance target) throws Exception {
        Node searchInstance = searchNearestNeighbor(root, target, 0);
        return searchInstance.instance;

    }

    @Override
    public Instances kNearestNeighbours(Instance target, int k) throws Exception {
        Node searchInstance = searchNearestNeighbor(root, target, 1);
        Instances temp = new Instances(searchInstance.instance.dataset(), 0);
        temp.add(searchInstance.instance);
        return temp;
    }

    private Node searchNearestNeighbor(Node root, Instance target, int depth) {

        if (root == null)
            return null;

        Node nextBranch = null;
        Node otherBranch = null;

        // compare the property appropriate for the current depth
        if (target.toDoubleArray()[depth % this.numDim] < root.splitDim) {
            nextBranch = root.left;
            otherBranch = root.right;
        } else {
            nextBranch = root.right;
            otherBranch = root.left;
        }

        // recurse down the branch that's best according to the current depth
        Node temp = searchNearestNeighbor(nextBranch, target, depth + 1);
        Node best = closest(temp, root, target);

        // long radiusSquared = distSquared(target, best.instance);
        double radiusSquared = euclidian_distance.distance(target, best.instance);

        /*
         * We may need to check the other side of the tree. If the other side is closer
         * than the radius,
         * then we must recurse to the other side as well. 'dist' is either a horizontal
         * or a vertical line
         * that goes to an imaginary line that is splitting the plane by the root point.
         */
        double dist = target.toDoubleArray()[depth % numDim] - root.instance.toDoubleArray()[root.splitDim];

        if (radiusSquared >= dist * dist) {
            temp = searchNearestNeighbor(otherBranch, target, depth + 1);
            best = closest(temp, best, target);
        }

        return best;
    }

    private Node closest(Node n0, Node n1, Instance target) {
        if (n0 == null)
            return n1;

        if (n1 == null)
            return n0;

        double d1 = euclidian_distance.distance(n0.instance, target);
        double d2 = euclidian_distance.distance(n1.instance, target);

        if (d1 < d2)
            return n0;
        else
            return n1;
    }

    @Override
    public double[] getDistances() throws Exception {
        throw new UnsupportedOperationException("Unimplemented method 'getDistances'");
    }

    @Override
    public void update(Instance ins) throws Exception {
        insert(ins);
    }

    private void insert(Instance ins) {
        int depth = 0;
        Node p = this.root;
        Node prev = null;

        while (p != null) {
            prev = p;
            int axis = depth % numDim;
            if (ins.toDoubleArray()[axis] < p.instance.toDoubleArray()[axis]) {
                p = p.left;
            } else {
                p = p.right;
            }

            depth++;
        }

        numNodes++;

        // Se for nulo o meu root
        if (root == null) {
            root = new Node(ins, null, 0);
            return;
        }

        // Profundidade de prev
        int axis = (depth - 1) % numDim;

        if (ins.toDoubleArray()[axis] < prev.instance.toDoubleArray()[axis]) {
            prev.left = new Node(ins, prev, (depth % numDim));
        } else {
            prev.right = new Node(ins, prev, (depth % numDim));
        }

        // Math: max(depth)
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
        ArrayList<Instance> instances_axis = new ArrayList<>();

        for (int i = 0; i < insts.size(); i++) {
            instances_axis.add(insts.get(i));
        }

        instances_axis.sort((a, b) -> Double.compare(a.value(axis), b.value(axis)));
        int median = instances_axis.size() / 2;
        Instance medianInstance = instances_axis.get(median);

        Instances instsToTheLeft = new Instances(insts, insts.size());
        Instances instsToTheRight = new Instances(insts, insts.size());

        for (int i = 0; i < instances_axis.size(); i++) {
            if (i < median)
                instsToTheLeft.add(instances_axis.get(i));
            else if (i > median)
                instsToTheRight.add(instances_axis.get(i));
        }

        // Monta a árvore em cima dessa recursão
        return new Node(medianInstance, buildBalancedTree(instsToTheLeft, (depth + 1)),
                buildBalancedTree(instsToTheRight, (depth + 1)), (depth % this.numDim));
    }

    // ESSA FUNÇÃO DE REMOÇÃO AINDA ESTÁ ERRADO, REVER ISSO NO LIVRO
    public void remove(Instance inst) {
        if (inst != null) {
            // Procura o nó
            Node p = findNode(this.root, inst, 0);
            if (p != null)
                delete(p, p.splitDim);
            // else {
            // System.out.println("NÃO ENCONTRADO O ELEMENTO PARA: " + inst.toString());
            // printInOrder();
            // }
        }
    }

    private Node findNode(Node p, Instance inst, int depth) {
        if (p == null) {
            return p;
        }

        if (Arrays.equals(p.instance.toDoubleArray(), inst.toDoubleArray()))
            return p;

        int dim = depth % numDim;
        if (inst.toDoubleArray()[dim] < p.instance.toDoubleArray()[dim]) {
            return findNode(p.left, inst, (depth + 1));
        } else {
            return findNode(p.right, inst, (depth + 1));
        }
    }

    private void delete(Node p, int discriminator) {

        if (p.isLeaf()) {
            if (p.parent != null) {
                if (p.parent.left == p)
                    p.parent.left = null;
                else if (p.parent.right == p)
                    p.parent.right = null;
            }
            return;
        }

        Node q = null;
        if (p.right != null) {
            q = smallest(p.right, discriminator, ((discriminator + 1) % numDim));
        } else {
            q = smallest(p.left, discriminator, ((discriminator + 1) % numDim));
            p.right = p.left;
            p.left = null;
        }

        // p.instance = q.instance;
        p.instance = q.instance.copy();
        delete(q, discriminator);
    }

    private Node smallest(Node q, int i, int j) {
        if (q == null) {
            return null;
        }

        Node qq = q;
        if (i == j) {
            if (q.left != null) {
                qq = q = q.left;
            } else {
                return q;
            }
        }

        if (q.left != null) {
            Node left = smallest(q.left, i, ((j + 1) % numDim));
            if (qq.instance.toDoubleArray()[i] >= left.instance.toDoubleArray()[i]) {
                qq = left;
            }
        }

        if (q.right != null) {
            Node right = smallest(q.right, i, ((j + 1) % numDim));
            if (qq.instance.toDoubleArray()[i] >= right.instance.toDoubleArray()[i]) {
                qq = right;
            }
        }

        return qq;
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

    private int height(Node root) {

        if (root == null) {
            return 0;
        }

        return 1 + (Math.max(height(root.left), height(root.right)));
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

}

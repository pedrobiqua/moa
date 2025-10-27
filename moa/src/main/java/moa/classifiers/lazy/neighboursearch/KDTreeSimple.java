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

    public KDTreeSimple(int numDim) {
        this.numDim = numDim;
    }

    @Override
    public Instance nearestNeighbour(Instance target) throws Exception {
        throw new UnsupportedOperationException("Unimplemented method 'getDistances'");
    }

    @Override
    public Instances kNearestNeighbours(Instance target, int k) throws Exception {
        throw new UnsupportedOperationException("Unimplemented method 'getDistances'");
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
        // Math: \log_2 (n+1) - 1
        int height_min = (int) logBase(2, (numNodes + 1)) - 1;
        return height_tree <= (height_min + 1);
    }

    private double logBase(int base, int number) {
        return Math.log(number) / Math.log(base);
    }

    public double balancedFactor() {
        // NÃO SEI SE ISSO ESTÁ CERTO
        if (numNodes <= 1)
            return 1.0;

        int height_min = (int) Math.floor(logBase(2, numNodes));
        int height_max = numNodes - 1;

        if (height_max == height_min)
            return 1.0;

        double factor = (double) (height_max - height_tree) / (height_max - height_min);

        return factor;
    }

    public void printInOrderToFile(String filename) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(filename))) {
            writer.println("digraph KDTree {");
            writer.println("    node [style=filled, fontname=\"Helvetica\", shape=circle];");
            inOrderToFile(this.root, writer);
            writer.println("}");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void inOrderToFile(Node node, PrintWriter writer) {
        if (node == null)
            return;

        String color;
        switch (node.splitDim % this.numDim) {
            case 0:
                color = "#FF9999";
                break;
            case 1:
                color = "#99CCFF";
                break;
            case 2:
                color = "#99FF99";
                break;
            case 3:
                color = "#FFD580";
                break;
            case 4:
                color = "#5e5bfcff";
                break;
            default:
                color = "#DDDDDD";
        }

        // Nó atual
        writer.printf("    \"%s\" [label=\"d=%s\", fillcolor=\"%s\", style=filled];\n",
                node, node.splitDim, color);

        if (node.parent != null) {
            writer.printf("    \"%s\" -> \"%s\" [label=\"P\"];\n", node, node.parent);
        }

        if (node.left != null) {
            writer.printf("    \"%s\" -> \"%s\" [label=\"L\"];\n", node, node.left);
            inOrderToFile(node.left, writer);
        }

        if (node.right != null) {
            writer.printf("    \"%s\" -> \"%s\" [label=\"R\"];\n", node, node.right);
            inOrderToFile(node.right, writer);
        }
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

}

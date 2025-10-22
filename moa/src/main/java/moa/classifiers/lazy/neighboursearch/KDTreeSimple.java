package moa.classifiers.lazy.neighboursearch;

import java.util.ArrayList;
import java.util.Collections;

import com.yahoo.labs.samoa.instances.Instance;
import com.yahoo.labs.samoa.instances.Instances;

public class KDTreeSimple extends NearestNeighbourSearch {

    public class Node {
        Node left, right;
        Instance instance;
        int splitDim;
        // boolean active;

        public Node(Instance inst, int splitDim) {
            this.instance = inst;
            this.splitDim = splitDim;
        }

        public Node(Instance value, Node left, Node right, int splitDim) {
            this.instance = value;
            this.left = left;
            this.right = right;
            this.splitDim = splitDim;
        }
    }

    private Node root;
    private int numDim;

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

        // NESSE EXEMPLO DE ARVORE A PARTIÇÃO DAS DIMENSIONALIDADES
        // É COM BASE NA PROFUNDIDADE DA ARVORE
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

        // Se for nulo o meu root
        if (root == null) {
            root = new Node(ins, 0);
            return;
        }

        // Profundidade de prev
        int axis = (depth - 1) % numDim;

        if (ins.toDoubleArray()[axis] < prev.instance.toDoubleArray()[axis]) {
            prev.left = new Node(ins, (depth % numDim));
        } else {
            prev.right = new Node(ins, (depth % numDim));
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
                buildBalancedTree(instsToTheRight, (depth + 1)), depth);
    }

    private void remove(Instance ins) {

    }

    public boolean isBalanced() {
        return isBalancedRecursive(root);
    }

    public boolean isBalancedRecursive(Node root) {
        // Verificar a altura da esquerda e da direita
        // ver se a diferença é muito grande
        if (root == null) {
            return true;
        }

        int leftHeight = height(root.left);
        int rightHeight = height(root.right);

        if (Math.abs((leftHeight - rightHeight)) > 1) {
            return false;
        }

        return isBalancedRecursive(root.left) && isBalancedRecursive(root.right);

    }

    public void printInOrder() {
        System.out.println("digraph KDTree {");
        System.out.println("    node [style=filled, fontname=\"Helvetica\", shape=circle];");
        inOrder(this.root);
        System.out.println("}");

    }

    public void inOrder(Node node) {
        if (node == null) {
            return;
        }
        String color;
        switch ((node.splitDim % this.numDim)) {
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

        System.out.printf("    \"%s\" [label=\"d=%d\", fillcolor=\"%s\"];\n", node, node.splitDim, color);

        if (node.left != null) {
            System.out.printf("    \"%s\" -> \"%s\" [label=\"L\"];\n", node, node.left);
            inOrder(node.left);
        }

        if (node.right != null) {
            System.out.printf("    \"%s\" -> \"%s\" [label=\"R\"];\n", node, node.right);
            inOrder(node.right);
        }

    }

    private int height(Node root) {

        if (root == null) {
            return 0;
        }

        return 1 + (Math.max(height(root.left), height(root.right)));
    }

}

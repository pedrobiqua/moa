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

    private void buildTree(Instances ins) {
        this.root = buildBalancedTree(ins, 0);
    }

    private Node buildBalancedTree(Instances insts, int depth) {
        if (insts.size() == 0) {
            return null;
        }

        ArrayList<Double> values_insts = new ArrayList<Double>();
        for (int i = 0; i < insts.size(); i++) {
            values_insts.add(insts.get(i).toDoubleArray()[depth]);
        }

        Collections.sort(values_insts);
        double median = values_insts.get((int) (values_insts.size() + 1) / 2 - 1);
        Instance medianInstance = null;

        Instances instsToTheLeft = new Instances(insts, insts.size());
        Instances instsToTheRight = new Instances(insts, insts.size());

        for (int i = 0; i < insts.size(); i++) {
            if (insts.get(i).toDoubleArray()[depth] == median && medianInstance == null) {
                medianInstance = insts.get(i);
            } else if (insts.get(i).toDoubleArray()[depth] < median)
                instsToTheLeft.add(insts.get(i));
            else
                instsToTheRight.add(insts.get(i));
        }

        // Monta a árvore em cima dessa recursão
        return new Node(medianInstance,
                buildBalancedTree(instsToTheLeft, (depth + 1) % this.numDim),
                buildBalancedTree(instsToTheRight, (depth + 1) % this.numDim),
                depth);
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

    private int height(Node root) {

        if (root == null) {
            return 0;
        }

        return 1 + (Math.max(height(root.left), height(root.right)));
    }

}

package moa.classifiers.lazy.neighboursearch;

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
            else
                System.out.println("NÃO ENCONTRADO O ELEMENTO PARA: " + inst.toString());
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
        if (node == null)
            return;

        String color;
        switch (node.splitDim % this.numDim) {
            case 0:
                color = "#FF9999"; // vermelho claro
                break;
            case 1:
                color = "#99CCFF"; // azul claro
                break;
            case 2:
                color = "#99FF99"; // verde claro
                break;
            case 3:
                color = "#FFD580"; // laranja claro
                break;
            case 4:
                color = "#5e5bfcff"; // roxo
                break;
            default:
                color = "#DDDDDD"; // cinza
        }

        // Nó atual
        System.out.printf("    \"%s\" [label=\"d=%s\", fillcolor=\"%s\", style=filled];\n",
                node, node.splitDim, color);

        // Se tem pai, mostra também essa relação (P -> N)
        if (node.parent != null) {
            System.out.printf("    \"%s\" -> \"%s\" [label=\"P\"];\n", node, node.parent);
        }

        // Lado esquerdo
        if (node.left != null) {
            System.out.printf("    \"%s\" -> \"%s\" [label=\"L\"];\n", node, node.left);
            inOrder(node.left);
        }

        // Lado direito
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

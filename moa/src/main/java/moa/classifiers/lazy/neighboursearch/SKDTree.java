package moa.classifiers.lazy.neighboursearch;

import javax.management.InstanceNotFoundException;

import com.yahoo.labs.samoa.instances.Instance;
import com.yahoo.labs.samoa.instances.Instances;
import com.yahoo.labs.samoa.instances.InstancesHeader;

public class SKDTree extends NearestNeighbourSearch {

    // Atributos da classe
    private int numDim;
    public SNode root;
    private EuclideanDistance distanceFunction = new EuclideanDistance();
    private double[] m_DistanceList;

    // Atributos de suporte
    private InstancesHeader instHead;

    // Atributos para coleta de metricas
    public int depthInsert;
    public int numNodes;
    public int heightTree;
    public int backtrack;
    public int maxDepthSearch;

    public SKDTree(InstancesHeader instHead) {
        // Por padrão no calculo da distância não uso normalização
        // Se for usar normalizazção eu preciso guardar as instances
        // para poder obter o min e max
        this.distanceFunction.setDontNormalize(true);
        this.distanceFunction.setInstances(instHead);
        this.m_Instances = new Instances(instHead);
    }

    public SKDTree(int numDim, InstancesHeader instHead) {
        this(instHead);
        this.numDim = numDim;
        this.instHead = instHead;
    }

    // FUNÇÕES DE INTERFACE

    @Override
    public Instance nearestNeighbour(Instance target) throws Exception {
        return (kNearestNeighbours(target, 1)).instance(0);
    }

    @Override
    public Instances kNearestNeighbours(Instance target, int k) throws Exception {
        if (this.numNodes == 0) {
            throw new Exception("The K-d tree was not initialized. Please use the method setInstances(Instances)");
        }

        this.backtrack = 0; // Reinicia a variavel que conta o número de backtracks
                            // Eu acredito que a inserção do jeito que é hoje ajuda a
                            // montar uma árvore que gera muitos backtracks
        this.maxDepthSearch = 0;

        MyHeap heap = new MyHeap(k);
        findNearestNeighbours(target, root, k, heap, 0);

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
        // Inserir instancias na árvore
        this.depthInsert = 0;
        insert(ins);
    }

    @Override
    public void setInstances(Instances insts) throws Exception {
        // AINDA NÃO ESTÁ IMPLEMENTADO
        buildTree(insts);
    }

    public void remove(Instance inst) throws Exception {
        SNode nodeToRemove = search(inst.toDoubleArray(), root);

        if (nodeToRemove == null)
            throw new InstanceNotFoundException(
                    "Instance not found on KDTree. Is there any missing data on the dataset?");

        // if (nodeToRemove.isALeaf()) { // Apenas para debug
        // System.out.println("Removendo uma instancia folha node index: " +
        // nodeToRemove.index);
        // }
        delete(nodeToRemove);
    }

    // FUNÇÕES INTRERNAS
    private void insert(Instance inst) {
        int depth = 0;
        SNode p = this.root;
        SNode prev = null;
        double[] newInstance = inst.toDoubleArray();

        while (p != null) {
            prev = p;
            int axis = depth % this.numDim;
            if (newInstance[axis] < p.inst[axis]) {
                p = p.left;
            } else {
                p = p.right;
            }

            depth++;
        }

        this.numNodes++;
        this.depthInsert = depth;

        m_Instances.add(inst);

        if (root == null) {
            root = new SNode(newInstance, null, null, null, 0, 0);
            return;
        }

        // Profundidade de prev
        int axis = (depth - 1) % this.numDim;

        if (newInstance[axis] < prev.inst[axis])
            prev.left = new SNode(newInstance, null, null, prev, m_Instances.size() - 1, (depth % this.numDim));
        else
            prev.right = new SNode(newInstance, null, null, prev, m_Instances.size() - 1, (depth % this.numDim));

        if (this.heightTree < depth) {
            this.heightTree = depth;
        }
    }

    private void delete(SNode node) throws Exception {
        // PRECISO VERIFICAR SE ISSO PODE GERAR BUGS, POIS NÃO SEI SE QUERO REMOVER OS
        // NÓS FOLHAS.
        // VOU DEIXAR, POREM COM A RESALVA QUE ISSO PODE OU NÃO INTERFERIR NA BUSCA
        if (node.isALeaf() && node.parent != null) {
            SNode prev = node.parent;
            if (prev.left == node)
                prev.left = null;
            else if (prev.right == node) {
                prev.right = null;
            } else {
                throw new Exception("Não foi possivel remover o nó");
            }
            numNodes--;
            return;
        }
        node.active = false;
    }

    public SNode search(double[] instTarget, SNode node) {
        if (node == null)
            return null;

        if (isInstanceEqual(node.inst, instTarget) && node.isActive())
            return node;

        int axis = node.splitDim;

        if (instTarget[axis] < node.inst[axis])
            return search(instTarget, node.left);
        else
            return search(instTarget, node.right);

    }

    private boolean isInstanceEqual(double[] instanceTree, double[] instanceTarget) {
        if (instanceTree.length != instanceTarget.length) {
            return false;
        }

        for (int i = 0; i < instanceTree.length; i++) {
            if (instanceTree[i] != instanceTarget[i]) {
                return false;
            }
        }
        return true;
    }

    private void findNearestNeighbours(Instance target, SNode node, int k,
            MyHeap heap, int depth) throws Exception {

        if (node == null) {
            return;
        }

        if (depth > this.maxDepthSearch) {
            this.maxDepthSearch = depth;
        }

        SNode best, other;
        if (target.value(node.splitDim) < node.inst[node.splitDim]) {
            best = node.left;
            other = node.right;
        } else {
            best = node.right;
            other = node.left;
        }

        findNearestNeighbours(target, best, k, heap, depth + 1);

        if (node.isActive()) {
            double distNode;
            if (heap.size() < k) {
                distNode = distanceFunction.distance(
                        node.toInstance(instHead),
                        target,
                        Double.POSITIVE_INFINITY);
                heap.put(node.index, distNode);
            } else {
                MyHeapElement worst = heap.peek();
                distNode = distanceFunction.distance(
                        node.toInstance(instHead),
                        target,
                        worst.distance);
                if (distNode < worst.distance) {
                    heap.putBySubstitute(node.index, distNode);
                } else if (distNode == worst.distance) {
                    heap.putKthNearest(node.index, distNode);
                }
            }
        }

        double planeDist = distanceFunction.sqDifference(
                node.splitDim,
                target.value(node.splitDim),
                node.inst[node.splitDim]);

        if (heap.size() < k || planeDist <= heap.peek().distance) {
            this.backtrack++;
            findNearestNeighbours(target, other, k, heap, depth + 1);
        }
    }

    private void buildTree(Instances insts) {
        throw new UnsupportedOperationException("Unimplemented method 'setInstances'");
    }

    // FUNÇÕES DE PRINT PARA VISUALIZAR CONJUNTOS PEQUENOS USADOS APENAS NO DEV
    public void print() {
        System.out.println("digraph KDTree {");
        System.out.println("node [shape=circle];");

        inorder(root);

        System.out.println("}");
    }

    private void inorder(SNode node) {
        if (node == null) {
            return;
        }

        // visita esquerda
        inorder(node.left);

        // imprime o nó | FUNCIONA APENAS PARA O DATASET DE TESTE
        String nodeId = nodeId(node);
        String label = "(" + node.index + ") [" + node.splitDim + "]";

        if (!node.active) {
            System.out.println(
                    nodeId + " [label=\"" + label + "\", style=filled, fillcolor=red];");
        } else {
            System.out.println(
                    nodeId + " [label=\"" + label + "\"];");
        }

        // aresta esquerda
        if (node.left != null) {
            System.out.println(
                    nodeId + " -> " + nodeId(node.left) + " [label=\"L\"];");
        }

        // aresta direita
        if (node.right != null) {
            System.out.println(
                    nodeId + " -> " + nodeId(node.right) + " [label=\"R\"];");
        }

        // visita direita
        inorder(node.right);
    }

    private String nodeId(SNode node) {
        return "n" + System.identityHashCode(node);
    }

}

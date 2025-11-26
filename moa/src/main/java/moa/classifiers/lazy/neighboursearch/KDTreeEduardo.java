package moa.classifiers.lazy.neighboursearch;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.io.PrintStream;
import java.io.Serializable;

import javax.management.InstanceNotFoundException;

import org.codehaus.plexus.personality.plexus.lifecycle.phase.InitializationException;

import com.yahoo.labs.samoa.instances.Instance;
import com.yahoo.labs.samoa.instances.Instances;
import com.yahoo.labs.samoa.instances.Attribute;

public class KDTreeEduardo extends NearestNeighbourSearch {

    public static class AttributesUtils {
        public static List<Attribute> copyAtributes(Instances originalDataset) {
            List<Attribute> atributos = new ArrayList<Attribute>(originalDataset.numAttributes());
            for (int i = 0; i < originalDataset.numAttributes(); i++) {
                Attribute atribOriginal = originalDataset.attribute(i);
                atributos.add(atribOriginal);
            }
            return atributos;
        }

        public static ArrayList<Attribute> copyAtributes(Instance instance) {
            ArrayList<Attribute> atributes = new ArrayList<Attribute>(instance.numAttributes());
            for (int i = 0; i < instance.numAttributes(); i++) {
                Attribute atribOriginal = instance.attribute(i);
                atributes.add(atribOriginal);
            }
            return atributes;
        }
    }

    public static class InstancesUtils {

        public static Instances gerarDataset(List<Instance> instancias, String nomeDataset) throws Exception {
            ArrayList<Attribute> atributos = AttributesUtils.copyAtributes(instancias.get(0));

            Instances retorno = new Instances(nomeDataset, atributos,
                    instancias.size());
            for (Instance inst : instancias) {
                retorno.add(inst);
            }

            retorno.setClassIndex(retorno.numAttributes() - 1);

            return retorno;
        }
    }

    public class KDTreeNode implements Serializable {

        private static final long serialVersionUID = 1L;
        public Instance m_InstanceInfo;
        public int splitDim;
        public KDTreeNode m_Left = null;
        public KDTreeNode m_Right = null;
        private boolean flag = true;
        public double[] lVex;
        public double[] uVex;
        public double[][] rect = { lVex, uVex };
        public ArrayList<KDTreeNode> children;

        public double[][] m_nodeRanges;

        public KDTreeNode() throws Exception {
        }

        public KDTreeNode(Instance inst, int splitDim) throws Exception {
            this.m_InstanceInfo = inst;
            this.splitDim = splitDim;
            if (this.splitDim < 0)
                this.splitDim = 0;
        }

        public void setInfo(Instance inst) {
            this.m_InstanceInfo = inst;
        }

        public Instance getInstance() {
            return this.m_InstanceInfo;
        }

        public double getSplitVal(int splitDim) {
            return this.m_InstanceInfo.toDoubleArray()[splitDim];
        }

        public double[] getInfo() {
            return this.m_InstanceInfo.toDoubleArray();
        }

        public void setFlagFalse() {
            this.flag = false;
        }

        public void setFlagTrue() {
            this.flag = true;
        }

        public boolean isNodeActive() {
            return this.flag;
        }

        public boolean isALeaf() {
            return (this.m_Left == null && this.m_Right == null);
        }

        public void updateRange(Instance inst) {
            double[] instInfo = inst.toDoubleArray();
            if (lVex == null) {
                lVex = instInfo;
                uVex = instInfo;
            } else {
                for (int i = 0; i < instInfo.length; i++) {
                    if (instInfo[i] < this.lVex[i]) {
                        this.lVex[i] = instInfo[i];
                    }
                    if (instInfo[i] > this.uVex[i]) {
                        this.uVex[i] = instInfo[i];
                    }
                }
            }
        }

        public boolean isInRange(Instance inst) {
            double[] instInfo = inst.toDoubleArray();
            boolean inRange = true;

            for (int i = 0; i < instInfo.length; i++) {
                if ((instInfo[i] < lVex[i]) || instInfo[i] > uVex[i]) {
                    inRange = false;
                    break;
                }
            }

            return inRange;
        }

        public double uDist(double[] p) {
            double[] v = getV(p);

            double[] sub = new double[v.length];

            for (int i = 0; i < sub.length; ++i) {
                sub[i] = v[i] - p[i];
            }

            double sum = 0;

            for (int i = 0; i < v.length; ++i) {
                double dat = sub[i];
                sum += dat * dat;
            }

            return Math.sqrt(sum);

        }

        public double[] getV(double[] p) {
            double[] cent = this.getCent();

            double[] v = this.getU(p);

            double[] dVec = this.getDVec();

            double[] u = this.getU(p);

            for (int i = 0; i < v.length; ++i) {
                v[i] = cent[i] + (dVec[i] * this.Gx(u[i]));
            }

            return v;
        }

        public double[] getDVec() {
            double[] cent = this.getCent();

            double[] dVec = new double[cent.length];

            for (int i = 0; i < cent.length; ++i) {
                dVec[i] = uVex[i] - cent[i];
            }
            return dVec;
        }

        public double[] getU(double[] p) {
            double[] cent = this.getCent();

            double[] u = new double[p.length];

            for (int i = 0; i < u.length; ++i) {
                u[i] = cent[i] - p[i];
            }
            return u;
        }

        public double[] getCent() {

            double[] cent = new double[this.lVex.length];

            for (int i = 0; i < cent.length; ++i) {
                cent[i] = (uVex[i] + lVex[i]) / 2;
            }

            return cent;
        }

        public double Gx(double x) {
            if (x >= 0)
                return 1;
            else
                return -1;
        }

        public double Ix(double x) {
            if (x >= 0)
                return 1;
            else
                return 0;
        }

        public double[][] getNodeRange() {

            double[][] range = { this.lVex, this.uVex };

            return range;
        }

    }

    private static final long serialVersionUID = 1L;

    private int nDims = 0;
    private int numInstances = 0;
    int numNeighbours = 5;

    protected ArrayList<Instance> instancesList = new ArrayList<Instance>();

    double[][] ranges;

    private KDTreeNode m_Root = null;

    private int numNodesDeactivated = 0;

    private NormalizableDistance distanceFunction;

    private int initialNumInstances = 0;

    private int nodesVisited = 0;

    public KDTreeEduardo() {
        super();
        this.distanceFunction = new EuclideanDistance();
        this.distanceFunction.setDontNormalize(true);
    }

    public KDTreeEduardo(Instances instances) throws Exception {
        super(instances);
        this.nDims = instances.get(0).numAttributes() - 1;
        this.buildKDTree(instances);
        this.distanceFunction = new EuclideanDistance();
        this.distanceFunction.setDontNormalize(true);
    }

    public KDTreeEduardo(Instances instances, NormalizableDistance distanceFunction) {
        super(instances);
        this.nDims = instances.get(0).numAttributes() - 1;
        this.distanceFunction = distanceFunction;
    }

    public KDTreeEduardo(Instances instances, NormalizableDistance distanceFunction, int numNeighbours) {
        super(instances);
        this.nDims = instances.get(0).numAttributes() - 1;
        this.distanceFunction = distanceFunction;
        this.numNeighbours = numNeighbours;
    }

    @Override
    public Instance nearestNeighbour(Instance target) throws Exception {

        kNearestNeighbours(target, 1);

        return this.instancesList.get(0);
    }

    public int getNumInstances() {
        return this.numInstances;
    }

    protected ArrayList<Double> getDistancesOfBranches(KDTreeNode node, Instance target, ArrayList<Double> distances) {
        if (node == null)
            return distances;

        this.nodesVisited++;

        if (node.isNodeActive()) {
            double distanceToNode = this.distanceFunction.distance(target, node.getInstance());
            // Add distance if within limits
            if (distances.size() < this.numNeighbours) {
                distances.add(distanceToNode);
                this.instancesList.add(node.getInstance());
            } else {
                int maxIndex = findMaxIndex(distances);
                if (distanceToNode < distances.get(maxIndex)) {
                    distances.set(maxIndex, distanceToNode);
                    this.instancesList.set(maxIndex, node.getInstance());
                }
            }
        }

        double[] targetInfo = target.toDoubleArray();
        KDTreeNode best = (targetInfo[node.splitDim] >= node.getInfo()[node.splitDim]) ? node.m_Right : node.m_Left;
        KDTreeNode other = (best == node.m_Right) ? node.m_Left : node.m_Right;

        // Search the best branch
        if (shouldSearchNode(node, target, distances)) {
            getDistancesOfBranches(best, target, distances);
        }

        // Check the other branch if necessary
        if (shouldSearchNode(node, target, distances)) {
            getDistancesOfBranches(other, target, distances);
        }

        return distances;
    }

    private int findMaxIndex(ArrayList<Double> distances) {
        double maxDistance = distances.get(0);
        int maxIndex = 0;
        for (int i = 0; i < distances.size(); i++) {
            if (distances.get(i) > maxDistance) {
                maxDistance = distances.get(i);
                maxIndex = i;
            }
        }
        return maxIndex;
    }

    public int getNumNodesVisited() {
        return this.nodesVisited;
    }

    @Override
    public Instances kNearestNeighbours(Instance target, int k) throws Exception {

        this.numNeighbours = k;
        this.nodesVisited = 0;

        this.instancesList.clear();
        this.getDistancesOfBranches(m_Root, target, new ArrayList<Double>());

        // if (this.instancesList.size() < k) {
        // System.out.println("Not enough neighbors");
        // }

        // System.out.println(this.nodesVisited + " " + this.numInstances);

        Instances insts = InstancesUtils.gerarDataset(this.instancesList, "Neighbors found");

        return insts;

    }

    @Override
    public double[] getDistances() throws Exception {
        return null;
    }

    @Override
    public void update(Instance ins) throws Exception {
        if (this.numInstances == 0) {
            throw new InitializationException("Tree was not created. "
                    + "Please call the BuildKDTree method first");
        }
        this.insert(ins);
    }

    private void buildKDTreeBalanced(ArrayList<Instance> insts, int depth) throws Exception {
        ArrayList<Double> values = new ArrayList<Double>();

        if (insts.size() == 0)
            return;

        if (insts.size() == 1) {
            this.insert(insts.get(0));
            ;
            return;
        }

        if (insts.size() == 2) {
            Instance inst1 = insts.get(0);
            Instance inst2 = insts.get(1);

            if (inst1.toDoubleArray()[depth] >= inst2.toDoubleArray()[depth]) {
                this.insert(inst1);
                this.insert(inst2);
                return;
            } else {
                this.insert(inst2);
                this.insert(inst1);
                return;
            }

        }

        for (int i = 0; i < insts.size(); i++) {
            values.add(insts.get(i).toDoubleArray()[depth]);
        }

        double median = getMedian(values);

        Instance medianInstance = null;

        ArrayList<Instance> instancesToTheLeft = new ArrayList<Instance>();
        ArrayList<Instance> instancesToTheRight = new ArrayList<Instance>();

        for (int i = 0; i < insts.size(); i++) {
            if (insts.get(i).toDoubleArray()[depth] == median && medianInstance == null) {
                medianInstance = insts.get(i);
            } else if (insts.get(i).toDoubleArray()[depth] < median)
                instancesToTheLeft.add(insts.get(i));
            else
                instancesToTheRight.add(insts.get(i));
        }

        this.insert(medianInstance);

        buildKDTreeBalanced(instancesToTheLeft, (depth + 1) % this.nDims);
        buildKDTreeBalanced(instancesToTheRight, (depth + 1) % this.nDims);

    }

    protected double getMedian(ArrayList<Double> values) {

        Collections.sort(values);
        return values.get((int) (values.size() + 1) / 2 - 1);

    }

    @Override
    public void setInstances(Instances instances) throws Exception {
        super.setInstances(instances);
        if (this.nDims == 0) {
            this.nDims = instances.get(0).numAttributes() - 1;
        }
        this.distanceFunction.setInstances(instances);
        this.buildKDTree(instances);
    }

    public void buildKDTree(Instances instances) throws Exception {

        ArrayList<Instance> insts = new ArrayList<Instance>();

        for (int i = 0; i < instances.size(); ++i) {
            insts.add(instances.get(i));
        }

        this.buildKDTreeBalanced(insts, 0);

        this.initialNumInstances = this.numInstances;
    }

    protected void insert(Instance inst) throws Exception {

        KDTreeNode p = this.m_Root;
        KDTreeNode prev = null;
        double[] info = inst.toDoubleArray();

        int i = 0;

        while (p != null) {
            prev = p;
            // prev.updateRange(inst);
            if (info[i] < p.getInfo()[i])
                p = p.m_Left;
            else
                p = p.m_Right;
            i = (i + 1) % nDims;
        }

        int index = (i - 1) % nDims;
        if (index < 0)
            index = nDims - 1;

        if (this.m_Root == null)
            this.m_Root = new KDTreeNode(inst, i);
        else if (info[index] < prev.getInfo()[index])
            prev.m_Left = new KDTreeNode(inst, i);
        else
            prev.m_Right = new KDTreeNode(inst, i);
        this.numInstances++;
    }

    public void removeInstance(Instance inst) throws Exception {

        KDTreeNode nodeToRemove = search(inst, m_Root);

        if (nodeToRemove == null)
            throw new InstanceNotFoundException(
                    "Instance not found on KDTree. Is there any missing data on the dataset?");

        deleteBack(nodeToRemove);
        this.numInstances--;
    }

    public KDTreeNode search(Instance inst, KDTreeNode node) throws Exception {
        double[] instInfo = inst.toDoubleArray();

        if (node == null) {
            return null;
        }

        if (isInstanceEqual(inst, node.getInstance()) && node.isNodeActive())
            return node;

        KDTreeNode nodeToReturn = null;

        if (instInfo[node.splitDim] < node.getInfo()[node.splitDim]) {
            nodeToReturn = search(inst, node.m_Left);
        } else {
            nodeToReturn = search(inst, node.m_Right);
        }

        return nodeToReturn;

    }

    public boolean isToRebuild() {

        boolean retorno = false;

        if (((double) this.numNodesDeactivated / (double) this.numInstances >= 0.3)) {
            retorno = true;
        }

        if (this.numInstances > this.initialNumInstances * 2) {
            retorno = true;
        }

        return retorno;
    }

    private boolean isInstanceEqual(Instance inst1, Instance inst2) {

        boolean found = true;

        double[] infoInst1 = inst1.toDoubleArray();
        double[] infoInst2 = inst2.toDoubleArray();

        for (int i = 0; i < infoInst1.length; i++) {
            if (infoInst1[i] != infoInst2[i]) {
                found = false;
                break;
            }
        }
        return found;
    }

    protected void deleteBack(KDTreeNode p) throws Exception {

        if (p.isALeaf()) {
            p = null;
            return;
        }
        p.setFlagFalse();
        this.numNodesDeactivated++;
    }

    @Override
    public Instances getInstances() {
        Instances insts = new Instances();
        for (Instance inst : this.instancesList) {
            insts.add(inst);
        }
        return insts;
    }

    private boolean shouldSearchNode(KDTreeNode node, Instance target, ArrayList<Double> distances) {

        if (distances.size() < this.numNeighbours) {
            return true;
        }

        double maximum = Collections.max(distances);
        int splitDim = node.splitDim;

        double minimumDistance = target.toDoubleArray()[splitDim] - node.getInfo()[splitDim];
        // double absv = FastMath.abs(minimumDistance);
        double absv = minimumDistance * minimumDistance;

        if (absv <= maximum)
            return true;

        return false;
    }

    /////////////////////////////////
    // PEDRO APENAS ALTEROU ISSO, RESTANTE COPIADO DO REPOSITÓRIO ORIGINAL
    public void print(PrintStream out) {
        printKDTree(m_Root, 0, out);
    }

    public void printKDTree(KDTreeNode node, int depth, PrintStream out) {
        if (node == null) {
            return;
        }

        // Print current node
        out.println("Depth: " + depth + ", Split Dim: " + node.splitDim + ", Value: " + node.getInstance());

        // Recursively print left and right branches
        printKDTree(node.m_Left, depth + 1, out);
        printKDTree(node.m_Right, depth + 1, out);
    }
    /////////////////////////////////

}
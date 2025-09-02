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
package moa.classifiers.lazy.neighboursearch;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;

import javax.management.InstanceNotFoundException;

import org.apache.commons.math3.util.FastMath;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.InitializationException;

import com.yahoo.labs.samoa.instances.Instance;
import com.yahoo.labs.samoa.instances.Instances;
import com.yahoo.labs.samoa.instances.InstancesHeader;

import moa.classifiers.lazy.neighboursearch.kdtrees.StreamNeighborSearch;

/**
 * KDTreeCanberra
 *
 * KDTree using Canberra distance.
 *
 * A significant part of this implementation is based on the original code
 * developed by Eduardo V.L. Barboza and Paulo R. Lisboa de Almeida, as part
 * of the research work:
 *
 * IncA-DES: An incremental and adaptive dynamic ensemble selection approach
 * using online K-d tree neighborhood search for data streams with concept drift
 * Eduardo V.L. Barboza, Paulo R. Lisboa de Almeida, Alceu de Souza Britto Jr.,
 * Robert Sabourin, Rafael M.O. Cruz
 *
 * This version has been adapted and modified by
 * Pedro Bianchini de Quadros (pedro.bianchini@ufpr.br)
 * to fit into the MOA framework.
 *
 * @author Pedro Bianchini de Quadros (pedro.bianchini@ufpr.br)
 * @version $Revision: 1 $
 */
public class KDTreeCanberra extends NearestNeighbourSearch implements StreamNeighborSearch {
	// AVISO: Coloquei tudo aqui dentro para não poluir a estrutura do MOA, não sei se posso criar um novo tipo de node
    private class Node implements Serializable {
        Instance instance;
        Node left;
        Node right;
        int splitDim;
        boolean active;

        public Node(Instance inst, int splitDim) {
            this.left = null;
            this.right = null;
            this.instance = inst;
            this.splitDim = splitDim;
            this.active = true;
        }

        public double[] getInfo() {
		    return this.instance.toDoubleArray();
	    }

        public boolean isNodeActive() {
		    return this.active;
	    }

        public Instance getInstance() {
            return instance;
        }

        public boolean isALeaf() {
		    return (this.left == null && this.right == null);
        }

        public void setActiveFalse() {
            this.active = false;
        }
    }

	public class CanberraDistance extends NormalizableDistance implements Cloneable, Serializable {

		public CanberraDistance() {
		}

		public CanberraDistance(Instances data) {
			super(data);
		}

		@Override
		public double distance(Instance first, Instance second) {
			double[] x = first.toDoubleArray();
			double[] y = second.toDoubleArray();

			int classIndex = first.classIndex();

			double sum = 0;

			for (int i = 0; i < x.length; i++) {
				if (i != classIndex) {
					double numerator = FastMath.abs(x[i]-y[i]);
					double denominator = FastMath.abs(x[i]) + FastMath.abs(y[i]);
					if (denominator != 0)
						sum += numerator / denominator;
				}
			}

			return sum;
		}

		@Override
		public double distance(Instance first, Instance second, double cutOffValue) {
			double[] x = first.toDoubleArray();
			double[] y = second.toDoubleArray();

			int classIndex = first.classIndex();

			double sum = 0;

			for (int i = 0; i < x.length; i++) {
				if (i != classIndex) {
					double numerator = FastMath.abs(x[i]-y[i]);
					double denominator = FastMath.abs(x[i]) + FastMath.abs(y[i]);
					if (denominator != 0)
						sum += numerator / denominator;
				}
			}

			return sum;
		}

		public double canbDifference(int index, double val1, double val2) {
			double val = difference(index, val1, val2);
			return Math.abs(val);
		}

		public double sqDifference(int index, double val1, double val2) {
			double val = difference(index, val1, val2);
			return val*val;
		}

		protected double updateDistance(double currDist, double diff) {

			return 0;
		}

		public boolean valueIsSmallerEqual(Instance instance,
				int dim, double value) {  //This stays
			return instance.value(dim) <= value;
		}

		public boolean valueIsSmaller(Instance instance,
				int dim, double value) {  //This stays
			return instance.value(dim) < value;
		}

		public boolean valueIsSmaller(double instanceValue,
				int dim, double value) {  //This stays
			return instanceValue == value;
		}

		public boolean valueIsEqual(Instance instance,
				int dim, double value) {  //This stays
			return instance.value(dim) == value;
		}



		public boolean valueIsEqual(double instanceValue,
				int dim, double value) {  //This stays
			return instanceValue == value;
		}

		@Override
		public String globalInfo() {
			return "Implementing Canberra Distance.";
		}

		public int closestPoint(Instance instance, Instances allPoints,
				int[] pointList) throws Exception {
			double minDist = Integer.MAX_VALUE;
			int bestPoint = 0;
			for (int i = 0; i < pointList.length; i++) {
				double dist = distance(instance, allPoints.instance(pointList[i]), Double.POSITIVE_INFINITY);
				if (dist < minDist) {
					minDist = dist;
					bestPoint = i;
				}
			}
			return pointList[bestPoint];
		}
	}

	private static final long serialVersionUID = 1505717283763272535L;
	private static final int factor = 20;

    private final int DEPTH = 0;
    private Node root;
    private int nDims = 0;
    private int numInstances = 0;
    private int numNodesDeactivated = 0;
    private int initialNumInstances = 0;
	private int numNeighbours = 5;
	private NormalizableDistance distanceFunction;
	private double a;
	private ArrayList<Instance> instancesList = new ArrayList<>();

    public KDTreeCanberra() {
		super();
		this.distanceFunction = new CanberraDistance();
	}

	public KDTreeCanberra(Instances instances) throws Exception {
		super(instances);
		this.nDims = instances.get(0).numAttributes()-1;
		this.buildKDTree(instances);
		this.distanceFunction = new CanberraDistance();
		this.a = this.nDims/factor;
		this.a = FastMath.max(a, 1);
	}

	public KDTreeCanberra(Instances instances, int numNeighbours) {
		super(instances);
		this.nDims = instances.get(0).numAttributes()-1;
		this.distanceFunction = new CanberraDistance();
		this.numNeighbours = numNeighbours;
		this.a = this.nDims/factor;
		this.a = FastMath.max(a, 1);
	}

	public int getNumInstances() {
		return numInstances;
	}

    @Override
    public Instance nearestNeighbour(Instance target) throws Exception {

        Instances dist = kNearestNeighbours(target, 1);
		return dist.get(0);
    }

    @Override
    public Instances kNearestNeighbours(Instance target, int k) throws Exception {
		if (this.numInstances == 0) {
			throw new Exception("The K-d tree was not initialized. Please use the method setInstances(Instances)");
		}

		InstancesHeader header = (InstancesHeader) target.dataset();

		this.numNeighbours = k;

		ArrayList<Double> distances =  getDistancesOfBranches(root, target);

		int kNeighbors;

		if (distances.size() < this.numNeighbours)
			kNeighbors = distances.size();
		else
			kNeighbors = this.numNeighbours;


		Instances insts = new Instances(header);
		for (int i = 0; i < kNeighbors; i++) {
			insts.add(instancesList.get(i));
		}

		return insts;
    }

	protected ArrayList<Double> getDistancesOfBranches(Node node, Instance target) {

		ArrayList<Double> distances = new ArrayList<Double>();

		this.instancesList.clear();

		double[] targetInfo = target.toDoubleArray();

		if (node.isNodeActive()) {
			double distanceToNode = this.distanceFunction.distance(node.getInstance(), target);
				distances.add(distanceToNode);
				this.instancesList.add(node.getInstance());
		}

		Node best = null;
		Node other = null;

		if (node.right != null && node.left != null) {

			if (targetInfo[node.splitDim] >= node.getInfo()[node.splitDim]) {
				best = node.right;
				other = node.left;
			} else {
				best = node.left;
				other = node.right;
			}
		} else if (node.right != null) {
			best = node.right;
		} else if (node.left != null) {
			best = node.left;
		} else return distances;


		distances = getDistancesOfBranches(best, target, distances);

		double maximum = Double.NEGATIVE_INFINITY;
		for (int i = 0; i < distances.size(); i++) {
			double toTest = distances.get(i);
			if (toTest > maximum)
				maximum = toTest;
		}

		if (other != null) {
			if (isToSearchNode(node, target, node.splitDim, maximum) || distances.size() < this.numNeighbours) {
				distances = getDistancesOfBranches(other, target, distances);
			}
		}

		return distances;
	}

	protected ArrayList<Double> getDistancesOfBranches(Node node, Instance target, ArrayList<Double> distances) {

		if (node == null)
			return distances;


		// this.nodesSearched++;


		if (node.isNodeActive()) {
			double distanceToNode = this.distanceFunction.distance(node.getInstance(), target);
			if (distances.size() < this.numNeighbours) {
				distances.add(distanceToNode);
				this.instancesList.add(node.getInstance());
		}
		else {
			double maximum = distances.get(0);
			int maxIndex = 0;
			for (int i = 0; i < distances.size(); i++) {
				double toTest = distances.get(i);
				if (toTest > maximum) {
					maximum = toTest;
					maxIndex = i;
				}
			}
			if (distanceToNode <= distances.get(maxIndex)) {
				distances.remove(maxIndex);
				distances.add(distanceToNode);
				this.instancesList.remove(maxIndex);
				this.instancesList.add(node.getInstance());
			}
		}
		}

		double[] targetInfo = target.toDoubleArray();

		if (node.right != null && node.left != null) {

			Node best = null;
			Node other = null;

			if (targetInfo[node.splitDim] >= node.getInfo()[node.splitDim]) {
				best = node.right;
				other = node.left;
			} else {
				best = node.left;
				other = node.right;
			}

			double maximum = Double.NEGATIVE_INFINITY;

			for (int i = 1; i < distances.size(); i++) {
				double toTest = distances.get(i);
				if (toTest > maximum)
					maximum = toTest;
			}

			if (isToSearchNode(node, target, node.splitDim, maximum) || distances.size() < this.numNeighbours) {
				distances = getDistancesOfBranches(best, target, distances);
			}

			maximum = Double.NEGATIVE_INFINITY;

			for (int i = 1; i < distances.size(); i++) {
				double toTest = distances.get(i);
				if (toTest > maximum)
					maximum = toTest;
			}

			if (other != null) {
				if (isToSearchNode(node, target, node.splitDim, maximum) || distances.size() < this.numNeighbours) {
					distances = getDistancesOfBranches(other, target, distances);
				}
			}
		} else if (node.right != null) {
			distances = getDistancesOfBranches(node.right, target, distances);
		} else if (node.left != null) {
			distances = getDistancesOfBranches(node.left, target, distances);
		}

		return distances;
	}


    @Override
    public double[] getDistances() throws Exception {
		return null;
    }

    @Override
    public void update(Instance ins) throws Exception {
        if (numInstances == 0) {
            throw new InitializationException("Tree was not created. "
					+ "Please call the BuildKDTree method first");
        }
        insertKDTreeNode(ins);
    }

    @Override
    public void setInstances(Instances insts) throws Exception {
        super.setInstances(insts);
		if (this.nDims == 0){
			this.nDims = insts.get(0).numAttributes()-1;
			this.a = this.nDims/factor;
			this.a = FastMath.max(a, 1);
		}
		this.distanceFunction.setInstances(insts);
        buildKDTree(insts);
    }

    @Override
    public void removeInstance(Instance inst) throws Exception {
        removeKDTreeNode(inst);
    }

    @Override
    public boolean isToRebuild() {
        // AQUI VAI SER MONTADO A POLITICA DE RECRIAÇÃO DA ARVORE
        boolean isRebuild = false;
		if (((double) this.numNodesDeactivated / (double) this.numInstances >= 0.3)) {
			isRebuild = true;
		}

		if (this.numInstances > this.initialNumInstances*2) {
			isRebuild = true;
		}
		return isRebuild;
    }

    private void buildKDTreeBalanced(ArrayList<Instance> insts, int depth) throws Exception {
		ArrayList<Double> values = new ArrayList<Double>();

		if (insts.size() == 0)
			return;

		if (insts.size() == 1){
			this.insertKDTreeNode(insts.get(0));
			return;
		}

			if (insts.size() == 2) {
				Instance inst1 = insts.get(0);
				Instance inst2 = insts.get(1);

				if (inst1.toDoubleArray()[depth] >= inst2.toDoubleArray()[depth]) {
					this.insertKDTreeNode(inst1);
					this.insertKDTreeNode(inst2);
					return;
				} else {
					this.insertKDTreeNode(inst2);
					this.insertKDTreeNode(inst1);
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
				if(insts.get(i).toDoubleArray()[depth] == median && medianInstance == null) {
					medianInstance = insts.get(i);
				}
				else if (insts.get(i).toDoubleArray()[depth] < median)
					instancesToTheLeft.add(insts.get(i));
				else
					instancesToTheRight.add(insts.get(i));
			}

		this.insertKDTreeNode(medianInstance);

		buildKDTreeBalanced(instancesToTheLeft, (depth+1)%this.nDims);
		buildKDTreeBalanced(instancesToTheRight, (depth+1)%this.nDims);

	}

    public void buildKDTree(Instances instances) throws Exception{
        buildKDTreeBalanced(instances, 0);
        initialNumInstances = numInstances;
    }

    private void buildKDTreeBalanced(Instances instances, int depth) throws Exception {
        ArrayList<Double> values = new ArrayList<Double>();

		if (instances.size() == 0)
			throw new InstanceNotFoundException("Instance list is empty.");

		if (instances.size() == 1){
			this.insertKDTreeNode(instances.get(0));
			return;
		}

		if (instances.size() == 2) {
			Instance inst1 = instances.get(0);
			Instance inst2 = instances.get(1);

			if (inst1.toDoubleArray()[depth] >= inst2.toDoubleArray()[depth]) {
				this.insertKDTreeNode(inst1);
				this.insertKDTreeNode(inst2);
			} else {
				this.insertKDTreeNode(inst2);
				this.insertKDTreeNode(inst1);
			}
			return;
		}

		for (int i = 0; i < instances.size(); i++) {
			values.add(instances.get(i).toDoubleArray()[depth]);
		}

		double median = getMedian(values);

		Instance medianInstance = null;

		ArrayList<Instance> instancesToTheLeft = new ArrayList<Instance>();
		ArrayList<Instance> instancesToTheRight = new ArrayList<Instance>();

		for (int i = 0; i < instances.size(); i++) {
			if(instances.get(i).toDoubleArray()[depth] == median && medianInstance == null) {
				medianInstance = instances.get(i);
			}
			else if (instances.get(i).toDoubleArray()[depth] < median)
				instancesToTheLeft.add(instances.get(i));
			else
				instancesToTheRight.add(instances.get(i));
		}

		this.insertKDTreeNode(medianInstance);

		buildKDTreeBalanced(instancesToTheLeft, (depth+1)%this.nDims);
		buildKDTreeBalanced(instancesToTheRight, (depth+1)%this.nDims);
    }

    private void insertKDTreeNode(Instance inst){
        Node p = this.root;
		Node prev = null;
		double[] info = inst.toDoubleArray();

		int i = 0;

		while (p != null) {
			prev = p;
			if (info[i] < p.getInfo()[i])
				p = p.left;
			else
				p = p.right;
			i = (i+1) % nDims;
		}

		int index = (i-1)%nDims;
		if (index < 0)
			index = nDims-1;

		if (this.root == null)
			this.root = new Node(inst, i);
		else if (info[index] < prev.getInfo()[index])
			prev.left = new Node(inst, i);
		else
			prev.right = new Node(inst, i);
		this.numInstances++;
	}

    private void removeKDTreeNode(Instance inst) throws Exception {

        Node nodeToRemove = search(inst, root);

		if (nodeToRemove == null)
			throw new InstanceNotFoundException("Instance not found on KDTree. Is there any missing data on the dataset?");

		delete(nodeToRemove);
		this.numInstances--;
    }

    public Node search(Instance inst, Node node) throws Exception {
		double[] instInfo = inst.toDoubleArray();

		if (node == null)
			return null;

		if (isInstanceEqual(inst, node.getInstance()) && node.isNodeActive())
			return node;

		Node nodeToReturn = null;

		if (instInfo[node.splitDim] < node.getInfo()[node.splitDim]) {
			nodeToReturn = search(inst, node.left);
		}
		else {
			nodeToReturn = search(inst, node.right);
		}

		return nodeToReturn;

	}

    private void delete(Node p) throws Exception {
		if (p.isALeaf()) {
			p = null;
			return;
		}
        p.setActiveFalse();
        this.numNodesDeactivated++;
	}

    private double getMedian(ArrayList<Double> values) {
		Collections.sort(values);
        return values.get( (int) (values.size() + 1) / 2 - 1);
	}

	private boolean isToSearchNode(Node root, Instance target, int splitDim, double maximum) {

		double minimumDistance = target.toDoubleArray()[splitDim] - root.getInfo()[splitDim];
		double denominator = FastMath.abs(target.toDoubleArray()[splitDim]) + FastMath.abs(root.getInfo()[splitDim]);
		double modDist = 0;

		if (denominator != 0) {
			modDist = FastMath.abs(minimumDistance) / denominator;
		}

		// double compare = a*modDist;
		double compare = 1*modDist;

		if (compare <= maximum)
			return true;

		return false;
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

    // Getters and setters
    public Node getRoot() {
        return root;
    }



}

package moa.classifiers.lazy.neighboursearch;

import java.io.Serializable;

import com.yahoo.labs.samoa.instances.DenseInstance;
import com.yahoo.labs.samoa.instances.Instance;
import com.yahoo.labs.samoa.instances.InstancesHeader;

public class SNode implements Serializable {
    private static final long serialVersionUID = 1L;
    public SNode left, right, parent;
    public int index;
    public int splitDim;
    public double[] inst;
    public boolean active;

    public SNode(double[] inst, SNode left, SNode right, SNode parent, int index, int splitDim) {
        this.inst = inst;
        this.left = left;
        this.right = right;
        this.parent = parent;
        this.index = index;
        this.splitDim = splitDim;
        this.active = true;
    }

    public boolean isALeaf() {
        return (this.left == null) && (this.right == null);
    }

    public boolean isActive() {
        return this.active;
    }

    public Instance toInstance(InstancesHeader streamHeader) {
        Instance inst = new DenseInstance(1.0, this.inst);
        inst.setDataset(streamHeader);
        return inst;
    }
}

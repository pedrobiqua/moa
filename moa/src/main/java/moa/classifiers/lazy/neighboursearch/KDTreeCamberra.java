package moa.classifiers.lazy.neighboursearch;

import com.yahoo.labs.samoa.instances.Instance;
import com.yahoo.labs.samoa.instances.Instances;

import moa.classifiers.lazy.neighboursearch.kdtrees.StreamNeighborSearch;

public class KDTreeCamberra extends NearestNeighbourSearch implements StreamNeighborSearch {

    @Override
    public Instance nearestNeighbour(Instance target) throws Exception {

        throw new UnsupportedOperationException("Unimplemented method 'nearestNeighbour'");
    }

    @Override
    public Instances kNearestNeighbours(Instance target, int k) throws Exception {

        throw new UnsupportedOperationException("Unimplemented method 'kNearestNeighbours'");
    }

    @Override
    public double[] getDistances() throws Exception {

        throw new UnsupportedOperationException("Unimplemented method 'getDistances'");
    }

    @Override
    public void update(Instance ins) throws Exception {

        throw new UnsupportedOperationException("Unimplemented method 'update'");
    }

    @Override
    public void removeInstance(Instance inst) throws Exception {

        throw new UnsupportedOperationException("Unimplemented method 'removeInstance'");
    }

    @Override
    public boolean isToRebuild() {

        throw new UnsupportedOperationException("Unimplemented method 'isToRebuild'");
    }

}

package moa.classifiers.lazy.neighboursearch.rebuildpolicies;

import moa.classifiers.lazy.neighboursearch.NSKDtree;

public class AlwaysRebuild implements RebuildPolicy {
    @Override
    public boolean checkRebuild(NSKDtree.MetricsTree metricsTree) throws Exception {
        return true;
    }
}

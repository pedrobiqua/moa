package moa.classifiers.lazy.neighboursearch.rebuildpolicies;

import moa.classifiers.lazy.neighboursearch.NSKDtree;

public class NoRebuild implements RebuildPolicy {
    @Override
    public boolean checkRebuild(NSKDtree.MetricsTree stats) throws Exception {
        return false;
    }
}

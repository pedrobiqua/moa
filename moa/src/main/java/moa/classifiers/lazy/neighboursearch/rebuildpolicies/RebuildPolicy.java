package moa.classifiers.lazy.neighboursearch.rebuildpolicies;

import moa.classifiers.lazy.neighboursearch.NSKDtree;

public interface RebuildPolicy {
    boolean checkRebuild(NSKDtree.MetricsTree metricsTree) throws Exception;
}

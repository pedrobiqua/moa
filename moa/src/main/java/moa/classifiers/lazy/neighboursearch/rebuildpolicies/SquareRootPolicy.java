package moa.classifiers.lazy.neighboursearch.rebuildpolicies;

import moa.classifiers.lazy.neighboursearch.NSKDtree;

public class SquareRootPolicy implements RebuildPolicy {

    private int threshold = 0;

    public SquareRootPolicy(int windowSize) {
        this.threshold = (int) Math.round(windowSize + Math.sqrt(windowSize));
    }

    @Override
    public boolean checkRebuild(NSKDtree.MetricsTree stats) throws Exception {
        return stats.getTreeSize() >= threshold;
    }
}

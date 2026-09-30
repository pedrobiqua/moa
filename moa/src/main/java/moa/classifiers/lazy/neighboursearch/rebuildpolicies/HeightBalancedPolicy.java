package moa.classifiers.lazy.neighboursearch.rebuildpolicies;

import moa.classifiers.lazy.neighboursearch.NSKDtree;

public class HeightBalancedPolicy implements RebuildPolicy {

    private final double alpha;
    private final int limitSize;

    public HeightBalancedPolicy(double alpha, int windowSize) {
        this.alpha = alpha;
        this.limitSize = 5 * windowSize;
    }

    @Override
    public boolean checkRebuild(NSKDtree.MetricsTree stats) throws Exception {
        // Limit size tree
        if (stats.getTreeSize() >= limitSize)
            return true;
        if (stats.getTreeSize() == 0 || stats.getTreeSize() == 1) {
            return false;
        }
        return stats.getHeightTree() > Math.log(stats.getTreeSize()) / Math.log(1.0 / alpha);
    }
}

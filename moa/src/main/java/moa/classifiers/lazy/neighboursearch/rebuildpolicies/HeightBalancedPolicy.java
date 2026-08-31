package moa.classifiers.lazy.neighboursearch.rebuildpolicies;

import moa.classifiers.lazy.neighboursearch.NSKDtree;

public class HeightBalancedPolicy implements RebuildPolicy {

    private final double alpha;

    public HeightBalancedPolicy(double alpha) {
        this.alpha = alpha;
    }

    @Override
    public boolean checkRebuild(NSKDtree.MetricsTree stats) throws Exception {
        // Se a altura é maior que \log_{1/\alpha}(n)
        if (stats.getTreeSize() == 0 || stats.getTreeSize() == 1) {
            return false;
        }
        return stats.getHeightTree() > Math.log(stats.getTreeSize()) / Math.log(1.0 / alpha);
    }
}

package moa.classifiers.lazy.neighboursearch.rebuildpolicies;

import moa.classifiers.lazy.neighboursearch.NSKDtree;

public class AlphaBetaPolicy implements RebuildPolicy {

    private final double alpha = 0.7;
    private final double beta = 0.3;

    @Override
    public boolean checkRebuild(NSKDtree.MetricsTree stats) throws Exception {

        if (stats.getTreeSize() == 0) {
            return false;
        }

        // beta
        double deletedRatio = (double) stats.getNumDeletedNodes()
                / (double) stats.getTreeSize();

        if (deletedRatio >= beta) {
            return true;
        }

        // alpha
        if (stats.getTreeSize() == 1) {
            return false;
        }

        double heightLimit = Math.log(stats.getTreeSize())
                / Math.log(1.0 / alpha);

        return stats.getHeightTree() > heightLimit;
    }
}

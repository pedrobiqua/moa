package moa.classifiers.lazy.neighboursearch.rebuildpolicies;

import moa.classifiers.lazy.neighboursearch.NSKDtree;
import moa.classifiers.lazy.neighboursearch.kdtrees.KDTreeStats;

public class IKDtreeRebuildPolicy implements RebuildPolicy {

    private final double alphaBal;
    private final double alphaDel;
    private final long minimalTreeSize;

    public IKDtreeRebuildPolicy(double alphaBal, double alphaDel, long minimalTreeSize) {
        this.alphaBal = alphaBal;
        this.alphaDel = alphaDel;
        this.minimalTreeSize = minimalTreeSize;
    }

    @Override
    public boolean checkRebuild(NSKDtree.MetricsTree stats) {

        if (stats.getTreeSize() <= minimalTreeSize) {
            return false;
        }

        double deleteEvaluation =
                (double) stats.getNumDeletedNodes()
                        / stats.getTreeSize();

        if (deleteEvaluation > alphaDel) {
            return true;
        }

        double balanceEvaluation =
                (double) stats.getChildTreeSize()
                        / (stats.getTreeSize() - 1);

        return balanceEvaluation > alphaBal
                || balanceEvaluation < 1.0 - alphaBal;
    }
}

package moa.classifiers.lazy.neighboursearch.rebuildpolicies;

import moa.classifiers.lazy.neighboursearch.NSKDtree;

public class LogRatioPolicy implements RebuildPolicy {

    private int threshold = 0;

    public LogRatioPolicy(int windowSize) {
        this.threshold = (int) Math.round(windowSize * (Math.log(windowSize) / (Math.log(Math.log(windowSize)))));
    }


    @Override
    public boolean checkRebuild(NSKDtree.MetricsTree stats) throws Exception {
        return stats.getTreeSize() >= threshold;
    }
}

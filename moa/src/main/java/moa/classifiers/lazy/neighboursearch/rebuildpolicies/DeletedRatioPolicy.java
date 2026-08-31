package moa.classifiers.lazy.neighboursearch.rebuildpolicies;

import moa.classifiers.lazy.neighboursearch.NSKDtree;

public class DeletedRatioPolicy implements RebuildPolicy {

    private final double ratio;

    public DeletedRatioPolicy(double ratio) {
        this.ratio = ratio;
    }

    @Override
    public boolean checkRebuild(NSKDtree.MetricsTree metricsTree) throws Exception {
        boolean check_result = false;
        if (metricsTree.getTreeSize() == 0)
            throw new Exception("Divisão por zero!");

        // System.out.println(((double) metricsTree.m_numNodesDeleted / (double) metricsTree.m_numNodes));
        if (((double) metricsTree.getNumDeletedNodes() / (double) metricsTree.getTreeSize()) >= ratio) {
            check_result = true;
        }

        return check_result;
    }
}

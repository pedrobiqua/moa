package moa.classifiers.lazy.neighboursearch.kdtrees;

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
    public boolean checkRebuild(KDTreeStats stats) throws Exception {
        return false;
    }

    @Override
    public boolean checkRebuild(StatsTree stats) {

        if (stats.m_numNodes <= minimalTreeSize) {
            return false;
        }

        double deleteEvaluation =
                (double) stats.m_numNodesDeleted
                        / stats.m_numNodes;

        if (deleteEvaluation > alphaDel) {
            return true;
        }

        // Ainda não tenho isso! Vou fazer criando no stats, pois preciso apenas da árvore global.
        double balanceEvaluation =
                (double) stats.getChildTreeSize()
                        / (stats.m_numNodes - 1);

        return balanceEvaluation > alphaBal
                || balanceEvaluation < 1.0 - alphaBal;
    }
}

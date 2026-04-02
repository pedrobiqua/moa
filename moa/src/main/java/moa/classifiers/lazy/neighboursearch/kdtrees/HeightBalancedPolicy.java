package moa.classifiers.lazy.neighboursearch.kdtrees;

public class HeightBalancedPolicy implements RebuildPolicy {

    private double alpha;

    public HeightBalancedPolicy(double alpha) {
        this.alpha = alpha;
    }

    @Override
    public boolean checkRebuild(KDTreeStats stats) throws Exception {
        // Se a altura é maior que \log_{1/\alpha}(n)
        if (stats.m_NumNodes == 0 || stats.m_NumNodes == 1) {
            return false;
        }

        return stats.m_MaxDepth > Math.log(stats.m_NumNodes) / Math.log(1.0 / alpha);
    }

    @Override
    public boolean checkRebuild(StatsTree stats) throws Exception {
        // Se a altura é maior que \log_{1/\alpha}(n)
        if (stats.m_numNodes == 0 || stats.m_numNodes == 1) {
            return false;
        }
        return stats.m_heightTree > Math.log(stats.m_numNodes) / Math.log(1.0 / alpha);
    }
}

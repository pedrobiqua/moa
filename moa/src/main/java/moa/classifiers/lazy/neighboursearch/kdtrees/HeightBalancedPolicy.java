package moa.classifiers.lazy.neighboursearch.kdtrees;

public class HeightBalancedPolicy implements RebuildPolicy {

    @Override
    public boolean checkRebuild(KDTreeStats stats) throws Exception {
        // Se a altura é maior que \log_{1/\alpha}(n)
        if (stats.m_NumNodes == 0 || stats.m_NumNodes == 1) {
            return false;
        }

        final double alpha = 0.6;
        return stats.m_MaxDepth > Math.log(stats.m_NumNodes) / Math.log(1.0 / alpha);
    }
}

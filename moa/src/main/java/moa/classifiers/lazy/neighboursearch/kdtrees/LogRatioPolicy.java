package moa.classifiers.lazy.neighboursearch.kdtrees;

public class LogRatioPolicy implements RebuildPolicy {

    int threshold = 0;

    @Override
    public boolean checkRebuild(KDTreeStats stats) throws Exception {
        return false;
    }

    @Override
    public boolean checkRebuild(StatsTree stats) throws Exception {
        if (threshold == 0) {
            threshold = (int) Math.round(stats.window_size * (Math.log(stats.window_size) / (Math.log(Math.log(stats.window_size)))));
        }

        return stats.m_numNodes >= threshold;
    }
}

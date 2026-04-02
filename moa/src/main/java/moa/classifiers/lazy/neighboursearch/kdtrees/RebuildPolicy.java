package moa.classifiers.lazy.neighboursearch.kdtrees;

import moa.classifiers.lazy.neighboursearch.StreamKDTree;

public interface RebuildPolicy {
    boolean checkRebuild(KDTreeStats stats) throws Exception;
    boolean checkRebuild(StatsTree stats) throws Exception;

}

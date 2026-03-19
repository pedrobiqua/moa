package moa.classifiers.lazy.neighboursearch.kdtrees;

import moa.classifiers.lazy.neighboursearch.StreamKDTree;

public class InstancesPerLeafPolicy implements RebuildPolicy {
    @Override
    public boolean checkRebuild(KDTreeStats stats) throws Exception {
        if (stats.m_NumLeaves == 0)
            return false;
        return ((double)stats.m_NumInstancias / stats.m_NumLeaves) > StreamKDTree.m_MaxInstInLeaf;
    }
}

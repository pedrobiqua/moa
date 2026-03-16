package moa.classifiers.lazy.neighboursearch.kdtrees;

public class DeletedRatioPolicy implements RebuildPolicy{
    @Override
    public boolean checkRebuild(KDTreeStats stats) throws Exception {
        if (stats.m_NumInstancias == 0)
            throw new Exception("Divisão por zero!");

        return ((double) stats.m_NumInstancesDeleted / stats.m_NumInstancias) > 0.5;
    }
}

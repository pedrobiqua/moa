package moa.classifiers.lazy.neighboursearch.kdtrees;

public class DeletedRatioPolicy implements RebuildPolicy {
    @Override
    public boolean checkRebuild(KDTreeStats stats) throws Exception {
        boolean check_result = false;
        if (stats.m_NumInstancias == 0)
            throw new Exception("Divisão por zero!");

        if (((double) stats.m_NumInstancesDeleted / (double) stats.m_NumInstancias) >= 0.3) {
            check_result = true;
        }

//        if (stats.m_NumInstancias > stats.m_InitialNumInstances * 2) {
//            check_result = true;
//        }

        return check_result;
    }
}

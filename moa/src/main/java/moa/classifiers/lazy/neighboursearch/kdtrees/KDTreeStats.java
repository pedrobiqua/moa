package moa.classifiers.lazy.neighboursearch.kdtrees;

import java.util.Locale;

public class KDTreeStats {
    ///  Metricas usadas nas kdtrees
    public int m_NumNodes, m_NumLeaves, m_MaxDepth, m_NumInstancesDeleted, m_NumInstancias, m_NumUpdates;

    public void printStats() {
        System.out.printf(Locale.US, "%d,%d,%d,%d,%d,%d,%.2f%n",
                m_NumInstancias,
                m_NumInstancesDeleted,
                m_NumNodes,
                m_NumLeaves,
                m_MaxDepth,
                m_NumUpdates,
                (double) m_NumInstancias / m_NumLeaves);
    }
}

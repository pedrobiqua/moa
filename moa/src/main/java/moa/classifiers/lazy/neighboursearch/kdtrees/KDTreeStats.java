package moa.classifiers.lazy.neighboursearch.kdtrees;

import java.util.Locale;

public class KDTreeStats {
    /// Metricas da árvore
    public int m_NumNodes, m_NumLeaves, m_MaxDepth, m_NumInstancesDeleted, m_NumInstancias, m_NumUpdates;
    public int m_InitialNumInstances = 0;

    ///  Métricas de funções usadas, lembrar de reiniciar elas
    public int m_BacktrackCount;
    public int m_VisitedNodes;
    public int m_VisitedInstances;
    public boolean m_Rebuild; // feito rebuild naquela instancia
    public int m_InsertDepth, m_SearchDepth;



    public void printStats() {
        System.out.printf(Locale.US, "%d,%d,%d,%d,%d,%d,%d,%.2f%n",
                m_NumInstancias,
                m_NumInstancesDeleted,
                m_NumNodes,
                m_NumLeaves,
                m_MaxDepth,
                m_NumUpdates,
                m_InitialNumInstances,
                (double) m_NumInstancias / m_NumLeaves);
    }

    public void printMetrics() {
        System.out.printf("%d,%d,%d,%d,%d,%d,%d,%d,%b%n", m_NumNodes, m_VisitedNodes, m_BacktrackCount, m_NumInstancias, m_VisitedInstances, m_InsertDepth, m_SearchDepth, m_MaxDepth, m_Rebuild);
    }

    public void resetTreeStats() {
        m_NumNodes = 0;
        m_NumLeaves = 0;
        m_MaxDepth = 0;
        m_NumInstancesDeleted = 0;
        m_NumInstancias = 0;
        m_NumUpdates = 0;
        m_InitialNumInstances = 0;
    }

    public void resetMetrics() {
        m_BacktrackCount = 0;
        m_VisitedNodes = 0;
        m_VisitedInstances = 0;
        m_Rebuild = false;
        m_InsertDepth = 0;
        m_SearchDepth = 0;
    }
}

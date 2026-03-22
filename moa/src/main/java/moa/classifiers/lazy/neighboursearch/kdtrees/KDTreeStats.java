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

    // Metricas de tempos
    public long totalInsertTime;
    public long totalUpdateTime;
    public long totalRebuildTime;
    public long totalSearchTime;

    public String headerMetrics() {
        return "NumNodes,NumLeaves,MaxDepth,NumInstancesDeleted,NumInstances,NumUpdates,m_BacktrackCount,VisitedNodes,VisitedInstances,Rebuild,InsertDepth,SearchDepth,totalInsertTime,totalUpdateTime,totalRebuildTime,totalSearchTime";
    }

    public String metricsAndStats() {
        return m_NumNodes + "," +
                m_NumLeaves + "," +
                m_MaxDepth + "," +
                m_NumInstancesDeleted + "," +
                m_NumInstancias + "," +
                m_NumUpdates + "," +
                m_BacktrackCount + "," +
                m_VisitedNodes + "," +
                m_VisitedInstances + "," +
                (m_Rebuild ? 1 : 0) + "," +
                m_InsertDepth + "," +
                m_SearchDepth + "," +
                totalInsertTime + "," +
                totalUpdateTime + "," +
                totalRebuildTime + "," +
                totalSearchTime;
    }

    public void printHeaderAuto() {
        java.lang.reflect.Field[] fields = this.getClass().getDeclaredFields();

        for (java.lang.reflect.Field field : fields) {
            System.out.printf("%-22s", field.getName());
        }
        System.out.println();

    }

    public void printValuesAuto() {
        java.lang.reflect.Field[] fields = this.getClass().getDeclaredFields();

        try {
            for (java.lang.reflect.Field field : fields) {
                field.setAccessible(true);
                System.out.printf("%-22s", field.get(this));
            }
            System.out.println();
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

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
        System.out.printf("%d,%d,%d,%d,%d,%d,%d,%d,%b%n", m_NumNodes, m_VisitedNodes, m_BacktrackCount, m_NumInstancias, m_VisitedInstances, m_InsertDepth, m_SearchDepth, m_MaxDepth, m_Rebuild, totalInsertTime, totalUpdateTime, totalRebuildTime, totalSearchTime);
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

        // Coleta dos tempos
        totalInsertTime = 0;
        totalUpdateTime = 0;
        totalRebuildTime = 0;
        totalSearchTime = 0;
    }

    /**
     * Metódo que testa se os valores coletados fazem sentido, não garante falhas porem pode pegar coisas mais bobas
     *
     * @throws Exception se tiver alguma nomalia em erro de coleta ou de alguma etapa do processo de inserção, delete, build e rebuild
     */
    public void validate() {
        // Valores negativos (nunca pode)
        if (m_NumNodes < 0 || m_NumLeaves < 0 || m_MaxDepth < 0 ||
                m_NumInstancesDeleted < 0 || m_NumInstancias < 0 || m_NumUpdates < 0 ||
                m_BacktrackCount < 0 || m_VisitedNodes < 0 || m_VisitedInstances < 0 ||
                m_InsertDepth < 0 || m_SearchDepth < 0) {

            throw new IllegalStateException("Negative metric detected!");
        }

        // Não pode ter mais folhas que nós
        if (m_NumLeaves > m_NumNodes) {
            throw new IllegalStateException("Leaves > Nodes (invalid tree)");
        }

        // Não pode ter mais nós visitados do que nós existentes
        if (m_VisitedNodes > m_NumNodes) {
            printValuesAuto();
            throw new IllegalStateException("Visited nodes > total nodes");
        }

        // Não pode visitar mais instâncias do que existem
        if (m_VisitedInstances > m_NumInstancias) {
            throw new IllegalStateException("Visited instances > total instances");
        }

        // Instâncias deletadas não podem passar do total
        if (m_NumInstancesDeleted > m_NumInstancias) {
            throw new IllegalStateException("Deleted instances > total instances");
        }

        // Profundidade não pode ser maior que número de nós
        if (m_MaxDepth > m_NumNodes) {
            throw new IllegalStateException("Max depth > number of nodes");
        }

        // Insert/Search depth não pode passar da profundidade máxima
        if (m_InsertDepth > m_MaxDepth || m_SearchDepth > m_MaxDepth) {
            printValuesAuto();
            throw new IllegalStateException("Operation depth > max depth");
        }

        // Backtrack absurdo (heurística simples)
        if (m_BacktrackCount > m_NumNodes) {
            throw new IllegalStateException("Too many backtracks (suspicious)");
        }

        // Árvores com instâncias mas sem folhas
        if (m_NumInstancias > 0 && m_NumLeaves == 0) {
            throw new IllegalStateException("Instances exist but no leaves");
        }

        // Divisão inválida (evita NaN/Infinity no print)
        if (m_NumLeaves == 0 && m_NumInstancias > 0) {
            throw new IllegalStateException("Division by zero risk (NumLeaves = 0)");
        }

        // Warning útil (não quebra execução)
        if (m_NumNodes == 0 && m_NumInstancias > 0) {
            System.err.println("Warning: instances exist but tree has no nodes");
        }
    }

    public void validateStructure() {

        if (m_NumNodes < 0 || m_NumLeaves < 0 || m_MaxDepth < 0 ||
                m_NumInstancesDeleted < 0 || m_NumInstancias < 0 || m_NumUpdates < 0) {
            throw new IllegalStateException("Negative tree metric detected!");
        }

        if (m_NumLeaves > m_NumNodes) {
            throw new IllegalStateException("Leaves > Nodes");
        }

        if (m_NumInstancesDeleted > m_NumInstancias) {
            throw new IllegalStateException("Deleted > Instances");
        }

        if (m_NumInstancias > 0 && m_NumLeaves == 0) {
            throw new IllegalStateException("Instances exist but no leaves");
        }

        if (m_MaxDepth > m_NumNodes) {
            throw new IllegalStateException("MaxDepth > NumNodes");
        }
    }

    public void validateSearch() {

        if (m_BacktrackCount < 0 || m_VisitedNodes < 0 ||
                m_VisitedInstances < 0 || m_SearchDepth < 0) {
            throw new IllegalStateException("Negative search metric detected!");
        }

        if (!m_Rebuild) {
            if (m_VisitedNodes > m_NumNodes) {
                printValuesAuto();
                throw new IllegalStateException("VisitedNodes > NumNodes");
            }

            if (m_VisitedInstances > m_NumInstancias) {
                throw new IllegalStateException("VisitedInstances too large");
            }

            if (m_SearchDepth > m_MaxDepth) {
                System.err.println("Warning: SearchDepth > MaxDepth (expected with backtracking)");
            }
        }

        if (m_BacktrackCount > m_NumNodes) {
            System.err.println("Warning: too many backtracks");
        }
    }

    public void validateUpdate() {

        if (m_InsertDepth < 0) {
            throw new IllegalStateException("Invalid insert depth");
        }

        if (!m_Rebuild) {
            if (m_InsertDepth > m_MaxDepth + 1) {
                throw new IllegalStateException("InsertDepth too large");
            }
        }

        if (m_NumUpdates < 0) {
            throw new IllegalStateException("Negative updates");
        }

        if (m_Rebuild && m_NumNodes == 0) {
            throw new IllegalStateException("Rebuild resulted in empty tree");
        }
    }
}

package moa.classifiers.lazy.neighboursearch.kdtrees;

public class StatsTree {
    /** Dados da árvore **/
    public int m_numNodes, m_heightTree, m_numNodesDeleted;

    /** Metricas reiniciaveis coletadas **/
    public int depthInsert, depthSearch, backtrack;

    /** Metricas de contagens **/
    public int countRebuild = 0;

    /** Metricas de tempo **/
    public double timeInsert, timeRebuild, timeSearch;

    public String getHeader() {
        return "m_numNodes,m_heightTree,m_numNodesDeleted,depthInsert,depthSearch,backtrack,countRebuild";
    }

    public String getMetrics() {
        return m_numNodes + ","
                + m_heightTree + ","
                + m_numNodesDeleted + ","
                + depthInsert + ","
                + depthSearch + ","
                + backtrack + ","
                + countRebuild;
    }

}

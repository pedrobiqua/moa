package moa.classifiers.lazy.neighboursearch.kdtrees;

import java.util.TreeSet;

public class StatsTree {
    /** Dados extras **/
    public int window_size;

    /** Dados da árvore **/
    public int m_numNodes, m_heightTree, m_numNodesDeleted;

    /** Metricas reiniciaveis coletadas **/
    public int depthInsert, depthSearch, backtrack;

    public TreeSet<Integer> visitedNodes = new TreeSet<Integer>();

    /** Metricas de contagens **/
    public int countRebuild = 0;

    /** Metricas de tempo **/
    public double timeInsert, timeRebuild, timeSearch;

    public String getHeader() {
        return "m_numNodes,m_heightTree,m_numNodesDeleted,depthInsert,depthSearch,visitedNodes,backtrack,countRebuild";
    }

    public String getMetrics() {
        return m_numNodes + ","
                + m_heightTree + ","
                + m_numNodesDeleted + ","
                + depthInsert + ","
                + depthSearch + ","
                + visitedNodes.size() + ","
                + backtrack + ","
                + countRebuild;
    }

}

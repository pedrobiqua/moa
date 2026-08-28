package moa.classifiers.lazy.neighboursearch.kdtrees;

import java.util.TreeSet;

public class StatsTree {
    /** Dados extras **/
    public int window_size;

    /** Dados da árvore **/
    public int m_numNodes, m_heightTree, m_numNodesDeleted;

    /** Dados reconstrução **/
    public int m_leftTreeSize;
    public int m_rightTreeSize;

    /** Metricas reiniciaveis coletadas **/
    public int depthInsert, depthSearch, backtrack;

    public TreeSet<Integer> visitedNodes = new TreeSet<Integer>();

    /** Metricas de contagens **/
    public int countRebuild = 0;

    /** Metricas de tempo **/
    public double timeInsert, timeRebuild, timeSearch;

    public String getHeader() {
        return "m_numNodes,m_heightTree,m_numNodesDeleted,m_leftTreeSize,m_rightTreeSize,depthInsert,depthSearch,visitedNodes,backtrack,countRebuild";
    }

    public String getMetrics() {
        return m_numNodes + ","
                + m_heightTree + ","
                + m_numNodesDeleted + ","
                + m_leftTreeSize + ","
                + m_rightTreeSize + ","
                + depthInsert + ","
                + depthSearch + ","
                + visitedNodes.size() + ","
                + backtrack + ","
                + countRebuild;
    }

    public int getChildTreeSize() {
        if (m_leftTreeSize != 0)
            return m_leftTreeSize;
        else if (m_rightTreeSize != 0)
            return m_rightTreeSize;
        else
            return 0;
    }

}

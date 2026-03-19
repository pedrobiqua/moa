package moa.classifiers.lazy.neighboursearch.kdtrees;

/**
 * Sempre retorna falso, pois, é para quando não quero recriar a árvore
 */
public class NoRebuild implements RebuildPolicy {
    @Override
    public boolean checkRebuild(KDTreeStats stats) throws Exception {
        return false;
    }
}

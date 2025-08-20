package moa.classifiers.lazy.neighboursearch;

import com.yahoo.labs.samoa.instances.Instance;
import com.yahoo.labs.samoa.instances.Instances;

import moa.classifiers.lazy.neighboursearch.kdtrees.StreamNeighborSearch;

public class KDTreeCamberra extends NearestNeighbourSearch implements StreamNeighborSearch {

    @Override
    public Instance nearestNeighbour(Instance target) throws Exception {

        throw new UnsupportedOperationException("Unimplemented method 'nearestNeighbour'");
    }

    @Override
    public Instances kNearestNeighbours(Instance target, int k) throws Exception {

        throw new UnsupportedOperationException("Unimplemented method 'kNearestNeighbours'");
    }

    @Override
    public double[] getDistances() throws Exception {

        throw new UnsupportedOperationException("Unimplemented method 'getDistances'");
    }

    @Override
    public void update(Instance ins) throws Exception {

        throw new UnsupportedOperationException("Unimplemented method 'update'");
    }

    @Override
    public void removeInstance(Instance inst) throws Exception {

        throw new UnsupportedOperationException("Unimplemented method 'removeInstance'");
    }

    @Override
    public boolean isToRebuild() {

        throw new UnsupportedOperationException("Unimplemented method 'isToRebuild'");
    }

    protected void buildKDTree(Instances instances) {
        this.buildKDTreeBalanced(instances, 0);


    }

    private void buildKDTreeBalanced(Instances instances, int i) {
        // 1 if sizeOf(𝑖𝑛𝑠𝑡𝑎𝑛𝑐𝑒𝑠) == 0 then
        // 2 return null
        // 3 𝑖𝑛𝑠𝑡𝑎𝑛𝑐𝑒𝑠𝑇 𝑜𝑇 ℎ𝑒𝐿𝑒𝑓 𝑡 ← ∅
        // 4 𝑖𝑛𝑠𝑡𝑎𝑛𝑐𝑒𝑠𝑇 𝑜𝑇 ℎ𝑒𝑅𝑖𝑔ℎ𝑡 ← ∅
        // 5 𝑚𝑒𝑑𝑖𝑎𝑛 ← getMedian(𝑖𝑛𝑠𝑡𝑎𝑛𝑐𝑒𝑠, 𝑠𝑝𝑙𝑖𝑡𝐷𝑖𝑚𝑒𝑛𝑠𝑖𝑜𝑛) ; ⊳ Get the
        // median of the current split dimension
        // 6 foreach 𝐼 ∈ 𝑖𝑛𝑠𝑡𝑎𝑛𝑐𝑒𝑠 do
        // 7 ⊳ Values lower than the median go to the left
        // subtree, and values greater or equal to the
        // right
        // 8 if 𝐼[𝑠𝑝𝑙𝑖𝑡𝐷𝑖𝑚𝑒𝑛𝑠𝑖𝑜𝑛] == 𝑚𝑒𝑑𝑖𝑎𝑛 then
        // 9 𝑚𝑒𝑑𝑖𝑎𝑛𝐼𝑛𝑠𝑡𝑎𝑛𝑐𝑒 ← 𝐼
        // 10 else if 𝐼[𝑠𝑝𝑙𝑖𝑡𝐷𝑖𝑚𝑒𝑛𝑠𝑖𝑜𝑛] < 𝑚𝑒𝑑𝑖𝑎𝑛 then
        // 11 𝑖𝑛𝑠𝑡𝑎𝑛𝑐𝑒𝑠𝑇 𝑜𝑇 ℎ𝑒𝐿𝑒𝑓 𝑡 ← 𝑖𝑛𝑠𝑡𝑎𝑛𝑐𝑒𝑠𝑇 𝑜𝑇 ℎ𝑒𝐿𝑒𝑓 𝑡 ∪ 𝐼
        // 12 else
        // 13 𝑖𝑛𝑠𝑡𝑎𝑛𝑐𝑒𝑠𝑇 𝑜𝑇 ℎ𝑒𝑅𝑖𝑔ℎ𝑡 ← 𝑖𝑛𝑠𝑡𝑎𝑛𝑐𝑒𝑠𝑇 𝑜𝑇 ℎ𝑒𝑅𝑖𝑔𝑡ℎ ∪ 𝐼
        // 14 𝑛𝑜𝑑𝑒 ← 𝐾𝐷𝑇 𝑟𝑒𝑒𝑁𝑜𝑑𝑒(𝑚𝑒𝑑𝑖𝑎𝑛𝐼𝑛𝑠𝑡𝑎𝑛𝑐𝑒, 𝑠𝑝𝑙𝑖𝑡𝐷𝑖𝑚𝑒𝑛𝑠𝑖𝑜𝑛)
        // 15 𝑛𝑜𝑑𝑒.𝑙𝑒𝑓 𝑡 ← Build K-d Tree(𝑖𝑛𝑠𝑡𝑎𝑛𝑐𝑒𝑠𝑇 𝑜𝑇 ℎ𝑒𝐿𝑒𝑓 𝑡,
        // (𝑠𝑝𝑙𝑖𝑡𝐷𝑖𝑚𝑒𝑠𝑖𝑜𝑛 + 1) mod 𝐾)
        // 16 𝑛𝑜𝑑𝑒.𝑟𝑖𝑔ℎ𝑡 ← Build K-d Tree(𝑖𝑛𝑠𝑡𝑎𝑛𝑐𝑒𝑠𝑇 𝑜𝑇 ℎ𝑒𝑅𝑖𝑔ℎ𝑡,
        // (𝑠𝑝𝑙𝑖𝑡𝐷𝑖𝑚𝑒𝑛𝑠𝑖𝑜𝑛 + 1) mod 𝐾)
        // 17 return 𝑛𝑜𝑑𝑒

        if (instances.size() == 0) {
            return;
        }

        Integer median = this.getMedian(instances, i);
    }

    private Integer getMedian(Instances instances, Integer splitDimension) {

        return 0;
    }



}

package moa;

import com.yahoo.labs.samoa.instances.Instance;
import com.yahoo.labs.samoa.instances.Instances;
import com.yahoo.labs.samoa.instances.InstancesHeader;

import moa.classifiers.lazy.neighboursearch.KDTreeSimple;
import moa.streams.generators.AssetNegotiationGenerator;

// PARA RODAR ESSE MONSTRO, BASTA COMPILAR E USAR:
// java -cp moa/target/test-classes:moa/target/classes moa.TestKdTree > output.txt
public class TestKdTree {
    public static void main(String[] args) {
        try {
            // CRIO UMA STREAM QUALQUER
            AssetNegotiationGenerator stream = new AssetNegotiationGenerator();
            stream.prepareForUse();

            int numDim = stream.nextInstance().instance.numValues();
            KDTreeSimple kdtree = new KDTreeSimple(numDim);
            InstancesHeader header = stream.getHeader();
            Instances dataset = new Instances(header);

            int count = 0;
            int total = 500;

            // FAÇO A EXECUÇÃO 500 VEZES E VERIFICO SE ESTÁ BALANCEADA
            while (stream.hasMoreInstances() && count < total) {
                Instance inst = stream.nextInstance().getData();
                dataset.add(inst);
                kdtree.update(inst);
                System.out.println(inst + " " + kdtree.isBalanced());
                count++;
            }

            System.out.println();
            kdtree.printInOrder();
            System.out.println();
            KDTreeSimple kdtree_balanced = new KDTreeSimple(numDim);
            kdtree_balanced.buildTree(dataset);
            System.out.println(kdtree_balanced.isBalanced());
            kdtree_balanced.printInOrder();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

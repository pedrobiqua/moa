package moa;

import com.yahoo.labs.samoa.instances.Instance;

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

            int numDim = stream.nextInstance().instance.numValues() - 1;
            KDTreeSimple kdtree = new KDTreeSimple(numDim);
            int count = 0;

            // FAÇO A EXECUÇÃO 500 VEZES E VERIFICO SE ESTÁ BALANCEADA
            while (stream.hasMoreInstances() && count < 500) {
                Instance inst = stream.nextInstance().getData();
                kdtree.update(inst);
                System.out.println(inst + " " + kdtree.isBalanced());
                count++;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

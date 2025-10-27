package moa;

import com.yahoo.labs.samoa.instances.Instance;

import moa.classifiers.lazy.neighboursearch.KDTreeSimple;
import moa.options.AbstractOptionHandler;
import moa.streams.InstanceStream;
import moa.streams.generators.AssetNegotiationGenerator;
import moa.streams.generators.RandomRBFGenerator;
import moa.streams.generators.SEAGenerator;

// PARA RODAR ESSE MONSTRO, BASTA COMPILAR E USAR:
// A MINHA IDEIA É TIRAR ISSO DAQUI, NÃO FAZ SENTIDO FICAR NO TEST
// mvn test-compile
// java -cp moa/target/test-classes:moa/target/classes moa.TestKdTree > output.txt
public class TestKdTree {

    public static void run_experiments(InstanceStream stream, int total) {
        try {
            if (stream instanceof AbstractOptionHandler)
                ((AbstractOptionHandler) stream).prepareForUse();
            else {
                throw new UnsupportedOperationException("Unimplemented method 'getDistances'");
            }

            String name_stream = stream.getClass().getName();

            // IGNORA A CLASSE
            int numDim = stream.nextInstance().getData().numValues() - 1;
            KDTreeSimple kdtree = new KDTreeSimple(numDim);

            int count = 0;

            while (stream.hasMoreInstances() && count < total) {
                Instance inst = stream.nextInstance().getData();
                inst.setMissing(inst.classAttribute());
                kdtree.update(inst);
                System.out.println(
                        name_stream + "," +
                                inst.toString().replace(',', ' ') + "," +
                                kdtree.getNumNodes() + "," +
                                kdtree.getHeightTree() + "," +
                                kdtree.isBalanced() + "," +
                                kdtree.balancedFactor());
                count++;
            }

            kdtree.printInOrderToFile(name_stream + "_kd_tree.dot");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        try {
            InstanceStream[] streams_teste = {
                    new AssetNegotiationGenerator(),
                    new SEAGenerator(),
                    new RandomRBFGenerator()
            };

            System.out.println("stream,instancia_adicionada,n,altura_arvore,balanceada,fator_balanceamento");
            for (int i = 0; i < streams_teste.length; i++) {
                TestKdTree.run_experiments(streams_teste[i], 500);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

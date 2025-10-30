package moa;

import java.io.File;

import com.yahoo.labs.samoa.instances.Instance;

import moa.classifiers.lazy.neighboursearch.KDTreeSimple;
import moa.options.AbstractOptionHandler;
import moa.streams.ArffFileStream;
import moa.streams.InstanceStream;
import moa.streams.generators.AgrawalGenerator;
import moa.streams.generators.AssetNegotiationGenerator;
import moa.streams.generators.HyperplaneGenerator;
import moa.streams.generators.LEDGenerator;
import moa.streams.generators.LEDGeneratorDrift;
import moa.streams.generators.RandomRBFGenerator;
import moa.streams.generators.RandomRBFGeneratorDrift;
import moa.streams.generators.RandomTreeGenerator;
import moa.streams.generators.SEAGenerator;
import moa.streams.generators.STAGGERGenerator;
import moa.streams.generators.WaveformGenerator;
import moa.streams.generators.WaveformGeneratorDrift;

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
                                kdtree.getExpectedHeightTree() + "," +
                                kdtree.isBalanced()
                // + "," + kdtree.balancedFactor()
                );
                count++;
            }

            kdtree.printInOrderToFile(name_stream + "_kd_tree.dot");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void run_real_experiment(String arffPath, int total) {
        try {
            File f = new File(arffPath);
            if (!f.exists()) {
                System.err.println("Arquivo não encontrado: " + arffPath);
                return;
            }

            // Cria o stream a partir do arquivo .arff
            InstanceStream stream = new ArffFileStream(arffPath, -1); // -1: última coluna como classe
            if (stream instanceof AbstractOptionHandler) {
                ((AbstractOptionHandler) stream).prepareForUse();
            }

            String name_stream = f.getName();

            // Ignora a classe para KDTree
            int numDim = stream.nextInstance().getData().numValues() - 1;
            KDTreeSimple kdtree = new KDTreeSimple(numDim);

            int count = 0;
            System.out.println(
                    "stream,instancia_adicionada,total_nos,altura_arvore,altura_min_esperada(\\lfloor \\log_2 n \\rfloor),balanceada"
            );

            while (stream.hasMoreInstances() && count < total) {
                Instance inst = stream.nextInstance().getData();
                inst.setMissing(inst.classAttribute());
                kdtree.update(inst);

                System.out.println(
                        name_stream + "," +
                                inst.toString().replace(',', ' ') + "," +
                                kdtree.getNumNodes() + "," +
                                kdtree.getHeightTree() + "," +
                                kdtree.getExpectedHeightTree() + "," +
                                kdtree.isBalanced()
                );
                count++;
            }

            // Salva árvore para visualização
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
                    new RandomRBFGenerator(),
                    new AgrawalGenerator(),
                    new HyperplaneGenerator(),
                    new STAGGERGenerator(),
                    new RandomTreeGenerator(),
                    new WaveformGenerator(),
                    new LEDGenerator(),
                    // Tem drift no nome, verificar se já é uma stream com drift, eu não tenho
                    // certeza
                    new WaveformGeneratorDrift(),
                    new RandomRBFGeneratorDrift(),
                    new LEDGeneratorDrift(),

            };

            System.out.println(
                    "stream,instancia_adicionada,total_nos,altura_arvore,altura_min_esperada(\\lfloor \\log_2 n \\rfloor),balanceada");
            for (int i = 0; i < streams_teste.length; i++) {
                TestKdTree.run_experiments(streams_teste[i], 1000);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

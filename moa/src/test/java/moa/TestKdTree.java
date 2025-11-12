package moa;

import java.io.File;
import java.util.logging.*;

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
import scala.App;

// PARA RODAR ESSE MONSTRO, BASTA COMPILAR E USAR:
// A MINHA IDEIA É TIRAR ISSO DAQUI, NÃO FAZ SENTIDO FICAR NO TEST
// mvn test-compile
// java -cp moa/target/test-classes:moa/target/classes moa.TestKdTree > output.txt
public class TestKdTree {

    private static final Logger LOG = Logger.getLogger(TestKdTree.class.getName());

    public static void run_experiments(InstanceStream stream, int total) {
        try {
            if (stream instanceof AbstractOptionHandler)
                ((AbstractOptionHandler) stream).prepareForUse();
            else {
                throw new UnsupportedOperationException("Unimplemented method 'getDistances'");
            }

            String name_stream = stream.getClass().getName();

            // IGNORA A CLASSE
            int numDim = stream.getHeader().numAttributes() - 1;
            KDTreeSimple kdtree = new KDTreeSimple(numDim);

            int count = 0;

            while (stream.hasMoreInstances() && count < total) {
                Instance inst = stream.nextInstance().getData();
                inst.setMissing(inst.classAttribute());
                kdtree.update(inst);
                System.out.println(
                        name_stream + "," +
                                kdtree.getNumNodes() + "," +
                                kdtree.getHeightTree() + "," +
                                kdtree.getExpectedHeightTree() + "," +
                                kdtree.isBalanced()
                // + "," + kdtree.balancedFactor()
                );
                count++;
            }

            // kdtree.printInOrderToFile(name_stream + "_kd_tree.dot");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void run_real_experiment(String arffPath) {
        try {
            File f = new File(arffPath);
            if (!f.exists()) {
                System.err.println("Arquivo não encontrado: " + arffPath);
                return;
            }

            // APENAS PARA LEMBRAR -1 É PRA PEGAR P ULTIMO ATRIBUTO COMO CLASSE
            InstanceStream stream = new ArffFileStream(arffPath, -1);
            if (stream instanceof AbstractOptionHandler) {
                ((AbstractOptionHandler) stream).prepareForUse();
            }

            String name_stream = f.getName();

            // Ignora a classe para KDTree
            int numDim = stream.getHeader().numAttributes();
            boolean remover_classe = false;
            // LEMBRAR: FAÇO ISSO PORQUE A AWS NÃO TEM CLASSE DEFINIDA
            // na minha cabeça isso não é muito comum, se precisar crio uma lista
            if (!name_stream.equals("aws-spot-pricing-market.arff")) {
                numDim = numDim - 1;
                remover_classe = true;
            }

            KDTreeSimple kdtree = new KDTreeSimple(numDim);

            int count = 0;
            while (stream.hasMoreInstances()) {
                Instance inst = stream.nextInstance().getData();
                count++;
                if (remover_classe) {
                    inst.setMissing(inst.classAttribute());
                }
                kdtree.update(inst);

                if (count == 1 || count % 1000 == 0) {
                    System.out.println(
                            name_stream + "," +
                                    kdtree.getNumNodes() + "," +
                                    kdtree.getHeightTree() + "," +
                                    kdtree.getExpectedHeightTree() + "," +
                                    kdtree.isBalanced());
                }
            }

            System.out.println(
                    name_stream + "," +
                            kdtree.getNumNodes() + "," +
                            kdtree.getHeightTree() + "," +
                            kdtree.getExpectedHeightTree() + "," +
                            kdtree.isBalanced());

            // Salva árvore para visualização
            // kdtree.printInOrderToFile(name_stream + "_kd_tree.dot");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        try {

            boolean rodar_sintetico = false;
            if (args.length > 0 && args[0].equals("1")) {
                rodar_sintetico = true;
            }

            ////////////////// STREAMS DATASETS EXPERIMENTO //////////////////
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

            // CABEÇALHO DOS EXPERIMENTOS
            System.out.println(
                    "stream,total_nos,altura_arvore,altura_min_esperada(\\lfloor \\log_2 n \\rfloor),balanceada");

            if (rodar_sintetico) {
                LOG.info("EXPERIMENTO DADOS SINTETICOS");
                for (int i = 0; i < streams_teste.length; i++) {
                    TestKdTree.run_experiments(streams_teste[i], 1000);
                }
            }

            ////////////////// STREAMS DATASETS REAIS //////////////////
            String[] arffFiles = {
                    "moa/classifiers/data/aws-spot-pricing-market.arff",
                    // "moa/classifiers/data/airlines.arff",
                    // "moa/classifiers/data/covtypeNorm.arff",
                    // "moa/classifiers/data/covertype.arff",
                    // "moa/classifiers/data/elecNormNew.arff",
                    // "moa/classifiers/data/electricity.arff",
                    // "moa/classifiers/data/pklot.arff",
                    "moa/classifiers/data/pklot_512.arff",
                    // "moa/classifiers/data/pklot_1000.arff",
                    // "moa/classifiers/data/poker-lsn.arff"
            };

            for (String file : arffFiles) {
                try {
                    LOG.info("EXPERIMENTO: " + file);
                    String arffFile = App.class.getClassLoader().getResource(file).getPath();
                    run_real_experiment(arffFile);
                } catch (OutOfMemoryError e) {
                    LOG.warning("Faltou memória em " + file);
                    // e.printStackTrace();

                    // Libera memória
                    System.gc();
                    Thread.sleep(2000);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

package moa.tasks;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;

import com.github.javacliparser.FileOption;
import com.yahoo.labs.samoa.instances.Instance;
import com.yahoo.labs.samoa.instances.Instances;

import moa.classifiers.lazy.neighboursearch.EuclideanDistance;
import moa.classifiers.lazy.neighboursearch.KDTreeSimple;
import moa.core.Example;
import moa.core.ObjectRepository;
import moa.core.TimingUtils;
import moa.options.ClassOption;
import moa.streams.ExampleStream;

public class ExperimentoTempos extends MainTask {

    //////////////////// PARAMETROS DO EXPERIMENTO
    public ClassOption streamOption = new ClassOption("stream", 's',
            "Stream to evaluate on.", ExampleStream.class,
            "generators.RandomTreeGenerator");

    public FileOption outputFileOption = new FileOption("outputFile", 'o',
            "File to append output temp train and test to.", null, ".txt", true);

    @Override
    public Class<?> getTaskResultType() {
        return null;
    }

    @Override
    protected Object doMainTask(TaskMonitor monitor, ObjectRepository repository) {
        ////////////////////////// CONFIGURAÇÕES
        ExampleStream<?> stream = (ExampleStream<?>) getPreparedClassOption(this.streamOption);
        monitor.setCurrentActivity("EXP: TEMPO KDTREE", -1.0);

        File outputTempFile = this.outputFileOption.getFile();
        PrintStream outputStream = null;
        if (outputTempFile != null) {
            try {
                if (outputTempFile.exists()) {
                    outputStream = new PrintStream(
                            new FileOutputStream(outputTempFile, false), true);
                } else {
                    outputStream = new PrintStream(
                            new FileOutputStream(outputTempFile), true);
                }
            } catch (Exception ex) {
                throw new RuntimeException(
                        "Unable to open prediction result file: " + outputTempFile, ex);
            }
        } else {
            System.out.println("NÃO TEM ARQUIVO DE SAÍDA");
            return null;
        }
        ///////////////////////////////////////////////////////////////////////////////////////////

        KDTreeSimple kdtree = null;
        long numeroInstancias = 0;

        // Cabeçalho
        outputStream.println(
                "numero_instancias,tempo_insert,tempo_busca,altura_arvore_pos_insercao,profundidade_insercao,profundidade_busca,backtracking");
        while (stream.hasMoreInstances()) {
            try {
                long start_search, end_search, start_insert, end_insert;
                double temp_insert, temp_search = 0.0;

                Example<?> ex = stream.nextInstance();
                Instance inst = (Instance) ex.getData();

                if (kdtree == null) {
                    EuclideanDistance distFn = new EuclideanDistance();
                    kdtree = new KDTreeSimple(inst.numAttributes() - 1);
                    kdtree.setInstances(new Instances(inst.dataset(), 0));
                    kdtree.setDistanceFunction(distFn);
                }

                // BUSCA
                if (kdtree.getNumNodes() > 0) {
                    start_search = TimingUtils.getNanoCPUTimeOfCurrentThread();
                    kdtree.nearestNeighbour(inst);
                    end_search = TimingUtils.getNanoCPUTimeOfCurrentThread();
                    temp_search = TimingUtils.nanoTimeToSeconds(end_search - start_search);
                }

                // INSERE
                start_insert = TimingUtils.getNanoCPUTimeOfCurrentThread();
                kdtree.update(inst);
                end_insert = TimingUtils.getNanoCPUTimeOfCurrentThread();
                temp_insert = TimingUtils.nanoTimeToSeconds(end_insert - start_insert);

                numeroInstancias++;

                // ADICIONA NO ARQUIVO
                outputStream.println(
                        numeroInstancias + "," + temp_insert + "," + temp_search + "," + kdtree.getHeightTree() + ","
                                + kdtree.depthInsert + "," + kdtree.depthSearch + "," + kdtree.backtrackCount);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (outputStream != null) {
            outputStream.close();
        }

        return null;

    }

}

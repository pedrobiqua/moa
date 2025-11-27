package moa.tasks;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;

import com.github.javacliparser.FileOption;
import com.yahoo.labs.samoa.instances.Instance;

import moa.classifiers.lazy.neighboursearch.KDTreeSimple;
import moa.core.Example;
import moa.core.ObjectRepository;
import moa.core.TimingUtils;
import moa.options.ClassOption;
import moa.streams.ExampleStream;

public class ExperimenterTimingKDTree extends MainTask {

    public ClassOption streamOption = new ClassOption("stream", 's',
            "Stream to evaluate on.", ExampleStream.class,
            "generators.RandomTreeGenerator");

    public FileOption outputResultsOption = new FileOption("outputTempFile", 'o',
            "File to append output temp train and test to.", null, ".txt", true);

    @Override
    public Class<?> getTaskResultType() {
        throw new UnsupportedOperationException("Unimplemented method 'getTaskResultType'");
    }

    @Override
    protected Object doMainTask(TaskMonitor monitor, ObjectRepository repository) {
        /////////////////// CONFIGURAÇÕES ///////////////////
        ExampleStream<?> stream = (ExampleStream<?>) getPreparedClassOption(this.streamOption);
        File outputResults = this.outputResultsOption.getFile();
        PrintStream outputResultStream = null;
        if (outputResults == null) {
            throw new RuntimeException("Arquivo não adicionado no args");
        } else {
            try {
                if (outputResults.exists()) {
                    outputResultStream = new PrintStream(
                            new FileOutputStream(outputResults, false), true);
                } else {
                    outputResultStream = new PrintStream(
                            new FileOutputStream(outputResults), true);
                }
            } catch (Exception ex) {
                throw new RuntimeException(
                        "Unable to open prediction result file: " + outputResults, ex);
            }
        }
        /////////////// FIM CONFIGURAÇÕES ///////////////////

        KDTreeSimple kdtree = null;
        while (stream.hasMoreInstances()) {
            try {
                Example<?> ex = stream.nextInstance();
                Instance inst = (Instance) ex.getData();

                if (kdtree == null) {
                    kdtree = new KDTreeSimple(inst.numAttributes() - 1);
                }

                // BUSCA
                long startNearestNeighbours = TimingUtils.getNanoCPUTimeOfCurrentThread();
                kdtree.nearestNeighbour(inst);
                long endNearestNeighbours = TimingUtils.getNanoCPUTimeOfCurrentThread();
                double timeNearestNeighbours = TimingUtils
                        .nanoTimeToSeconds(endNearestNeighbours - startNearestNeighbours);

                // INSERT
                long startInsert = TimingUtils.getNanoCPUTimeOfCurrentThread();
                kdtree.update(inst);
                long endInsert = TimingUtils.getNanoCPUTimeOfCurrentThread();
                double timeInsert = TimingUtils.nanoTimeToSeconds(endInsert - startInsert);

                outputResultStream.println(timeInsert + "," + timeNearestNeighbours);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        outputResultStream.close();
        System.out.println("Finalizado os experimentos!");

        return null;
    }

}

package moa.tasks;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;

import com.github.javacliparser.FileOption;
import com.yahoo.labs.samoa.instances.Instance;
// import com.yahoo.labs.samoa.instances.Instances;

// import moa.classifiers.lazy.neighboursearch.KDTree;
import moa.classifiers.lazy.neighboursearch.SKDTree;
import moa.core.Example;
import moa.core.ObjectRepository;
import moa.core.TimingUtils;
import moa.options.AbstractOptionHandler;
import moa.options.ClassOption;
import moa.streams.ExampleStream;
import moa.streams.generators.AgrawalGenerator;
import moa.streams.generators.STAGGERGenerator;

public class ExperimentosKDtreeMOA extends MainTask {
    public ClassOption streamOption = new ClassOption("stream", 's',
            "Stream to evaluate on.", ExampleStream.class,
            "generators.RandomTreeGenerator");

    public FileOption outputFileOption = new FileOption("outputFile", 'o',
            "File to append output temp train and test to.", null, ".txt", true);

    @Override
    public Class<?> getTaskResultType() {
        throw new UnsupportedOperationException("Unimplemented method 'getTaskResultType'");
    }

    public PrintStream configOutputMetrics() {

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

                return outputStream;
            } catch (Exception ex) {
                throw new RuntimeException(
                        "Unable to open prediction result file: " + outputTempFile, ex);
            }
        } else {
            throw new RuntimeException("NÃO TEM ARQUIVO DE SAÍDA");
        }
    }

    @Override
    protected Object doMainTask(TaskMonitor monitor, ObjectRepository repository) {

        // ExampleStream<?> stream = (ExampleStream<?>)
        // getPreparedClassOption(this.streamOption);
        ExampleStream<?> stream = new STAGGERGenerator();
        if (stream instanceof AbstractOptionHandler)
            ((AbstractOptionHandler) stream).prepareForUse();
        else {
            throw new UnsupportedOperationException("Unimplemented method 'prepareForUse'");
        }

        PrintStream output = configOutputMetrics();

        output.println("numero_instancias,tempo_insert,tempo_busca");

        try {
            long start_search, end_search, start_insert, end_insert;
            double temp_insert = 0.0, temp_search = 0.0;

            // Aquecimento
            System.out.println("Aquecendo \n");
            for (int i = 0; i < 3; i++) {
                SKDTree search = null;
                search = new SKDTree((stream.getHeader().numAttributes() - 1), stream.getHeader());
                int numInstancias = 0;
                int maxInstancias = 200000;

                while (stream.hasMoreInstances() && numInstancias < maxInstancias) {

                    Example<?> ex = stream.nextInstance();
                    Instance inst = (Instance) ex.getData();

                    // Busca
                    if (numInstancias != 0) {
                        start_search = TimingUtils.getNanoCPUTimeOfCurrentThread();
                        search.nearestNeighbour(inst);
                        end_search = TimingUtils.getNanoCPUTimeOfCurrentThread();
                        temp_search = TimingUtils.nanoTimeToSeconds(end_search - start_search);
                    }

                    start_insert = TimingUtils.getNanoCPUTimeOfCurrentThread();
                    search.update(inst);
                    end_insert = TimingUtils.getNanoCPUTimeOfCurrentThread();

                    temp_insert = TimingUtils.nanoTimeToSeconds(end_insert - start_insert);
                    numInstancias++;
                    if (numInstancias % 10000 == 0) {
                        System.out.println(numInstancias);
                    }
                }
            }

            System.out.println("Realizando o Experimento \n");
            SKDTree search = null;
            search = new SKDTree((stream.getHeader().numAttributes() - 1), stream.getHeader());
            stream.restart();
            int numInstancias = 0;
            int maxInstancias = 200000;

            while (stream.hasMoreInstances() && numInstancias < maxInstancias) {

                Example<?> ex = stream.nextInstance();
                Instance inst = (Instance) ex.getData();

                // Busca
                if (numInstancias != 0) {
                    start_search = TimingUtils.getNanoCPUTimeOfCurrentThread();
                    search.nearestNeighbour(inst);
                    end_search = TimingUtils.getNanoCPUTimeOfCurrentThread();

                    temp_search = TimingUtils.nanoTimeToSeconds(end_search - start_search);
                    // System.out.println("Temp Busca: " + temp_search);
                }

                start_insert = TimingUtils.getNanoCPUTimeOfCurrentThread();
                // window.add(inst);
                search.update(inst);
                end_insert = TimingUtils.getNanoCPUTimeOfCurrentThread();

                temp_insert = TimingUtils.nanoTimeToSeconds(end_insert - start_insert);
                // System.out.println("Temp Insert: " + temp_insert);

                // output.println(window.size() + "," + temp_insert + "," + temp_search);
                output.println(numInstancias + "," + temp_insert + "," + temp_search);

                numInstancias++;
                if (numInstancias % 10000 == 0) {
                    System.out.println(numInstancias);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

        return null;
    }

}

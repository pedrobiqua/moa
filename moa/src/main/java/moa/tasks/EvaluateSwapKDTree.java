package moa.tasks;

import com.github.javacliparser.FileOption;
import com.github.javacliparser.IntOption;
import com.yahoo.labs.samoa.instances.Instance;
import com.yahoo.labs.samoa.instances.Instances;
import moa.classifiers.lazy.neighboursearch.SwapKDTree;
import moa.core.Example;
import moa.core.ObjectRepository;
import moa.options.AbstractOptionHandler;
import moa.options.ClassOption;
import moa.streams.ExampleStream;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class EvaluateSwapKDTree extends MainTask {

    public FileOption outputFileOption = new FileOption(
            "outputFile", 'o',
            "File to append output temp train and test to.",
            null, ".txt", true);

    public ClassOption streamOption = new ClassOption(
            "stream", 's',
            "Stream to evaluate on.",
            ExampleStream.class,
            "generators.RandomTreeGenerator");

    public IntOption windowSizeOption = new IntOption(
            "window_size", 'w',
            "Number of instances maintained in the sliding window.",
            1000, 3, Integer.MAX_VALUE);

    private static final int BATCH_SIZE = 100_000;

    private boolean flag = true;

    public class Result {
        private final Map<String, Object> values = new LinkedHashMap<>();

        public void add(String name, Object value) {
            values.put(name, value);
        }

        public void addMetrics(String prefix, Map<String, Object> metrics) {
            metrics.forEach((key, value) ->
                    values.put(prefix + key, value)
            );
        }

        public Map<String, Object> getValues() {
            return values;
        }

        public String getHeader() {
            return String.join(",", values.keySet());
        }

        @Override
        public String toString() {
            return values.values().stream()
                    .map(String::valueOf)
                    .collect(Collectors.joining(","));
        }
    }

    public PrintStream configOutputMetrics() throws Exception {
        File outputTempFile = this.outputFileOption.getFile();
        PrintStream outputStream = null;
        if (outputTempFile != null) {
            try {
                if (outputTempFile.exists()) {
                    outputStream = new PrintStream(
                            new FileOutputStream(outputTempFile, false), false);
                } else {
                    outputStream = new PrintStream(
                            new FileOutputStream(outputTempFile), false);
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

    private void saveResults(PrintStream output, List<Result> results) {
        if (!results.isEmpty()) {
            if (flag) {
                output.println(results.get(0).getHeader());
                flag = false;
            }
            System.out.println("Saving the results...");
            for (Result result : results) {
                output.println(result);
            }
        }
    }

    private void expSlidingWindow(ExampleStream<?> stream, boolean isArff,
                                  String datasetName) {
        // Create output files
        PrintStream output;
        try {
            output = configOutputMetrics();
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        int count = 0;
        long maxInstances = maxInstances(isArff);

        SwapKDTree skdtree = new SwapKDTree(windowSizeOption.getValue());
        try {
            skdtree.setInstances(new Instances(stream.getHeader()));
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        List<Result> results = new ArrayList<Result>();
        boolean flag = true;
        try {
            System.out.println("Executando exp sliding window...");
            while (stream.hasMoreInstances() && count < maxInstances) {
                Example<?> ex = stream.nextInstance();
                Instance target = (Instance) ex.getData();

                long time_search = 0;
                if (count != 0) {
                    long start_search = System.nanoTime();
                    skdtree.nearestNeighbour(target);
                    long end_search = System.nanoTime();
                    time_search = end_search - start_search;
                }

                // Update tree
                long start_update = System.nanoTime();
                skdtree.update(target);
                long end_update = System.nanoTime();
                long time_update = end_update - start_update;


                Result result = new Result();
                result.addMetrics("init_", skdtree.metricsInitTree.getMetrics());
                result.add("time_update", time_update);
                result.add("time_search", time_search);
                result.addMetrics("new_", skdtree.metricsNewTree.getMetrics());
                results.add(result);
                count++;

                // Salva no disco e libera memória a cada lote
                if (results.size() >= BATCH_SIZE) {
                    // Adiciona o header apenas uma vez
                    saveResults(output, results);
                    results.clear();
                }
            }
            saveResults(output, results);

        } catch (StackOverflowError | Exception e) {
            saveResults(output, results);
            e.printStackTrace();
        }
    }

    @Override
    protected Object doMainTask(TaskMonitor monitor, ObjectRepository repository) {
        ExampleStream<?> stream = (ExampleStream<?>) getPreparedClassOption(this.streamOption);
        if (stream instanceof AbstractOptionHandler)
            ((AbstractOptionHandler) stream).prepareForUse();
        else {
            throw new UnsupportedOperationException("Unimplemented method 'prepareForUse'");
        }

        boolean isArff = false;
        String datasetName = "";
        if (stream.getClass().getSimpleName().equals("ArffFileStream")) {
            isArff = true;
            // Cast pra ArffFileStream
            moa.streams.ArffFileStream arffStream = (moa.streams.ArffFileStream) stream;
            // Pega o nome do arquivo (sem o caminho completo e sem extensão)
            datasetName = arffStream.arffFileOption.getFile().getName().split(".arff")[0];
        } else {
            datasetName = stream.getClass().getSimpleName();
        }

        System.out.printf("%-30s %-30s\n",
                "Dataset", "Window Size, Structure");
        System.out.printf("%-30s %-30s %-30s\n",
                datasetName,
                windowSizeOption.getValue(),
                "SwapKDTree");
        expSlidingWindow(stream, isArff, datasetName);

        return null;

    }
// Teste no doMainTask
//    ExampleStream<?> stream = (ExampleStream<?>) getPreparedClassOption(this.streamOption);
//        if (stream instanceof AbstractOptionHandler)
//            ((AbstractOptionHandler) stream).prepareForUse();
//        else {
//        throw new UnsupportedOperationException("Unimplemented method 'prepareForUse'");
//    }
//
//    SwapKDTree kdtree = new SwapKDTree(windowSizeOption.getValue());
//        try {
//        kdtree.setInstances(new Instances(stream.getHeader()));
//    } catch (Exception e) {
//        throw new RuntimeException(e);
//    }
//
//    int count = 0;
//        while (stream.hasMoreInstances()) {
//        Example<?> ex = stream.nextInstance();
//        Instance target = (Instance) ex.getData();
//        try {
//            if (count != 0)
//                kdtree.nearestNeighbour(target);
//            kdtree.update(target);
//
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//        count++;
//    }
//
//        return null;

    @Override
    public Class<?> getTaskResultType() {
        return null;
    }

    private long maxInstances(boolean isArff) {
        long maxInstances;
        if (isArff)
            maxInstances = Integer.MAX_VALUE;
        else
            maxInstances = 500000;

        return maxInstances;
    }
}

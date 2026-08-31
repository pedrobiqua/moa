package moa.tasks;

import com.github.javacliparser.*;

import com.yahoo.labs.samoa.instances.Instance;
import com.yahoo.labs.samoa.instances.Instances;

import moa.classifiers.lazy.neighboursearch.NSKDtree;
import moa.classifiers.lazy.neighboursearch.Window;
import moa.classifiers.lazy.neighboursearch.rebuildpolicies.*;
import moa.core.Example;
import moa.core.ObjectRepository;
import moa.options.AbstractOptionHandler;
import moa.options.ClassOption;
import moa.streams.ExampleStream;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Task for evaluating a naive k-d tree in a streaming environment.
 *
 *
 * @author Anonymous
 */
public class EvaluateNKDTree extends MainTask {

    public FileOption outputFileOption = new FileOption(
            "outputFile", 'o',
            "File to append output temp train and test to.",
            null, ".txt", true);

    public ClassOption streamOption = new ClassOption(
            "stream", 's',
            "Stream to evaluate on.",
            ExampleStream.class,
            "generators.RandomTreeGenerator");

    public MultiChoiceOption policyOption = new MultiChoiceOption(
            "policy", 'p',
            "Policy used to determine when the tree should be rebuilt.",
            new String[] {
                    "DeletedRatioPolicy",
                    "HeightBalancedPolicy",
                    "SquareRoot",
                    "Log",
                    "LogRatio"
            },
            new String[] {
                    "Deleted Ratio",
                    "Alpha Height",
                    "Square Root",
                    "Logarithmic",
                    "Logarithmic Ratio"
            },
            0);

    public FloatOption alphaOption = new FloatOption(
            "param", 'a',
            "Parameter used by the height-based rebuild policy and deleted ratio.",
            0.6, 0.2, 1.0);

    public IntOption windowSize = new IntOption(
            "window_size", 'w',
            "Number of instances maintained in the sliding window.",
            1000, 0, Integer.MAX_VALUE);

    public FlagOption collectSearchMetricsOption = new FlagOption(
            "collect_search_metrics", 'm',
            "Enable the collection of search metrics.");


    public interface SearchMetrics {
        void nodeVisited();
        void reset();
    }

    public class VisitedNodesMetrics implements SearchMetrics {

        private long visitedNodes = 0;

        @Override
        public void nodeVisited() {
            visitedNodes++;
        }

        @Override
        public void reset() {
            visitedNodes = 0;
        }

        public long getVisitedNodes() {
            return visitedNodes;
        }
    }

    public class Result {

        private final Map<String, Object> values = new LinkedHashMap<>();

        public void add(String name, Object value) {
            values.put(name, value);
        }

        public void addMetrics(Map<String, Object> metrics) {
            this.values.putAll(metrics);
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

    private void saveResults(PrintStream output, List<Result> results) {
        if (!results.isEmpty()) {
            System.out.println("Saving the results...");
            output.println(results.get(0).getHeader());

            for (Result result : results) {
                output.println(result);
            }
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

    public PrintStream timeOutputExp(String dataset_name, RebuildPolicy policy) throws Exception {
        String parameters;
        if (windowSize.getValue() != 0)
            parameters = "_a" + alphaOption.getValue() + "_p" + policy.getClass().getSimpleName() + "_w" + windowSize.getValue();
        else
            parameters = "_w0";
        String nameFile = dataset_name + parameters + "_time_exp.csv";
        File outputTempFile = new File(this.outputFileOption.getFile().getParent() + "/" + nameFile);

        PrintStream outputStream = null;
        try {
            if (outputTempFile.exists()) {
                outputStream = new PrintStream(
                        new FileOutputStream(outputTempFile, true), false);
            } else {
                outputStream = new PrintStream(
                        Files.newOutputStream(outputTempFile.toPath()), false);
                outputStream.println("time_exp");
            }

            return outputStream;
        } catch (Exception ex) {
            throw new RuntimeException(
                    "Unable to open prediction result file: " + outputTempFile, ex);
        }
    }

    @Override
    public Class<?> getTaskResultType() {
        return null;
    }

    private void expSlidingWindow(ExampleStream<?> stream, RebuildPolicy rebuildPolicy, int window_size, boolean isArff, String datasetName) {

        // Create output files
        PrintStream output;
        PrintStream exp_time_output;
        try {
            output = configOutputMetrics();
            exp_time_output = timeOutputExp(datasetName, rebuildPolicy);
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        int count = 0;
        long maxInstances = maxInstances(isArff);

        NSKDtree skdtree = new NSKDtree();
        VisitedNodesMetrics visitedMetrics = null;
        try {
            skdtree.setInstances(new Instances(stream.getHeader(), window_size));
            if (collectSearchMetricsOption.isSet()) {
                visitedMetrics = new VisitedNodesMetrics();
                skdtree.setSearchMetrics(visitedMetrics);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }
        List<Result> results = new ArrayList<Result>();
        long countRebuild = 0;

        Window window = new Window(window_size);
        try {
            System.out.println("Executando exp sliding window...");
            double start_exp_time = System.nanoTime();
            while (stream.hasMoreInstances() && count < maxInstances) {
                Example<?> ex = stream.nextInstance();
                Instance target = (Instance) ex.getData();

                long time_search = 0;
                if (skdtree.getInstances() != null && skdtree.getInstances().numInstances() > 0) {
                    long start_search = System.nanoTime();
                    skdtree.nearestNeighbour(target);
                    long end_search = System.nanoTime();
                    time_search = end_search - start_search;
                }

                // Slide window
                int idx_remove = window.update(skdtree.getNumInstances());
                if (idx_remove != -1)
                    skdtree.delete(idx_remove);

                // Update tree
                long start_update = System.nanoTime();
                skdtree.update(target);
                long end_update = System.nanoTime();
                long time_update = end_update - start_update;

                // Should rebuild?
                long time_rebuild = 0;
                if (rebuildPolicy.checkRebuild(skdtree.metricsTree)) {
                    int[] toCopy = window.toCopy();
                    Instances insts_window = new Instances(skdtree.getInstances(), toCopy[0], toCopy[1]);

                    long start_rebuild = System.nanoTime();
                    skdtree.buildTree(insts_window);
                    long end_rebuild = System.nanoTime();

                    countRebuild++;
                    window.reset();
                    time_rebuild = end_rebuild - start_rebuild;
                }

                Result result = new Result();
                result.addMetrics(skdtree.metricsTree.getMetrics());
                if (collectSearchMetricsOption.isSet()) {
                    assert visitedMetrics != null;
                    result.add("visited_nodes", visitedMetrics.getVisitedNodes());
                }
                result.add("time_update", time_update);
                result.add("time_search", time_search);
                result.add("time_rebuild", time_rebuild);
                result.add("count_rebuild", countRebuild);
                results.add(result);

                count++;
            }
            double end_exp_time = System.nanoTime();
            double time_exp = end_exp_time - start_exp_time;

            exp_time_output.println(time_exp);
            saveResults(output, results);

        } catch (StackOverflowError | Exception e) {
            saveResults(output, results);
            e.printStackTrace();
        }
    }

    private long maxInstances(boolean isArff) {
        long maxInstances;
        if (isArff)
            maxInstances = Integer.MAX_VALUE;
        else
            maxInstances = 500000;

        return maxInstances;
    }

    private void warmup(ExampleStream<?> stream, RebuildPolicy rebuildPolicy, int window_size) {
        try {
            int count = 0;
            long maxInstances = 100000;
            NSKDtree skdtree = new NSKDtree();
            skdtree.setInstances(new Instances(stream.getHeader(), window_size)); // Cria instances vazio alocando tamanho do array

            System.out.println("Executando warmup...");

            while (stream.hasMoreInstances() && count < maxInstances) {
                Example<?> ex = stream.nextInstance();
                Instance target = (Instance) ex.getData();

                if (skdtree.getInstances() != null && skdtree.getInstances().numInstances() > 0) {
                    skdtree.nearestNeighbour(target);
                }

                skdtree.update(target); // Atualiza a estrutura
                count++;
            }

            stream.restart();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void expInsertSearch(ExampleStream<?> stream, boolean isArff, String datasetName) {
        RebuildPolicy rebuildPolicy = new NoRebuild();
        // Configurações de output
        PrintStream output;
        PrintStream exp_time_output;
        try {
            output = configOutputMetrics();
            exp_time_output = timeOutputExp(datasetName, rebuildPolicy);
        } catch (Exception e){
            e.printStackTrace();
            return;
        }

        int count = 0;
        long maxInstances = maxInstances(isArff);

        NSKDtree skdtree = new NSKDtree();
        VisitedNodesMetrics visitedMetrics = new VisitedNodesMetrics();
        try {
            skdtree.setInstances(new Instances(stream.getHeader(), (int)stream.estimatedRemainingInstances()));
            if (collectSearchMetricsOption.isSet()) {
                visitedMetrics = new VisitedNodesMetrics();
                skdtree.setSearchMetrics(visitedMetrics);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        List<Result> results = new ArrayList<>();
        try {
            System.out.println("Executando exp insert search...");

            double start_exp_time = System.nanoTime();
            while (stream.hasMoreInstances() && count < maxInstances) {
                Example<?> ex = stream.nextInstance();
                Instance target = (Instance) ex.getData();

                long time_search = 0;
                if (skdtree.getInstances() != null && skdtree.getInstances().numInstances() > 0) {
                    long start_search = System.nanoTime();
                    skdtree.nearestNeighbour(target);
                    long end_search = System.nanoTime();
                    time_search = end_search - start_search;
                }

                long start_update = System.nanoTime();
                skdtree.update(target);
                long end_update = System.nanoTime();
                long time_update = end_update - start_update;

                Result result = new Result();
                result.addMetrics(skdtree.metricsTree.getMetrics());
                if (collectSearchMetricsOption.isSet()) {
                    assert visitedMetrics != null;
                    result.add("visited_nodes", visitedMetrics.getVisitedNodes());
                }
                result.add("time_update", time_update);
                result.add("time_search", time_search);
                results.add(result);
                count++;
            }
            double end_exp_time = System.nanoTime();
            double time_exp = end_exp_time - start_exp_time;

            // Salva os resultados
            exp_time_output.println(time_exp);
            saveResults(output, results);

        } catch (StackOverflowError | Exception e) {
            saveResults(output, results);
            e.printStackTrace();
        }
    }

    @Override
    protected Object doMainTask(TaskMonitor monitor, ObjectRepository repository) {
        ///////////////////////////////////////////////////////
        // Tratamento dos parâmetros do experimento
        ExampleStream<?> stream = (ExampleStream<?>) getPreparedClassOption(this.streamOption);
        if (stream instanceof AbstractOptionHandler)
            ((AbstractOptionHandler) stream).prepareForUse();
        else {
            throw new UnsupportedOperationException("Unimplemented method 'prepareForUse'");
        }

        int window_size = windowSize.getValue();

        int policyChosenIndex = policyOption.getChosenIndex();
        RebuildPolicy rebuildPolicy;
        if (policyChosenIndex == 0)
            rebuildPolicy = new DeletedRatioPolicy(alphaOption.getValue());
        else if (policyChosenIndex == 1)
            rebuildPolicy = new HeightBalancedPolicy(alphaOption.getValue());
        else if (policyChosenIndex == 2)
            rebuildPolicy = new IKDtreeRebuildPolicy(0.6, 0.5, window_size); // Parametro da ikdtree
        else if (policyChosenIndex == 3) // Usando o tamanho da janela como base
            rebuildPolicy = new SquareRootPolicy(window_size);
        else if (policyChosenIndex == 4)
            rebuildPolicy = new LogPolicy(window_size);
        else if (policyChosenIndex == 5) {
            rebuildPolicy = new LogRatioPolicy(window_size);
        }
        else
            rebuildPolicy = new DeletedRatioPolicy(0.3);
        ///////////////////////////////////////////////////////

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

        if (window_size != 0) {
            System.out.printf("%-30s %-30s %-30s %-30s\n",
                    "Dataset", "RebuildPolicy", "Parameters", "Window Size");
            System.out.printf("%-30s %-30s %-30s %-30s\n",
                    datasetName,
                    rebuildPolicy.getClass().getSimpleName(),
                    alphaOption.getValue(),
                    window_size);
            for (int i = 0; i < 3; i++) {
                warmup(stream, rebuildPolicy, window_size);
            }
            expSlidingWindow(stream, rebuildPolicy, window_size, isArff, datasetName);
        } else {
            System.out.printf("%-30s %-30s %-30s\n",
                    "Dataset", "RebuildPolicy", "Parameters");
            System.out.printf("%-30s %-30s %-30s\n",
                    datasetName,
                    "No Rebuild",
                    window_size);
            for (int i = 0; i < 3; i++) {
                warmup(stream, rebuildPolicy, window_size);
            }
            expInsertSearch(stream, isArff, datasetName);
        }
        return null;
    }

}

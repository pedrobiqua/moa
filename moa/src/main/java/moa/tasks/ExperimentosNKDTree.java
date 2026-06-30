package moa.tasks;

import com.github.javacliparser.FileOption;
import com.github.javacliparser.FloatOption;
import com.github.javacliparser.IntOption;
import com.github.javacliparser.MultiChoiceOption;
import com.yahoo.labs.samoa.instances.Instance;
import com.yahoo.labs.samoa.instances.Instances;
import moa.classifiers.lazy.neighboursearch.kdtrees.*;
import moa.core.Example;
import moa.core.ObjectRepository;
import moa.options.AbstractOptionHandler;
import moa.options.ClassOption;
import moa.streams.ExampleStream;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.nio.file.Files;

public class ExperimentosNKDTree extends MainTask {

    public FileOption outputFileOption = new FileOption("outputFile", 'o',
            "File to append output temp train and test to.", null, ".txt", true);

    public ClassOption streamOption = new ClassOption("stream", 's',
            "Stream to evaluate on.", ExampleStream.class,
            "generators.RandomTreeGenerator");

    public MultiChoiceOption policyOption = new MultiChoiceOption(
            "policy", 'p', "Method rebuild tree", new String[] {
                    "DeletedRatioPolicy", "HeightBalancedPolicy", "SquareRoot", "Log", "LogRatio" },
            new String[] { "Deleted Ratio 30%. ",
                    "Desbalanceamento da árvore.",
                    "Raiz Quadrada",
                    "Log",
                    "Razao do Log"
            }, 0);

    public FloatOption alphaOption = new FloatOption("alpha", 'a', "alpha value.", 0.6, 0.2, 1.0);

    public IntOption windowSize = new IntOption("window_size", 'w', "Window size.", 1000, 0, Integer.MAX_VALUE);

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
        try {
            PrintStream output = configOutputMetrics();
            PrintStream exp_time_output = timeOutputExp(datasetName, rebuildPolicy);

            int count = 0;
            long maxInstances;
            if (isArff)
                maxInstances = Integer.MAX_VALUE;
            else
                maxInstances = 500000;

            NSKDtree skdtree = new NSKDtree();
            skdtree.setWindowSize(window_size);
            skdtree.setRebuildPolicies(rebuildPolicy);
            skdtree.setInstances(new Instances(stream.getHeader(), window_size)); // Cria instances vazio

            System.out.println("Executando exp sliding window...");

            /// Cabeçalho
            output.println(skdtree.stats.getHeader() + ",time_update,time_search");
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

                // Extraindo as metricas coletadas do update e busca e tempo de inserção e busca
                output.println(skdtree.stats.getMetrics() + "," + time_update + "," + time_search);
                count++;
            }
            double end_exp_time = System.nanoTime();
            double time_exp = end_exp_time - start_exp_time;
            exp_time_output.println(time_exp);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void warmup(ExampleStream<?> stream, RebuildPolicy rebuildPolicy, int window_size) {
        try {
            int count = 0;
            long maxInstances = 100000;
            NSKDtree skdtree = new NSKDtree();
            skdtree.setWindowSize(window_size);
            skdtree.setRebuildPolicies(rebuildPolicy);
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
        try {
            RebuildPolicy rebuildPolicy = new NoRebuild();
            // Configurações de output
            PrintStream output = configOutputMetrics();
            PrintStream exp_time_output = timeOutputExp(datasetName, rebuildPolicy);

            int count = 0;
            long maxInstances;
            if (isArff)
                maxInstances = Integer.MAX_VALUE;
            else
                maxInstances = 500000;

            // Cria a instancia da árvore, e desliga a janela deslizante
            NSKDtree skdtree = new NSKDtree();
            skdtree.setRebuildPolicies(rebuildPolicy); // Usando NoRebuild
            skdtree.setTurnOffWindow(false); // Padrão é true!
            skdtree.setInstances(new Instances(stream.getHeader(), (int)stream.estimatedRemainingInstances())); // Cria instances vazio

            System.out.println("Executando exp insert search...");

            /// Cabeçalho
            output.println(skdtree.stats.getHeader() + ",time_update,time_search");
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

                // Extraindo as metricas coletadas do update e busca e tempo de inserção e busca
                output.println(skdtree.stats.getMetrics() + "," + time_update + "," + time_search);
                count++;
            }
            double end_exp_time = System.nanoTime();
            double time_exp = end_exp_time - start_exp_time;
            exp_time_output.println(time_exp);

        } catch (Exception e) {
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

        int policyChosenIndex = policyOption.getChosenIndex();
        RebuildPolicy rebuildPolicy;
        if (policyChosenIndex == 0)
            rebuildPolicy = new DeletedRatioPolicy(alphaOption.getValue());
        else if (policyChosenIndex == 1)
            rebuildPolicy = new HeightBalancedPolicy(alphaOption.getValue());
        else if (policyChosenIndex == 2) // Usando o tamanho da janela como base
            rebuildPolicy = new SquareRootPolicy();
        else if (policyChosenIndex == 3)
            rebuildPolicy = new LogPolicy();
        else if (policyChosenIndex == 4) {
            rebuildPolicy = new LogRatioPolicy();
        }
        else
            rebuildPolicy = new DeletedRatioPolicy(0.3);

        int window_size = windowSize.getValue();
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

            for (int i = 0; i <= 3; i++) {
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
            for (int i = 0; i <= 3; i++) {
                warmup(stream, rebuildPolicy, window_size);
            }
            expInsertSearch(stream, isArff, datasetName);
        }
        return null;
    }

}

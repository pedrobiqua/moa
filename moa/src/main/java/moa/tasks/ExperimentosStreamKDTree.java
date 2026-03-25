package moa.tasks;

import com.github.javacliparser.FileOption;
import com.github.javacliparser.MultiChoiceOption;
import com.yahoo.labs.samoa.instances.Instance;
import moa.classifiers.lazy.neighboursearch.StreamKDTree;
import moa.classifiers.lazy.neighboursearch.kdtrees.*;
import moa.core.Example;
import moa.core.ObjectRepository;
import moa.options.AbstractOptionHandler;
import moa.options.ClassOption;
import moa.streams.ExampleStream;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;

public class ExperimentosStreamKDTree extends MainTask {

    public FileOption outputFileOption = new FileOption("outputFile", 'o',
            "File to append output temp train and test to.", null, ".txt", true);

    public ClassOption streamOption = new ClassOption("stream", 's',
            "Stream to evaluate on.", ExampleStream.class,
            "generators.RandomTreeGenerator");

    // TODO: LEMBRAR DE ARRUMAR ESSA DESCRIÇÃO
    public MultiChoiceOption splitterOption = new MultiChoiceOption(
            "splitter", 'c', "Method Splitter option", new String[] {
                    "SlidingMidPointOfWidestSide", "MedianOfWidestDimension", "MidPointOfWidestDimension" },
            new String[] { "Sliding Mid Point Of Widest side. ",
                    "Median Of Widest Dimension.",
                    "Mid Point of widest dimension."
            }, 0);

    public MultiChoiceOption policyOption = new MultiChoiceOption(
            "policy", 'p', "Method rebuild tree", new String[] {
                    "DeletedRatioPolicy", "HeightBalancedPolicy", "NoRebuild" },
            new String[] { "Deleted Ratio 30%. ",
                    "Desbalanceamento da árvore.",
                    "Sem build"
            }, 0);

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

    @Override
    public Class<?> getTaskResultType() {
        return null;
    }

    private void expSlidingWindow(ExampleStream<?> stream, KDTreeNodeSplitter splitter, RebuildPolicy rebuildPolicy,
            boolean isArff) {
        try {
            // restartStream(stream);
            PrintStream output = configOutputMetrics();

            int count = 0;
            long maxInstances;
            if (isArff)
                maxInstances = Integer.MAX_VALUE;
            else
                maxInstances = 500000;

            StreamKDTree skdtree = new StreamKDTree();
            skdtree.setMaxInstInLeaf(40);
            skdtree.setNodeSplitter(splitter);
            skdtree.setWindowSize(1000);
            skdtree.setRebuildPolicies(rebuildPolicy);

            System.out.println("Executando exp sliding window...");
            output.println(skdtree.m_Stats.headerMetrics());
            while (stream.hasMoreInstances() && count < maxInstances) {
                Example<?> ex = stream.nextInstance();
                Instance target = (Instance) ex.getData();

                if (skdtree.getInstances() != null && skdtree.getInstances().numInstances() > 0) {
                    skdtree.nearestNeighbour(target);
                }
                skdtree.update(target);

                // Extraindo as metricas coletadas do update e busca
                output.println(skdtree.m_Stats.metricsAndStats());
                skdtree.m_Stats.resetMetrics();
                count++;
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected Object doMainTask(TaskMonitor monitor, ObjectRepository repository) {
        // Retirando o tempo do MOA
        // System.setErr(new java.io.PrintStream(new java.io.OutputStream() {
        // public void write(int b) {}
        // }));

        ///////////////////////////////////////////////////////
        // Tratamento dos parâmetros do experimento
        ExampleStream<?> stream = (ExampleStream<?>) getPreparedClassOption(this.streamOption);
        if (stream instanceof AbstractOptionHandler)
            ((AbstractOptionHandler) stream).prepareForUse();
        else {
            throw new UnsupportedOperationException("Unimplemented method 'prepareForUse'");
        }

        KDTreeNodeSplitter splitter = new MidPointOfWidestDimension();
        int splitterChosenIndex = splitterOption.getChosenIndex();
        if (splitterChosenIndex == 0) {
            System.out.println("Escolhido: Sliding Mid Point");
            splitter = new StreamSlidingMidPointOfWidestSide();
        } else if (splitterChosenIndex == 1) {
            System.out.println("Escolhido: Mid Point Of Widest Dimension");
            splitter = new MidPointOfWidestDimension();
        } else if (splitterChosenIndex == 2) {
            // Esse split não funciona para datastreams
            return null;
            // System.out.println("Escolhido: Median Of Widest Dimension");
            // splitter = new MedianOfWidestDimension();
        } else {
            System.err.print("Nenhum splitter escolhido!");
            return null;
        }

        int policyChosenIndex = policyOption.getChosenIndex();
        RebuildPolicy rebuildPolicy;
        if (policyChosenIndex == 0)
            rebuildPolicy = new DeletedRatioPolicy();
        else if (policyChosenIndex == 1)
            rebuildPolicy = new HeightBalancedPolicy();
        else if (policyChosenIndex == 2)
            rebuildPolicy = new InstancesPerLeafPolicy();
        else if (policyChosenIndex == 3)
            rebuildPolicy = new NoRebuild();
        else
            rebuildPolicy = new DeletedRatioPolicy();
        ///////////////////////////////////////////////////////

        boolean isArff = false;
        String datasetName = "";
        if (stream.getClass().getSimpleName().equals("ArffFileStream")) {
            isArff = true;
            // Cast pra ArffFileStream
            moa.streams.ArffFileStream arffStream = (moa.streams.ArffFileStream) stream;
            // Pega o nome do arquivo (sem o caminho completo)
            datasetName = arffStream.arffFileOption.getFile().getName();
        } else {
            datasetName = stream.getClass().getSimpleName();
        }

        System.out.printf("%-30s %-30s %-30s\n",
                "Dataset", "Splitter", "RebuildPolicy");

        System.out.printf("%-30s %-30s %-30s\n",
                datasetName,
                splitter.getClass().getSimpleName(),
                rebuildPolicy.getClass().getSimpleName());

        expSlidingWindow(stream, splitter, rebuildPolicy, isArff);
        return null;
    }

}

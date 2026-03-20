package moa.tasks;

import moa.classifiers.lazy.neighboursearch.EuclideanDistance;
import moa.classifiers.lazy.neighboursearch.kdtrees.*;
import moa.classifiers.lazy.neighboursearch.KDTree;
import moa.options.AbstractOptionHandler;
import moa.options.ClassOption;
import moa.classifiers.lazy.neighboursearch.StreamKDTree;
import moa.core.Example;
import moa.core.ObjectRepository;
import moa.streams.ExampleStream;

import com.github.javacliparser.MultiChoiceOption;
import com.yahoo.labs.samoa.instances.Instance;
import com.yahoo.labs.samoa.instances.Instances;

import java.util.ArrayList;

public class TesteStreamKDTRee extends MainTask {
    public ClassOption streamOption = new ClassOption("stream", 's',
            "Stream to evaluate on.", ExampleStream.class,
            "generators.RandomTreeGenerator");

    // TODO: LEMBRAR DE ARRUMAR ESSA DESCRIÇÃO
    public MultiChoiceOption splitterOption = new MultiChoiceOption(
            "splitter", 'c', "Method Splitter option", new String[]{
            "SlidingMidPointOfWidestSide", "MedianOfWidestDimension", "MidPointOfWidestDimension"},
            new String[]{"Sliding Mid Point Of Widest side. ",
                    "Median Of Widest Dimension.",
                    "Mid Point of widest dimension."
            }, 0);

    @Override
    public Class<?> getTaskResultType() {
        return null;
    }

    public void restartStream(ExampleStream<?> stream) {
        if (stream.isRestartable()) {
            System.out.println("Restart Stream");
            stream.restart();
        }
    }

    public static boolean isSameAttributes(Instance a, Instance b) {

        // Verifica quantidade de atributos
        if (a.numAttributes() != b.numAttributes())
            return false;

        double[] da = a.toDoubleArray();
        double[] db = b.toDoubleArray();

        int numAtributos = a.numAttributes();

        for (int i = 0; i < numAtributos; i++) {
            double va = da[i];
            double vb = db[i];

            // Trata NaN corretamente (NaN != NaN)
            if (Double.isNaN(va) && Double.isNaN(vb))
                continue;

            if (va != vb)
                return false;
        }

        return true;
    }

    private void validateAgainstKDTree(ExampleStream<?> stream, KDTreeNodeSplitter splitter, RebuildPolicy rebuildPolicy) {
        try {
            restartStream(stream);
            int count = 0;
            int maxInstances = 500000;
            int window_size = 1000;

            Instances window = new Instances(stream.getHeader(), 0);
            ArrayList<Instance> results_kdtree = new ArrayList<>();
            EuclideanDistance euclideanDistance = new EuclideanDistance();
            euclideanDistance.setDontNormalize(true);

            // Configurações da skdtree
            StreamKDTree skdtree = new StreamKDTree();
            skdtree.setMaxInstInLeaf(40);
            skdtree.setNodeSplitter(splitter);
            skdtree.setWindowSize(window_size);
            skdtree.setRebuildPolicies(rebuildPolicy);

            // KDTree com janela deslizante montando a toda busca
            System.out.println("Executando KDtree ...");
            while (stream.hasMoreInstances() && count < maxInstances) {
                count++;
                Example<?> ex = stream.nextInstance();
                Instance inst = (Instance) ex.getData();

                if (window.numInstances() > 0) {
                    KDTree kdtree = new KDTree();
                    kdtree.setDistanceFunction(euclideanDistance);
                    kdtree.setMaxInstInLeaf(20);
                    kdtree.setNodeSplitter(splitter);
                    kdtree.setInstances(window);

                    results_kdtree.add(kdtree.nearestNeighbour(inst));
                }

                if (window_size <= window.numInstances()) {
                    window.delete(0);
                }
                window.add(inst);
            }

            int index = 0;
            count = 0;
            if (stream.isRestartable()){
                System.out.println("Reiniciando stream ...");
                stream.restart();
            }

            // StreamKDTree
            System.out.println("Executando SKDtree ...");
            while (stream.hasMoreInstances() && count < maxInstances) {
                count++;
                Example<?> ex = stream.nextInstance();
                Instance target = (Instance) ex.getData();

                if (skdtree.getInstances() != null && skdtree.getInstances().numInstances() != 0) {
                    Instance result = skdtree.nearestNeighbour(target);
                    Instance result_kdtree = results_kdtree.get(index);

                    if (!isSameAttributes(result, result_kdtree)) {
                        System.out.println("Diferença encontrada na busca: " + count);
                        skdtree.m_Stats.printStats();
                        return;
                    }
                    index++;
                }

                skdtree.update(target);
            }

            System.out.println("OK - Compatível com KDTree!");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void validateExactSearch(ExampleStream<?> stream, KDTreeNodeSplitter splitter, RebuildPolicy rebuildPolicy) {
        try {
            restartStream(stream);

            int count = 0;
            int maxInstances = 500000;
            int window_size = 1000;

            StreamKDTree skdtree = new StreamKDTree();
            skdtree.setMaxInstInLeaf(40);
            skdtree.setNodeSplitter(splitter);
            skdtree.setWindowSize(window_size);
            skdtree.setRebuildPolicies(rebuildPolicy);

            System.out.println("Executando teste de insert + exactSearch...");

            while (stream.hasMoreInstances() && count < maxInstances) {
                Example<?> ex = stream.nextInstance();
                Instance target = (Instance) ex.getData();

                skdtree.update(target);

                if (skdtree.exactSearch(target) == -1) {
                    System.out.println("Erro: instância não encontrada após inserção!");
                    return;
                }

                skdtree.m_Stats.printMetrics();
                count++;
            }

            System.out.println("OK - exactSearch consistente!");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void validateMetrics(ExampleStream<?> stream, KDTreeNodeSplitter splitter, RebuildPolicy rebuildPolicy) {
        try {
            restartStream(stream);

            int count = 0;
            int maxInstances = 20;

            StreamKDTree skdtree = new StreamKDTree();
            skdtree.setMaxInstInLeaf(3);
            skdtree.setNodeSplitter(splitter);
            skdtree.setWindowSize(8);
            skdtree.setRebuildPolicies(rebuildPolicy);

            System.out.println("Executando teste de insert + exactSearch...");

            while (stream.hasMoreInstances() && count < maxInstances) {
                Example<?> ex = stream.nextInstance();
                Instance target = (Instance) ex.getData();

                if (skdtree.getInstances() != null && skdtree.getInstances().numInstances() > 0) {
                    skdtree.nearestNeighbour(target);
                }
                skdtree.update(target);

                skdtree.m_Stats.printStats();
                count++;
            }

            System.out.println("OK - exactSearch consistente!");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void expSlidingWindow(ExampleStream<?> stream, KDTreeNodeSplitter splitter, RebuildPolicy rebuildPolicy) {
        try {
            restartStream(stream);

            int count = 0;
            int maxInstances = 100000;

            StreamKDTree skdtree = new StreamKDTree();
            skdtree.setMaxInstInLeaf(40);
            skdtree.setNodeSplitter(splitter);
            skdtree.setWindowSize(1000);
            skdtree.setRebuildPolicies(rebuildPolicy);

            System.out.println("Executando exp sliding window...");

            while (stream.hasMoreInstances() && count < maxInstances) {
                Example<?> ex = stream.nextInstance();
                Instance target = (Instance) ex.getData();

                if (skdtree.getInstances() != null && skdtree.getInstances().numInstances() > 0) {
                    skdtree.nearestNeighbour(target);
                }
                skdtree.update(target);

                skdtree.m_Stats.printMetrics();
                skdtree.m_Stats.resetMetrics();
                count++;
            }

            System.out.printf("%-15s %-30s %-30s %-30s\n",
                    "Status", "Dataset", "Splitter", "RebuildPolicy");

            System.out.printf("%-15s %-30s %-30s %-30s\n",
                    "done",
                    stream.getHeader().getRelationName(),
                    splitter.getClass().getSimpleName(),
                    rebuildPolicy.getClass().getSimpleName()
            );

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

        KDTreeNodeSplitter splitter = new MidPointOfWidestDimension();
//        int splitterChosenIndex = splitterOption.getChosenIndex();
//        if (splitterChosenIndex == 0) {
//            System.out.println("Escolhido: Sliding Mid Point");
//            splitter = new SlidingMidPointOfWidestSide();
//        } else if (splitterChosenIndex == 1) {
//            System.out.println("Escolhido: Median Of Widest Dimension"); // Ainda não está funcionando
//            splitter = new MedianOfWidestDimension();
//        } else if (splitterChosenIndex == 2) {
//            System.out.println("Escolhido: Mid Point Of Widest Dimension"); // Não sei se funciona
//            splitter = new MidPointOfWidestDimension();
//        } else {
//            System.err.print("Nenhum splitter escolhido!");
//            return null;
//        }
        ///////////////////////////////////////////////////////

        // Politicas de rebuild da árvore
        RebuildPolicy instancesPerLeafPolicy = new InstancesPerLeafPolicy();
        RebuildPolicy noRebuildPolicy = new NoRebuild();
        RebuildPolicy deletedPolicy = new DeletedRatioPolicy();

        System.out.println(splitter.getClass().getName());
        System.out.println(stream.getHeader().getRelationName());
        System.out.println();

        monitor.setCurrentActivity("Testando Estrutura de Dados", -1.0);
        // validateExactSearch(stream, splitter, noRebuildPolicy);
        // validateAgainstKDTree(stream, splitter, instancesPerLeafPolicy);
        // validateMetrics(stream, splitter, noRebuildPolicy);
        expSlidingWindow(stream, splitter, deletedPolicy);
        return null;
    }

}

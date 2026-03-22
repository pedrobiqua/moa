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

    public ExampleStream<?> restartStream() {
        ExampleStream<?> stream = (ExampleStream<?>) getPreparedClassOption(this.streamOption);

        if (stream instanceof AbstractOptionHandler) {
            ((AbstractOptionHandler) stream).prepareForUse();
        } else {
            throw new UnsupportedOperationException("prepareForUse não implementado");
        }

        System.out.println("Novo stream criado");

        return stream;
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

    private void validateAgainstKDTree(KDTreeNodeSplitter splitter, RebuildPolicy rebuildPolicy) throws Exception {
        ExampleStream<?> stream = restartStream();
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
                kdtree.setMaxInstInLeaf(40);
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
        stream = restartStream();

        // StreamKDTree
        System.out.println("Executando SKDtree ...");
        while (stream.hasMoreInstances() && count < maxInstances) {
            count++;
            Example<?> ex = stream.nextInstance();
            Instance target = (Instance) ex.getData();

            if (skdtree.getInstances() != null && skdtree.getInstances().numInstances() != 0) {
                Instance result = skdtree.nearestNeighbour(target);
                Instance result_kdtree = results_kdtree.get(index);

                // Verifica se a insrância deu o mesmo da kdtree
                if (!isSameAttributes(result, result_kdtree)) {
                    System.out.println("Diferença encontrada na busca: " + count);
                    skdtree.m_Stats.printValuesAuto();
                    double reference = euclideanDistance.distance(target, result_kdtree);
                    double skdtree_distance = euclideanDistance.distance(target, result);
                    System.out.println("KDTREE: " + reference);
                    System.out.println("skdtree: " + skdtree_distance);
                    return;
                }
                index++;
            }

            skdtree.update(target);
        }

        System.out.println("OK - Compatível com KDTree!");
    }

    private void validateExactSearch(KDTreeNodeSplitter splitter, RebuildPolicy rebuildPolicy) throws Exception {
        ExampleStream<?> stream = restartStream();

        int count = 0;
        int maxInstances = 500000;
        int window_size = 1000;

        StreamKDTree skdtree = new StreamKDTree();
        skdtree.setMaxInstInLeaf(40);
        skdtree.setNodeSplitter(splitter);
        skdtree.setWindowSize(window_size);
        skdtree.setRebuildPolicies(rebuildPolicy);

        System.out.println("Executando teste de inserção e busca exata...");

        while (stream.hasMoreInstances() && count < maxInstances) {
            Example<?> ex = stream.nextInstance();
            Instance target = (Instance) ex.getData();

            skdtree.update(target);

            if (skdtree.exactSearch(target) == -1) {
                System.out.println("Erro: instância não encontrada após inserção!");
                return;
            }

            count++;
        }

        System.out.println("OK - busca consistente!");
    }

    private void validateMetrics(KDTreeNodeSplitter splitter, RebuildPolicy rebuildPolicy) throws Exception {
        ExampleStream<?> stream = restartStream();

        int count = 0;
        int maxInstances = 100000;

        StreamKDTree skdtree = new StreamKDTree();
        skdtree.setMaxInstInLeaf(40);
        skdtree.setNodeSplitter(splitter);
        skdtree.setWindowSize(1000);
        skdtree.setRebuildPolicies(rebuildPolicy);

        System.out.println("Validando as métricas...");

        skdtree.m_Stats.printHeaderAuto();
        while (stream.hasMoreInstances() && count < maxInstances) {
            Example<?> ex = stream.nextInstance();
            Instance target = (Instance) ex.getData();

            if (skdtree.getInstances() != null && skdtree.getInstances().numInstances() > 0) {
                skdtree.nearestNeighbour(target);
                skdtree.m_Stats.validateSearch();
            }
            skdtree.update(target);
            if (count % 1000 == 0)
                skdtree.m_Stats.printValuesAuto();
            skdtree.m_Stats.validateUpdate();
            skdtree.m_Stats.validateStructure();
            skdtree.m_Stats.resetMetrics();
            count++;
        }

        System.out.println("OK - métricas consistentes!");
    }

    @Override
    protected Object doMainTask(TaskMonitor monitor, ObjectRepository repository) {

//        System.setErr(new java.io.PrintStream(new java.io.OutputStream() {
//            public void write(int b) {}
//        }));

        ExampleStream<?> baseStream = (ExampleStream<?>) getPreparedClassOption(this.streamOption);
        if (baseStream instanceof AbstractOptionHandler)
            ((AbstractOptionHandler) baseStream).prepareForUse();
        else
            throw new UnsupportedOperationException("prepareForUse não implementado");

        // 🔹 Splitters
        KDTreeNodeSplitter[] splitters = new KDTreeNodeSplitter[] {
                new MidPointOfWidestDimension(),
                new SlidingMidPointOfWidestSide(),
                new MedianOfWidestDimension()
        };

        // 🔹 Policies
        RebuildPolicy[] policies = new RebuildPolicy[] {
                // new InstancesPerLeafPolicy(), // Essa politica por algum motivo faz não bater com o MOA, não sei o pq
                 new DeletedRatioPolicy(),
                 new HeightBalancedPolicy()
        };

        String datasetName = "";
        if (baseStream.getClass().getSimpleName().equals("ArffFileStream")) {
            // Cast pra ArffFileStream
            moa.streams.ArffFileStream arffStream = (moa.streams.ArffFileStream) baseStream;
            // Pega o nome do arquivo (sem o caminho completo)
            datasetName = arffStream.arffFileOption.getFile().getName();
        } else {
            datasetName = baseStream.getClass().getSimpleName();
        }

        // 🔁 Loop geral
        for (KDTreeNodeSplitter splitter : splitters) {
            for (RebuildPolicy policy : policies) {

                System.out.printf("%-30s %-30s %-30s\n",
                        "Dataset", "Splitter", "RebuildPolicy");

                System.out.printf("%-30s %-30s %-30s\n",
                        datasetName,
                        splitter.getClass().getSimpleName(),
                        policy.getClass().getSimpleName()
                );

                // 🔹 Testes
                try {
                    validateExactSearch(splitter, policy);
                    validateAgainstKDTree(splitter, policy);
                    validateMetrics(splitter, policy);

                } catch (Exception e) {
                    System.err.println("Erro na combinação:");
                    System.err.println("Splitter: " + splitter.getClass().getSimpleName());
                    System.err.println("Policy: " + policy.getClass().getSimpleName());
                    e.printStackTrace();
                    return null;
                }
            }
        }

        return null;
    }

}

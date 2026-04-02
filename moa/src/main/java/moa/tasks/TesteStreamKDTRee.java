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

import com.yahoo.labs.samoa.instances.Instance;
import com.yahoo.labs.samoa.instances.Instances;

import java.util.ArrayList;

public class TesteStreamKDTRee extends MainTask {
    public ClassOption streamOption = new ClassOption("stream", 's',
            "Stream to evaluate on.", ExampleStream.class,
            "generators.RandomTreeGenerator");

    public int maxInstances_;

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
        int maxInstances = maxInstances_;
        int window_size = 1000;

        Instances window = new Instances(stream.getHeader(), 0);
        ArrayList<Instance> results_kdtree = new ArrayList<>();
        EuclideanDistance euclideanDistance = new EuclideanDistance();
        euclideanDistance.setDontNormalize(true);

        double ERROR_THRESHOLD = 0.05;

        int exactMatches = 0;
        int acceptableErrors = 0;
        int badErrors = 0;
        double totalError = 0.0;
        double maxError = 0.0;
        int comparisons = 0;

        // Configurações da skdtree
        StreamKDTree skdtree = new StreamKDTree();
        skdtree.setMaxInstInLeaf(40);
        skdtree.setNodeSplitter(splitter);
        skdtree.setWindowSize(window_size);
        skdtree.setRebuildPolicies(rebuildPolicy);

        // KDTree (baseline)
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

                double reference = euclideanDistance.distance(target, result_kdtree);
                double skdtree_distance = euclideanDistance.distance(target, result);

                // Evita divisão por zero
                double error = (reference == 0.0) ? 0.0 :
                        Math.abs(skdtree_distance - reference) / reference;

                totalError += error;
                maxError = Math.max(maxError, error);
                comparisons++;

                if (isSameAttributes(result, result_kdtree)) {
                    exactMatches++;
                } else if (error <= ERROR_THRESHOLD) {
                    acceptableErrors++;
                } else {
                    badErrors++;

                    System.out.println("Diferença relevante encontrada na busca: " + count);
                    skdtree.m_Stats.printValuesAuto();
                    System.out.println("KDTREE: " + reference);
                    System.out.println("SKDTREE: " + skdtree_distance);
                    System.out.println("Erro relativo: " + error);
                }

                index++;
            }

            skdtree.update(target);
        }

        // ===== Resultado final =====
        System.out.println("\n===== RESULTADOS =====");
        System.out.println("Comparações: " + comparisons);
        System.out.println("Match exato: " + exactMatches);
        System.out.println("Erro aceitável: " + acceptableErrors);
        System.out.println("Erro ruim: " + badErrors);
        System.out.println("Erro médio: " + (totalError / comparisons));
        System.out.println("Erro máximo: " + maxError);

        System.out.println("Taxa match exato: " + (exactMatches * 100.0 / comparisons) + "%");
        System.out.println("Taxa aceitável: " + (acceptableErrors * 100.0 / comparisons) + "%");
        System.out.println("Taxa erro ruim: " + (badErrors * 100.0 / comparisons) + "%");
    }

    private void validateExactSearch(KDTreeNodeSplitter splitter, RebuildPolicy rebuildPolicy) throws Exception {
        ExampleStream<?> stream = restartStream();

        int count = 0;
        int window_size = 1000;
        int maxInstances = maxInstances_;

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
            int num_nodes = skdtree.countNumNodes();
            if (skdtree.m_Stats.m_NumNodes != num_nodes)
                throw new Exception("Erro!, os números de nós não batem");
            skdtree.m_Stats.resetMetrics();

            if (skdtree.exactSearch(target) == -1) {
                System.out.println("Erro: instância não encontrada após inserção!");
                System.out.println(count);
                System.out.println(target);
                throw new Exception();
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

        KDTreeNodeSplitter[] splitters = new KDTreeNodeSplitter[] {
                // new MidPointOfWidestDimension(),
                new StreamSlidingMidPointOfWidestSide(),
                // new StreamMedianOfWidestDimension()
                // new VarianceMidPoint()
                // new VarianceMedianPoint()
                // new AxisMidPoint()
        };

        double alpha = 0.6;

        RebuildPolicy[] policies = new RebuildPolicy[] {
                 // new InstancesPerLeafPolicy(), // Essa politica por algum motivo faz não bater com o MOA, não sei o pq
                 // new DeletedRatioPolicy(),
                 new HeightBalancedPolicy(alpha)
                 // new NoRebuild()
        };

        String datasetName = "";
        if (baseStream.getClass().getSimpleName().equals("ArffFileStream")) {
            // Cast pra ArffFileStream
            moa.streams.ArffFileStream arffStream = (moa.streams.ArffFileStream) baseStream;
            // Pega o nome do arquivo (sem o caminho completo)
            datasetName = arffStream.arffFileOption.getFile().getName();
            maxInstances_ = Integer.MAX_VALUE;
        } else {
            maxInstances_ = 100000;
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

                try {
                    validateExactSearch(splitter, policy);
                    validateAgainstKDTree(splitter, policy);
//                    validateMetrics(splitter, policy);

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

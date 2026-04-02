package moa.tasks;

import com.github.javacliparser.FloatOption;
import com.yahoo.labs.samoa.instances.Instance;
import moa.classifiers.lazy.neighboursearch.kdtrees.*;
import moa.core.Example;
import moa.core.ObjectRepository;
import moa.options.AbstractOptionHandler;
import moa.options.ClassOption;
import moa.streams.ExampleStream;

public class TesteNSKDtree extends MainTask {
    public ClassOption streamOption = new ClassOption("stream", 's',
            "Stream to evaluate on.", ExampleStream.class,
            "generators.RandomTreeGenerator");

    public FloatOption alphaOption = new FloatOption("alpha", 'a', "alpha value.", 0.6, 0.5, 1.0);

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

    private void validateExactSearch(RebuildPolicy rebuildPolicy) throws Exception {
        ExampleStream<?> stream = restartStream();

        int count = 0;
        int window_size = 5;
        int maxInstances = maxInstances_;

        maxInstances = 15;

        NSKDtree skdtree = new NSKDtree();
        skdtree.setWindowSize(window_size);
        skdtree.setRebuildPolicies(rebuildPolicy);
        skdtree.setInstances(stream.getHeader()); // Cria instances vazio

        System.out.println("Executando teste de inserção e busca exata...");

        // Instances allInstances = new Instances(stream.getHeader(), 0);

        while (stream.hasMoreInstances() && count < maxInstances) {
            Example<?> ex = stream.nextInstance();
            Instance target = (Instance) ex.getData();

            // allInstances.add(target);

             skdtree.update(target);
             skdtree.printTree();

            if (skdtree.exactSearch(target) == -1) {
                System.out.println("Erro: instância não encontrada após inserção!");
                System.out.println(count);
                System.out.println(target);
                throw new Exception();
            }

            count++;
        }

        // skdtree.buildTree(allInstances);
        // skdtree.printTree();


        System.out.println("OK - busca consistente!");
    }

    private void validateKNN(RebuildPolicy rebuildPolicy) throws Exception {
        ExampleStream<?> stream = restartStream();

        int count = 0;
        int window_size = 20;
        int maxInstances = maxInstances_;

        /// / APENAS TESTE
        // maxInstances = 15;

        NSKDtree skdtree = new NSKDtree();
        skdtree.setWindowSize(window_size);
        skdtree.setRebuildPolicies(rebuildPolicy);
        skdtree.setInstances(stream.getHeader()); // Cria instances vazio

        System.out.println("Executando teste do calculo do vizinho mais proximo...");

        while (stream.hasMoreInstances() && count < maxInstances) {
            Example<?> ex = stream.nextInstance();
            Instance target = (Instance) ex.getData();

            if (skdtree.getInstances() != null && skdtree.getInstances().size() != 0){
                Instance result = skdtree.nearestNeighbour(target);
                skdtree.printPoints();
                System.out.println(target.value(0) + "," + target.value(1) + "," + -1 + "," + "fora_arvore");
                System.out.println(result.value(0) + "," + result.value(1) + "," + -1 + "," + "result_knn\n");
            }

            skdtree.update(target);

            count++;
        }

        System.out.println("Quantidade de rebuilds: " + skdtree.stats.countRebuild);

        // skdtree.buildTree(allInstances);
        // skdtree.printTree();


        System.out.println("OK - kNN retorna resultados!");
    }

    @Override
    protected Object doMainTask(TaskMonitor monitor, ObjectRepository repository) {

        ExampleStream<?> baseStream = (ExampleStream<?>) getPreparedClassOption(this.streamOption);
        if (baseStream instanceof AbstractOptionHandler)
            ((AbstractOptionHandler) baseStream).prepareForUse();
        else
            throw new UnsupportedOperationException("prepareForUse não implementado");

        double alpha = alphaOption.getValue();

        RebuildPolicy[] policies = new RebuildPolicy[] {
                // new InstancesPerLeafPolicy(), // Essa politica por algum motivo faz não bater com o MOA, não sei o pq
                new DeletedRatioPolicy(),
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
        for (RebuildPolicy policy : policies) {

            if (policy instanceof HeightBalancedPolicy) {
                System.out.printf("%-30s %-30s %-30s\n",
                        "Dataset", "RebuildPolicy", "Alpha");

                System.out.printf("%-30s %-30s %-30f\n",
                        datasetName,
                        policy.getClass().getSimpleName(),
                        alpha
                );
            } else {
                System.out.printf("%-30s %-30s\n",
                        "Dataset", "RebuildPolicy");

                System.out.printf("%-30s %-30s\n",
                        datasetName,
                        policy.getClass().getSimpleName()
                );
            }

            try {
                // validateExactSearch(policy);
                validateKNN(policy);

            } catch (Exception e) {
                System.err.println("Erro na combinação:");
                System.err.println("Policy: " + policy.getClass().getSimpleName());
                e.printStackTrace();
                return null;
            }
        }
        return null;
    }

}
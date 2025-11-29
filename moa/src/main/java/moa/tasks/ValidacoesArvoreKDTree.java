package moa.tasks;

import com.yahoo.labs.samoa.instances.Instance;
import com.yahoo.labs.samoa.instances.Instances;

import moa.classifiers.lazy.neighboursearch.EuclideanDistance;
import moa.classifiers.lazy.neighboursearch.KDTree;
import moa.classifiers.lazy.neighboursearch.KDTreeSimple;
import moa.classifiers.lazy.neighboursearch.LinearNNSearch;
import moa.core.Example;
import moa.core.ObjectRepository;
import moa.options.ClassOption;
import moa.streams.ExampleStream;

public class ValidacoesArvoreKDTree extends MainTask {

    //////////////////// PARAMETROS DO TESTE
    public ClassOption streamOption = new ClassOption("stream", 's',
            "Stream to evaluate on.", ExampleStream.class,
            "generators.RandomTreeGenerator");

    @Override
    public Class<?> getTaskResultType() {
        throw new UnsupportedOperationException("Unimplemented method 'getTaskResultType'");
    }

    public double distancia_euclidiana(Instance n1, Instance n2) {
        double total = 0;
        int numDim = n1.numAttributes() - 1;

        double[] p0 = n1.toDoubleArray();
        double[] p1 = n2.toDoubleArray();

        for (int i = 0; i < numDim; i++) {
            double diff = Math.abs(p0[i] - p1[i]);
            total += Math.pow(diff, 2);
        }
        return total;
    }

    public static boolean sameInstance(Instance a, Instance b) {
        if (a.numAttributes() != b.numAttributes())
            return false;

        for (int i = 0; i < a.numAttributes(); i++) {
            if (a.value(i) != b.value(i)) {
                return false;
            }
        }

        if (a.classValue() != b.classValue())
            return false;

        return true;
    }

    @Override
    protected Object doMainTask(TaskMonitor monitor, ObjectRepository repository) {
        ExampleStream<?> stream = (ExampleStream<?>) getPreparedClassOption(this.streamOption);
        KDTreeSimple kdtree = null;
        monitor.setCurrentActivityDescription("TESTANDO INSERÇÃO E BUSCA DO NÓ INSERIDO");

        // Instances window = new Instances(stream.getHeader(), 0);

        // while (stream.hasMoreInstances()) {
        // try {
        // Example<?> ex = stream.nextInstance();
        // Instance inst = (Instance) ex.getData();

        // if (window.numInstances() >= 10) {
        // KDTree search = new KDTree();
        // System.out.println(search.globalInfo());
        // search.setNormalizeNodeWidth(false);
        // search.setInstances(window);
        // } else {
        // window.add(inst);
        // }
        // // search.update(inst);

        // } catch (Exception e) {
        // // TODO: handle exception
        // }
        // }

        // while (stream.hasMoreInstances()) {
        // try {
        // Example<?> ex = stream.nextInstance();
        // Instance inst = (Instance) ex.getData();

        // if (kdtree == null) {
        // kdtree = new KDTreeSimple(inst.numAttributes() - 1);
        // }

        // // INSERIR
        // kdtree.update(inst);

        // // BUSCAR O NÓ INSERIDO
        // Instance instancia_encontrada = kdtree.findInstanceInTree(inst);

        // // VERIFICA SE FOI INSERIDO CORRETAMENTE
        // if (instancia_encontrada == null) {
        // System.out.println("INSTÂNCIA NÃO FOI ENCONTRADA!");
        // }
        // if (instancia_encontrada != inst) {
        // System.out.println("INSTÂNCIA ERRADA!");
        // }

        // } catch (Exception e) {
        // e.printStackTrace();
        // }
        // }

        stream.restart();
        kdtree = null;
        monitor.setCurrentActivityDescription("TESTANDO BUSCA DO VIZINHO MAIS PRÓXIMO COMPARANDO COM O MOA");
        Instances window = new Instances(stream.getHeader(), 0); // AQUI VOU SEMPRE
        // ADICIONAR, POR CONTA DO MOA

        int instanciasProcessadas = 0;
        while (stream.hasMoreInstances()) {
            Example<?> ex = stream.nextInstance();
            Instance inst = (Instance) ex.getData();

            ///////////// MOA
            // BUSCANDO
            Instance inst_kdtree_moa = null;
            Instance inst_truth_moa = null;
            try {
                if (window.numInstances() > 0) {
                    EuclideanDistance dist_fn = new EuclideanDistance();
                    dist_fn.setDontNormalize(true);

                    KDTree kdtree_moa = new KDTree(); // CRIA A ÁRVORE
                    kdtree_moa.setDistanceFunction(dist_fn);
                    kdtree_moa.setNormalizeNodeWidth(false);
                    kdtree_moa.setInstances(window);
                    inst_kdtree_moa = kdtree_moa.nearestNeighbour(inst); // BUSCA O VIZINHO

                    LinearNNSearch linear_moa = new LinearNNSearch();
                    linear_moa.setDistanceFunction(dist_fn);
                    linear_moa.setInstances(window);
                    inst_truth_moa = linear_moa.nearestNeighbour(inst);

                }

                // INSERINDO
                window.add(inst);
            } catch (Exception e) {
                e.printStackTrace();
            }

            ///////////// PEDRO
            if (kdtree == null) {
                kdtree = new KDTreeSimple(inst.numAttributes() - 1);
            }
            Instance inst_kdtree_pedro = null;
            try {
                // BUSCAR
                if (kdtree.getNumNodes() > 0) {
                    inst_kdtree_pedro = kdtree.nearestNeighbour(inst);
                }

                // INSERIR
                kdtree.update(inst);
            } catch (Exception e) {
                e.printStackTrace();
            }

            instanciasProcessadas++;

            if (inst_kdtree_moa == null && inst_kdtree_pedro == null) {
                System.out.println("AMBAS NÃO DERAM RESULTADOS!");
            } else if (!sameInstance(inst_kdtree_moa, inst_kdtree_pedro)) {
                System.out.println(instanciasProcessadas);
                System.out.println("INSTANCIA CORRETA: " + inst_truth_moa);
                System.out.println(
                        "NÃO É A MESMA INSTANCIA: \n pedro: " + inst_kdtree_pedro + "\n moa: " +
                                inst_kdtree_moa + "\nDistância Pedro: " + distancia_euclidiana(inst,
                                        inst_kdtree_pedro)
                                + "\nDistância MOA: " + distancia_euclidiana(inst, inst_kdtree_moa));
            }
        }

        return null;
    }

}

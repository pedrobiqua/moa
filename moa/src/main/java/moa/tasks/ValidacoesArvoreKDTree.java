package moa.tasks;

import com.yahoo.labs.samoa.instances.Instance;
import com.yahoo.labs.samoa.instances.Instances;

import moa.classifiers.lazy.neighboursearch.EuclideanDistance;
// import moa.classifiers.lazy.neighboursearch.KDTree;
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

    public static double distancia_euclidiana(EuclideanDistance dist_fn, Instance n1, Instance n2) {
        return dist_fn.distance(n1, n2);
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

    public static boolean sameDistance(Instance a, Instance b, Instance target, EuclideanDistance dist_fn) {
        double distance_a = distancia_euclidiana(dist_fn, target, a);
        double distance_b = distancia_euclidiana(dist_fn, target, b);
        return distance_a == distance_b;
    }

    @Override
    protected Object doMainTask(TaskMonitor monitor, ObjectRepository repository) {
        ExampleStream<?> stream = (ExampleStream<?>) getPreparedClassOption(this.streamOption);
        KDTreeSimple kdtree = null;
        monitor.setCurrentActivityDescription("TESTANDO INSERÇÃO E BUSCA DO NÓ INSERIDO");

        ///////////// FUNÇÃO DE DISTÂNCIA UTILIZADA (SEM NORMALIZAÇÃO!)
        EuclideanDistance dist_fn = new EuclideanDistance();
        dist_fn.setDontNormalize(true);

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

        // stream.restart();
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
            // Instance inst_kdtree_moa = null;
            Instance inst_truth_moa = null;
            try {
                if (window.numInstances() > 0) {
                    // KDTree kdtree_moa = new KDTree(); // CRIA A ÁRVORE
                    // kdtree_moa.setDistanceFunction(dist_fn);
                    // kdtree_moa.setNormalizeNodeWidth(false);
                    // kdtree_moa.setInstances(window);
                    // inst_kdtree_moa = kdtree_moa.nearestNeighbour(inst); // BUSCA O VIZINHO

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

            if (inst_truth_moa == null && inst_kdtree_pedro == null) {
                System.out.println("AMBAS NÃO DERAM RESULTADOS!");
            } else if (!sameDistance(inst_kdtree_pedro, inst_truth_moa, inst, dist_fn)) {
                System.out.println("NÃO SÃO A MESMA DISTÂNCIA | INSTANCIA: " + instanciasProcessadas);
                System.out.println("DISTANCIAS: ");
                System.out.println("MOA: " + distancia_euclidiana(dist_fn, inst, inst_truth_moa));
                System.out.println("PED: " + distancia_euclidiana(dist_fn, inst, inst_kdtree_pedro));
            } else if (!sameInstance(inst_truth_moa, inst_kdtree_pedro)) {
                System.out.println("NÃO SÃO A MESMA INSTÂNCIA | INSTANCIA: " + instanciasProcessadas);
                System.out.println("DISTANCIAS: ");
                System.out.println("MOA: " + distancia_euclidiana(dist_fn, inst, inst_truth_moa));
                System.out.println("PED: " + distancia_euclidiana(dist_fn, inst, inst_kdtree_pedro));
            }

            ////////////////// QUANTIDADE DE INSTÂNCIAS PROCESSADAS
            if (instanciasProcessadas % 10000 == 0) {
                System.out.println("INSTANCIAS PROCESSADAS" + instanciasProcessadas + " \n");
            }
        }

        return null;
    }

}

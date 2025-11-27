package moa.tasks;

import com.yahoo.labs.samoa.instances.Instance;
import com.yahoo.labs.samoa.instances.Instances;

import moa.classifiers.lazy.neighboursearch.KDTree;
import moa.classifiers.lazy.neighboursearch.KDTreeSimple;
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

        while (stream.hasMoreInstances()) {
            try {
                Example<?> ex = stream.nextInstance();
                Instance inst = (Instance) ex.getData();

                if (kdtree == null) {
                    kdtree = new KDTreeSimple(inst.numAttributes() - 1);
                }

                // INSERIR
                kdtree.update(inst);

                // BUSCAR O NÓ INSERIDO
                Instance instancia_encontrada = kdtree.findInstanceInTree(inst);

                // VERIFICA SE FOI INSERIDO CORRETAMENTE
                if (instancia_encontrada == null) {
                    System.out.println("INSTÂNCIA NÃO FOI ENCONTRADA!");
                }
                if (instancia_encontrada != inst) {
                    System.out.println("INSTÂNCIA ERRADA!");
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        stream.restart();
        kdtree = null;
        monitor.setCurrentActivityDescription("TESTANDO BUSCA DO VIZINHO MAIS PRÓXIMO COMPARANDO COM O MOA");
        Instances window = new Instances(stream.getHeader(), 0); // AQUI VOU SEMPRE ADICIONAR, POR CONTA DO MOA

        while (stream.hasMoreInstances()) {
            Example<?> ex = stream.nextInstance();
            Instance inst = (Instance) ex.getData();

            ///////////// MOA
            // BUSCANDO
            Instance inst_kdtree_moa = null;
            try {
                if (window.numInstances() > 0) {
                    KDTree kdtree_moa = new KDTree(); // CRIA A ÁRVORE
                    kdtree_moa.setInstances(window);
                    inst_kdtree_moa = kdtree_moa.nearestNeighbour(inst); // BUSCA O VIZINHO
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

            if (inst_kdtree_moa == null && inst_kdtree_pedro == null) {
                System.out.println("AMBAS NÃO DERAM RESULTADOS!");
            } else if (!sameInstance(inst_kdtree_moa, inst_kdtree_pedro)) {
                System.out.println(
                        "NÃO É A MESMA INSTANCIA: \n pedro: " + inst_kdtree_pedro + "\n moa: " + inst_kdtree_moa);
            }
        }

        return null;
    }

}

package moa.tasks;

import com.yahoo.labs.samoa.instances.Instance;
import com.yahoo.labs.samoa.instances.Instances;

import moa.classifiers.lazy.neighboursearch.EuclideanDistance;
import moa.classifiers.lazy.neighboursearch.KDTree;
// import moa.classifiers.lazy.neighboursearch.KDTree;
import moa.classifiers.lazy.neighboursearch.KDTreeSimple;
import moa.classifiers.lazy.neighboursearch.LinearNNSearch;
import moa.core.Example;
import moa.core.ObjectRepository;
import moa.core.TimingUtils;
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

        monitor.setCurrentActivityDescription("TESTANDO BUSCA DO VIZINHO MAIS PRÓXIMO COMPARANDO COM O MOA");

        ExampleStream<?> stream = (ExampleStream<?>) getPreparedClassOption(this.streamOption);

        // ---------------------------------------------------------
        // CONFIGURAÇÕES GERAIS
        // ---------------------------------------------------------
        EuclideanDistance distFn = new EuclideanDistance();
        distFn.setDontNormalize(true);

        KDTreeSimple kdtreePedro = null;
        KDTreeSimple kdtreePedroBalanceada = null;

        Instances windowMOA = new Instances(stream.getHeader(), 0);
        Instances instsBalanceada = new Instances(stream.getHeader(), 0);

        int instanciasProcessadas = 0;

        // ---------------------------------------------------------
        // LOOP PRINCIPAL
        // ---------------------------------------------------------
        while (stream.hasMoreInstances() && instanciasProcessadas <= 100000) {

            long start, end;
            double timePedro = 0.0;
            double timeMOA = 0.0;
            double timeMOAKDTree = 0.0;
            double timeBalanceada = 0.0;

            Example<?> ex = stream.nextInstance();
            Instance inst = (Instance) ex.getData();

            // =====================================================================
            // 1. MOA
            // =====================================================================
            Instance moaNN = null;
            Instance moaTruth = null;
            try {
                if (windowMOA.numInstances() > 0) {

                    // KDTree MOA
                    KDTree kdtreeMOA = new KDTree();
                    kdtreeMOA.setDistanceFunction(distFn);
                    kdtreeMOA.setNormalizeNodeWidth(false);
                    kdtreeMOA.setMaxInstInLeaf(1);
                    kdtreeMOA.setInstances(windowMOA);

                    start = TimingUtils.getNanoCPUTimeOfCurrentThread();
                    moaNN = kdtreeMOA.nearestNeighbour(inst);
                    end = TimingUtils.getNanoCPUTimeOfCurrentThread();

                    timeMOAKDTree = TimingUtils.nanoTimeToSeconds(end - start);

                    // Linear MOA (ground truth)
                    LinearNNSearch linearMOA = new LinearNNSearch();
                    linearMOA.setDistanceFunction(distFn);
                    linearMOA.setInstances(windowMOA);

                    start = TimingUtils.getNanoCPUTimeOfCurrentThread();
                    moaTruth = linearMOA.nearestNeighbour(inst);
                    end = TimingUtils.getNanoCPUTimeOfCurrentThread();

                    timeMOA = TimingUtils.nanoTimeToSeconds(end - start);
                }

                // INSERE NO WINDOW (após a busca!)
                windowMOA.add(inst);

            } catch (Exception e) {
                e.printStackTrace();
            }

            // =====================================================================
            // 2. KDTREE PEDRO (incremental)
            // =====================================================================
            Instance pedroNN = null;
            try {

                if (kdtreePedro == null)
                    kdtreePedro = new KDTreeSimple(inst.numAttributes() - 1);

                if (kdtreePedro.getNumNodes() > 0) {
                    kdtreePedro.backtrackCount = 0;
                    start = TimingUtils.getNanoCPUTimeOfCurrentThread();
                    pedroNN = kdtreePedro.nearestNeighbour(inst);
                    end = TimingUtils.getNanoCPUTimeOfCurrentThread();

                    timePedro = TimingUtils.nanoTimeToSeconds(end - start);
                }

                kdtreePedro.update(inst);

            } catch (Exception e) {
                e.printStackTrace();
            }

            // =====================================================================
            // 3. KDTREE PEDRO BALANCEADA (sempre rebuild)
            // =====================================================================
            Instance pedroBalanceadaNN = null;
            try {

                if (instsBalanceada.numInstances() > 0) {
                    kdtreePedroBalanceada = new KDTreeSimple(inst.numAttributes() - 1);
                    kdtreePedroBalanceada.buildTree(instsBalanceada);

                    start = TimingUtils.getNanoCPUTimeOfCurrentThread();
                    kdtreePedroBalanceada.backtrackCount = 0;
                    pedroBalanceadaNN = kdtreePedroBalanceada.nearestNeighbour(inst);
                    end = TimingUtils.getNanoCPUTimeOfCurrentThread();

                    timeBalanceada = TimingUtils.nanoTimeToSeconds(end - start);
                }

                instsBalanceada.add(inst);

            } catch (Exception e) {
                e.printStackTrace();
            }

            instanciasProcessadas++;

            // =====================================================================
            // 4. VALIDAÇÃO DO RESULTADO DO MEU KDTREE
            // =====================================================================
            if (moaTruth == null && pedroNN == null) {
                System.out.println("AMBAS NÃO DERAM RESULTADOS!");
            } else if (!sameDistance(pedroNN, moaTruth, inst, distFn)) {
                System.out.println("NÃO SÃO A MESMA DISTÂNCIA | INST: " + instanciasProcessadas);
                System.out.println("MOA: " + distancia_euclidiana(distFn, inst, moaTruth));
                System.out.println("PED: " + distancia_euclidiana(distFn, inst, pedroNN));
                return null;
            } else if (!sameInstance(moaTruth, pedroNN) || !sameInstance(moaTruth, pedroBalanceadaNN)) {
                System.out.println("NÃO SÃO A MESMA INSTÂNCIA | INST: " + instanciasProcessadas);
                System.out.println("MOA: " + distancia_euclidiana(distFn, inst, moaTruth));
                System.out.println("PED: " + distancia_euclidiana(distFn, inst, pedroNN));
                System.out.println("BAL: " + distancia_euclidiana(distFn, inst, pedroBalanceadaNN));
            }

            // =====================================================================
            // 5. CRITÉRIO DE PARADA (VERIFICAÇÃO DOS TEMPOS DE CADA ESTRUTURA)
            // =====================================================================
            // if (instanciasProcessadas == 10025) {
            // System.out.println("INSTANCIAS PROCESSADAS: " + instanciasProcessadas);
            // System.out.println("BACKTRACK PEDRO: " + kdtreePedro.backtrackCount);
            // System.out.println("TEMPO PEDRO: " + timePedro);
            // System.out.println("TEMPO BALANCEADA: " + timeBalanceada);
            // System.out.println("BACKTRACK BALANCEADA: " +
            // kdtreePedroBalanceada.backtrackCount);
            // System.out.println("TEMPO MOA: " + timeMOA);
            // System.out.println("TEMPO MOA KDTREE: " + timeMOAKDTree);
            // return null;
            // }

            // =====================================================================
            // 6. LOG PERIÓDICO
            // =====================================================================
            if (instanciasProcessadas % 10000 == 0) {
                System.out.println("INSTANCIAS PROCESSADAS " + instanciasProcessadas);
                System.out.println("TEMPO MOA: " + timeMOA);
                System.out.println("TEMPO PEDRO: " + timePedro);
                System.out.println("TEMPO BALANCEADA: " + timeBalanceada);
            }
        }

        return null;
    }

}

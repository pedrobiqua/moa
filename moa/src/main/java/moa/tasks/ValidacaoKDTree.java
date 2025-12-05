package moa.tasks;

import com.yahoo.labs.samoa.instances.Instance;
import com.yahoo.labs.samoa.instances.Instances;

import moa.classifiers.lazy.neighboursearch.EuclideanDistance;
import moa.classifiers.lazy.neighboursearch.KDTree;
import moa.classifiers.lazy.neighboursearch.KDTreeSimple;
import moa.classifiers.lazy.neighboursearch.LinearNNSearch;
import moa.core.Example;
import moa.core.ObjectRepository;
import moa.core.TimingUtils;
import moa.options.ClassOption;
import moa.streams.ExampleStream;

public class ValidacaoKDTree extends MainTask {

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

    public static boolean compareInstances(Instances a, Instances b) {

        // Verifica quantidade de instâncias
        if (a.numInstances() != b.numInstances())
            return false;

        // Verifica quantidade de atributos
        if (a.numAttributes() != b.numAttributes())
            return false;

        int n = a.numInstances();
        int m = a.numAttributes();

        for (int i = 0; i < n; i++) {
            Instance ia = a.instance(i);
            Instance ib = b.instance(i);

            // Compara atributo por atributo
            for (int j = 0; j < m; j++) {
                double va = ia.value(j);
                double vb = ib.value(j);

                // Trata NaN corretamente (NaN != NaN)
                if (Double.isNaN(va) && Double.isNaN(vb))
                    continue;

                if (va != vb)
                    return false;
            }
        }

        return true;
    }

    @Override
    protected Object doMainTask(TaskMonitor monitor, ObjectRepository repository) {
        monitor.setCurrentActivityDescription("Teste");

        ExampleStream<?> stream = (ExampleStream<?>) getPreparedClassOption(this.streamOption);

        // ---------------------------------------------------------
        // CONFIGURAÇÕES GERAIS
        // ---------------------------------------------------------
        EuclideanDistance distFn = new EuclideanDistance();

        KDTreeSimple kdtreePedro = null;
        Instances windowMOA = new Instances(stream.getHeader(), 0);

        int instanciasProcessadas = 0;

        // ---------------------------------------------------------
        // LOOP PRINCIPAL
        // ---------------------------------------------------------
        while (stream.hasMoreInstances() && instanciasProcessadas <= 100000) {

            long start, end;
            double timePedro = 0.0;
            double timeMOA = 0.0;
            double timeMOAKDTree = 0.0;
            // double timeBalanceada = 0.0;

            Example<?> ex = stream.nextInstance();
            Instance inst = (Instance) ex.getData();

            // =====================================================================
            // 1. MOA
            // =====================================================================
            Instances moaNN = null;
            Instances moaTruth = null;

            KDTree kdtreeMOA = new KDTree();
            try {
                if (windowMOA.numInstances() > 0) {
                    // KD Tree do MOA
                    kdtreeMOA.setDistanceFunction(distFn);
                    kdtreeMOA.setNormalizeNodeWidth(false);
                    kdtreeMOA.setMaxInstInLeaf(1); // para ter uma comparação justa
                    kdtreeMOA.setInstances(windowMOA);

                    start = TimingUtils.getNanoCPUTimeOfCurrentThread();
                    moaNN = kdtreeMOA.kNearestNeighbours(inst, 3);
                    end = TimingUtils.getNanoCPUTimeOfCurrentThread();

                    timeMOAKDTree = TimingUtils.nanoTimeToSeconds(end - start);

                    // Linear MOA (ground truth)
                    LinearNNSearch linearMOA = new LinearNNSearch();
                    linearMOA.setDistanceFunction(distFn);
                    linearMOA.setInstances(windowMOA);

                    start = TimingUtils.getNanoCPUTimeOfCurrentThread();
                    moaTruth = linearMOA.kNearestNeighbours(inst, 3);
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
            Instances pedroNN = null;
            try {

                if (kdtreePedro == null) {
                    kdtreePedro = new KDTreeSimple(inst.numAttributes() - 1);
                    kdtreePedro.setInstances(new Instances(inst.dataset(), 0));
                    kdtreePedro.setDistanceFunction(distFn);
                }

                if (kdtreePedro.getNumNodes() > 0) {
                    kdtreePedro.backtrackCount = 0;
                    start = TimingUtils.getNanoCPUTimeOfCurrentThread();
                    pedroNN = kdtreePedro.kNearestNeighbours(inst, 3);
                    end = TimingUtils.getNanoCPUTimeOfCurrentThread();

                    timePedro = TimingUtils.nanoTimeToSeconds(end - start);
                }

                kdtreePedro.update(inst);

            } catch (Exception e) {
                e.printStackTrace();
            }

            // =====================================================================
            // 3. Validações das instâncias
            // =====================================================================
            if (moaNN != null && moaTruth != null && pedroNN != null) {
                // if (!compareInstances(moaNN, moaTruth)) {
                // System.out.println("KDTREE DO MOA NÃO BATE COM O GROUNDTRUTH | PORQUE O KNN
                // DO MOA É APROXIMADO");
                // }

                if (!compareInstances(pedroNN, moaTruth)) {
                    System.out.println("INSTÂNCIAS PROCESSADAS: " + instanciasProcessadas);
                    System.out.println("KDTREE DO PED NÃO BATE COM O GROUNDTRUTH");
                    return null;
                }
            }

            instanciasProcessadas++;

            // =====================================================================
            // 6. LOG PERIÓDICO
            // =====================================================================
            if (instanciasProcessadas % 100 == 0) {
                System.out.println("INSTANCIAS PROCESSADAS " + instanciasProcessadas);
                System.out.println("TEMPO MOA: " + timeMOA);
                System.out.println("TEMPO MOA KDTREE: " + timeMOAKDTree);
                System.out.println("-------------------------");
                System.out.println("TEMPO PEDRO: " + timePedro);
                System.out.println("BACKTRACK PEDRO: " + kdtreePedro.backtrackCount);
                System.out.println("-------------------------");
                System.out.println("MOA DISTÂNCIAS: ");
                try {
                    for (double distance : kdtreeMOA.getDistances()) {
                        System.out.println(distance);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                System.out.println("-------------------------");
                System.out.println("PED DISTÂNCIAS: ");
                try {
                    for (double distance : kdtreePedro.getDistances()) {
                        System.out.println(distance);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

        }
        return null;
    }

}

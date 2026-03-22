package moa.tasks;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;

import com.github.javacliparser.FileOption;
import com.github.javacliparser.IntOption;
import com.yahoo.labs.samoa.instances.Instance;
import com.yahoo.labs.samoa.instances.Instances;

import moa.classifiers.lazy.neighboursearch.EuclideanDistance;
import moa.classifiers.lazy.neighboursearch.KDTree;
import moa.classifiers.lazy.neighboursearch.KDTreeSimple;
import moa.core.Example;
import moa.core.ObjectRepository;
import moa.options.ClassOption;
import moa.streams.ExampleStream;

public class ValidacaoJanelaDeslizante extends MainTask {
    public ClassOption streamOption = new ClassOption("stream", 's',
            "Stream to evaluate on.", ExampleStream.class,
            "generators.RandomTreeGenerator");

    public FileOption outputFileOption = new FileOption("outputFile", 'o',
            "File to append output temp train and test to.", null, ".txt", true);

    public IntOption instancesFrequencyOption = new IntOption(
            "instancesFrequency",
            'n',
            "Quantidade de instancias para medir o CPU RAM HOURS.",
            1000, 0, Integer.MAX_VALUE);

    public IntOption windowSizeOption = new IntOption(
            "windowSize",
            'w',
            "Tamanho da janela deslizante",
            10,
            10,
            Integer.MAX_VALUE);

    @Override
    public Class<?> getTaskResultType() {
        return null;
    }

    public static boolean compareInstances(Instance a, Instance b) {

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

    public static double distancia_euclidiana(EuclideanDistance dist_fn, Instance n1, Instance n2) {
        return dist_fn.distance(n1, n2);
    }

    public static boolean sameDistance(Instance a, Instance b, Instance target, EuclideanDistance dist_fn) {
        double distance_a = distancia_euclidiana(dist_fn, target, a);
        double distance_b = distancia_euclidiana(dist_fn, target, b);
        System.out.println("A:" + distance_a);
        System.out.println("B:" + distance_b);
        return distance_a == distance_b;
    }

    @Override
    protected Object doMainTask(TaskMonitor monitor, ObjectRepository repository) {
        ////////////////////////// CONFIGURAÇÕES
        ExampleStream<?> stream = (ExampleStream<?>) getPreparedClassOption(this.streamOption);
        monitor.setCurrentActivity("VAL: KDTREE WINDOW", -1.0);

        File outputTempFile = this.outputFileOption.getFile();
        PrintStream outputStream = null;
        if (outputTempFile != null) {
            try {
                if (outputTempFile.exists()) {
                    outputStream = new PrintStream(
                            new FileOutputStream(outputTempFile, false), true);
                } else {
                    outputStream = new PrintStream(
                            new FileOutputStream(outputTempFile), true);
                }
            } catch (Exception ex) {
                throw new RuntimeException(
                        "Unable to open prediction result file: " + outputTempFile, ex);
            }
        } else {
            System.out.println("NÃO TEM ARQUIVO DE SAÍDA");
            return null;
        }

        // FILA CIRCULAR ONDE VAI FICAR A JANELA DESLIZANTE
        CircularQueue window = new CircularQueue(windowSizeOption.getValue());
        KDTreeSimple kdtree = null;
        KDTree kdtreeMoa = null;
        ///////////////////////////////////////////////////////////////////////////////////////////
        /// TODO 13/01: COPIAR A FUNÇÃO DE BUSCA DO EDUARDO E
        // VERIFICAR SE BATE COM O DO MOA.
        // VER COM O PROFESSOR SOBRE A MÁQUINA

        EuclideanDistance distFn = new EuclideanDistance();
        int instanciasProcessadas = 0;
        while (stream.hasMoreInstances()) {
            try {

                Example<?> ex = stream.nextInstance();
                Instance inst = (Instance) ex.getData();
                if (kdtree == null) {
                    kdtree = new KDTreeSimple(inst.numAttributes() - 1);
                    kdtree.setInstances(new Instances(inst.dataset(), 0));
                    kdtree.setDistanceFunction(distFn);
                }

                // BUSCA na árvore ignorando as inativas
                Instance inst_pedro = null;
                Instance inst_moa = null;

                if (!window.isEmpty()) {
                    kdtreeMoa = new KDTree();
                    kdtreeMoa.setMaxInstInLeaf(1);
                    kdtreeMoa.setNormalizeNodeWidth(false);
                    kdtreeMoa.setDistanceFunction(distFn);
                    kdtreeMoa.setInstances(window.toInstances());

                    inst_pedro = kdtree.nearestNeighbourActive(inst);
                    inst_moa = kdtreeMoa.nearestNeighbour(inst);
                }

                // Se estiver cheio remove a primeira instância
                if (window.isFull())
                    kdtree.delete(window.remove());

                // Adiciona a nova instancia
                kdtree.update(inst);
                window.add(inst);

                instanciasProcessadas++;

                if (inst_pedro != null && inst_moa != null) {
                    if (!compareInstances(inst_pedro, inst_moa)) {
                        window.showQueue();

                        System.out.println("INSTÂNCIAS PROCESSADAS: " + instanciasProcessadas);
                        sameDistance(inst_moa, inst_pedro, inst, distFn);
                        System.out.println("MOA: " + inst_moa);
                        System.out.println("PED: " + inst_pedro);
                        System.out.println("BUS: " + inst);
                        System.out.println("KDTREE DO PED NÃO BATE COM O GROUNDTRUTH");
                        return null;
                    }
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (outputStream != null) {
            outputStream.close();
        }

        return null;

    }

    /**
     * Fila circular
     */
    public class CircularQueue {

        int first, last, size, nItens;
        Instance[] window;

        public CircularQueue(int size) {
            // Inicialização da fila circular
            this.size = size;
            window = new Instance[this.size];
            this.first = 0;
            this.last = -1;
            this.nItens = 0;
        }

        private boolean isFull() {
            return nItens == size;
        }

        private boolean isEmpty() {
            return nItens == 0;
        }

        public void showQueue() {
            int i, cont;
            for (cont = 0, i = first; cont < nItens; cont++) {
                System.out.println(window[i]);
                i++;

                if (i == size) {
                    i = 0;
                }
            }

            System.out.println();
        }

        public int getNItens() {
            return nItens;
        }

        public void add(Instance inst) {
            // Essa função serve para sempre colocar um novo valor na fila circular
            if (isFull()) {
                remove();
            }
            insert(inst);
        }

        private void insert(Instance inst) {
            if (last == size - 1)
                last = -1;

            last++;
            window[last] = inst;
            nItens++;
        }

        private Instance remove() {
            Instance temp = window[first];
            window[first] = null;
            first++;
            if (first == size) {
                first = 0;
            }

            nItens--;
            return temp;
        }

        public Instances toInstances() {
            if (isEmpty()) {
                return null;
            }

            Instance ref = window[first];
            Instances dataset = new Instances(ref.dataset(), nItens);

            int i = first;
            for (int count = 0; count < nItens; count++) {
                Instance inst = window[i];

                if (inst != null) {
                    dataset.add(inst);
                }

                i++;
                if (i == size) {
                    i = 0;
                }
            }

            return dataset;
        }

    }

}

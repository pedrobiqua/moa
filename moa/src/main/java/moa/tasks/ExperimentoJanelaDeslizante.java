package moa.tasks;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;

import com.github.javacliparser.FileOption;
import com.github.javacliparser.IntOption;
import com.yahoo.labs.samoa.instances.Instance;
import com.yahoo.labs.samoa.instances.Instances;

import moa.classifiers.lazy.neighboursearch.EuclideanDistance;
import moa.classifiers.lazy.neighboursearch.KDTreeSimple;
import moa.core.Example;
import moa.core.ObjectRepository;
// import moa.core.TimingUtils;
import moa.options.ClassOption;
import moa.streams.ExampleStream;

public class ExperimentoJanelaDeslizante extends MainTask {
    //////////////////// PARAMETROS DO EXPERIMENTO
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
            1000,
            10,
            Integer.MAX_VALUE);

    @Override
    public Class<?> getTaskResultType() {
        return null;
    }

    @Override
    protected Object doMainTask(TaskMonitor monitor, ObjectRepository repository) {
        ////////////////////////// CONFIGURAÇÕES
        ExampleStream<?> stream = (ExampleStream<?>) getPreparedClassOption(this.streamOption);
        monitor.setCurrentActivity("EXP: TEMPO KDTREE", -1.0);

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
        ///////////////////////////////////////////////////////////////////////////////////////////

        while (stream.hasMoreInstances()) {
            try {

                Example<?> ex = stream.nextInstance();
                Instance inst = (Instance) ex.getData();
                if (kdtree == null) {
                    EuclideanDistance distFn = new EuclideanDistance();
                    kdtree = new KDTreeSimple(inst.numAttributes() - 1);
                    kdtree.setInstances(new Instances(inst.dataset(), 0));
                    kdtree.setDistanceFunction(distFn);
                }

                // BUSCA
                if (!window.isEmpty())
                    kdtree.nearestNeighbour(inst);

                if (window.isFull())
                    kdtree.delete(inst);

                kdtree.update(inst);
                window.add(inst);

                // APENAS DEBUG
                // window.showQueue();

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (outputStream != null) {
            outputStream.close();
        }

        return null;

    }

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

    }

}

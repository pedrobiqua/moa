package moa.tasks;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import com.github.javacliparser.FileOption;
import com.github.javacliparser.FlagOption;
import com.github.javacliparser.IntOption;
import com.github.javacliparser.MultiChoiceOption;
import com.yahoo.labs.samoa.instances.Instance;
import com.yahoo.labs.samoa.instances.InstancesHeader;

import moa.classifiers.lazy.neighboursearch.CircularQueue;
import moa.classifiers.lazy.neighboursearch.EuclideanDistance;
import moa.classifiers.lazy.neighboursearch.LinearNNSearch;
import moa.classifiers.lazy.neighboursearch.SKDTree;
import moa.classifiers.lazy.neighboursearch.SNode;
import moa.core.Example;
import moa.core.ObjectRepository;
import moa.core.TimingUtils;
import moa.options.AbstractOptionHandler;
import moa.options.ClassOption;
import moa.streams.ExampleStream;
import moa.streams.InstanceStream;
import moa.streams.generators.AgrawalGenerator;
import moa.streams.generators.AssetNegotiationGenerator;
import moa.streams.generators.HyperplaneGenerator;
// import moa.streams.generators.LEDGenerator;
// import moa.streams.generators.LEDGeneratorDrift;
import moa.streams.generators.RandomRBFGenerator;
// import moa.streams.generators.RandomRBFGeneratorDrift;
// import moa.streams.generators.RandomTreeGenerator;
import moa.streams.generators.SEAGenerator;
import moa.streams.generators.STAGGERGenerator;
// import moa.streams.generators.WaveformGenerator;
// import moa.streams.generators.WaveformGeneratorDrift;

// TODO: CONTINUAR OS TESTES e MONTAR SCRIPT DESSA NOVA BATERIA DE TESTE
// Scripts sinteticos montados, falta apenas os scripts de datasets reais
// Vou deixar para amanhã a validação

public class ExperimentosSKDtree extends MainTask {
    public ClassOption streamOption = new ClassOption("stream", 's',
            "Stream to evaluate on.", ExampleStream.class,
            "generators.RandomTreeGenerator");

    public FileOption outputFileOption = new FileOption("outputFile", 'o',
            "File to append output temp train and test to.", null, ".txt", true);

    public IntOption windowSizeOption = new IntOption(
            "windowSize",
            'w',
            "Tamanho da janela deslizante",
            10,
            10,
            Integer.MAX_VALUE);

    public IntOption sizeSinteticDatasetOption = new IntOption(
            "sizeSinteticDataset",
            't',
            "Tamanho do dataset sintetico",
            1000,
            1000,
            Integer.MAX_VALUE);

    public FlagOption isSitenticDataOption = new FlagOption(
            "isSinteticData",
            'b',
            "Ativado para quando o experimento for com dados sinteticos");

    public FlagOption isValidationOption = new FlagOption(
            "isValidation",
            'v',
            "Flag para inidicar se deve rodar no modo de validação");

    // Mudar o nome das funções dos experimentos
    public MultiChoiceOption numExperimentOption = new MultiChoiceOption(
            "numExperiment", 'e', "Choice experiment", new String[] {
                    "Experiment 1", "Experiment 2" },
            new String[] { "Insert and Search SKDtree colect metrics. ",
                    "Insert, Remove and Search SKDtree colect metrics. "
            }, 0);

    public PrintStream configOutputMetrics(boolean isSinteticData, String nameStream) {

        File outputTempFile = this.outputFileOption.getFile();
        if (isSinteticData) { // Se for dado sintetico cria csv de resultados
            String[] splitName = nameStream.split("\\.");
            nameStream = splitName[splitName.length - 1];

            LocalDateTime now = LocalDateTime.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH:mm:ss");
            String timeStamp = now.format(formatter);

            File streamSinteticFile = new File(outputTempFile.getParent(), nameStream + "_" + timeStamp + ".csv");
            outputTempFile = streamSinteticFile;
        }
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

                return outputStream;
            } catch (Exception ex) {
                throw new RuntimeException(
                        "Unable to open prediction result file: " + outputTempFile, ex);
            }
        } else {
            throw new RuntimeException("NÃO TEM ARQUIVO DE SAÍDA");
        }
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

    /**
     * Função que é usado para validação, usado durante o desenvolvimento
     */
    public void runValidation(ExampleStream<?> stream, boolean isSinteticData) {

        // INICIALIZANDO A ÁRVORE E CONFIGURAÇÕES
        long maxInstancias;
        if (isSinteticData)
            maxInstancias = sizeSinteticDatasetOption.getValue();
        else
            maxInstancias = Long.MAX_VALUE;

        InstancesHeader streamHeader = stream.getHeader();
        SKDTree kdtree = new SKDTree(stream.getHeader().numAttributes() - 1, stream.getHeader());
        CircularQueue queue = new CircularQueue(3, streamHeader);

        EuclideanDistance distanceFunction = new EuclideanDistance();
        distanceFunction.setDontNormalize(true);
        distanceFunction.setInstances(streamHeader);

        int instanciasProcessadas = 0;
        while (stream.hasMoreInstances() && instanciasProcessadas < maxInstancias) {
            try {

                if (instanciasProcessadas == 123) {
                    System.out.println("DEBUG");
                }
                Example<?> ex = stream.nextInstance();
                Instance inst = (Instance) ex.getData();

                Instance instanciaBusca1NN = null;
                Instance instanciaBusca1NNMOA = null;

                if (!queue.isEmpty()) {
                    instanciaBusca1NN = kdtree.nearestNeighbour(inst);

                    // Construção da árvore kdtree, coloco para não normalizar
                    // coloco para o bucket ter tamanho máximo 1, isso para ficar o
                    // mais próximo com a minha implementação para ser um teste justo
                    LinearNNSearch linearMOA = new LinearNNSearch();
                    linearMOA.setDistanceFunction(distanceFunction);
                    linearMOA.setInstances(queue.toInstances());

                    instanciaBusca1NNMOA = linearMOA.nearestNeighbour(inst);

                    // VALIDAÇÃO
                    double distPed = distanceFunction.distance(instanciaBusca1NN, inst);
                    double distLinear = distanceFunction.distance(instanciaBusca1NNMOA, inst);

                    if (distPed != distLinear) {

                        System.out.println("ERRO NA BUSCA 1-NN");
                        System.out.println("Instâncias processadas: " + instanciasProcessadas);

                        System.out.println("Instacias na minha janela");
                        queue.showQueue();

                        System.out.println("QUERY:");
                        System.out.println(inst + "\n");

                        System.out.println("PED NN (dist = " + distPed + "):");
                        System.out.println(instanciaBusca1NN);

                        System.out.println("LINEAR NN (dist = " + distLinear + "):");
                        System.out.println(instanciaBusca1NNMOA);

                        System.out.println("Diferença: " + (distPed - distLinear));

                        // Buscando a instancia encontrada na árvore:
                        SNode node = kdtree.search(instanciaBusca1NN.toDoubleArray(), kdtree.root);
                        System.out.println("Nó está ativo: " + node.active);
                        System.out.println("Split node: " + node.splitDim);
                        System.out.println("Indice: " + node.index);

                        kdtree.print();
                        return;
                    }

                }

                if (queue.isFull()) {
                    Instance instanciaRemovida = queue.remove();
                    System.out.println("Instancia removida: " + instanciaRemovida);
                    kdtree.remove(instanciaRemovida);
                }
                queue.insert(inst);
                // queue.showQueue();
                kdtree.update(inst);

                instanciasProcessadas++;

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        kdtree.print();

        System.out.println("\nTerminou!\n Instâncias Processadas:" + instanciasProcessadas);
        System.out.println("Instancias finais da janela: \n" + queue.toInstances());
    }

    public void runExperimentSlideWindow(ExampleStream<?> stream, int windowSize, boolean sinteticData) {

        PrintStream output = configOutputMetrics(sinteticData, stream.getClass().getName());

        long maxInstancias;
        if (sinteticData)
            maxInstancias = sizeSinteticDatasetOption.getValue();
        else
            maxInstancias = Long.MAX_VALUE;
        int numInstancias = 0;

        InstancesHeader streamHeader = stream.getHeader();
        SKDTree kdtree = new SKDTree(stream.getHeader().numAttributes() - 1, stream.getHeader());
        CircularQueue queue = new CircularQueue(windowSize, streamHeader);

        output.println(
                "numero_instancias,tempo_insert,tempo_busca,tempo_remove,altura_arvore_pos_insercao,profundidade_insercao,profundidade_busca,backtracking");

        while (stream.hasMoreInstances() && numInstancias < maxInstancias) {
            try {
                long start_search, end_search, start_insert, end_insert, start_remove, end_remove;
                double temp_insert = 0.0, temp_search = 0.0, temp_remove = 0.0;

                Example<?> ex = stream.nextInstance();
                Instance inst = (Instance) ex.getData();

                if (!queue.isEmpty()) {
                    // COLETAR O TEMPO
                    start_search = TimingUtils.getNanoCPUTimeOfCurrentThread();
                    kdtree.nearestNeighbour(inst); // BUSCA
                    end_search = TimingUtils.getNanoCPUTimeOfCurrentThread();
                    temp_search = TimingUtils.nanoTimeToSeconds(end_search - start_search);
                }

                if (queue.isFull()) {
                    Instance instanciaRemovida = queue.remove();
                    // COLETAR O TEMPO
                    start_remove = TimingUtils.getNanoCPUTimeOfCurrentThread();
                    kdtree.remove(instanciaRemovida); // REMOVE
                    end_remove = TimingUtils.getNanoCPUTimeOfCurrentThread();
                    temp_remove = TimingUtils.nanoTimeToSeconds(end_remove - start_remove);
                }

                queue.insert(inst);
                // COLETAR O TEMPO
                start_insert = TimingUtils.getNanoCPUTimeOfCurrentThread();
                kdtree.update(inst); // INSERE
                end_insert = TimingUtils.getNanoCPUTimeOfCurrentThread();
                temp_insert = TimingUtils.nanoTimeToSeconds(end_insert - start_insert);

                numInstancias++;
                // Armazena o resultado no arquivo de output
                output.println(
                        numInstancias + ","
                                + temp_insert + ","
                                + temp_search + ","
                                + temp_remove + ","
                                + kdtree.heightTree + ","
                                + kdtree.depthInsert + ","
                                + kdtree.maxDepthSearch + ","
                                + kdtree.backtrack);

            } catch (Exception e) {
                e.printStackTrace();
                return;
            }
        }
    }

    public void runExperimentInsertSearch(ExampleStream<?> stream, boolean sinteticData) {
        PrintStream output = configOutputMetrics(sinteticData, stream.getClass().getName());

        long maxInstancias;
        if (sinteticData)
            maxInstancias = sizeSinteticDatasetOption.getValue();
        else
            maxInstancias = Long.MAX_VALUE;
        int numInstancias = 0;

        SKDTree kdtree = new SKDTree(stream.getHeader().numAttributes() - 1, stream.getHeader());

        output.println(
                "numero_instancias,tempo_insert,tempo_busca,altura_arvore_pos_insercao,profundidade_insercao,profundidade_busca,backtracking");

        while (stream.hasMoreInstances() && numInstancias < maxInstancias) {
            try {
                long start_search, end_search, start_insert, end_insert;
                double temp_insert = 0.0, temp_search = 0.0;

                Example<?> ex = stream.nextInstance();
                Instance inst = (Instance) ex.getData();

                if (numInstancias != 0) {
                    // COLETAR O TEMPO
                    start_search = TimingUtils.getNanoCPUTimeOfCurrentThread();
                    kdtree.nearestNeighbour(inst); // BUSCA
                    end_search = TimingUtils.getNanoCPUTimeOfCurrentThread();
                    temp_search = TimingUtils.nanoTimeToSeconds(end_search - start_search);
                }

                // COLETAR O TEMPO
                start_insert = TimingUtils.getNanoCPUTimeOfCurrentThread();
                kdtree.update(inst); // INSERE
                end_insert = TimingUtils.getNanoCPUTimeOfCurrentThread();
                temp_insert = TimingUtils.nanoTimeToSeconds(end_insert - start_insert);

                numInstancias++;
                // Armazena o resultado no arquivo de output
                output.println(
                        numInstancias + ","
                                + temp_insert + ","
                                + temp_search + ","
                                + kdtree.heightTree + ","
                                + kdtree.depthInsert + ","
                                + kdtree.maxDepthSearch + ","
                                + kdtree.backtrack);

            } catch (Exception e) {
                e.printStackTrace();
                return;
            }
        }
    }

    @Override
    public Class<?> getTaskResultType() {
        throw new UnsupportedOperationException("Unimplemented method 'getTaskResultType'");
    }

    @Override
    protected Object doMainTask(TaskMonitor monitor, ObjectRepository repository) {
        ExampleStream<?> stream = (ExampleStream<?>) getPreparedClassOption(this.streamOption);
        if (numExperimentOption.getChosenIndex() == 0)
            monitor.setCurrentActivity("SKDtree Experiment Insert and Search", -1.0);
        else
            monitor.setCurrentActivity("SKDtree Experiment Sliding window", -1.0);

        boolean isSinteticData = isSitenticDataOption.isSet();
        boolean isValidation = isValidationOption.isSet();
        int windowSize = windowSizeOption.getValue();

        if (isSinteticData) { // STREAMS DATASETS SINTETICOS EXPERIMENTO
            InstanceStream[] streams_teste = {
                    new AssetNegotiationGenerator(),
                    new SEAGenerator(),
                    new RandomRBFGenerator(),
                    new AgrawalGenerator(),
                    new HyperplaneGenerator(),
                    new STAGGERGenerator(),
                    // new RandomTreeGenerator(),
                    // new WaveformGenerator(),
                    // new LEDGenerator(),
                    // // Tem drift no nome, verificar se já é uma stream com drift, eu não tenho
                    // // certeza
                    // new WaveformGeneratorDrift(),
                    // new RandomRBFGeneratorDrift(),
                    // new LEDGeneratorDrift(),

            };
            for (int i = 0; i < streams_teste.length; i++) {
                if (streams_teste[i] instanceof AbstractOptionHandler)
                    ((AbstractOptionHandler) streams_teste[i]).prepareForUse();
                else {
                    throw new UnsupportedOperationException("Unimplemented method 'prepareForUse'");
                }

                if (isValidation) {
                    runValidation(streams_teste[i], isSinteticData);
                } else {
                    String[] streamAllName = streams_teste[i].getClass().getName().split("\\.");
                    String streamName = streamAllName[streamAllName.length - 1];
                    if (numExperimentOption.getChosenIndex() == 0)
                        runExperimentInsertSearch(streams_teste[i], isSinteticData); // Experimento 1
                    else
                        runExperimentSlideWindow(streams_teste[i], windowSize, isSinteticData); // Experimento 2
                    System.out.println("Resultado: ~/Output/" + streamName + ".csv\n");
                }
            }
        } else { // STREAMS DATASETS REAIS
            if (isValidation) {
                runValidation(stream, isSinteticData);
            } else {
                if (numExperimentOption.getChosenIndex() == 0)
                    runExperimentInsertSearch(stream, isSinteticData); // Experimento 1
                else
                    runExperimentSlideWindow(stream, windowSize, isSinteticData); // Experimento 2
            }
        }
        return null;
    }

}

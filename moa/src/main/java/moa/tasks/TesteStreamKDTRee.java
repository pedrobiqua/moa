package moa.tasks;

import moa.classifiers.lazy.neighboursearch.CircularQueue;
import moa.classifiers.lazy.neighboursearch.kdtrees.*;
import moa.options.AbstractOptionHandler;
import moa.options.ClassOption;
import moa.classifiers.lazy.neighboursearch.StreamKDTree;
import moa.core.Example;
import moa.core.ObjectRepository;
import moa.streams.ExampleStream;

import com.github.javacliparser.MultiChoiceOption;
import com.yahoo.labs.samoa.instances.Instance;
import com.yahoo.labs.samoa.instances.Instances;

import java.util.Locale;

public class TesteStreamKDTRee extends MainTask {
    public ClassOption streamOption = new ClassOption("stream", 's',
            "Stream to evaluate on.", ExampleStream.class,
            "generators.RandomTreeGenerator");

    // TODO: LEMBRAR DE ARRUMAR ESSA DESCRIÇÃO
    public MultiChoiceOption splitterOption = new MultiChoiceOption(
            "splitter", 'c', "Method Splitter option", new String[]{
            "SlidingMidPointOfWidestSide", "MedianOfWidestDimension", "MidPointOfWidestDimension"},
            new String[]{"Sliding Mid Point Of Widest side. ",
                    "Median Of Widest Dimension.",
                    "Mid Point of widest dimension."
            }, 0);

    @Override
    public Class<?> getTaskResultType() {
        return null;
    }

    @Override
    protected Object doMainTask(TaskMonitor monitor, ObjectRepository repository) {
        // Tratamento dos parâmetros do experimento
        ExampleStream<?> stream = (ExampleStream<?>) getPreparedClassOption(this.streamOption);
        if (stream instanceof AbstractOptionHandler)
            ((AbstractOptionHandler) stream).prepareForUse();
        else {
            throw new UnsupportedOperationException("Unimplemented method 'prepareForUse'");
        }

        KDTreeNodeSplitter splitter;
        int splitterChosenIndex = splitterOption.getChosenIndex();
        if (splitterChosenIndex == 0) {
            System.out.println("Escolhido: Sliding Mid Point");
            splitter = new SlidingMidPointOfWidestSide();
        } else if (splitterChosenIndex == 1) {
            System.out.println("Escolhido: Median Of Widest Dimension");
            splitter = new MedianOfWidestDimension();
        } else if (splitterChosenIndex == 2) {
            System.out.println("Escolhido: Mid Point Of Widest Dimension");
            splitter = new MidPointOfWidestDimension();
        } else {
            System.err.print("Nenhum splitter escolhido!");
            return null;
        }
        //////

        monitor.setCurrentActivity("Testando Estrutura de Dados", -1.0);

        // Instância a StreamKDTree
        StreamKDTree stream_kdtree = null;
        Instances insts = new Instances(stream.getHeader(), 0);
        CircularQueue queue = new CircularQueue(stream.getHeader(), 4);

        int maxInstances = 10;

        System.out.print("Total_Instancias,Número_Nós,Número_Folhas,ProfundidadeMaxima,MédiaInstânciasPorNó\n");
        int count = 0;
        while (stream.hasMoreInstances() && (count < maxInstances)) {
            Example<?> ex = stream.nextInstance();
            Instance inst = (Instance) ex.getData();
            try {
                // Realiza a busca
                if (stream_kdtree == null) {
                    stream_kdtree = new StreamKDTree();
                    stream_kdtree.setMaxInstInLeaf(2);
                    stream_kdtree.setSplitter(splitter);
                    stream_kdtree.setRebuildPolicies(new DeletedRatioPolicy());
                } else {
                    stream_kdtree.nearestNeighbour(inst);
                }

                // Se cheio, remover
                if (queue.isFull())
                    stream_kdtree.delete(queue.remove());
                // Insere
                stream_kdtree.update(inst);
                queue.insert(inst);

                int total_inst = stream_kdtree.getInstances().numInstances();
                System.out.printf(Locale.US, "%d,%d,%d,%d,%d,%d,%.2f%n",
                        total_inst,
                        stream_kdtree.m_Stats.m_NumInstancias,
                        stream_kdtree.m_Stats.m_NumInstancesDeleted,
                        stream_kdtree.m_Stats.m_NumNodes,
                        stream_kdtree.m_Stats.m_NumLeaves,
                        stream_kdtree.m_Stats.m_MaxDepth,
                        (double) total_inst / stream_kdtree.m_Stats.m_NumLeaves);

                // Adicionar aqui, e depois monta a árvore inteira
                insts.add(inst);
            } catch (Exception e) {
                System.out.println("Erro na stream:");
                e.printStackTrace();
                break;
            }
            count++;
        }

        if (stream_kdtree != null)
            stream_kdtree.print();
        return null;
    }

}

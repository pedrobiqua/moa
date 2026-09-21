package moa.tasks;

import com.yahoo.labs.samoa.instances.Instance;
import moa.classifiers.lazy.neighboursearch.IKDTree;
import moa.classifiers.lazy.neighboursearch.Window;
import moa.core.Example;
import moa.core.ObjectRepository;
import moa.options.AbstractOptionHandler;
import moa.options.ClassOption;
import moa.streams.ExampleStream;

import java.util.ArrayDeque;
import java.util.Deque;

public class EvaluateIKDTree extends MainTask {

    public ClassOption streamOption = new ClassOption(
            "stream", 's',
            "Stream to evaluate on.",
            ExampleStream.class,
            "generators.RandomTreeGenerator");

    public class Window {

        private final int capacity;
        private final Deque<Instance> instances;

        public Window(int capacity) {
            if (capacity <= 0) {
                throw new IllegalArgumentException("Window size must be greater than 0.");
            }

            this.capacity = capacity;
            this.instances = new ArrayDeque<>(capacity);
        }

        /**
         * Add new instance in window.
         *
         * @return a instância removida da janela, ou null
         *         caso nenhuma instância tenha sido removida.
         */
        public Instance add(Instance instance) {

            Instance removed = null;

            if (instances.size() >= capacity) {
                removed = instances.removeFirst();
            }

            instances.addLast(instance);

            return removed;
        }

        public int size() {
            return instances.size();
        }

        public boolean isFull() {
            return instances.size() >= capacity;
        }

        public void clear() {
            instances.clear();
        }
    }

    @Override
    protected Object doMainTask(TaskMonitor monitor, ObjectRepository repository) {

        ExampleStream<?> stream = (ExampleStream<?>) getPreparedClassOption(this.streamOption);
        if (stream instanceof AbstractOptionHandler)
            ((AbstractOptionHandler) stream).prepareForUse();
        else {
            throw new UnsupportedOperationException("Unimplemented method 'prepareForUse'");
        }

        long maxInstances = 100;
        long count = 0;
        int window_size = 10;

        IKDTree ikdtree = new IKDTree();
        Window window = new Window(window_size);
        while (stream.hasMoreInstances() && count < maxInstances) {
            Example<?> ex = stream.nextInstance();
            Instance target = (Instance) ex.getData();
            try {
                // Sliding window
                Instance removed = window.add(target);
                if (removed != null) {
                    ikdtree.delete(removed);
                }
                ikdtree.update(target);
            } catch (Exception e) {
                e.printStackTrace();
            }

            count++;
            // Print tree
            if (count % 5 == 0){
                try {
                    String filename = count + ".dot";
                    ikdtree.printTree(filename);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        return null;
    }

    @Override
    public Class<?> getTaskResultType() {
        return null;
    }
}

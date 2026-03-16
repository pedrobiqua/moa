package moa.classifiers.lazy.neighboursearch;

import com.yahoo.labs.samoa.instances.Instance;
import com.yahoo.labs.samoa.instances.Instances;
import com.yahoo.labs.samoa.instances.InstancesHeader;

/**
 * Fila circular
 */
public class CircularQueue {

    int first, last, size, nItens;
    Instance[] window;
    InstancesHeader streamHeader;

    public CircularQueue(InstancesHeader streamHeader, int size) {
        // Inicialização da fila circular
        this.size = size;
        window = new Instance[this.size];
        this.first = 0;
        this.last = -1;
        this.nItens = 0;
        this.streamHeader = streamHeader;
    }

    public boolean isFull() {
        return nItens == size;
    }

    public boolean isEmpty() {
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

    public void insert(Instance inst) {
        if (last == size - 1)
            last = -1;

        last++;
        window[last] = inst;
        nItens++;
    }

    public Instance remove() {
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
        Instances dataset = new Instances(streamHeader, nItens);

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

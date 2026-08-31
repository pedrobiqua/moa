package moa.classifiers.lazy.neighboursearch;

import com.yahoo.labs.samoa.instances.Instances;

public class Window {
    private int first = 0, last = 0, size = 0;
    private final int window_size;

    public Window(int window_size) {
        this.window_size = window_size;
        this.first = 0;
        this.last = 0;
        this.size = 0;
    }

    public int getSize() {
        return size;
    }

    /**
     *
     * @return idx de m_Instances e −1 nenhum índice removido.
     */
    public int update(int numInstances) {
        int oldFirst = first;

        last = numInstances + 1;
        first = Math.max(0, last - window_size);
        size = last - first;

        if (first > oldFirst)
            return oldFirst;
        return -1;
    }

    public void reset() {
        first = 0;
    }

    /**
     * Range copy
     * @return first and last
     */
    public int[] toCopy() {
        int toCopy = last - first;
        return new int[]{first, toCopy};
    }

}

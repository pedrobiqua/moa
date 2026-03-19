package moa.classifiers.lazy.neighboursearch;

import com.yahoo.labs.samoa.instances.Instances;

public class Window {
    private Instances m_Instances;
    private int first = 0, last = 0, size = 0;
    public int window_size;

    public Window(int window_size) {
        this.window_size = window_size;
        this.first = 0;
        this.last = 0;
        this.size = 0;
    }

    public void setInstances(Instances instances) {
        // Faço isso apenas uma vez e nunca mais
        this.m_Instances = instances;
        this.first = 0;
        this.last = instances.numInstances();
        this.size = last - first;
    }

    public int getSize() {
        return size;
    }

    /**
     *
     * @return idx de m_Instances e −1 nenhum índice removido.
     */
    public int update() {
        if (m_Instances == null)
            return -1;

        int oldFirst = first;

        // considera a próxima instância que será inserida
        last = m_Instances.numInstances() + 1;
        first = last - window_size;

        if (first < 0)
            first = 0;

        size = last - first;

        if (first > oldFirst)
            return oldFirst;

        return -1;
    }

    // retorna apenas a janela ativa
    public Instances getInstancesWindow() {
        int numInstances = m_Instances.numInstances();
        if (numInstances == 0) {
            first = last = 0;
            return new Instances(m_Instances, 0);
        }
        last = numInstances;
        first = last - window_size;
        if (first < 0)
            first = 0;

        int toCopy = last - first;
        return new Instances(m_Instances, first, toCopy);
    }

}

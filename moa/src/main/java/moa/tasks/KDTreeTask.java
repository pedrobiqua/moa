package moa.tasks;

import java.io.PrintStream;

import com.yahoo.labs.samoa.instances.Instance;
import com.yahoo.labs.samoa.instances.Instances;

import moa.classifiers.lazy.neighboursearch.KDTreeEduardo;
import moa.classifiers.lazy.neighboursearch.KDTreeSimple;
import moa.core.Example;
import moa.core.ObjectRepository;
import moa.core.TimingUtils;
import moa.core.Utils;
import moa.evaluation.preview.LearningCurve;
import moa.options.ClassOption;
import moa.streams.ExampleStream;

public class KDTreeTask extends ClassificationMainTask {

    public ClassOption streamOption = new ClassOption("stream", 's',
            "Stream to evaluate on.", ExampleStream.class,
            "generators.RandomTreeGenerator");

    @Override
    public Class<?> getTaskResultType() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getTaskResultType'");
    }

    @Override
    protected Object doMainTask(TaskMonitor monitor, ObjectRepository repository) {

        ExampleStream stream = (ExampleStream) getPreparedClassOption(this.streamOption);
        long instancesProcessed = 0;
        monitor.setCurrentActivity("TESTANDO O KDTREE, USANDO COMO BASE KDTREE DO EDUARDO INCADES!", -1.0);
        KDTreeEduardo kd_eduardo = null;

        while (stream.hasMoreInstances()) {
            try {
                Example ex = stream.nextInstance();
                Instance inst = (Instance) ex.getData();
                if (kd_eduardo == null) {
                    Instances temp = new Instances(inst.dataset(), 0);
                    temp.add(inst);
                    kd_eduardo = new KDTreeEduardo(temp);
                } else
                    kd_eduardo.update(inst);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        try (PrintStream out = new PrintStream("debug_kdtree_eduardo.txt")) {
            kd_eduardo.print(out);
        } catch (Exception e) {
            e.printStackTrace();
        }

        /////////////////////////////////////////////////////////

        stream.restart();
        KDTreeSimple kd_pedro = null;

        while (stream.hasMoreInstances()) {
            try {
                Example ex = stream.nextInstance();
                Instance inst = (Instance) ex.getData();
                if (kd_pedro == null) {
                    kd_pedro = new KDTreeSimple(inst.numAttributes() - 1);
                }
                kd_pedro.update(inst);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        try (PrintStream out = new PrintStream("debug_kdtree_pedro.txt")) {
            kd_pedro.print(out);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

}

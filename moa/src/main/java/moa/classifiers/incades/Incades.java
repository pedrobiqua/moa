package moa.classifiers.incades;

import com.yahoo.labs.samoa.instances.Instance;

import moa.classifiers.AbstractClassifier;
import moa.classifiers.core.driftdetection.ChangeDetector;
import moa.classifiers.core.driftdetection.RDDM;
import moa.classifiers.incades.factory.AbstractClassifierFactory;
import moa.classifiers.incades.factory.HoeffdingTreeFactory;
import moa.core.Measurement;
import moa.core.StringUtils;

public class Incades extends AbstractClassifier {

    // Atribute class
    // private ChangeDetector changeDetector;
    // private IPruningEngine<PruningMetrics> pruningEngine;
    // private AbstractClassifierFactory classifierFactory;

    public Incades() throws Exception {
        // this.changeDetector = new RDDM();
        // this.pruningEngine = new AgeBasedPruningEngine(75);
        // this.classifierFactory = new HoeffdingTreeFactory();
    }

    @Override
    public boolean isRandomizable() {
        return false;
    }

    // TODO: Fazer a implementação do treinamento, lembrar amanha de ler o artigo no
    // onibus
    // para eu continuar a implementação
    @Override
    public double[] getVotesForInstance(Instance inst) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getVotesForInstance'");
    }

    @Override
    public void resetLearningImpl() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'resetLearningImpl'");
    }

    @Override
    public void trainOnInstanceImpl(Instance inst) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'trainOnInstanceImpl'");
    }

    @Override
    protected Measurement[] getModelMeasurementsImpl() {
        return null;
    }

    @Override
    public void getModelDescription(StringBuilder out, int indent) {
        StringUtils.appendIndented(out, indent, toString());
        StringUtils.appendNewline(out);
    }

}

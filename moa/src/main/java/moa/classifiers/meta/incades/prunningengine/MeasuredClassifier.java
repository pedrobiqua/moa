package moa.classifiers.meta.incades.prunningengine;

import com.yahoo.labs.samoa.instances.Instance;

import moa.classifiers.Classifier;

public class MeasuredClassifier {
    private final Classifier baseClassifier;
    private final PruningMetrics metrics;

    public MeasuredClassifier(Classifier baseClassifier) {
        this.baseClassifier = baseClassifier;
        this.metrics = new PruningMetrics();
    }

    public Classifier getBaseClassifier() {
        return baseClassifier;
    }

    public PruningMetrics getMetrics() {
        return metrics;
    }

    // Exemplo de delegações com impacto nas métricas
    public void trainOnInstance(Instance inst) {
        baseClassifier.trainOnInstance(inst);
        metrics.increaseUseageFactor();
    }

    public double[] getVotesForInstance(Instance inst) {
        return baseClassifier.getVotesForInstance(inst);
    }

    public void resetLearning() {
        baseClassifier.resetLearning();
    }
}


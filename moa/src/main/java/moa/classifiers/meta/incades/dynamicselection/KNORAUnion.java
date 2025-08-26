package moa.classifiers.meta.incades.dynamicselection;

import com.yahoo.labs.samoa.instances.Instance;
import com.yahoo.labs.samoa.instances.Instances;

import moa.classifiers.Classifier;

public class KNORAUnion extends DynamicSelectionClassifier {

    @Override
    public double[] classify(Classifier[] classifiers, Instances roc, Instance target) {
        throw new UnsupportedOperationException("Unimplemented method 'classify'");
    }

}

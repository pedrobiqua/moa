package moa.classifiers.meta.incades.dynamicselection;

import java.io.Serializable;

import com.yahoo.labs.samoa.instances.Instance;
import com.yahoo.labs.samoa.instances.Instances;

import moa.classifiers.Classifier;

public abstract class DynamicSelectionClassifier implements Serializable {
    public abstract double[] classify(Classifier[] classifiers, Instances roc, Instance target) throws Exception;
}

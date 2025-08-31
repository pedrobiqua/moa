/*
 *    Incades.java
 *    Copyright (C) 2025 Universidade Federal do Paraná, Paraná, Brasil
 *    @author Pedro Bianchini de Quadros (pedro.bianchini@ufpr.br)
 *
 *    This program is free software; you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation; either version 3 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
 */
package moa.classifiers.meta.incades;

import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

import com.github.javacliparser.IntOption;
import com.github.javacliparser.MultiChoiceOption;
import com.yahoo.labs.samoa.instances.Instance;
import com.yahoo.labs.samoa.instances.Instances;
import com.yahoo.labs.samoa.instances.InstancesHeader;

import moa.classifiers.AbstractClassifier;
import moa.classifiers.Classifier;
import moa.classifiers.MultiClassClassifier;
import moa.classifiers.core.driftdetection.ChangeDetector;
import moa.classifiers.lazy.neighboursearch.KDTreeCanberra;
import moa.classifiers.lazy.neighboursearch.kdtrees.StreamNeighborSearch;
import moa.classifiers.meta.incades.dynamicselection.KNORAEliminate;
import moa.classifiers.meta.incades.prunningengine.AgeBasedPruningEngine;
import moa.classifiers.meta.incades.prunningengine.MeasuredClassifier;
import moa.core.Measurement;
import moa.core.StringUtils;
import moa.options.ClassOption;

/**
 * IncA-DES
 *
 * A significant part of this implementation is based on the original code
 * developed by Eduardo V.L. Barboza, as part of the research work:
 *
 * IncA-DES: An incremental and adaptive dynamic ensemble selection approach
 * using online K-d tree neighborhood search for data streams with concept drift
 * Eduardo V.L. Barboza, Paulo R. Lisboa de Almeida, Alceu de Souza Britto Jr.,
 * Robert Sabourin, Rafael M.O. Cruz
 *
 * This version has been adapted and modified by
 * Pedro Bianchini de Quadros (pedro.bianchini@ufpr.br)
 * to fit into the MOA framework.
 *
 * @author Pedro Bianchini de Quadros (pedro.bianchini@ufpr.br)
 * @version $Revision: 1 $
 */
public class Incades extends AbstractClassifier implements MultiClassClassifier {
    private static class OverlapMeasurer {
        public static double measureOverlap(Instances neighborhood) {

            int numClasses = neighborhood.numClasses();
            int numNeighbours = neighborhood.size();

            double[] distribution = new double[numClasses];

            for (int i =0; i < neighborhood.size(); ++i) {
                int classVal = (int) neighborhood.get(i).classValue();
                distribution[classVal]++;
            }

            double maximum = 0;

            for (int i = 0; i < distribution.length; ++i) {
                if(distribution[i] > maximum)
                    maximum = distribution[i];
            }

            double maxClassDist = maximum / numNeighbours;

            return maxClassDist;
        }
    }

    private static final long serialVersionUID = 1L;

    // Random
    private Random random = ThreadLocalRandom.current();

    // Control parameters
    private int trainingCount;
    private int instanceCount;
	private boolean warning = false;
    private int warningLevel = 0;
    private boolean updateNNSearch = true;
    private boolean knnWasSet = false;
    private int TRAINING_SIZE = 200;
    private boolean changeWasDetected = false;

    // Instances
    private InstancesHeader header;
    private Instances DSEW;

    // Classifiers
    private List<MeasuredClassifier> poolClassifiers = new LinkedList<MeasuredClassifier>();

    // Tree
    private StreamNeighborSearch search;

    // DS
    private KNORAEliminate knorae = new KNORAEliminate();

    // Options GUI
    public ClassOption driftDetectionMethodOption = new ClassOption(
        "driftDetectionMethod", 'd',
        "Drift detection method to use.",
        ChangeDetector.class, "DDM"
    );

    public ClassOption classifierOption = new ClassOption(
        "classifierIncremental", 'l',
        "Classifier method to use.",
        Classifier.class, "trees.HoeffdingTree"
    );

    // Adicionar outras formas de KDTree Online
    public MultiChoiceOption nearestNeighbourSearchOption = new MultiChoiceOption(
    "nearestNeighbourSearchOnline", 'n', "Nearest Neighbour Search to use", new String[]{"KDTreeCanberra"},
    new String[]{"KDtree search algorithm for nearest neighbour search using Canberra Distance"}, 0);

    public IntOption windowSize = new IntOption(
        "windowSize", 'p',
        "Window size parameter",
        1000000, 1000, 1000000
    );

    public IntOption trainingSize = new IntOption(
        "TrainingSize", 't', "Training size parameter", 200, 10, 200
    );

    public IntOption numNeighborsOptions = new IntOption(
        "NumNeighbors", 'o', "Number Neighbors", 5, 1, 20
    );

    private ChangeDetector driftDetector;
    private Classifier defaultClassifier;
    private int numNeighbors;

    @Override
    public boolean isRandomizable() {
        return false;
    }

    @Override
    public double[] getVotesForInstance(Instance inst) {
        try {
            Instances neighborhood;
            if (updateNNSearch == true){
				this.rebuildTree();
			} else {
				if(poolClassifiers.size() < 1) {
					int majorityIndex = random.nextInt(inst.classAttribute().numValues());
					double[] probs = new double[inst.classAttribute().numValues()];
					probs[majorityIndex] = 1;
					return probs;
				}
			}

            neighborhood = search.kNearestNeighbours(inst, numNeighbors);

            // Validar o overlap
            double complexity = OverlapMeasurer.measureOverlap(neighborhood);
            if (complexity >= 1.0) {
                // Não tem overlap
                double[] classes = new double[inst.numClasses()];
				for (int i = 0; i < neighborhood.size(); i++) {
					int neighborClass = (int) neighborhood.get(i).classValue();
					classes[neighborClass]++;
				}
				return classes;
            }

            // Tem overlap, mandar o DS retornar a classe
            Classifier[] classifiers = new Classifier[poolClassifiers.size()];

            // Se tiver overlap, mandar os classificadores, roc e intancia para o algoritmo knorae
            for (int i = 0; i < poolClassifiers.size(); i++) {
                classifiers[i] = poolClassifiers.get(i).getBaseClassifier();
            }

            // Se não o RoC já vai ter a classe predita
            return knorae.classify(classifiers, neighborhood, inst);


        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        double[] votes = new double[inst.numClasses()];
        if (votes.length > 0) {
            votes[0] = 1.0;
        }
        return votes;


    }

    @Override
    public void resetLearningImpl() {
        // Inicialize default detector
        driftDetector = (ChangeDetector) getPreparedClassOption(this.driftDetectionMethodOption);
        // Inicialize default classifier
        defaultClassifier = (Classifier) getPreparedClassOption(this.classifierOption);
        poolClassifiers.clear();

        numNeighbors = numNeighborsOptions.getValue();

        knnWasSet = false;
        updateNNSearch = true;
    }

    @Override
    public void trainOnInstanceImpl(Instance inst) {
        try {
            // Inicialize sliding window
            if (DSEW == null) {
                this.DSEW = new Instances(this.header, 0);
            }

            if (search == null) {
                createInstanceKDTRee();
            }

            // Atualiza a mudança de conceito
            if (instanceCount > TRAINING_SIZE)
                updateDetector(inst);

            // Adiciona na janela e na arvore se estiver montada
            DSEW.add(inst);
            if (knnWasSet)
                search.update(inst);

            // Se estiver cheio a janela, remover e tirar da arvore a instancia removida
            if (DSEW.size() > windowSize.getValue()) {
                Instance firstInstance = DSEW.get(0);
                DSEW.delete(0);
                search.removeInstance(firstInstance);
            }

            if (search.isToRebuild()) {
                knnWasSet = false;
                rebuildTree();
            }

            if (driftDetector.getWarningZone() && warning == false) {
                warning = true;
                warningLevel = instanceCount;
            }

            if (poolClassifiers.size() == 0 || this.isChangeDetected())
                addNewIncrementalClassifier(inst);
            else
                updateIncADES(inst);

            if (this.changeWasDetected && (this.warningLevel != this.instanceCount)) {
				resetDetector();
				shrinkDSEW();
                rebuildTree();
                // Reset control atributes
				warning = false;
				warningLevel = 0;
				changeWasDetected = false;
				trainingCount = 0;
			}

            if (!driftDetector.getWarningZone() && this.warning == true) {
				this.warning = false;
				this.warningLevel = 0;
			}

            trainingCount++;
            instanceCount++;

        } catch (Exception e) {
			throw new RuntimeException(e);
		}
    }

    @Override
    public void setModelContext(InstancesHeader ih) {
        super.setModelContext(ih);
        this.header = ih;
    }

    @Override
    protected Measurement[] getModelMeasurementsImpl() {
        return new Measurement[0];
    }

    private MeasuredClassifier lastClassifier() {
        return this.poolClassifiers.get(this.poolClassifiers.size() - 1);
    }

    private void updateLastClassifier(Instance instance) throws Exception {
		MeasuredClassifier lastClassifier = lastClassifier();
		lastClassifier.trainOnInstance(instance);
    }

    protected void updateIncADES(Instance instance) throws Exception {
		if (trainingCount >= trainingSize.getValue()) {
			addNewIncrementalClassifier(instance);
			this.trainingCount = 0;
		} else {
			this.updateLastClassifier(instance);
		}
	}

    private void shrinkDSEW() {

		int diff = instanceCount - warningLevel;
		if (diff < 5 || diff == instanceCount)
			diff = 5;

		while (DSEW.size() > diff) {
			DSEW.delete(0);
		}
	}

    private void createInstanceKDTRee() {
        // Fiz isso por conta que vou adicionar mais metodos depois
        if (this.nearestNeighbourSearchOption.getChosenIndex()== 0) {
            search = new KDTreeCanberra();
        } else {
            search = new KDTreeCanberra();
        }
    }

    private void rebuildTree() {
        try {
            createInstanceKDTRee();
            search.setInstances(DSEW);
            updateNNSearch = false;
            knnWasSet = true;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private boolean isChangeDetected() {
		if (driftDetector.getChange()) {
			changeWasDetected = true;
			return true;
		}
		return false;
	}

    private void updateDetector(Instance instance) {
		if (poolClassifiers.size() > 0) {
            Boolean predictionCorrect = super.correctlyClassifies(instance);
			this.driftDetector.input(predictionCorrect ? 0 : 1);
		}
	}

    private void resetDetector() {
		if (driftDetector != null)
			driftDetector.resetLearning();
	}

    private void addNewIncrementalClassifier(Instance instance) {
        Classifier newClassifier = defaultClassifier;
        newClassifier.trainOnInstance(instance);

        MeasuredClassifier measuredClassifier = new MeasuredClassifier(newClassifier);
        AgeBasedPruningEngine engine = new AgeBasedPruningEngine();
        List<MeasuredClassifier> classifiersToPrune = engine.pruneClassifiers(measuredClassifier, poolClassifiers);

        for(MeasuredClassifier ic : classifiersToPrune ){
			if(ic != measuredClassifier){
				this.pruneClassifier(ic);
			}else{
				measuredClassifier = null;
			}
		}

		if(measuredClassifier != null){
			this.poolClassifiers.add(measuredClassifier);
		}

		this.trainingCount = 0;
    }

    private void pruneClassifier(MeasuredClassifier classifier) {
        poolClassifiers.remove(classifier);
    }

    @Override
    public void getModelDescription(StringBuilder out, int indent) {
        StringUtils.appendIndented(out, indent, "IncA-DES: An incremental and adaptive dynamic ensemble selection");
        StringUtils.appendNewline(out);
    }

    @Override
    public String getPurposeString() {
        return "IncA-DES: An incremental and adaptive dynamic ensemble selection\n" + //
            "approach using online K-d tree neighborhood search for data streams with\n" + //
            "concept drift";
    }
}

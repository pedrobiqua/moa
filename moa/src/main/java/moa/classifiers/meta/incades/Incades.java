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

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

import com.github.javacliparser.IntOption;
import com.github.javacliparser.MultiChoiceOption;
import com.yahoo.labs.samoa.instances.Attribute;
import com.yahoo.labs.samoa.instances.Instance;
import com.yahoo.labs.samoa.instances.Instances;
import com.yahoo.labs.samoa.instances.InstancesHeader;

import moa.classifiers.AbstractClassifier;
import moa.classifiers.Classifier;
import moa.classifiers.MultiClassClassifier;
import moa.classifiers.core.driftdetection.ChangeDetector;
import moa.classifiers.lazy.neighboursearch.KDTreeCanberra;
// import moa.classifiers.lazy.neighboursearch.kdtrees.StreamNeighborSearch;
import moa.classifiers.meta.incades.dynamicselection.KNORAEliminate;
import moa.classifiers.meta.incades.prunningengine.AgeBasedPruningEngine;
import moa.classifiers.meta.incades.prunningengine.MeasuredClassifier;
import moa.classifiers.trees.HoeffdingTree;
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

    public static class AttributesUtils {
        public static List<Attribute> copyAtributes(Instances originalDataset){
            List<Attribute> atributos = new ArrayList<Attribute>(originalDataset.numAttributes());
            for(int i =0; i < originalDataset.numAttributes(); i++){
                    Attribute atribOriginal = originalDataset.attribute(i);
                    atributos.add(atribOriginal);
            }
            return atributos;
        }

        public static ArrayList<Attribute> copyAtributes(Instance instance){
            ArrayList<Attribute> atributes = new ArrayList<Attribute>(instance.numAttributes());
            for(int i =0; i < instance.numAttributes(); i++){
                    Attribute atribOriginal = instance.attribute(i);
                    atributes.add(atribOriginal);
            }
            return atributes;
        }
    }

    public static class InstancesUtils {

        public static Instances gerarDataset(List<Instance> instancias, String nomeDataset) throws Exception{
            ArrayList<Attribute> atributos = AttributesUtils.copyAtributes(instancias.get(0));

            Instances retorno = new Instances(nomeDataset, atributos,
                    instancias.size());
            for(Instance inst : instancias){
                retorno.add(inst);
            }

            retorno.setClassIndex(retorno.numAttributes() - 1);

            return retorno;
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

    // Prunning
    AgeBasedPruningEngine engine = new AgeBasedPruningEngine(75);

    // Instances
    // private InstancesHeader header;
    private LinkedList<Instance> DSEW = new LinkedList<Instance>();

    // Classifiers
    private List<MeasuredClassifier> poolClassifiers = new LinkedList<MeasuredClassifier>();

    // Tree
    private KDTreeCanberra search;

    // DS
    private KNORAEliminate knorae = new KNORAEliminate();

    // Options GUI
    public ClassOption driftDetectionMethodOption = new ClassOption(
        "driftDetectionMethod", 'd',
        "Drift detection method to use.",
        ChangeDetector.class, "RDDM"
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

            Instances neighborhood = search.kNearestNeighbours(inst, numNeighbors);

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
            if (search == null) {
                createInstanceKDTRee();
            }

            // Atualiza a mudança de conceito
            if (instanceCount > TRAINING_SIZE)
                updateDetector(inst);

            // Adiciona na janela e na arvore se estiver montada
            Instance removedInstance = null;
			this.DSEW.addLast(inst);

            if (knnWasSet)
                search.update(inst);

            // Se estiver cheio a janela, remover e tirar da arvore a instancia removida
            if (DSEW.size() > windowSize.getValue()) {
                removedInstance = DSEW.getFirst();
				DSEW.removeFirst();
                search.removeInstance(removedInstance);
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

    // @Override
    // public void setModelContext(InstancesHeader ih) {
    //     super.setModelContext(ih);
    //     this.header = ih;
    // }

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
			DSEW.removeFirst();
		}
	}

    private void createInstanceKDTRee() {
        // Fiz isso por conta que vou adicionar mais metodos depois
        // if (this.nearestNeighbourSearchOption.getChosenIndex()== 0) {
        //     search = new KDTreeCanberra();
        // } else {
        //     search = new KDTreeCanberra();
        // }
        search = new KDTreeCanberra();
    }

    private void rebuildTree() {
        try {
            createInstanceKDTRee();
            // Antes eu estava fazendo assim
            // Aprendi da pior maneira o porque estava usando LinkedList ao inves do Instances
            // search.setInstances(DSEW);
            search.setInstances(InstancesUtils.gerarDataset(DSEW, "Validation Instances"));
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
            if (super.correctlyClassifies(instance)) {
				driftDetector.input(0);
			} else {
				driftDetector.input(1);
			}
		}
	}

    private void resetDetector() {
		if (driftDetector != null)
			driftDetector.resetLearning();
	}

    private void addNewIncrementalClassifier(Instance instance) {
        HoeffdingTree newClassifier = new HoeffdingTree();
        newClassifier.prepareForUse();
        newClassifier.trainOnInstance(instance);

        MeasuredClassifier measuredClassifier = new MeasuredClassifier(newClassifier);
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

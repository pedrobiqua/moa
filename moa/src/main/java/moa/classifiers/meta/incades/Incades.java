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

import com.github.javacliparser.IntOption;
import com.github.javacliparser.MultiChoiceOption;
import com.yahoo.labs.samoa.instances.Instance;
import com.yahoo.labs.samoa.instances.Instances;
import com.yahoo.labs.samoa.instances.InstancesHeader;

import moa.classifiers.AbstractClassifier;
import moa.classifiers.Classifier;
import moa.classifiers.MultiClassClassifier;
import moa.classifiers.core.driftdetection.ChangeDetector;
import moa.classifiers.lazy.neighboursearch.KDTree;
import moa.classifiers.lazy.neighboursearch.KDTreeCanberra;
import moa.classifiers.lazy.neighboursearch.LinearNNSearch;
import moa.classifiers.lazy.neighboursearch.NearestNeighbourSearch;
import moa.classifiers.meta.incades.dynamicselection.KNORAEliminate;
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

    // TODO: Revisar o Prunning Engine
    // TODO: Revisar o KDTree para ver se está tudo certo
    //    -> Refatorar o KDTree para usar as funções basicas de todos os buscadores knn e 1nn
    // TODO: Falta fazer a montagem da arvore, pois nem sempre que vou precisar dela

    // Classe que verifica o Overlap
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

    // Control parameters
    private int trainingCount;
    private int instanceCount;
	private boolean warning = false;
    private int warningLevel = 0;

    // Instances
    private InstancesHeader header;
    private Instances DSEW;

    // Classifiers
    private List<Classifier> poolClassifiers = new LinkedList<Classifier>();

    // Tree
    private NearestNeighbourSearch search;
    private int numNeighbors = 5;

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

    public MultiChoiceOption nearestNeighbourSearchOption = new MultiChoiceOption(
    "nearestNeighbourSearch", 'n', "Nearest Neighbour Search to use", new String[]{
        "LinearNN", "KDTree", "KDTreeCanberra"},
    new String[]{"Brute force search algorithm for nearest neighbour search. ",
        "KDTree search algorithm for nearest neighbour search",
        "KDtree search algorithm for nearest neighbour search using Canberra Distance"
    }, 0);

    public IntOption windowSize = new IntOption(
        "windowSize", 'p',
        "Window size parameter",
        10, 2, 200
    );

    public IntOption trainingSize = new IntOption(
        "TrainingSize", 't', "Training size parameter", 10, 10, 200
    );

    private ChangeDetector driftDetector;
    private Classifier defaultClassifier;

    @Override
    public boolean isRandomizable() {
        return false;
    }

    @Override
    public double[] getVotesForInstance(Instance inst) {
        // COLOCAR AQUI A PARTE DE PREDIÇÃO DA CLASSE
        // Pegar a Roc
        Instances neighborhood;
        try {
            // TODO: AQUI AINDA ESTÁ ERRADO POIS NÃO ESTOU VALIDANDO SE A ARVORE ESTÁ MONTADA
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
            for (int i = 0; i < poolClassifiers.size() - 1; i++) {
                classifiers[i] = poolClassifiers.get(i);
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
        this.driftDetector = (ChangeDetector) getPreparedClassOption(this.driftDetectionMethodOption);
        // Inicialize default classifier
        this.defaultClassifier = (Classifier) getPreparedClassOption(this.classifierOption);
        this.poolClassifiers.add(defaultClassifier);
    }

    @Override
    public void trainOnInstanceImpl(Instance inst) {

        if (DSEW == null) {
            this.DSEW = new Instances(this.header, 0);
        }

        // Approach for searching instances
        if (search == null) {
            createKDTRee();
        }

        // Atualiza a mudança de conceito
        Boolean predictionCorrect = this.lastClassifier().correctlyClassifies(inst);
        this.driftDetector.input(predictionCorrect ? 0 : 1);

        Boolean driftIsTrue = false;
        if (this.driftDetector.getChange()){
            driftIsTrue = true;
        }

        DSEW.add(inst);

        if (this.driftDetector.getWarningZone() && this.warning == false) {
            this.warning = true;
            this.warningLevel = this.instanceCount;
		}

        if (DSEW.size() > windowSize.getValue()) {
            DSEW.delete(0);
        }
        // Control classifiers
        if (driftIsTrue) {
            shrinkDSEW();
            //      𝐶𝑘 ← a new classifier
            poolClassifiers.add(defaultClassifier);
            //      𝑝𝑟𝑢𝑛𝑒(𝐶, 𝐷𝑆𝐸𝑊 , 𝐶𝑘−1 , 𝐷) // Remove um classificador com base no metodo de poda
            poolClassifiers.remove(0); // POR ENQUANTO ESTOU REMOVENDO O PRIMEIRO, SEI QUE NÃO É
            //      𝐶 ← 𝐶 ∪ 𝐶𝑘
        }

        if (trainingCount >= trainingSize.getValue()) {
            //      𝐶𝑘 ← a new classifier
            poolClassifiers.add(defaultClassifier);
            //      𝑝𝑟𝑢𝑛𝑒(𝐶, 𝐷𝑆𝐸𝑊 , 𝐶𝑘−1 , 𝐷) // Remove um classificador com base no metodo de poda
            poolClassifiers.remove(0); // POR ENQUANTO ESTOU REMOVENDO O PRIMEIRO, SEI QUE NÃO É
            //      𝐶 ← 𝐶 ∪ 𝐶𝑘
            trainingCount = 0;
        }
        this.lastClassifier().trainOnInstance(inst);
        trainingCount++;
		instanceCount++;
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

    private Classifier lastClassifier() {
        return this.poolClassifiers.get(this.poolClassifiers.size() - 1);
    }

    private void shrinkDSEW() {

		int diff = instanceCount - warningLevel;
		if (diff < 5 || diff == instanceCount)
			diff = 5;

		while (DSEW.size() > diff) {
			DSEW.delete(0);
		}
	}

    private void createKDTRee() {
        if (this.nearestNeighbourSearchOption.getChosenIndex()== 0) {
            search = new LinearNNSearch();
        } else if (this.nearestNeighbourSearchOption.getChosenIndex()== 1) {
            search = new KDTree();
        } else {
            search = new KDTreeCanberra();
        }
    }

    @Override
    public void getModelDescription(StringBuilder out, int indent) {
        StringUtils.appendIndented(out, indent, "Dummy Incades Classifier (sempre prediz classe 0)");
        StringUtils.appendNewline(out);
    }

    @Override
    public String getPurposeString() {
        return "IncA-DES: An incremental and adaptive dynamic ensemble selection\n" + //
            "approach using online K-d tree neighborhood search for data streams with\n" + //
            "concept drift";
    }
}

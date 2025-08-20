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

import com.github.javacliparser.IntOption;
import com.yahoo.labs.samoa.instances.Instance;

import moa.classifiers.AbstractClassifier;
import moa.classifiers.Classifier;
import moa.classifiers.MultiClassClassifier;
import moa.classifiers.core.driftdetection.ChangeDetector;
import moa.core.Measurement;
import moa.core.StringUtils;
import moa.options.ClassOption;

/**
 * IncA-DES
 *
 *  IncA-DES: An incremental and adaptive dynamic ensemble selection
 *  approach using online K-d tree neighborhood search for data streams with
 *  concept drift
 *  <p>Eduardo V.L. Barboza, Paulo R. Lisboa de Almeida, Alceu de Souza Britto Jr., Robert Sabourin, Rafael M.O. Cruz
 *  </p>
 *
 * @author Pedro Bianchini de Quadros (pedro.bianchini@ufpr.br)
 * @version $Revision: 1 $
 */
public class Incades extends AbstractClassifier implements MultiClassClassifier {

    private static final long serialVersionUID = 1L;

    public ClassOption driftDetectionMethodOption = new ClassOption(
        "driftDetectionMethod", 'd',
        "Drift detection method to use.",
        ChangeDetector.class, "DDM"
    );

    public ClassOption classifierOption = new ClassOption(
        "classifier", 'l',
        "Classifier method to use.",
        Classifier.class, "trees.HoeffdingTree"
    );

    public IntOption windowSize = new IntOption(
        "windowSize", 'p',
        "Window size parameter",
        10, 2, 200
    );

    private ChangeDetector driftDetector;
    private Classifier classifier; // Não vai ser assim, vou ter que montar a pool de classificadores

    public Incades() {
        this.driftDetector = (ChangeDetector) getPreparedClassOption(this.driftDetectionMethodOption);
        this.classifier = (Classifier) getPreparedClassOption(this.classifierOption);
    }


    @Override
    public boolean isRandomizable() {
        return false;
    }

    @Override
    public double[] getVotesForInstance(Instance inst) {
        // Sempre retorna 100% de probabilidade na classe 0 (dummy)
        double[] votes = new double[inst.numClasses()];
        if (votes.length > 0) {
            votes[0] = 1.0;
        }
        return votes;
    }

    @Override
    public void resetLearningImpl() {
        // Nada a resetar no dummy
    }

    @Override
    public void trainOnInstanceImpl(Instance inst) {
        // Algoritmo simplificado
        // 1. Atualiza a mudança de conceito
        Boolean predictionCorrect = this.classifier.correctlyClassifies(inst);
        this.driftDetector.input(predictionCorrect ? 0 : 1);

        Boolean driftIsTrue = false;
        if (this.driftDetector.getChange()){
            driftIsTrue = true;
        }
        // 2. 𝐷𝑆𝐸𝑊 ← 𝐷𝑆𝐸𝑊 ∪ 𝐼 ; // Adiciona no DSEW
        // Aqui vou ter que montar a parte da arvore

        // 3. Se o tamanho da janela for maior que > W
        //      𝑟𝑒𝑚𝑜𝑣𝑒𝑂𝑙𝑑𝑒𝑠𝑡𝐼𝑛𝑠𝑡𝑎𝑛𝑐𝑒(𝐷𝑆𝐸𝑊 ) ; // Remove a instancia mais velha
        // 4. Se o concept drift for detectado
        if (driftIsTrue) {

        }
        //      Reduzir o 𝐷𝑆𝐸𝑊
        //      𝐶𝑘 ← a new classifier
        //      𝑝𝑟𝑢𝑛𝑒(𝐶, 𝐷𝑆𝐸𝑊 , 𝐶𝑘−1 , 𝐷) // Remove um classificador com base no metodo de poda
        //      𝐶 ← 𝐶 ∪ 𝐶𝑘
        // 5. Se o 𝐶𝑘 já foi treinado com 𝐹 instancias
        //      𝐶𝑘 ← a new classifier
        //      𝑝𝑟𝑢𝑛𝑒(𝐶, 𝐷𝑆𝐸𝑊 , 𝐶𝑘−1 , 𝐷) // Remove um classificador com base no metodo de poda
        //      𝐶 ← 𝐶 ∪ 𝐶𝑘
        // 6. 𝐶𝑘 ← 𝑙𝑎𝑡𝑒𝑠𝑡𝐶𝑙𝑎𝑠𝑠𝑖𝑓𝑖𝑒𝑟𝐴𝑣𝑎𝑖𝑙𝑎𝑏𝑙𝑒(𝐶) ; // Pega o ultimo classificador disponivel
        // 7. 𝑡𝑟𝑎𝑖𝑛(𝐶𝑘 , 𝐼) ; // Treina 𝐶𝑘 na instancia de treinamento
        this.classifier.trainOnInstance(inst);
    }

    @Override
    protected Measurement[] getModelMeasurementsImpl() {
        return new Measurement[0];
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

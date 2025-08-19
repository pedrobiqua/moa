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
import moa.classifiers.MultiClassClassifier;
import moa.core.Measurement;
import moa.core.StringUtils;

/**
 * IncA-DES
 *
 * An incremental and adaptive dynamic ensemble selection
 * approach using online K-d tree neighborhood search for data streams with
 * concept drift
 *
 * @author Pedro Bianchini de Quadros (pedro.bianchini@ufpr.br)
 * @version $Revision: 1 $
 */
public class Incades extends AbstractClassifier implements MultiClassClassifier {

    private static final long serialVersionUID = 1L;

    // Opção só para ver no GUI
    public IntOption parametroExemploOption = new IntOption(
            "parametroExemplo", 'p',
            "Descrição do parâmetro que aparecerá no GUI.",
            10, 1, 100);

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
        // Não faz nada - classificador dummy
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
        return "Incades Dummy: classificador de teste que sempre prediz a classe 0.";
    }
}

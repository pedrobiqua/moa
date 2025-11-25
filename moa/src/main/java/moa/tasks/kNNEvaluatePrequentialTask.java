package moa.tasks;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;

import com.github.javacliparser.FileOption;
import com.github.javacliparser.IntOption;
import moa.capabilities.CapabilitiesHandler;
import moa.capabilities.Capability;
import moa.capabilities.ImmutableCapabilities;
import moa.classifiers.MultiClassClassifier;
import moa.core.Example;
import moa.core.Measurement;
import moa.core.ObjectRepository;
import moa.core.TimingUtils;
import moa.core.Utils;
import moa.evaluation.EWMAClassificationPerformanceEvaluator;
import moa.evaluation.FadingFactorClassificationPerformanceEvaluator;
import moa.evaluation.LearningEvaluation;
import moa.evaluation.LearningPerformanceEvaluator;
import moa.evaluation.WindowClassificationPerformanceEvaluator;
import moa.evaluation.preview.LearningCurve;
import moa.learners.Learner;
import moa.options.ClassOption;
import moa.streams.ExampleStream;
import com.yahoo.labs.samoa.instances.Instance;

/**
 * Task for evaluating a static model on a stream.
 *
 * @author Richard Kirkby (rkirkby@cs.waikato.ac.nz)
 * @version $Revision: 7 $
 */
public class kNNEvaluatePrequentialTask extends ClassificationMainTask implements CapabilitiesHandler {

        @Override
        public String getPurposeString() {
                return "Evaluates a classifier on a stream by testing then training with each example in sequence.";
        }

        private static final long serialVersionUID = 1L;

        public ClassOption learnerOption = new ClassOption("learner", 'l',
                        "Learner to train.", MultiClassClassifier.class, "moa.classifiers.lazy.kNNSimple");

        public ClassOption evaluatorOption = new ClassOption("evaluator", 'e',
                        "Classification performance evaluation method.",
                        LearningPerformanceEvaluator.class,
                        "WindowClassificationPerformanceEvaluator");

        public ClassOption streamOption = new ClassOption("stream", 's',
                        "Stream to evaluate on.", ExampleStream.class,
                        "generators.RandomTreeGenerator");

        public FileOption outputTempFileOption = new FileOption("outputTempFile", 'o',
                        "File to append output temp train and test to.", null, ".txt", true);

        @Override
        public Class<?> getTaskResultType() {
                return LearningEvaluation.class;
        }

        @SuppressWarnings({ "rawtypes", "unchecked" })
        @Override
        public Object doMainTask(TaskMonitor monitor, ObjectRepository repository) {
                Learner learner = (Learner) getPreparedClassOption(this.learnerOption);
                System.out.println(learner.getClass().getName());
                ExampleStream stream = (ExampleStream) getPreparedClassOption(this.streamOption);
                LearningPerformanceEvaluator evaluator = (LearningPerformanceEvaluator) getPreparedClassOption(
                                this.evaluatorOption);
                LearningCurve learningCurve = new LearningCurve(
                                "learning evaluation instances");

                learner.setModelContext(stream.getHeader());
                long instancesProcessed = 0;
                monitor.setCurrentActivity("Evaluating learner...", -1.0);

                // File for output temps
                File outputTempFile = this.outputTempFileOption.getFile();
                PrintStream outputPredictionResultStream = null;
                if (outputTempFile != null) {
                        try {
                                if (outputTempFile.exists()) {
                                        outputPredictionResultStream = new PrintStream(
                                                        new FileOutputStream(outputTempFile, true), true);
                                } else {
                                        outputPredictionResultStream = new PrintStream(
                                                        new FileOutputStream(outputTempFile), true);
                                }
                        } catch (Exception ex) {
                                throw new RuntimeException(
                                                "Unable to open prediction result file: " + outputTempFile, ex);
                        }
                }

                boolean preciseCPUTiming = TimingUtils.enablePreciseTiming();

                if (outputTempFile != null) {
                        outputPredictionResultStream.println("instanceProcessed, pred_temp, train_temp");
                }

                while (stream.hasMoreInstances()) {
                        Example trainInst = stream.nextInstance();
                        Example testInst = (Example) trainInst;
                        long startPred = TimingUtils.getNanoCPUTimeOfCurrentThread();
                        double[] prediction = learner.getVotesForInstance(testInst);
                        long endPred = TimingUtils.getNanoCPUTimeOfCurrentThread();

                        evaluator.addResult(testInst, prediction);
                        long startTrain = TimingUtils.getNanoCPUTimeOfCurrentThread();
                        learner.trainOnInstance(trainInst);
                        long endTrain = TimingUtils.getNanoCPUTimeOfCurrentThread();

                        // Output prediction
                        if (outputTempFile != null) {
                                outputPredictionResultStream.println(
                                                instancesProcessed + ", " +
                                                                (endPred - startPred) + ", " +
                                                                (endTrain - startTrain));
                        }

                        instancesProcessed++;
                }
                return learningCurve;
        }

        @Override
        public ImmutableCapabilities defineImmutableCapabilities() {
                if (this.getClass() == kNNEvaluatePrequentialTask.class)
                        return new ImmutableCapabilities(Capability.VIEW_STANDARD, Capability.VIEW_LITE);
                else
                        return new ImmutableCapabilities(Capability.VIEW_STANDARD);
        }
}

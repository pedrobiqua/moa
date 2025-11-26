package moa.tasks;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;

import com.github.javacliparser.FileOption;
import com.yahoo.labs.samoa.instances.Instance;

import moa.capabilities.CapabilitiesHandler;
import moa.capabilities.Capability;
import moa.capabilities.ImmutableCapabilities;
import moa.classifiers.MultiClassClassifier;
import moa.classifiers.lazy.kNNSimple;
import moa.core.Example;
import moa.core.ObjectRepository;
import moa.core.TimingUtils;
import moa.core.Utils;
import moa.evaluation.LearningEvaluation;
import moa.evaluation.LearningPerformanceEvaluator;
import moa.evaluation.preview.LearningCurve;
import moa.learners.Learner;
import moa.options.ClassOption;
import moa.streams.ExampleStream;

public class kNNEvaluatePrequentialTask extends ClassificationMainTask implements CapabilitiesHandler {

    @Override
    public String getPurposeString() {
        return "Evaluates a classifier on a stream by testing then training with each example in sequence.";
    }

    private static final long serialVersionUID = 1L;

    public ClassOption streamOption = new ClassOption("stream", 's',
            "Stream to evaluate on.", ExampleStream.class,
            "generators.RandomTreeGenerator");

    public FileOption outputTempFileOption = new FileOption("outputTempFile", 'o',
            "File to append output temp train and test to.", null, ".txt", true);

    public FileOption outputKDTreeTempFileOption = new FileOption("outputKDTreeTempFile", 'd',
            "File to append output temp train and test to.", null, ".txt", true);

    @Override
    public Class<?> getTaskResultType() {
        return LearningEvaluation.class;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    public Object doMainTask(TaskMonitor monitor, ObjectRepository repository) {

        ExampleStream stream = (ExampleStream) getPreparedClassOption(this.streamOption);
        LearningCurve learningCurve = new LearningCurve(
                "learning evaluation instances");

        long instancesProcessed = 0;
        monitor.setCurrentActivity("Evaluating learner...", -1.0);

        // File for output temps
        File outputTempFile = this.outputTempFileOption.getFile();
        PrintStream outputPredictionResultStream = null;
        if (outputTempFile != null) {
            try {
                if (outputTempFile.exists()) {
                    outputPredictionResultStream = new PrintStream(
                            new FileOutputStream(outputTempFile, false), true);
                } else {
                    outputPredictionResultStream = new PrintStream(
                            new FileOutputStream(outputTempFile), true);
                }
            } catch (Exception ex) {
                throw new RuntimeException(
                        "Unable to open prediction result file: " + outputTempFile, ex);
            }
        }

        // OUTPUT DOS TEMPOS DO KDTREE
        File outputKDTreeTemp = this.outputKDTreeTempFileOption.getFile();
        PrintStream outputKDTreeTempResult = null;
        if (outputKDTreeTemp != null) {
            try {
                if (outputKDTreeTemp.exists()) {
                    outputKDTreeTempResult = new PrintStream(
                            new FileOutputStream(outputKDTreeTemp, false), true);
                } else {
                    outputKDTreeTempResult = new PrintStream(
                            new FileOutputStream(outputKDTreeTemp), true);
                }
            } catch (Exception ex) {
                throw new RuntimeException(
                        "Unable to open prediction result file: " + outputTempFile, ex);
            }
        }

        boolean preciseCPUTiming = TimingUtils.enablePreciseTiming();

        if (outputTempFile != null) {
            outputPredictionResultStream.println("instance,busca,insert,pred,true_class,altura_arvore");
        }

        kNNSimple learner;
        if (outputKDTreeTempResult != null)
            learner = new kNNSimple(outputKDTreeTempResult);
        else
            learner = new kNNSimple();

        learner.setModelContext(stream.getHeader());

        while (stream.hasMoreInstances()) {
            Example trainInst = stream.nextInstance();
            Example testInst = (Example) trainInst;
            long startPred = TimingUtils.getNanoCPUTimeOfCurrentThread();
            double[] prediction = learner.getVotesForInstance(testInst);
            long endPred = TimingUtils.getNanoCPUTimeOfCurrentThread();

            int pred = Utils.maxIndex(prediction);
            int trueClass = (int) ((Instance) trainInst.getData()).classValue();

            long startTrain = TimingUtils.getNanoCPUTimeOfCurrentThread();
            learner.trainOnInstance(trainInst);
            int altura_arvore = learner.getSearch().getHeightTree();
            long endTrain = TimingUtils.getNanoCPUTimeOfCurrentThread();

            double predTime = TimingUtils.nanoTimeToSeconds(endPred - startPred);
            double trainTime = TimingUtils.nanoTimeToSeconds(endTrain - startTrain);

            // Output TREINO E TESTE TEMPO
            if (instancesProcessed % 10 == 0) {
                if (outputTempFile != null) {
                    outputPredictionResultStream.println(
                            instancesProcessed + "," +
                                    predTime + "," +
                                    trainTime + "," +
                                    pred + "," +
                                    trueClass + "," +
                                    altura_arvore);
                }
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

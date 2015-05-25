/*
 * Copyright (c) 2014 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.venkman.rules;

import junit.framework.Assert;
import org.janelia.it.venkman.TestUtilities;
import org.janelia.it.venkman.config.rules.BehaviorLimitedKinematicVariableFunctionList;
import org.janelia.it.venkman.config.rules.IntensityValue;
import org.janelia.it.venkman.config.rules.LEDFlashPattern;
import org.janelia.it.venkman.config.rules.LEDStimulus;
import org.janelia.it.venkman.config.rules.NoiseGenerator;
import org.janelia.it.venkman.config.rules.OutOfRangeErrorHandlingMethod;
import org.janelia.it.venkman.config.rules.SingleVariableFunction;
import org.janelia.it.venkman.data.LarvaBehaviorMode;
import org.janelia.it.venkman.data.LarvaFrameData;
import org.janelia.it.venkman.log.StandardOutLogger;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.janelia.it.venkman.rules.ScaledRunIntensity.DEFAULT_NON_RUN_INTENSITY_VALUE;


/**
 * Tests the {@link ScaledRunIntensity} and
 * {@link ScaledRunIntensityWithRandomDelay} classes.
 *
 * @author Eric Trautman
 */
public class ScaledRunIntensityTest {

    private static final double[] SCALING_FACTORS = {
            2.0, 3.0, 4.0, 5.0, 6.0
    };
    private static final LEDFlashPattern DURATION = new LEDFlashPattern("33");

    private ScaledRunIntensity rules;
    private IntensityValue runIntensityPercentage;
    private long millisecondsDelay;
    private SingleVariableFunction svf;
    private double expectedIntensity;
    private long captureTime;
    private List<LarvaFrameData> frameHistory;

    @Before
    public void setUp() {
        runIntensityPercentage = new IntensityValue(20.0);
        millisecondsDelay = 45;
        svf = new SingleVariableFunction(0,
                                         60,
                                         OutOfRangeErrorHandlingMethod.END_SESSION_FOR_MINIMUM_AND_MAXIMUM,
                                         SCALING_FACTORS);
        expectedIntensity = runIntensityPercentage.getValue();
        captureTime = -30;
        frameHistory = new ArrayList<LarvaFrameData>();
    }

    @Test
    public void testDetermineStimulus() {

        rules = new ScaledRunIntensity(DURATION,
                                       DEFAULT_NON_RUN_INTENSITY_VALUE,
                                       0.0,
                                       runIntensityPercentage,
                                       millisecondsDelay,
                                       svf,
                                       0.0,
                                       false,
                                       0,
                                       new SingleVariableFunction(),
                                       0.0,
                                       new BehaviorLimitedKinematicVariableFunctionList());
        rules.init(new StandardOutLogger());

        validateStimulusForContinuousRun();

        addFrame(LarvaBehaviorMode.TURN_LEFT);
        final List<LEDStimulus> stimulusList =
                rules.determineStimulus(frameHistory, null);
        Assert.assertEquals("invalid stimulus list returned for turn: " +
                            stimulusList,
                            1, stimulusList.size());
        TestUtilities.assertEquals(
                "invalid stimulus intensity returned for turn: " +
                stimulusList,
                0.0,
                stimulusList.get(0).getIntensityPercentage());

        // new run after turn should re-start scaling
        validateStimulusForContinuousRun();
    }

    @Test
    public void testDetermineStimulusWithRandomFunction() {

        final double primaryFactor = 2.0;
        SingleVariableFunction primaryFunction =
                new SingleVariableFunction(0,
                                           10000,
                                           OutOfRangeErrorHandlingMethod.END_SESSION_FOR_MINIMUM_AND_MAXIMUM,
                                           new double[] {primaryFactor});

        final double alternateFactor = 0.5;
        SingleVariableFunction alternateFunction =
                new SingleVariableFunction(0,
                                           10000,
                                           OutOfRangeErrorHandlingMethod.END_SESSION_FOR_MINIMUM_AND_MAXIMUM,
                                           new double[] {alternateFactor});
        rules = new ScaledRunIntensity(DURATION,
                                       DEFAULT_NON_RUN_INTENSITY_VALUE,
                                       0.0,
                                       runIntensityPercentage,
                                       0,
                                       primaryFunction,
                                       0.0,
                                       true,
                                       0,
                                       alternateFunction,
                                       0.0,
                                       new BehaviorLimitedKinematicVariableFunctionList());
        rules.init(new StandardOutLogger());

        final double baseIntensity =
                runIntensityPercentage.getValueWithoutNoise();

        LEDStimulus stimulus = addFrameAndGetStimulus(LarvaBehaviorMode.RUN);
        double value = stimulus.getIntensityPercentage();
        final boolean firstFunctionIsRandom = (value < baseIntensity);
        validateSameFunctionUsedForRun(firstFunctionIsRandom);

        // 10 tries should be plenty to trigger random function change
        boolean functionChanged = false;
        for (int i = 0; i < 10; i++) {
            addFrameAndGetStimulus(LarvaBehaviorMode.TURN_LEFT);
            stimulus = addFrameAndGetStimulus(LarvaBehaviorMode.RUN);
            value = stimulus.getIntensityPercentage();
            if (firstFunctionIsRandom) {
                functionChanged = (value > baseIntensity);
            } else {
                functionChanged = (value < baseIntensity);
            }
            if (functionChanged) {
                System.out.println("\nfunction changed after " + i +
                                   " attempt(s)\n");
                break;
            }
        }

        Assert.assertTrue("function never changed after 10 attempts " +
                          "(possible but unlikely so try again)",
                          functionChanged);

        validateSameFunctionUsedForRun(!firstFunctionIsRandom);
    }

    @Test
    public void testDetermineStimulusWithRandomFunctionPersisted() {

        final int numberOfFrames =
                1 +       // first run
                (10 * 2); // loop 10 times through turn and run
        final long persistenceTime =
                numberOfFrames * 30; // should be 30 more than needed

        final double primaryFactor = 2.0;
        SingleVariableFunction primaryFunction =
                new SingleVariableFunction(0,
                                           10000,
                                           OutOfRangeErrorHandlingMethod.END_SESSION_FOR_MINIMUM_AND_MAXIMUM,
                                           new double[] {primaryFactor});

        final double alternateFactor = 0.5;
        SingleVariableFunction alternateFunction =
                new SingleVariableFunction(0,
                                           10000,
                                           OutOfRangeErrorHandlingMethod.END_SESSION_FOR_MINIMUM_AND_MAXIMUM,
                                           new double[] {alternateFactor});
        rules = new ScaledRunIntensity(DURATION,
                                       DEFAULT_NON_RUN_INTENSITY_VALUE,
                                       0.0,
                                       runIntensityPercentage,
                                       0,
                                       primaryFunction,
                                       0.0,
                                       true,
                                       persistenceTime,
                                       alternateFunction,
                                       0.0,
                                       new BehaviorLimitedKinematicVariableFunctionList());
        rules.init(new StandardOutLogger());

        final double baseIntensity =
                runIntensityPercentage.getValueWithoutNoise();

        LEDStimulus stimulus = addFrameAndGetStimulus(LarvaBehaviorMode.RUN);
        double value = stimulus.getIntensityPercentage();
        final boolean firstFunctionIsRandom = (value < baseIntensity);

        // 10 tries should be plenty to trigger random function change
        // (if it is being invoked - which it shouldn't be!)
        boolean functionChanged = false;
        for (int i = 0; i < 10; i++) {
            addFrameAndGetStimulus(LarvaBehaviorMode.TURN_LEFT);
            stimulus = addFrameAndGetStimulus(LarvaBehaviorMode.RUN);
            value = stimulus.getIntensityPercentage();
            if (firstFunctionIsRandom) {
                functionChanged = (value > baseIntensity);
            } else {
                functionChanged = (value < baseIntensity);
            }
            if (functionChanged) {
                System.out.println("\nfunction changed after " + i +
                                   " attempt(s)\n");
                break;
            }
        }

        Assert.assertFalse("function changed but should have persisted, " +
                           "captureTime is " + captureTime,
                           functionChanged);
    }

    @Test
    public void testDetermineStimulusWithNoise() {

        runIntensityPercentage = new IntensityValue(20.0,
                                                    new NoiseGenerator());
        millisecondsDelay = Long.MAX_VALUE;
        rules = new ScaledRunIntensity(DURATION,
                                       DEFAULT_NON_RUN_INTENSITY_VALUE,
                                       0.0,
                                       runIntensityPercentage,
                                       millisecondsDelay,
                                       svf,
                                       0.0,
                                       false,
                                       0,
                                       new SingleVariableFunction(),
                                       0.0,
                                       new BehaviorLimitedKinematicVariableFunctionList());
        rules.init(new StandardOutLogger());

        double previousResult = -1.0;
        double result;
        for (int i = 0; i < 5; i++) {
            addFrame(LarvaBehaviorMode.RUN);
            final List<LEDStimulus> stimulusList =
                    rules.determineStimulus(frameHistory, null);
            final LEDStimulus stimulus = stimulusList.get(0);
            result = stimulus.getIntensityPercentage();
            Assert.assertNotSame("random noise missing from intensity",
                                 result,
                                 previousResult);
            previousResult = result;
            if ((result > 30.0) || (result < 10.0)) {
                Assert.fail("intensity outside of range for random " +
                            "noise factor, result=" + result);
            }
        }
    }

    @Test
    public void testSignalToNoiseRatioWithRandomFunctionSelection() {

        final double primaryFactor = 2.0;
        SingleVariableFunction primaryFunction =
                new SingleVariableFunction(0,
                                           10000,
                                           OutOfRangeErrorHandlingMethod.END_SESSION_FOR_MINIMUM_AND_MAXIMUM,
                                           new double[] {primaryFactor});

        final double alternateFactor = 0.5;
        SingleVariableFunction alternateFunction =
                new SingleVariableFunction(0,
                                           10000,
                                           OutOfRangeErrorHandlingMethod.END_SESSION_FOR_MINIMUM_AND_MAXIMUM,
                                           new double[] {alternateFactor});

        final double runSignalToNoiseRatio = 1.0;
        final long randomFunctionPersistenceDuration = 100;

        rules = new ScaledRunIntensity(DURATION,
                                       DEFAULT_NON_RUN_INTENSITY_VALUE,
                                       0.0,
                                       runIntensityPercentage,
                                       0,
                                       primaryFunction,
                                       runSignalToNoiseRatio,
                                       true,
                                       randomFunctionPersistenceDuration,
                                       alternateFunction,
                                       runSignalToNoiseRatio,
                                       new BehaviorLimitedKinematicVariableFunctionList());
        rules.init(new StandardOutLogger());

        //  30: run (start)
        //  60: run (duration = 30)
        //  90: turn left (SNR set to zero)
        // 120: run (duration = 0, original function reused, original SNR should also be reused)

        addFrameAndGetStimulus(LarvaBehaviorMode.RUN);
        Assert.assertEquals("invalid SNR for run 1, frame 1",
                            runSignalToNoiseRatio, rules.getCurrentSignalToNoiseRatio());

        addFrameAndGetStimulus(LarvaBehaviorMode.RUN);
        Assert.assertEquals("invalid SNR for run 1, frame 2",
                            runSignalToNoiseRatio, rules.getCurrentSignalToNoiseRatio());

        addFrameAndGetStimulus(LarvaBehaviorMode.TURN_LEFT);
        Assert.assertEquals("invalid SNR for turn",
                            0.0, rules.getCurrentSignalToNoiseRatio());

        addFrameAndGetStimulus(LarvaBehaviorMode.RUN);
        Assert.assertEquals("invalid SNR for run 2, frame 1 (SNR should be preserved)",
                            runSignalToNoiseRatio, rules.getCurrentSignalToNoiseRatio());
    }

    @Test
    public void testDetermineStimulusWithSignalToNoiseRatio() {

        final double scaledIntensityWithoutSNR = runIntensityPercentage.getValue() * svf.getValue(0);

        millisecondsDelay = 0;
        rules = new ScaledRunIntensity(DURATION,
                                       DEFAULT_NON_RUN_INTENSITY_VALUE,
                                       0.0,
                                       runIntensityPercentage,
                                       millisecondsDelay,
                                       svf,
                                       scaledIntensityWithoutSNR, // this SNR ensures noise is just N(0,1)
                                       false,
                                       0,
                                       null,
                                       0.0,
                                       new BehaviorLimitedKinematicVariableFunctionList());
        rules.init(new StandardOutLogger());

        addFrame(LarvaBehaviorMode.RUN);

        final List<LEDStimulus> stimulusList = rules.determineStimulus(frameHistory, null);
        final LEDStimulus stimulus = stimulusList.get(0);
        final double result = stimulus.getIntensityPercentage();
        Assert.assertNotSame("signal to noise ratio not applied",
                             result,
                             scaledIntensityWithoutSNR);
        // usage of SNR == base intensity, ensures noise is just N(0,1): -3 < noise < 3
        final double maxNoise = 3.0;
        final double minNoise = -maxNoise;
        final double noise = result - scaledIntensityWithoutSNR;
        if ((noise > maxNoise) || (noise < minNoise)) {
            Assert.fail("noise value of " + noise + " is not between " + minNoise + " and " + maxNoise);
        }
    }

    @Test
    public void testDetermineStimulusWithRandomDelay() {

        final int maxDelay = (int) millisecondsDelay;

        rules = new ScaledRunIntensityWithRandomDelay(DURATION,
                                                      DEFAULT_NON_RUN_INTENSITY_VALUE,
                                                      0.0,
                                                      runIntensityPercentage,
                                                      svf,
                                                      0.0,
                                                      false,
                                                      0,
                                                      new SingleVariableFunction(),
                                                      0.0,
                                                      new BehaviorLimitedKinematicVariableFunctionList(),
                                                      maxDelay);
        rules.init(new StandardOutLogger());

        addFrame(LarvaBehaviorMode.RUN);
        rules.determineStimulus(frameHistory, null);
        final long firstDelay = rules.getMillisecondsDelay();
        Assert.assertTrue("first random delay (" + firstDelay +
                          ") exceeds max (" + maxDelay + ")",
                          firstDelay < maxDelay);

        addFrame(LarvaBehaviorMode.RUN);
        rules.determineStimulus(frameHistory, null);
        Assert.assertEquals("random delay changed during continuous run",
                            firstDelay, rules.getMillisecondsDelay());

        addFrame(LarvaBehaviorMode.TURN_LEFT);
        final List<LEDStimulus> stimulusList =
                rules.determineStimulus(frameHistory, null);
        Assert.assertEquals("invalid stimulus list returned for turn: " +
                            stimulusList,
                            1, stimulusList.size());
        TestUtilities.assertEquals(
                "invalid stimulus intensity returned for turn: " +
                stimulusList,
                0.0,
                stimulusList.get(0).getIntensityPercentage());

        addFrame(LarvaBehaviorMode.RUN);
        rules.determineStimulus(frameHistory, null);
        final long secondDelay = rules.getMillisecondsDelay();
        Assert.assertTrue("second random delay (" + secondDelay +
                          ") exceeds max (" + maxDelay + ")",
                          secondDelay < maxDelay);

        Assert.assertNotSame("first and second random delays are same",
                             firstDelay, secondDelay);
    }

    private void validateStimulusForContinuousRun() {
        expectedIntensity = runIntensityPercentage.getValue();

        validateStimulus(LarvaBehaviorMode.RUN); // 0ms
        validateStimulus(LarvaBehaviorMode.RUN); // 30ms

        expectedIntensity = runIntensityPercentage.getValue() *
                            SCALING_FACTORS[1];
        validateStimulus(LarvaBehaviorMode.RUN); // 60ms (15ms + 45ms)

        expectedIntensity = runIntensityPercentage.getValue() *
                            SCALING_FACTORS[3];
        validateStimulus(LarvaBehaviorMode.RUN); // 90ms (45ms + 45ms)
    }

    private void validateSameFunctionUsedForRun(boolean useRandom) {
        final double baseIntensity =
                runIntensityPercentage.getValueWithoutNoise();
        LEDStimulus stimulus;
        double value;
        for (int i = 0; i < 5; i++) {
            stimulus = addFrameAndGetStimulus(LarvaBehaviorMode.RUN);
            value = stimulus.getIntensityPercentage();
            if (useRandom) {
                if (value > baseIntensity) {
                    Assert.fail("primary function applied prematurely, " +
                                "value is " + value);
                }
            } else if (value < baseIntensity) {
                    Assert.fail("alternate function applied prematurely, " +
                                "value is " + value);
            }
        }
    }

    private void validateStimulus(LarvaBehaviorMode behaviorMode) {
        final LEDStimulus stimulus = addFrameAndGetStimulus(behaviorMode);
        final LEDStimulus expectedStimulus =
                DURATION.getStimulusList(expectedIntensity).get(0);
        final String errorMessage =
                "invalid stimulus returned for " + behaviorMode +
                " (" + captureTime + " ms)";
        Assert.assertEquals(errorMessage,
                            expectedStimulus, stimulus);
    }

    private LEDStimulus addFrameAndGetStimulus(LarvaBehaviorMode behaviorMode) {
        addFrame(behaviorMode);
        final List<LEDStimulus> stimulusList =
                rules.determineStimulus(frameHistory, null);
        Assert.assertEquals("invalid size for returned stimulus list",
                            1, stimulusList.size());
        return stimulusList.get(0);
    }

    private void addFrame(LarvaBehaviorMode behaviorMode) {
        captureTime += 30;
        frameHistory.add(0, TestUtilities.getFrameDataWithTime(captureTime,
                                                               behaviorMode));
    }
}
/*
 * Copyright (c) 2014 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.venkman.rules;

import junit.framework.Assert;
import org.janelia.it.venkman.TestUtilities;
import org.janelia.it.venkman.config.LarvaBehaviorParameters;
import org.janelia.it.venkman.config.rules.BehaviorLimitedKinematicVariableFunction;
import org.janelia.it.venkman.config.rules.BehaviorLimitedKinematicVariableFunctionList;
import org.janelia.it.venkman.config.rules.IntensityValue;
import org.janelia.it.venkman.config.rules.KinematicVariable;
import org.janelia.it.venkman.config.rules.LEDFlashPattern;
import org.janelia.it.venkman.config.rules.LEDStimulus;
import org.janelia.it.venkman.config.rules.PositionalVariable;
import org.janelia.it.venkman.config.rules.PositionalVariableFunction;
import org.janelia.it.venkman.config.rules.SingleVariableFunction;
import org.janelia.it.venkman.data.LarvaBehaviorMode;
import org.janelia.it.venkman.data.LarvaFrameData;
import org.janelia.it.venkman.data.TrackerPoint;
import org.janelia.it.venkman.log.StandardOutLogger;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.janelia.it.venkman.config.rules.OutOfRangeErrorHandlingMethod.END_SESSION_FOR_MINIMUM_AND_MAXIMUM;
import static org.janelia.it.venkman.config.rules.OutOfRangeErrorHandlingMethod.REPEAT_MINIMUM_AND_MAXIMUM;

/**
 * Tests the {@link DefinedEnvironment} classes.
 *
 * @author Eric Trautman
 */
public class DefinedEnvironmentTest {

    private static final double CENTER_INTENSITY = 44.0;

    private static final TrackerPoint CENTER_COORDINATE =
            new TrackerPoint(2.0, 2.0);

    private static final double[][] VALUES = {
            {1.0, 1.0,              1.0, 1.0, 1.0},
            {1.0, 2.0,              2.0, 2.0, 1.0},
            {1.0, 2.0, CENTER_INTENSITY, 2.0, 1.0},
            {1.0, 2.0,              2.0, 2.0, 1.0},
            {1.0, 1.0,              1.0, 1.0, 1.0}
    };

    private static final LEDFlashPattern DURATION = new LEDFlashPattern("33");

    private static final PositionalVariableFunction PV_FUNCTION =
            new PositionalVariableFunction(PositionalVariable.HEAD, VALUES);

    private LarvaBehaviorParameters behaviorParameters;
    private DefinedEnvironment rules;
    private TrackerPoint head;
    private double length;
    private double expectedIntensity;
    private long captureTime;
    private List<LarvaFrameData> frameHistory;

    @Before
    public void setUp() {
        behaviorParameters = new LarvaBehaviorParameters();
        behaviorParameters.setMaxLengthDerivationDuration(70);
        head = CENTER_COORDINATE;
        length = 0.0;
        expectedIntensity = CENTER_INTENSITY;
        captureTime = -30;
        frameHistory = new ArrayList<LarvaFrameData>();
    }

    @Test
    public void testDetermineStimulus() {
        rules = new DefinedEnvironment(DURATION, PV_FUNCTION, 0.0);
        validateStimulus("invalid stimulus returned",
                         LarvaBehaviorMode.RUN);

        head = new TrackerPoint(2.5, 2.0);
        expectedIntensity = ((CENTER_INTENSITY - 2.0) / 2.0) + 2.0;
        validateStimulus("invalid stimulus returned for interpolated value",
                         LarvaBehaviorMode.RUN);
    }

    @Test
    public void testDetermineStimulusWithRatio() {

        rules = new DefinedEnvironment(DURATION,
                                       PV_FUNCTION,
                                       expectedIntensity); // this SNR ensures noise is just N(0,1)
        final LEDStimulus stimulus = addFrameAndGetStimulus(LarvaBehaviorMode.RUN);

        // usage of SNR == base intensity, ensures noise is just N(0,1): -3 < noise < 3
        final double maxNoise = 3.0;
        final double minNoise = -maxNoise;
        final double noise = stimulus.getIntensityPercentage() - expectedIntensity;
        if ((noise > maxNoise) || (noise < minNoise)) {
            Assert.fail("noise value of " + noise + " is not between " + minNoise + " and " + maxNoise);
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testDetermineStimulusWithBadYCoordinate() {
        rules = new DefinedEnvironment(DURATION, PV_FUNCTION, 0.0);
        head = new TrackerPoint(1.0, -2.0);
        addFrame(head, LarvaBehaviorMode.RUN);
        rules.determineStimulus(frameHistory, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testDetermineStimulusWithBadXCoordinate() {
        rules = new DefinedEnvironment(DURATION, PV_FUNCTION, 0.0);
        head = new TrackerPoint(22.0, 1.0);
        addFrame(head, LarvaBehaviorMode.RUN);
        rules.determineStimulus(frameHistory, null);
    }

    @Test
    public void testDetermineStimulusForMaximumLengthWithAdditiveFunction() {

        final double gradientValue = 100.0;
        final double[][] gradientValues = {{gradientValue}};
        final PositionalVariableFunction gradient =
                new PositionalVariableFunction(PositionalVariable.HEAD,
                                               111.0, // maxX
                                               111.0, // maxY
                                               END_SESSION_FOR_MINIMUM_AND_MAXIMUM,
                                               gradientValues);
        final double activationPercentage = 90.0;
        final IntensityValue defaultIntensity = new IntensityValue(33.0);

        final double[] additiveValues = { 0.0, 0.0, 0.0, 0.0, -5.0, 10.0 };
        final double maxTime = (additiveValues.length - 1) * 30;
        final SingleVariableFunction additiveFunction =
                new SingleVariableFunction(0.0,
                                           maxTime,
                                           END_SESSION_FOR_MINIMUM_AND_MAXIMUM,
                                           additiveValues);

        rules = new DefinedEnvironmentForMaximumLengthWithAdditiveFunction(
                        DURATION,
                        gradient,
                        0.0,
                        false,
                        DefinedEnvironment.DEFAULT_ORIENTATION_DERIVATION_DURATION,
                        DefinedEnvironment.DEFAULT_CENTROID_DISTANCE_FROM_CENTER,
                        DefinedEnvironment.DEFAULT_ORIENTATION_OFFSET,
                        new BehaviorLimitedKinematicVariableFunctionList(),
                        activationPercentage,
                        defaultIntensity,
                        additiveFunction);

        final LEDStimulus zeroIntensity =
                LedActivationDurationRule.ZERO_INTENSITY_FOR_ONE_SECOND.get(0);

        length = 5.0;
        LEDStimulus stimulus = addFrameAndGetStimulus(LarvaBehaviorMode.RUN);
        Assert.assertEquals(
                "invalid stimulus for first frame at time " + captureTime,
                zeroIntensity,
                stimulus);

        final double maxLength = 10.0; // threshold should be 9.0
        length = maxLength;
        stimulus = addFrameAndGetStimulus(LarvaBehaviorMode.RUN);
        Assert.assertEquals(
                "invalid stimulus for second frame at time " + captureTime,
                zeroIntensity,
                stimulus);

        length = 7.0;
        stimulus = addFrameAndGetStimulus(LarvaBehaviorMode.RUN);
        Assert.assertEquals(
                "invalid stimulus for third frame at time " + captureTime,
                zeroIntensity,
                stimulus);

        length = 5.0;
        expectedIntensity = defaultIntensity.getValueWithoutNoise();
        validateStimulus("invalid intensity when threshold NOT met",
                         LarvaBehaviorMode.RUN);

        // fifth frame, negative additive value: verify sum
        length = maxLength * activationPercentage / 100.0;
        expectedIntensity = gradientValue + additiveValues[4];
        validateStimulus("invalid intensity when threshold met (min)",
                         LarvaBehaviorMode.RUN);

        // sixth frame, additive value sum exceeds 100, should be capped at 100
        length = maxLength;
        expectedIntensity = gradientValue;
        validateStimulus("invalid intensity when threshold met (max)",
                         LarvaBehaviorMode.RUN);
    }

    private void validateStimulus(String errorMessage,
                                  LarvaBehaviorMode behaviorMode) {
        final LEDStimulus stimulus = addFrameAndGetStimulus(behaviorMode);
        final LEDStimulus expectedStimulus =
                DURATION.getStimulusList(expectedIntensity).get(0);
        Assert.assertEquals(errorMessage,
                            expectedStimulus, stimulus);
    }

    private LEDStimulus addFrameAndGetStimulus(LarvaBehaviorMode behaviorMode) {
        addFrame(head, behaviorMode);
        @SuppressWarnings("unchecked")
        final List<LEDStimulus> stimulusList = (List<LEDStimulus>)
                rules.determineStimulus(frameHistory, behaviorParameters);
        return stimulusList.get(0);
    }

    private void addFrame(TrackerPoint head,
                          LarvaBehaviorMode behaviorMode) {
        captureTime += 30;
        final LarvaFrameData frameData =
                new LarvaFrameData(TestUtilities.getSkeleton(captureTime,
                                                             length,
                                                             head,
                                                             0.0),
                                   behaviorMode);

        // derive data so that max length values are properly set
        frameData.calculateDerivedData(frameHistory, behaviorParameters);

        // override behavior mode for testing
        frameData.setValuesForTesting(behaviorMode, frameData.getBodyAngleSpeed());

        frameHistory.add(0, frameData);
    }
}
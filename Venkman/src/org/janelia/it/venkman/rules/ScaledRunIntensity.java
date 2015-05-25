/*
 * Copyright (c) 2014 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.venkman.rules;

import org.janelia.it.venkman.config.LarvaBehaviorParameters;
import org.janelia.it.venkman.config.rules.BehaviorLimitedKinematicVariableFunctionList;
import org.janelia.it.venkman.config.rules.IntensityValue;
import org.janelia.it.venkman.config.rules.LEDFlashPattern;
import org.janelia.it.venkman.config.rules.LEDStimulus;
import org.janelia.it.venkman.config.rules.NoiseGenerator;
import org.janelia.it.venkman.config.rules.SingleVariableFunction;
import org.janelia.it.venkman.data.LarvaFrameData;
import org.janelia.it.venkman.gui.parameter.annotation.VenkmanParameter;
import org.janelia.it.venkman.log.Logger;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import java.util.List;
import java.util.Random;

/**
 * Stimulus rule implementation for:
 *
 * 2.1 During runs, larvae are exposed to a fixed LED intensity which is
 * predefined by the user (might be interesting to subject this fixed
 * intensity to an additive Gaussian noise). After a preset delay during
 * which the run mode is maintained, the larva is exposed to a steady
 * increase in LED intensity, starting from the default run value.
 * This intensity increase will be determined by a function of time,
 * I_{+}(t-t0), where t is the current time and t0 is the time when
 * the function starts. I_{+}(t) is upload by the experimenter (txt file).
 *
 * Hypothesis to test:
 * transitions from run to turn are suppressed when the LED intensity
 * monotonically increases in time. This situation is aimed to mimic the
 * olfactory experience of larvae crawling up-gradient.
 *
 * 3.1 Same as 2. After a preset delay during which the run mode is
 * maintained, the larva is exposed to a steady decrease in LED intensity,
 * starting from the default run value. This intensity decrease is determined
 * by a function of time, I_{-}(t-t0), uploaded by the experimenter.
 *
 * Hypothesis to test:
 * transitions from run to turn can be artificially
 * initiated upon a monotonic decrease in LED intensity. This situation
 * is aimed to mimic the olfactory experience of larvae crawling down-gradient.
 *
 * <h3>December 2011 Enhancement:</h3>
 * Support option to randomly alternate between two intensity change functions
 * (at the onset of each run).
 *
 * Hypothesis to test:
 * Given an initial ramp or hill of intensity, does a subsequent decrease
 * quickly trigger a cast?
 *
 * May 2014 update:
 * Added intensity filters to all rules.
 *
 * NOTE: To support JAXB translation, each rule implementation class
 *       must be added to {@link StimulusRuleImplementations}.
 *
 * @author Eric Trautman
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class ScaledRunIntensity
        extends LedActivationDurationRule {

    public static IntensityValue DEFAULT_NON_RUN_INTENSITY_VALUE =
            new IntensityValue(0.0);

    @VenkmanParameter(displayName = "Non-Run Intensity Percentage (0.0 to 100.0)")
    @XmlElement
    private IntensityValue nonRunIntensityPercentage;

    @VenkmanParameter(displayName = "Run Intensity Percentage (0.0 to 100.0)")
    @XmlElement
    private IntensityValue runIntensityPercentage;

    @VenkmanParameter(displayName = "Delay Before Changing Run Intensity (milliseconds)",
                      minimum = "0",
                      maximum = "20000")
    @XmlElement
    private long millisecondsDelay;

    @VenkmanParameter(displayName = "Run Intensity Scaling Function",
                      inputUnits = "milliseconds",
                      minimum = "0",
                      minimumOutput = "-100",
                      maximumOutput = "100",
                      outputUnits = "times intensity %")
    @XmlElement
    private SingleVariableFunction runIntensityScalingFunction;

    @VenkmanParameter(displayName = "Randomly Select Function at Onset of Run")
    @XmlElement
    private boolean isRandomFunctionSelectionActive;

    @VenkmanParameter(displayName = "Minimum Duration to Maintain Function (milliseconds)",
                      minimum = "0",
                      maximum = "120000")
    @XmlElement
    private long randomFunctionPersistenceDuration;

    @VenkmanParameter(displayName = "Alternate Run Intensity Scaling Function",
                      inputUnits = "milliseconds",
                      minimum = "0",
                      minimumOutput = "-100",
                      maximumOutput = "100",
                      outputUnits = "times intensity %")
    @XmlElement
    private SingleVariableFunction alternateRunIntensityScalingFunction;

    @VenkmanParameter(displayName = "Intensity Filter(s)",
                      displayOrder = 90, // force intensity filters to be displayed next to last
                      required = false,
                      listItemBaseName = "Filter")
    @XmlElement
    private BehaviorLimitedKinematicVariableFunctionList intensityFilterFunctionList;

    @VenkmanParameter(displayName = "Non-Run Signal To Noise Ratio",
                      displayOrder = 95, // force SNR parameters to be displayed last
                      minimum = "0",
                      maximum = "100")
    @XmlElement
    private double nonRunSignalToNoiseRatio;

    @VenkmanParameter(displayName = "Run Signal To Noise Ratio",
                      displayOrder = 96, // force SNR parameters to be displayed last
                      minimum = "0",
                      maximum = "100")
    @XmlElement
    private double runSignalToNoiseRatio;

    @VenkmanParameter(displayName = "Alternate Run Signal To Noise Ratio",
                      displayOrder = 97, // force SNR parameters to be displayed last
                      minimum = "0",
                      maximum = "100")
    @XmlElement
    private double alternateRunSignalToNoiseRatio;

    @XmlTransient
    private Long runOnsetTime;

    @XmlTransient
    private Random randomFunctionSelector;

    @XmlTransient
    private Long randomFunctionSelectionTime;

    @XmlTransient
    private SingleVariableFunction currentRunIntensityScalingFunction;

    @XmlTransient
    private NoiseGenerator whiteNoiseGenerator;

    @XmlTransient
    private double currentSignalToNoiseRatio;

    @XmlTransient
    private boolean useAlternateFunction;

    // no-arg constructor needed for JAXB and EditStimulusDialog
    @SuppressWarnings({"UnusedDeclaration"})
    public ScaledRunIntensity() {
        this(DEFAULT_LED_ACTIVATION_DURATION,
             DEFAULT_NON_RUN_INTENSITY_VALUE,
             0.0,
             new IntensityValue(),
             0,
             new SingleVariableFunction(),
             0.0,
             false,
             0,
             new SingleVariableFunction(),
             0.0,
             new BehaviorLimitedKinematicVariableFunctionList());
    }

    public ScaledRunIntensity(LEDFlashPattern ledActivationDuration,
                              IntensityValue nonRunIntensityPercentage,
                              double nonRunSignalToNoiseRatio,
                              IntensityValue runIntensityPercentage,
                              long millisecondsDelay,
                              SingleVariableFunction runIntensityScalingFunction,
                              double runSignalToNoiseRatio,
                              boolean isRandomFunctionSelectionActive,
                              long randomFunctionPersistenceDuration,
                              SingleVariableFunction alternateRunIntensityScalingFunction,
                              double alternateRunSignalToNoiseRatio,
                              BehaviorLimitedKinematicVariableFunctionList intensityFilterFunctionList)
            throws IllegalArgumentException {

        super(ledActivationDuration);
        this.nonRunIntensityPercentage = nonRunIntensityPercentage;
        this.nonRunSignalToNoiseRatio = nonRunSignalToNoiseRatio;
        this.runIntensityPercentage = runIntensityPercentage;
        this.millisecondsDelay = millisecondsDelay;
        this.runIntensityScalingFunction = runIntensityScalingFunction;
        this.runSignalToNoiseRatio = runSignalToNoiseRatio;
        this.isRandomFunctionSelectionActive = isRandomFunctionSelectionActive;
        this.randomFunctionPersistenceDuration = randomFunctionPersistenceDuration;
        this.alternateRunIntensityScalingFunction = alternateRunIntensityScalingFunction;
        this.alternateRunSignalToNoiseRatio = alternateRunSignalToNoiseRatio;
        this.intensityFilterFunctionList = intensityFilterFunctionList;

        this.runOnsetTime = null;
        this.randomFunctionSelector = new Random();
        this.randomFunctionSelectionTime = null;
        this.currentRunIntensityScalingFunction = runIntensityScalingFunction;
        this.whiteNoiseGenerator = new NoiseGenerator();
        this.currentSignalToNoiseRatio = runSignalToNoiseRatio;
        this.useAlternateFunction = false;
    }

    @Override
    public String getCode() {
        return "2.1/3.1";
    }

    @Override
    public String getDescription() {
        return "Elongation of runs/induction of turns through synthesis of positive/negative olfactory experiences.";
    }

    @Override
    public void init(Logger logger) {
        super.init(logger);
        currentRunIntensityScalingFunction = runIntensityScalingFunction;
        currentSignalToNoiseRatio = nonRunSignalToNoiseRatio;
    }

    public long getMillisecondsDelay() {
        return millisecondsDelay;
    }

    public double getCurrentSignalToNoiseRatio() {
        return currentSignalToNoiseRatio;
    }

    public SingleVariableFunction getRunIntensityScalingFunction() {
        return runIntensityScalingFunction;
    }

    @Override
    public List<LEDStimulus> determineStimulus(List<LarvaFrameData> frameHistory,
                                               LarvaBehaviorParameters behaviorParameters) {

        double derivedIntensity;

        final LarvaFrameData frameData = frameHistory.get(0);

        if (frameData.isRunning()) {

            final double intensityPercentage = runIntensityPercentage.getValue();
            final long time = frameData.getTime();
            derivedIntensity = intensityPercentage;

            if (runOnsetTime == null) {
                startRun(time);

                if (isRandomFunctionSelectionActive) {

                    final boolean hasCurrentFunctionBeenActiveLongEnough =
                            (randomFunctionSelectionTime == null) ||
                            ((time - randomFunctionSelectionTime) > randomFunctionPersistenceDuration);

                    if (hasCurrentFunctionBeenActiveLongEnough) {

                        useAlternateFunction = randomFunctionSelector.nextBoolean();

                        if (useAlternateFunction) {
                            currentRunIntensityScalingFunction = alternateRunIntensityScalingFunction;
                            currentSignalToNoiseRatio = alternateRunSignalToNoiseRatio;
                            logRuleData(time, RuleData.INTENSITY_FUNCTION_NAME, RuleData.ALTERNATE_NAME);
                        } else {
                            currentRunIntensityScalingFunction = runIntensityScalingFunction;
                            currentSignalToNoiseRatio = runSignalToNoiseRatio;
                            logRuleData(time, RuleData.INTENSITY_FUNCTION_NAME, RuleData.PRIMARY_NAME);
                        }

                        randomFunctionSelectionTime = time;

                    } else if (useAlternateFunction) {
                        currentSignalToNoiseRatio = alternateRunSignalToNoiseRatio;
                    } else {
                        currentSignalToNoiseRatio = runSignalToNoiseRatio;
                    }

                } else {
                    currentSignalToNoiseRatio = runSignalToNoiseRatio;
                }
            }

            // runOnsetTime should always be defined by startRun call above
            @SuppressWarnings("ConstantConditions")
            final long timeSinceOnset = time - runOnsetTime;

            if (timeSinceOnset >= millisecondsDelay) {
                final long timeSinceScalingStarted = timeSinceOnset - millisecondsDelay;
                final double scalingFactor = currentRunIntensityScalingFunction.getValue(timeSinceScalingStarted);
                derivedIntensity = derivedIntensity * scalingFactor;
            }

        } else {
            runOnsetTime = null;
            derivedIntensity = nonRunIntensityPercentage.getValue();
            currentSignalToNoiseRatio = nonRunSignalToNoiseRatio;
        }

        List<LEDStimulus> stimulusList = getStimulusList(derivedIntensity);

        intensityFilterFunctionList.applyValues(frameData, stimulusList, 0.0);
        whiteNoiseGenerator.addNoiseUsingRatio(currentSignalToNoiseRatio, stimulusList);

        return stimulusList;
    }

    protected void setMillisecondsDelay(Integer millisecondsDelay) {
        this.millisecondsDelay = millisecondsDelay;
    }

    protected void startRun(long time) {
        runOnsetTime = time;
    }
}

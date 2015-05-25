/*
 * Copyright (c) 2014 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.venkman.rules;

import org.janelia.it.venkman.config.rules.BehaviorLimitedKinematicVariableFunctionList;
import org.janelia.it.venkman.config.rules.IntensityValue;
import org.janelia.it.venkman.config.rules.LEDFlashPattern;
import org.janelia.it.venkman.config.rules.SingleVariableFunction;
import org.janelia.it.venkman.gui.parameter.annotation.VenkmanParameter;
import org.janelia.it.venkman.gui.parameter.annotation.VenkmanParameterFilter;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import java.util.Random;

/**
 * Stimulus rule implementation for:
 *
 * 2.2 Same as 2.1 with a random delay triggering the onset of I_{+}(t).
 *
 * 3.2 Same as 2.1 with a random delay triggering the onset of I_{-}(t).
 *
 * NOTE: To support JAXB translation, each rule implementation class
 *       must be added to {@link StimulusRuleImplementations}.
 *
 * @author Eric Trautman
 */
@VenkmanParameterFilter(hiddenParameters = {"millisecondsDelay"})
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class ScaledRunIntensityWithRandomDelay
        extends ScaledRunIntensity {

    @VenkmanParameter(displayName = "Maximum Delay Before Changing Run Intensity (milliseconds)",
                      minimum = "0",
                      maximum = "20000")
    @XmlElement
    private int maximumMillisecondsDelay;

    @XmlTransient
    private Random randomDelayGenerator;

     // no-arg constructor needed for JAXB and EditStimulusDialog
    @SuppressWarnings({"UnusedDeclaration"})
    public ScaledRunIntensityWithRandomDelay() {
        this(DEFAULT_LED_ACTIVATION_DURATION,
             DEFAULT_NON_RUN_INTENSITY_VALUE,
             0.0,
             new IntensityValue(),
             new SingleVariableFunction(),
             0.0,
             false,
             0,
             new SingleVariableFunction(),
             0.0,
             new BehaviorLimitedKinematicVariableFunctionList(),
             0);
    }

    public ScaledRunIntensityWithRandomDelay(LEDFlashPattern ledActivationDuration,
                                             IntensityValue nonRunIntensityPercentage,
                                             double nonRunSignalToNoiseRatio,
                                             IntensityValue runIntensityPercentage,
                                             SingleVariableFunction runIntensityScalingFunction,
                                             double signalToNoiseRatio,
                                             boolean isRandomFunctionSelectionActive,
                                             long randomFunctionPersistenceDuration,
                                             SingleVariableFunction alternateRunIntensityScalingFunction,
                                             double alternateSignalToNoiseRatio,
                                             BehaviorLimitedKinematicVariableFunctionList intensityFilterFunctionList,
                                             int maximumMillisecondsDelay) {
        super(ledActivationDuration,
              nonRunIntensityPercentage,
              nonRunSignalToNoiseRatio,
              runIntensityPercentage,
              0,
              runIntensityScalingFunction,
              signalToNoiseRatio,
              isRandomFunctionSelectionActive,
              randomFunctionPersistenceDuration,
              alternateRunIntensityScalingFunction,
              alternateSignalToNoiseRatio,
              intensityFilterFunctionList);
        this.maximumMillisecondsDelay = maximumMillisecondsDelay;
        this.randomDelayGenerator = new Random();
    }

    @Override
    public String getCode() {
        return "2.2/3.2";
    }

    @Override
    public String getDescription() {
        return "Elongation of runs/induction of turns through synthesis of positive/negative olfactory experiences with random delay.";
    }

    @Override
    protected void startRun(long time) {
        super.startRun(time);
        if (maximumMillisecondsDelay > 0) {
            setMillisecondsDelay(
                randomDelayGenerator.nextInt(maximumMillisecondsDelay));
        }
    }

}

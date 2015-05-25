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
import org.janelia.it.venkman.config.rules.PositionalVariableFunction;
import org.janelia.it.venkman.config.rules.SingleVariableFunction;
import org.janelia.it.venkman.data.LarvaFrameData;
import org.janelia.it.venkman.gui.parameter.annotation.VenkmanParameter;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;

/**
 * Stimulus rule implementation for:
 *
 * 1.5 Discrete versus continuous sampling with time based additive intensity.
 *
 * This rule supports experiments in a spatial landscape of light with a
 * small additive intensity of the light in time series.
 * It is a combination of Rule 1.4 and 7.1 with the 'intensity %' of 7.1
 * switched to 'additive intensity %'.
 *
 * This allows the experimenter to superimpose various types of perturbations
 * in a given static landscape of light (ex. Gaussian noise in time,
 * sine function, square function, ...).
 *
 * NOTE: To support JAXB translation, each rule implementation class
 *       must be added to {@link org.janelia.it.venkman.rules.StimulusRuleImplementations}.
 *
 * @author Eric Trautman
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class DefinedEnvironmentForMaximumLengthWithAdditiveFunction
        extends DefinedEnvironment {

    @VenkmanParameter(displayName = "Percentage of Max Length to Activate Gradient (0.0 to 100.0)",
                      minimum = "0",
                      maximum = "100")
    @XmlElement
    private double percentageOfMaxLengthToActivateGradient;

    @VenkmanParameter(displayName = "Default Intensity Percentage when Gradient Inactive (0.0 to 100.0)")
    @XmlElement
    private IntensityValue defaultIntensityPercentage;

    @VenkmanParameter(displayName = "Additive Intensity Function",
                      inputUnits = "milliseconds",
                      minimum = "0",
                      minimumOutput = "0",
                      maximumOutput = "100",
                      outputUnits = "additive intensity %")
    @XmlElement
    private SingleVariableFunction additiveIntensityFunction;

    // kept here to support legacy version of rule (parameter was migrated to behavior parameters in May 2014)
    @SuppressWarnings("UnusedDeclaration")
    @XmlElement(name = "maxLengthDerivationDuration")
    private Long deprecatedMaxLengthDerivationDuration;

     // no-arg constructor needed for JAXB and EditStimulusDialog
    @SuppressWarnings({"UnusedDeclaration"})
    public DefinedEnvironmentForMaximumLengthWithAdditiveFunction() {
        this(DEFAULT_LED_ACTIVATION_DURATION,
             new PositionalVariableFunction(),
             0.0,
             false,
             DEFAULT_ORIENTATION_DERIVATION_DURATION,
             DEFAULT_CENTROID_DISTANCE_FROM_CENTER,
             DEFAULT_ORIENTATION_OFFSET,
             new BehaviorLimitedKinematicVariableFunctionList(),
             90.0,
             new IntensityValue(),
             new SingleVariableFunction());
    }

    public DefinedEnvironmentForMaximumLengthWithAdditiveFunction(LEDFlashPattern ledActivationDuration,
                                                                  PositionalVariableFunction intensityFunction,
                                                                  double signalToNoiseRatio,
                                                                  boolean enableOrientationLogic,
                                                                  long orientationDerivationDuration,
                                                                  double centroidDistanceFromArenaCenter,
                                                                  double centeredOrientationOffsetInDegrees,
                                                                  BehaviorLimitedKinematicVariableFunctionList intensityFilterFunctionList,
                                                                  double percentageOfMaxLengthToActivateGradient,
                                                                  IntensityValue defaultIntensityPercentage,
                                                                  SingleVariableFunction additiveIntensityFunction) {
        super(ledActivationDuration,
              intensityFunction,
              signalToNoiseRatio,
              enableOrientationLogic,
              orientationDerivationDuration,
              centroidDistanceFromArenaCenter,
              centeredOrientationOffsetInDegrees,
              intensityFilterFunctionList);

        // must use either length intensity filter or activation percentage
        if (intensityFilterFunctionList.hasFunctions()) {
            this.percentageOfMaxLengthToActivateGradient = 0.0; // always active when using filter
        } else {
            this.percentageOfMaxLengthToActivateGradient = percentageOfMaxLengthToActivateGradient;
        }

        this.defaultIntensityPercentage = defaultIntensityPercentage;
        this.additiveIntensityFunction = additiveIntensityFunction;
    }

    @Override
    public String getCode() {
        return "1.5";
    }

    @Override
    public String getDescription() {
        return "Discrete versus continuous sampling with time based additive intensity.";
    }

    /**
     * @param  behaviorParameters  the configured behavior parameters.
     *
     * @return the specified parameters with updated max length derivation duration
     *         (if this rule instance was configured before that parameter was promoted).
     */
    @Override
    public LarvaBehaviorParameters overrideBehaviorParameters(LarvaBehaviorParameters behaviorParameters) {
        if (deprecatedMaxLengthDerivationDuration != null) {
            behaviorParameters.setMaxLengthDerivationDuration(deprecatedMaxLengthDerivationDuration);
            deprecatedMaxLengthDerivationDuration = null; // set to null so that element does not get persisted
        }
        return behaviorParameters;
    }

    @Override
    public List<LEDStimulus> determineStimulus(List<LarvaFrameData> frameHistory,
                                               LarvaBehaviorParameters behaviorParameters) {

        final LarvaFrameData frameData = frameHistory.get(0);
        final Double percentageOfMaxLength = frameData.getPercentageOfMaxLength();

        long time = frameData.getTime();

        List<LEDStimulus> list;
        if (! frameData.isMaxLengthDerivationComplete()) {

            list = ZERO_INTENSITY_FOR_ONE_SECOND;

        } else if (percentageOfMaxLength < percentageOfMaxLengthToActivateGradient) {

            // threshold not met - return default intensity
            list = getDefaultStimulus();

        } else {

            // threshold met (elongated animal) - apply gradient
            list = determinePositionBasedStimulus(frameHistory);

            if (isOriented()) {
                if (time > additiveIntensityFunction.getMaximumInputValue()) {
                    time = (long) additiveIntensityFunction.getMaximumInputValue();
                }
                final double additiveIntensityPercentage =
                        additiveIntensityFunction.getValue(time);

                // base class determineStimulus always returns single entry list
                for (LEDStimulus stimulus : list) {
                    stimulus.addIntensityPercentage(additiveIntensityPercentage);
                }
            }

            list = applyIntensityFiltersAndWhiteNoise(frameHistory, list, defaultIntensityPercentage.getValue());
        }

        return list;
    }

    @Override
    protected List<LEDStimulus> getDefaultStimulus() {
        return getStimulusList(defaultIntensityPercentage.getValue());
    }

}

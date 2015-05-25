/*
 * Copyright (c) 2014 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.venkman.config.rules;

import org.janelia.it.venkman.data.LarvaBehaviorMode;
import org.janelia.it.venkman.data.LarvaFrameData;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Set of values indexed by a kinematic variable that are only applied for a specific set of behavior modes.
 *
 * @author Eric Trautman
 */
@XmlRootElement
public class BehaviorLimitedKinematicVariableFunction
        extends KinematicVariableFunction {

    /**
     * The set of behavior modes for which this function should be applied.
     */
    @XmlElementWrapper(name="behaviorModes")
    @XmlElement(name="larvaBehaviorMode")
    private Set<LarvaBehaviorMode> behaviorModes;

    @XmlElement
    private boolean isAdditive;

    public BehaviorLimitedKinematicVariableFunction() {
        this(KinematicVariable.PERCENTAGE_OF_MAX_LENGTH, new double[]{1});
    }

    public BehaviorLimitedKinematicVariableFunction(KinematicVariable variable,
                                                    double[] values)
            throws IllegalArgumentException {

        this(LarvaBehaviorMode.getDiscreteModes(),
             variable,
             0,
             values.length - 1,
             OutOfRangeErrorHandlingMethod.END_SESSION_FOR_MINIMUM_AND_MAXIMUM,
             values);
    }

    public BehaviorLimitedKinematicVariableFunction(Set<LarvaBehaviorMode> behaviorModes,
                                                    KinematicVariable variable,
                                                    double minimumInputValue,
                                                    double maximumInputValue,
                                                    OutOfRangeErrorHandlingMethod outOfRangeInputHandlingMethod,
                                                    double[] values)
            throws IllegalArgumentException {

        this(behaviorModes,
             false,
             variable,
             minimumInputValue,
             maximumInputValue,
             outOfRangeInputHandlingMethod,
             values);
    }

    public BehaviorLimitedKinematicVariableFunction(Set<LarvaBehaviorMode> behaviorModes,
                                                    boolean isAdditive,
                                                    KinematicVariable variable,
                                                    double minimumInputValue,
                                                    double maximumInputValue,
                                                    OutOfRangeErrorHandlingMethod outOfRangeInputHandlingMethod,
                                                    double[] values)
            throws IllegalArgumentException {

        super(variable,
              minimumInputValue,
              maximumInputValue,
              outOfRangeInputHandlingMethod,
              values);
        this.behaviorModes = new LinkedHashSet<LarvaBehaviorMode>();
        this.isAdditive = isAdditive;
        if (behaviorModes == null) {
            this.behaviorModes = new LinkedHashSet<LarvaBehaviorMode>();
        } else {
            this.behaviorModes.addAll(behaviorModes);
        }

    }

    /**
     * @return true if this function is active for at least one behavior mode;
     *         otherwise false.
     */
    public boolean hasBehaviorModes() {
        return (behaviorModes.size() > 0);
    }

    public boolean isAdditive() {
        return isAdditive;
    }

    public Set<LarvaBehaviorMode> getBehaviorModes() {
        return new LinkedHashSet<LarvaBehaviorMode>(behaviorModes);
    }

    /**
     * @param  mode  derived behavior mode for current frame.
     *
     * @return true if this function should be applied for the specified mode;
     *         otherwise false.
     */
    public boolean isActiveFor(LarvaBehaviorMode mode) {
        return behaviorModes.contains(mode);
    }

    /**
     * Applies this function's result to the specified stimulus list.
     *
     * @param  currentFrameData  data for the current frame (used for behavior mode).
     * @param  stimulusList      base list of stimulus values to modify.
     * @param  minimum           floor for scaled intensity values.
     */
    public void applyValue(LarvaFrameData currentFrameData,
                           List<LEDStimulus> stimulusList,
                           double minimum) {

        if (isActiveFor(currentFrameData.getBehaviorMode())) {

            final double functionResultValue = getValue(currentFrameData);

            if (isAdditive) {
                for (LEDStimulus stimulus : stimulusList) {
                    stimulus.addIntensityPercentage(functionResultValue);
                }
            } else {
                for (LEDStimulus stimulus : stimulusList) {
                    stimulus.scaleIntensityPercentageWithFloor(functionResultValue, minimum);
                }
            }
        }
    }

}

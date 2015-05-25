/*
 * Copyright (c) 2014 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.venkman.rules;

import org.janelia.it.venkman.config.rules.BehaviorLimitedKinematicVariableFunctionList;
import org.janelia.it.venkman.config.rules.LEDFlashPattern;
import org.janelia.it.venkman.config.rules.PositionalVariableFunction;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * NOTE: This rule was pulled directly into the base {@link DefinedEnvironment} rule
 * so that it's logic could be shared by all of the Defined Environment rules.
 * This implementation remains defined here for backwards-compatibility reasons.
 *
 * Stimulus rule implementation for:
 *
 * 1.6 Position landscape according to initial larval orientation.
 *
 * This rule orients a landscape (spatial intensity function) according to
 * the initial larval orientation.  It was developed because the initial
 * intensity history that the larva experiences is important for
 * successful chemotaxis.
 *
 * After a configured amount of time (e.g. 15 seconds), this rule records
 * the angle required to rotate the larva's tail-to-midpoint vector
 * parallel to the centroid-to-arena-center vector in the direction of the
 * arena center.  It also records the offsets required to reposition the
 * larva's centroid a configured distance from the arena center.
 * The rotation (relative to the centroid) and transformation parameters
 * are then applied for all subsequent frames to derive intensity values.
 *
 * NOTE: To support JAXB translation, each rule implementation class
 *       must be added to {@link StimulusRuleImplementations}.
 *
 * @author Eric Trautman
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class DefinedEnvironmentBasedUponOrientation
        extends DefinedEnvironment {

    // no-arg constructor needed for JAXB and EditStimulusDialog
    @SuppressWarnings({"UnusedDeclaration"})
    public DefinedEnvironmentBasedUponOrientation() {
        this(DEFAULT_LED_ACTIVATION_DURATION,
             new PositionalVariableFunction(),
             0.0,
             DEFAULT_ORIENTATION_DERIVATION_DURATION,
             DEFAULT_CENTROID_DISTANCE_FROM_CENTER,
             DEFAULT_ORIENTATION_OFFSET,
             new BehaviorLimitedKinematicVariableFunctionList());
    }

    public DefinedEnvironmentBasedUponOrientation(LEDFlashPattern ledActivationDuration,
                                                  PositionalVariableFunction intensityFunction,
                                                  double signalToNoiseRatio,
                                                  long orientationDerivationDuration,
                                                  double centroidDistanceFromArenaCenter,
                                                  double centeredOrientationOffsetInDegrees,
                                                  BehaviorLimitedKinematicVariableFunctionList intensityFilterFunctionList) {
        super(ledActivationDuration,
              intensityFunction,
              signalToNoiseRatio,
              true,
              orientationDerivationDuration,
              centroidDistanceFromArenaCenter,
              centeredOrientationOffsetInDegrees,
              intensityFilterFunctionList);
    }

    @Override
    public String getCode() {
        return "1.6";
    }

    @Override
    public String getDescription() {
        return "Position landscape according to initial larval orientation.";
    }

}
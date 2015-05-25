/*
 * Copyright (c) 2014 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.venkman.config.rules;

import org.janelia.it.venkman.data.LarvaFrameData;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Set of values indexed by a kinematic variable.
 *
 * @author Eric Trautman
 */
@XmlRootElement
public class KinematicVariableFunction extends SingleVariableFunction {

    /**
     * The kinematic variable for indexing into this function.
     */
    @XmlElement
    private KinematicVariable variable;

    public KinematicVariableFunction() {
        this(KinematicVariable.BODY_ANGLE, new double[] {0});
    }

    public KinematicVariableFunction(KinematicVariable variable,
                                     double[] values)
            throws IllegalArgumentException {

        this(variable,
             0,
             values.length - 1,
             OutOfRangeErrorHandlingMethod.END_SESSION_FOR_MINIMUM_AND_MAXIMUM,
             values);
    }

    public KinematicVariableFunction(KinematicVariable variable,
                                     double minimumInputValue,
                                     double maximumInputValue,
                                     OutOfRangeErrorHandlingMethod outOfRangeInputHandlingMethod,
                                     double[] values)
            throws IllegalArgumentException {

        super(minimumInputValue,
              maximumInputValue,
              outOfRangeInputHandlingMethod,
              values);

        if (variable == null) {
            throw new IllegalArgumentException(
                    "A kinematic variable must be specified.");
        }
        this.variable = variable;
    }

    public KinematicVariable getVariable() {
        return variable;
    }

    /**
     * @param  frameData  current frame data.
     *
     * @return this function's value for the specified frame data.
     */
    public double getValue(LarvaFrameData frameData) {
        return getValue(variable.getValue(frameData));
    }

}

/*
 * Copyright (c) 2014 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.venkman.config.rules;

import org.janelia.it.venkman.data.Calculator;
import org.janelia.it.venkman.jaxb.DoubleArrayAdapter;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

/**
 * Set of values indexed by another value.
 *
 * @author Eric Trautman
 */
@XmlRootElement
public class SingleVariableFunction {

    /** The minimum input value. */
    @XmlElement
    private double minimumInputValue;

    /** The maximum input value. */
    @XmlElement
    private double maximumInputValue;

    /** The method for handling out of range input values. */
    @XmlElement
    private OutOfRangeErrorHandlingMethod inputRangeErrorHandlingMethod;

    /** The minimum output value. */
    @XmlElement
    private double minimumOutputValue;

    /** The maximum output value. */
    @XmlElement
    private double maximumOutputValue;

    /**
     * The derived multiplicative factor to map input values
     * to the range of indices for this function's output values.
     */
    @XmlElement
    private double factor;

    /** The output values for this function. */
    @XmlJavaTypeAdapter(DoubleArrayAdapter.class)
    @XmlElement
    private double[] values;

    /**
     * Constructs and empty function that always returns zero.
     */
    public SingleVariableFunction() {
        this(new double[] {0});
    }

    /**
     * Constructs a function with the specified output values.
     * The variable range is assumed to match the the range of values.
     *
     * @param  values    the output values for this function.
     */
    public SingleVariableFunction(double[] values) {
        this(0,
             values.length - 1,
             OutOfRangeErrorHandlingMethod.END_SESSION_FOR_MINIMUM_AND_MAXIMUM,
             values);
    }

    /**
     * Constructs a function with the specified output values.
     * The variable input range is identified by the specified
     * minimum and maximum values, allowing inputs to be scaled.
     *
     * @param  minimumInputValue              the minimum input value.
     * @param  maximumInputValue              the maximum input value.
     * @param  inputRangeErrorHandlingMethod  method for handling out of range input values.
     * @param  values                         the output values for this function.
     *
     * @throws IllegalArgumentException
     *   if the specified minimum is greater than the specified maximum or
     *   if the variable or values are not specified.
     */
    public SingleVariableFunction(double minimumInputValue,
                                  double maximumInputValue,
                                  OutOfRangeErrorHandlingMethod inputRangeErrorHandlingMethod,
                                  double[] values)
            throws IllegalArgumentException {

        if ((values == null) || values.length == 0) {
            throw new IllegalArgumentException(
                    "values not defined for function");
        }
        this.values = values;

        if (minimumInputValue > maximumInputValue) {
            throw new IllegalArgumentException(
                    "minimum variable value (" + minimumInputValue +
                    ") must not be greater than maximum variable value (" +
                    maximumInputValue + ")");
        }
        this.minimumInputValue = minimumInputValue;
        this.maximumInputValue = maximumInputValue;

        this.inputRangeErrorHandlingMethod = inputRangeErrorHandlingMethod;

        final double range = maximumInputValue - minimumInputValue;
        this.factor = (double) (this.values.length - 1) / range;
        if (Double.isNaN(this.factor) || Double.isInfinite(this.factor)) {
            this.factor = 0;
        }

        double min = Double.MAX_VALUE;
        double max = -1 * Double.MAX_VALUE;
        for (double value : values) {
            if (value < min) {
                min = value;
            }
            if (value > max) {
                max = value;
            }
        }
        minimumOutputValue = min;
        maximumOutputValue = max;
    }

    public double getMinimumInputValue() {
        return minimumInputValue;
    }

    public double getMaximumInputValue() {
        return maximumInputValue;
    }

    public double getMinimumOutputValue() {
        return minimumOutputValue;
    }

    public double getMaximumOutputValue() {
        return maximumOutputValue;
    }

    public OutOfRangeErrorHandlingMethod getInputRangeErrorHandlingMethod() {
        return inputRangeErrorHandlingMethod;
    }

    public double[] getValues() {
        return values;
    }

    /**
     * @param  inputValue  the input value for this function.
     *
     * @return this function's (output) value for the specified input value.
     *
     * @throws IllegalArgumentException
     *   if the input value cannot be mapped by this function.
     */
    public double getValue(double inputValue)
            throws IllegalArgumentException {
        return getLinearInterpolatedResult(inputValue,
                                           factor,
                                           minimumInputValue,
                                           maximumInputValue);
    }

    /**
     * @param  inputValue         the input value to evaluate.
     * @param  factor             the scaling factor to apply
     *                            to the input value.
     * @param  minimumInputValue  the minimum expected input value.
     * @param  maximumInputValue  the maximum expected input value.
     *
     * @return  this function's linear interpolated output for the specified
     *          input value using the specified scaling factors.
     *
     * @throws IllegalArgumentException
     *   if the specified input value is out of range for this function
     *   (after scaling).
     */
    public double getLinearInterpolatedResult(double inputValue,
                                              double factor,
                                              double minimumInputValue,
                                              double maximumInputValue)
            throws IllegalArgumentException {

        if ((inputValue < minimumInputValue) && inputRangeErrorHandlingMethod.repeatMinimum()) {
            inputValue = minimumInputValue;
        } else if ((inputValue > maximumInputValue) && inputRangeErrorHandlingMethod.repeatMaximum()) {
            inputValue = maximumInputValue;
        }

        final double scaledInputValue =
                (inputValue - minimumInputValue) * factor;
        final int previousScaledInputValue = (int) scaledInputValue;
        int nextScaledInputValue = previousScaledInputValue + 1;

        if ((previousScaledInputValue < 0) ||
            (previousScaledInputValue >= values.length)) {
            throw new IllegalArgumentException(
                    "input value " + inputValue +
                    " is not within the expected function range of " +
                    minimumInputValue + " to " + maximumInputValue);
        }

        if (nextScaledInputValue == values.length) {
            if (inputValue > maximumInputValue) {
                throw new IllegalArgumentException(
                        "input value " + inputValue +
                        " is greater than the expected function maximum " +
                        maximumInputValue);

            } else {
                nextScaledInputValue = previousScaledInputValue;
            }
        }

        return Calculator.getLinearInterpolation(
                scaledInputValue,
                previousScaledInputValue, values[previousScaledInputValue],
                nextScaledInputValue, values[nextScaledInputValue]);
    }

}

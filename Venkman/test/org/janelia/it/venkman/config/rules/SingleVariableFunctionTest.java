/*
 * Copyright (c) 2014 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.venkman.config.rules;

import org.janelia.it.venkman.TestUtilities;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import static org.janelia.it.venkman.config.rules.OutOfRangeErrorHandlingMethod.*;

/**
 * Tests the {@link SingleVariableFunction} class.
 *
 * @author Eric Trautman
 */
public class SingleVariableFunctionTest {

    private SingleVariableFunction svFunction;

    private static final double[] DEFAULT_VALUES = {
            10.0, 20.0, 30.0, 40.0, 50.0
    };
    private static final int MIN_INPUT_VALUE = 0;
    private static final int MAX_INPUT_VALUE = DEFAULT_VALUES.length - 1;

    @Before
    public void setup() {
        svFunction = new SingleVariableFunction(MIN_INPUT_VALUE,
                                                MAX_INPUT_VALUE,
                                                END_SESSION_FOR_MINIMUM_AND_MAXIMUM,
                                                DEFAULT_VALUES);
    }

    @Test
    public void testGetValueWithSameFunctionAndVariableRanges()
            throws Exception {

        final double[] values = DEFAULT_VALUES;
        final int max = values.length - 1;

        for (int i = 0; i < values.length; i++) {
            validateGetValueResult(i, values[i]);

            if (i < max) {
                validateGetValueResult((i + 0.5),
                                       ((values[i] + values[i + 1]) / 2));
            }
        }

    }

    @Test
    public void testGetValueWithFunctionRangeGreaterThanVariableRange()
            throws Exception {

        final double min = -4.0;
        final double max = 4.0;
        svFunction = new SingleVariableFunction(min,
                                                max,
                                                END_SESSION_FOR_MINIMUM_AND_MAXIMUM,
                                                DEFAULT_VALUES);

        validateGetValueResult(min, DEFAULT_VALUES[0]);
        validateGetValueResult(0, DEFAULT_VALUES[2]);
        validateGetValueResult(max, DEFAULT_VALUES[4]);
    }

    @Test
    public void testGetValueWithFunctionRangeLessThanVariableRange()
            throws Exception {

        final double min = -1.0;
        final double max = 1.0;
        svFunction = new SingleVariableFunction(min,
                                                max,
                                                END_SESSION_FOR_MINIMUM_AND_MAXIMUM,
                                                DEFAULT_VALUES);

        validateGetValueResult(min, DEFAULT_VALUES[0]);
        validateGetValueResult(0, DEFAULT_VALUES[2]);
        validateGetValueResult(max, DEFAULT_VALUES[4]);
    }

    @Test
    public void testGetValueWithOutOfRangeInput() {
        // END_SESSION_FOR_MINIMUM_AND_MAXIMUM
        validateGetValueResultForOutOfRangeInput(false, false);

        setSvFunctionInputRangeErrorHandlingMethod(END_SESSION_FOR_MINIMUM_REPEAT_MAXIMUM);
        validateGetValueResultForOutOfRangeInput(false, true);

        setSvFunctionInputRangeErrorHandlingMethod(REPEAT_MINIMUM_END_SESSION_FOR_MAXIMUM);
        validateGetValueResultForOutOfRangeInput(true, false);

        setSvFunctionInputRangeErrorHandlingMethod(REPEAT_MINIMUM_AND_MAXIMUM);
        validateGetValueResultForOutOfRangeInput(true, true);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testMaximumGreaterThanMinimum() {
        svFunction = new SingleVariableFunction(2,
                                                1,
                                                END_SESSION_FOR_MINIMUM_AND_MAXIMUM,
                                                DEFAULT_VALUES);
    }

    private void validateGetValueResult(double inputValue,
                                        double expectedOutputValue) {
        TestUtilities.assertEquals("invalid value returned for " + inputValue,
                                   expectedOutputValue,
                                   svFunction.getValue(inputValue));
    }

    private void setSvFunctionInputRangeErrorHandlingMethod(OutOfRangeErrorHandlingMethod method) {
        svFunction = new SingleVariableFunction(svFunction.getMinimumInputValue(),
                                                svFunction.getMaximumInputValue(),
                                                method,
                                                svFunction.getValues());
    }

    private void validateGetValueResultForOutOfRangeInput(String context,
                                                          int outOfRangeInputValue,
                                                          int boundaryInputValue,
                                                          boolean repeat) {

        if (repeat) {
            try {
                TestUtilities.assertEquals("invalid value returned for out of range " + context +
                                           " with method " + svFunction.getInputRangeErrorHandlingMethod(),
                                           svFunction.getValue(boundaryInputValue),
                                           svFunction.getValue(outOfRangeInputValue)
                );
            } catch (IllegalArgumentException e) {
                Assert.fail("out of range " + context + " should not have caused exception with method " +
                            svFunction.getInputRangeErrorHandlingMethod().name() + ", exception message was: " +
                            e.getMessage());
            }
        } else {
            try {
                svFunction.getValue(outOfRangeInputValue);
                Assert.fail("out of range " + context + " should cause exception with method " +
                            svFunction.getInputRangeErrorHandlingMethod().name());
            } catch (IllegalArgumentException e) {
                // test passed
            }
        }
    }

    private void validateGetValueResultForOutOfRangeInput(boolean repeatMin,
                                                          boolean repeatMax) {
        final int valueLessThanMin = MIN_INPUT_VALUE - 1;
        final int valueGreaterThanMax = MAX_INPUT_VALUE + 1;
        validateGetValueResultForOutOfRangeInput("minimum", valueLessThanMin, MIN_INPUT_VALUE, repeatMin);
        validateGetValueResultForOutOfRangeInput("maximum", valueGreaterThanMax, MAX_INPUT_VALUE, repeatMax);
    }

}

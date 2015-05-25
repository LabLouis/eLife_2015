/*
 * Copyright (c) 2014 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.venkman.config.rules;

import org.janelia.it.venkman.TestUtilities;
import org.janelia.it.venkman.data.LarvaFrameData;
import org.janelia.it.venkman.data.TrackerPoint;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import static org.janelia.it.venkman.config.rules.OutOfRangeErrorHandlingMethod.*;

/**
 * Tests the {@link PositionalVariableFunction} class.
 *
 * @author Eric Trautman
 */
public class PositionalVariableFunctionTest {

    private PositionalVariableFunction pvFunction;

    private static final double[][] DEFAULT_VALUES = {
            {10.0, 20.0, 30.0, 40.0, 50.0},
            {20.0, 30.0, 40.0, 50.0, 60.0},
            {30.0, 40.0, 50.0, 60.0, 70.0},
            {40.0, 50.0, 60.0, 70.0, 80.0},
            {50.0, 60.0, 70.0, 80.0, 90.0},
    };
    private static final int MIN_POSITION = 0;
    private static final int MAX_X_POSITION = DEFAULT_VALUES[0].length - 1;
    private static final int MAX_Y_POSITION = DEFAULT_VALUES.length - 1;

    @Before
    public void setup() {
        pvFunction = new PositionalVariableFunction(PositionalVariable.HEAD,
                                                    MAX_X_POSITION,
                                                    MAX_Y_POSITION,
                                                    END_SESSION_FOR_MINIMUM_AND_MAXIMUM,
                                                    DEFAULT_VALUES);
    }

    @Test
    public void testGetValueWithSameFunctionAndVariableRanges()
            throws Exception {

        final double[][] values = DEFAULT_VALUES;
        final int maxX = values[0].length - 1;
        final int maxY = values.length - 1;

        TrackerPoint head;
        LarvaFrameData frameData;
        double expectedValue;
        for (int y = 0; y < values.length; y++) {
            for (int x = 0; x < values[y].length; x++) {

                head = new TrackerPoint(x, y);
                frameData = TestUtilities.getFrameDataWithHead(head);
                expectedValue = values[y][x];
                TestUtilities.assertEquals("invalid value returned for " + head,
                                           expectedValue,
                                           pvFunction.getValue(frameData));

                if (x < maxX) {
                    head = new TrackerPoint(x + 0.5, y);
                    frameData = TestUtilities.getFrameDataWithHead(head);
                    expectedValue = (values[y][x] + values[y][x+1]) / 2;
                    TestUtilities.assertEquals("invalid value returned for " +
                                               head,
                                               expectedValue,
                                               pvFunction.getValue(frameData));
                    if (y < maxY) {
                        head = new TrackerPoint(x + 0.5, y + 0.5);
                        frameData = TestUtilities.getFrameDataWithHead(head);
                        expectedValue = ((values[y][x] + 5) +
                                         (values[y+1][x]) + 5) / 2;
                        TestUtilities.assertEquals(
                                "invalid value returned for " +
                                head,
                                expectedValue,
                                pvFunction.getValue(frameData));
                    }
                }

                if (y < maxY) {
                    head = new TrackerPoint(x, y + 0.5);
                    frameData = TestUtilities.getFrameDataWithHead(head);
                    expectedValue = (values[y][x] + values[y+1][x]) / 2;
                    TestUtilities.assertEquals("invalid value returned for " +
                                               head,
                                               expectedValue,
                                               pvFunction.getValue(frameData));
                }
            }
        }

    }

    @Test
    public void testGetValueWithFunctionRangeGreaterThanVariableRange()
            throws Exception {

        final double maxVariableX = 8.0;
        final double maxVariableY = 8.0;

        pvFunction = new PositionalVariableFunction(PositionalVariable.HEAD,
                                                    maxVariableX,
                                                    maxVariableY,
                                                    END_SESSION_FOR_MINIMUM_AND_MAXIMUM,
                                                    DEFAULT_VALUES);

        TrackerPoint head = new TrackerPoint(0.0, 0.0);
        LarvaFrameData frameData = TestUtilities.getFrameDataWithHead(head);
        double expectedValue = DEFAULT_VALUES[0][0];
        TestUtilities.assertEquals("invalid value returned for " + head,
                                   expectedValue,
                                   pvFunction.getValue(frameData));

        head = new TrackerPoint(4.0, 4.0);
        frameData = TestUtilities.getFrameDataWithHead(head);
        expectedValue = DEFAULT_VALUES[2][2];
        TestUtilities.assertEquals("invalid value returned for " + head,
                                   expectedValue,
                                   pvFunction.getValue(frameData));

        head = new TrackerPoint(maxVariableX, maxVariableY);
        frameData = TestUtilities.getFrameDataWithHead(head);
        expectedValue = DEFAULT_VALUES[4][4];
        TestUtilities.assertEquals("invalid value returned for " + head,
                                   expectedValue,
                                   pvFunction.getValue(frameData));


    }

    @Test
    public void testGetValueWithFunctionRangeLessThanVariableRange()
            throws Exception {

        final double maxVariableX = 2.0;
        final double maxVariableY = 2.0;

        pvFunction = new PositionalVariableFunction(PositionalVariable.HEAD,
                                                    maxVariableX,
                                                    maxVariableY,
                                                    END_SESSION_FOR_MINIMUM_AND_MAXIMUM,
                                                    DEFAULT_VALUES);

        TrackerPoint head = new TrackerPoint(0.0, 0.0);
        LarvaFrameData frameData = TestUtilities.getFrameDataWithHead(head);
        double expectedValue = DEFAULT_VALUES[0][0];
        TestUtilities.assertEquals("invalid value returned for " + head,
                                   expectedValue,
                                   pvFunction.getValue(frameData));

        head = new TrackerPoint(1.0, 1.0);
        frameData = TestUtilities.getFrameDataWithHead(head);
        expectedValue = DEFAULT_VALUES[2][2];
        TestUtilities.assertEquals("invalid value returned for " + head,
                                   expectedValue,
                                   pvFunction.getValue(frameData));

        head = new TrackerPoint(maxVariableX, maxVariableY);
        frameData = TestUtilities.getFrameDataWithHead(head);
        expectedValue = DEFAULT_VALUES[4][4];
        TestUtilities.assertEquals("invalid value returned for " + head,
                                   expectedValue,
                                   pvFunction.getValue(frameData));


    }

    @Test(expected = IllegalArgumentException.class)
    public void testMaximumXZeroOrLess() {
        pvFunction =
                new PositionalVariableFunction(
                        pvFunction.getVariable(),
                        -10.0,
                        pvFunction.getMaximumVariableY(),
                        END_SESSION_FOR_MINIMUM_AND_MAXIMUM,
                        DEFAULT_VALUES);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testMaximumYZeroOrLess() {
        pvFunction =
                new PositionalVariableFunction(
                        pvFunction.getVariable(),
                        pvFunction.getMaximumVariableX(),
                        -10.0,
                        END_SESSION_FOR_MINIMUM_AND_MAXIMUM,
                        DEFAULT_VALUES);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNullValues() {
        pvFunction =
                new PositionalVariableFunction(
                        pvFunction.getVariable(),
                        pvFunction.getMaximumVariableX(),
                        pvFunction.getMaximumVariableY(),
                        END_SESSION_FOR_MINIMUM_AND_MAXIMUM,
                        null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testZeroRowValues() {
        pvFunction =
                new PositionalVariableFunction(
                        pvFunction.getVariable(),
                        pvFunction.getMaximumVariableX(),
                        pvFunction.getMaximumVariableY(),
                        END_SESSION_FOR_MINIMUM_AND_MAXIMUM,
                        new double[0][0]);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testZeroColumnValues() {
        pvFunction =
                new PositionalVariableFunction(
                        pvFunction.getVariable(),
                        pvFunction.getMaximumVariableX(),
                        pvFunction.getMaximumVariableY(),
                        END_SESSION_FOR_MINIMUM_AND_MAXIMUM,
                        new double[][] {{},{}});
    }

    @Test
    public void testGetValueWithOutOfRangePosition() {
        // END_SESSION_FOR_MINIMUM_AND_MAXIMUM
        validateGetValueResultForOutOfRangeHead(false, false);

        setPvFunctionErrorHandlingMethod(END_SESSION_FOR_MINIMUM_REPEAT_MAXIMUM);
        validateGetValueResultForOutOfRangeHead(false, true);

        setPvFunctionErrorHandlingMethod(REPEAT_MINIMUM_END_SESSION_FOR_MAXIMUM);
        validateGetValueResultForOutOfRangeHead(true, false);

        setPvFunctionErrorHandlingMethod(REPEAT_MINIMUM_AND_MAXIMUM);
        validateGetValueResultForOutOfRangeHead(true, true);
    }

    private void setPvFunctionErrorHandlingMethod(OutOfRangeErrorHandlingMethod method) {
        pvFunction = new PositionalVariableFunction(pvFunction.getVariable(),
                                                    pvFunction.getMaximumVariableX(),
                                                    pvFunction.getMaximumVariableY(),
                                                    method,
                                                    pvFunction.getValues());
    }

    private void validateGetValueResultForOutOfRangeHead(String context,
                                                         TrackerPoint outOfRangeHead,
                                                         TrackerPoint boundaryHead,
                                                         boolean repeat) {

        final LarvaFrameData outOfRangeFrameData = TestUtilities.getFrameDataWithHead(outOfRangeHead);

        if (repeat) {
            final LarvaFrameData boundaryFrameData = TestUtilities.getFrameDataWithHead(boundaryHead);

            try {
                TestUtilities.assertEquals("invalid value returned for out of range " + context +
                                           " with method " + pvFunction.getPositionRangeErrorHandlingMethod(),
                                           pvFunction.getValue(boundaryFrameData),
                                           pvFunction.getValue(outOfRangeFrameData)
                );
            } catch (IllegalArgumentException e) {
                Assert.fail("out of range " + context + " should not have caused exception with method " +
                            pvFunction.getPositionRangeErrorHandlingMethod().name() + ", exception message was: " +
                            e.getMessage());
            }
        } else {
            try {
                pvFunction.getValue(outOfRangeFrameData);
                Assert.fail("out of range " + context + " should cause exception with method " +
                            pvFunction.getPositionRangeErrorHandlingMethod().name());
            } catch (IllegalArgumentException e) {
                // test passed
            }
        }
    }

    private void validateGetValueResultForOutOfRangeHead(boolean repeatMin,
                                                         boolean repeatMax) {

        final int valueLessThanMin = MIN_POSITION - 1;
        final int valueGreaterThanMaxX = MAX_X_POSITION + 1;
        final int valueGreaterThanMaxY = MAX_Y_POSITION + 1;
        final int yValueInRange = MAX_Y_POSITION / 2;

        final TrackerPoint lessThanMinXYHead = new TrackerPoint(valueLessThanMin, valueLessThanMin);
        final TrackerPoint minXYBoundaryHead = new TrackerPoint(MIN_POSITION, MIN_POSITION);

        final TrackerPoint greaterThanMaxXYHead = new TrackerPoint(valueGreaterThanMaxX, valueGreaterThanMaxY);
        final TrackerPoint maxXYBoundaryHead = new TrackerPoint(MAX_X_POSITION, MAX_Y_POSITION);

        validateGetValueResultForOutOfRangeHead("minimum x and y",
                                                lessThanMinXYHead, minXYBoundaryHead, repeatMin);
        validateGetValueResultForOutOfRangeHead("maximum x and y",
                                                greaterThanMaxXYHead, maxXYBoundaryHead, repeatMax);

        if (! repeatMin) {
            // y checked before x, use valid y to make sure we get to x exception
            final TrackerPoint lessThanMinXOnlyHead = new TrackerPoint(valueLessThanMin, yValueInRange);
            validateGetValueResultForOutOfRangeHead("minimum x only",
                                                    lessThanMinXOnlyHead, minXYBoundaryHead, false);
        }

        if (! repeatMax) {
            // y checked before x, use valid y to make sure we get to x exception
            final TrackerPoint greaterThanMaxXOnlyHead = new TrackerPoint(valueGreaterThanMaxX, yValueInRange);
            validateGetValueResultForOutOfRangeHead("maximum x only",
                                                    greaterThanMaxXOnlyHead, minXYBoundaryHead, false);
        }

    }

}

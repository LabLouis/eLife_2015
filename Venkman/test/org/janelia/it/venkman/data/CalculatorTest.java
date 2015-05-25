/*
 * Copyright (c) 2013 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.venkman.data;

import org.janelia.it.venkman.TestUtilities;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;

import static org.janelia.it.venkman.TestUtilities.getScaledValue;


/**
 * Tests the {@link org.janelia.it.venkman.data.Calculator} class.
 *
 * @author Eric Trautman
 */
public class CalculatorTest {

    private static final int POINTS[][] = {
            // x,y
            {0,0},
            {1,0},
            {2,0},
            {3,0},
            {4,0},
            {4,1},
            {4,2},
            {4,3},
            {4,4},
            {3,4},
            {2,4},
            {1,4},
            {0,4},
            {0,3},
            {0,2},
            {0,1}
    };

    @Test
    public void testGetUnitVectorPointForTrackerAngle() throws Exception {

        // Tracker Angles:
        //                 (0,-1) 90 deg
        //
        //
        //  (-1,0) 0 deg               (1,0) +-180 deg
        //
        //
        //                 (0,1) -90 deg

        final double d = 1 / Math.sqrt(2);
        final Object[][] angleToExpectedPoint = {
                { -180.0, new TrackerPoint( 1,  0) },
                { -135.0, new TrackerPoint( d,  d) },
                {  -90.0, new TrackerPoint( 0,  1) },
                {  -45.0, new TrackerPoint(-d,  d) },
                {    0.0, new TrackerPoint(-1,  0) },
                {   45.0, new TrackerPoint(-d, -d) },
                {   90.0, new TrackerPoint( 0, -1) },
                {  135.0, new TrackerPoint( d, -d) },
                {  180.0, new TrackerPoint( 1,  0) }
        };

        double angle;
        TrackerPoint expected;
        TrackerPoint terminal;
        for (Object[] test : angleToExpectedPoint) {
            angle = (Double) test[0];
            expected = (TrackerPoint) test[1];

            terminal = Calculator.getUnitVectorPointForTrackerAngle(angle);
            TestUtilities.assertEquals(
                    "invalid x coordinate for angle " + angle,
                    expected.getX(), terminal.getX());
            TestUtilities.assertEquals(
                    "invalid y coordinate for angle " + angle,
                    expected.getY(), terminal.getY());
        }
    }

    @Test
    public void testMidpointAngleMethods() throws Exception {


        final TrackerPoint midpoint = new TrackerPoint(2,2);
        TrackerPoint tail;
        TrackerPoint head;
        double dotProductAngle;
        double cosineRuleAngle;
        for (int[] tailPoint : POINTS) {

            tail = new TrackerPoint(tailPoint[0], tailPoint[1]);

            for (int[] headPoint : POINTS) {
                head = new TrackerPoint(headPoint[0], headPoint[1]);

                dotProductAngle =
                        Calculator.getDotProductMidpointAngle(tail,
                                                              midpoint,
                                                              head);

                cosineRuleAngle =
                        Calculator.getCosineRuleMidpointAngle(tail,
                                                              midpoint,
                                                              head);
                Assert.assertEquals(
                        "results do not match for tail " +
                        tail + ", midpoint " +
                        midpoint + " and head " + head,
                        getScaledValue(dotProductAngle),
                        getScaledValue(cosineRuleAngle));
            }
        }
    }

    @Test
    public void testIsCoordinateLeftOfVector() throws Exception {

        // Note: when coordinate is on line, expected result is false
        ArrayList<Boolean> expected = new ArrayList<Boolean>(POINTS.length);
        expected.addAll(Arrays.asList(
                false, false, false, false, false, false, false, false,
                false, true,  true,  true,  true,  true,  true,  true));

        Assert.assertEquals("expected value list size " +
                            "must be same as point list length",
                            POINTS.length,
                            expected.size());

        final TrackerPoint vectorStart = new TrackerPoint(2,2);
        TrackerPoint vectorStop;
        TrackerPoint coordinate;
        boolean result;
        for (int[] stopPoint : POINTS) {

            vectorStop = new TrackerPoint(stopPoint[0], stopPoint[1]);

            for (int j = 0; j < POINTS.length; j++) {

                coordinate = new TrackerPoint(POINTS[j][0], POINTS[j][1]);

                result = Calculator.isCoordinateLeftOfVector(coordinate,
                                                             vectorStart,
                                                             vectorStop);
                Assert.assertEquals(
                        "invalid result for coordinate " +
                        coordinate + " and vector " +
                        vectorStart + " to " + vectorStop,
                        expected.get(j),
                        result);
            }

            Boolean last = expected.remove(expected.size() - 1);
            expected.add(0, last);
        }
    }
    
    @Test
    public void testGetIntersectionOfLines() throws Exception {

        Integer testData[][] = {
                // a1     a2     b1     b2     expected
                {  1,0,   2,0,   0,1,   0,2,   0,0},
                {  1,1,   3,3,   9,1,   2,2,   2,2},
                {  0,0,   0,2,   2,0,   1,0,   0,0}, // line 1 slope infinity
                {  2,0,   1,0,   0,0,   0,2,   0,0}, // line 2 slope infinity
                {  2,2,   3,3,   1,2,   1,3,   1,1}, // line 2 slope infinity
                {  1,0,   2,0,   1,1,   2,1,   null,null}, // parallel
                {  1,0,   2,0,   1,0,   2,0,   null,null}, // same
                {  0,0,   0,2,   0,0,   0,2,   null,null}, // same infinity
                {  0,0,   0,2,   0,2,   0,0,   null,null}  // opposite infinity
        };

        TrackerPoint a1;
        TrackerPoint a2;
        TrackerPoint b1;
        TrackerPoint b2;
        TrackerPoint expected;
        Integer expectedX;
        TrackerPoint result;
        for (Integer[] test : testData) {
            a1 = new TrackerPoint(test[0], test[1]);
            a2 = new TrackerPoint(test[2], test[3]);
            b1 = new TrackerPoint(test[4], test[5]);
            b2 = new TrackerPoint(test[6], test[7]);
            expectedX = test[8];
            if (expectedX == null) {
                expected = null;
            } else {
                expected = new TrackerPoint(expectedX, test[9]);
            }

            result = Calculator.getIntersectionOfLines(a1, a2, b1, b2);

            Assert.assertEquals(
                    "invalid intersection for line 1 through " +
                    a1 + " and " + a2 + " and line 2 through " +
                    b1 + " and " + b2,
                    String.valueOf(expected),
                    String.valueOf(result));
        }

    }

    @Test
    public void testGetArea() throws Exception {

        ArrayList<TrackerPoint> perimeter = null;
        double area = Calculator.getArea(perimeter);
        Assert.assertEquals(
                "invalid area for null perimeter",
                getScaledValue(0.0),
                getScaledValue(area));

        perimeter = new ArrayList<TrackerPoint>();
        area = Calculator.getArea(perimeter);
        Assert.assertEquals(
                "invalid area for empty perimeter",
                getScaledValue(0.0),
                getScaledValue(area));

        perimeter.add(new TrackerPoint(0,0));
        area = Calculator.getArea(perimeter);
        Assert.assertEquals(
                "invalid area for single point",
                getScaledValue(0.0),
                getScaledValue(area));

        perimeter.add(new TrackerPoint(0,2));
        area = Calculator.getArea(perimeter);
        Assert.assertEquals(
                "invalid area for line",
                getScaledValue(0.0),
                getScaledValue(area));

        perimeter.add(new TrackerPoint(2,2));
        area = Calculator.getArea(perimeter);
        Assert.assertEquals(
                "invalid area for triangle",
                getScaledValue(2.0),
                getScaledValue(area));

        perimeter.add(new TrackerPoint(2,0));
        area = Calculator.getArea(perimeter);
        Assert.assertEquals(
                "invalid area for square",
                getScaledValue(4.0),
                getScaledValue(area));
    }

    @Test
    public void testGetLinearInterpolation() throws Exception {

        double[][] data = {
                // actualInput, previousInput, previousOutput, nextInput, nextOutput, expectedOutput
                {  0.0,         0.0,           10.0,           1.0,       20.0,       10.0},
                {  0.3,         0.0,           10.0,           1.0,       20.0,       13.0},
                {  0.5,         0.0,           10.0,           1.0,       20.0,       15.0},
                {  1.0,         0.0,           10.0,           1.0,       20.0,       20.0}
        };

        double actualInput;
        double previousInput;
        double previousOutput;
        double nextInput;
        double nextOutput;
        double expectedOutput;
        for (double[] row : data) {
            actualInput = row[0];
            previousInput = row[1];
            previousOutput = row[2];
            nextInput = row[3];
            nextOutput = row[4];
            expectedOutput = row[5];

            TestUtilities.assertEquals(
                    "invalid result for actual input " + actualInput,
                    expectedOutput,
                    Calculator.getLinearInterpolation(actualInput,
                                                      previousInput,
                                                      previousOutput,
                                                      nextInput,
                                                      nextOutput));
        }

    }

    @Test
    public void testRotatePoint() {

        TrackerPoint rotationCenter = new TrackerPoint(0, 0);
        TrackerPoint originalPoint = new TrackerPoint(4, 0);

        // c = 4; a^2 + b^2 = 16; a = b; 2(a^2) = 16; a^2 = 8; a = b = sqrt of 8
        final double sqRtOf8 = Math.sqrt(8.0);

        double[][] data = {
                // angle, rotatedX, rotatedY
                {    0.0,      4.0,      0.0},
                {   45.0,  sqRtOf8,  sqRtOf8},
                {   90.0,      0.0,      4.0},
                {  135.0, -sqRtOf8,  sqRtOf8},
                {  180.0,     -4.0,      0.0},
                {  225.0, -sqRtOf8, -sqRtOf8},
                {  270.0,      0.0,     -4.0},
                {  315.0,  sqRtOf8, -sqRtOf8},
                {  360.0,      4.0,      0.0},
                { -360.0,      4.0,      0.0},
                { -315.0,  sqRtOf8,  sqRtOf8},
                { -270.0,      0.0,      4.0},
                { -225.0, -sqRtOf8,  sqRtOf8},
                { -180.0,     -4.0,      0.0},
                { -135.0, -sqRtOf8, -sqRtOf8},
                {  -90.0,      0.0,     -4.0},
                {  -45.0,  sqRtOf8, -sqRtOf8}
        };


        double angleInDegrees;
        TrackerPoint expectedRotatedPoint;
        for (double[] row : data) {
            angleInDegrees = row[0];
            expectedRotatedPoint = new TrackerPoint(row[1], row[2]);

            TestUtilities.assertEquals(
                    "invalid result for " + angleInDegrees + " degree angle",
                    expectedRotatedPoint,
                    Calculator.getRotatedPoint(originalPoint,
                                               Math.toRadians(angleInDegrees),
                                               rotationCenter));
        }
    }

    @Test
    public void testGetPointOnVector() {

        final TrackerPoint vectorStart = new TrackerPoint(0, 0);

        final double sqRtOf2 = Math.sqrt(2.0);

        double[][] data = {
                // stopX,  stopY,  distance, expectedX, expectedY
                {    2.0,    0.0,       1.0,       1.0,       0.0},
                {    2.0,    0.0,       3.0,       3.0,       0.0},
                {    2.0,    2.0,   sqRtOf2,       1.0,       1.0}
        };


        TrackerPoint vectorStop;
        double distance;
        TrackerPoint expectedPoint;
        for (double[] row : data) {
            vectorStop = new TrackerPoint(row[0], row[1]);
            distance = row[2];
            expectedPoint = new TrackerPoint(row[3], row[4]);

            TestUtilities.assertEquals(
                    "invalid result for " + vectorStop +
                    " and distance " + distance,
                    expectedPoint,
                    Calculator.getPointOnVector(vectorStart,
                                                vectorStop,
                                                distance));
        }
    }

    @Test
    public void testGetAngleBetweenVectors() {

        double[][] data = {
                // v1-start,    v1-stop,   v2-start,    v2-stop,   angle
                {  2.0, 2.0,   3.0, 3.0,   4.0, 1.0,   3.0, 2.0,   90.0},
                {  2.0, 2.0,   1.0, 1.0,   4.0, 1.0,   3.0, 2.0,   90.0},
                {  2.0, 2.0,   2.0, 3.0,   4.0, 1.0,   4.0, 3.0,    0.0},
                {  4.0, 0.0,   5.0, 0.0,   4.0, 0.0,   4.0, 4.0,   90.0},
        };

        TrackerPoint v1Start;
        TrackerPoint v1Stop;
        TrackerPoint v2Start;
        TrackerPoint v2Stop;
        double expectedAngleInDegrees;
        double absoluteRotationAngleInRadians;
        for (double[] row : data) {
            v1Start = new TrackerPoint(row[0], row[1]);
            v1Stop = new TrackerPoint(row[2], row[3]);
            v2Start = new TrackerPoint(row[4], row[5]);
            v2Stop = new TrackerPoint(row[6], row[7]);
            expectedAngleInDegrees = row[8];

            absoluteRotationAngleInRadians =
                    Calculator.getAngleBetweenVectors(v1Start, v1Stop,
                                                      v2Start, v2Stop);
            TestUtilities.assertEquals(
                    "invalid result for v1:" + v1Start + ", " + v1Stop +
                    " and v2: " + v2Start + ", " + v2Stop,
                    expectedAngleInDegrees,
                    Math.toDegrees(absoluteRotationAngleInRadians));
        }
    }

}
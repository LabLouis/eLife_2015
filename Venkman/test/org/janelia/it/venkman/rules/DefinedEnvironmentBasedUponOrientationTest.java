/*
 * Copyright (c) 2014 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.venkman.rules;

import junit.framework.Assert;
import org.janelia.it.venkman.TestUtilities;
import org.janelia.it.venkman.config.rules.BehaviorLimitedKinematicVariableFunctionList;
import org.janelia.it.venkman.config.rules.LEDFlashPattern;
import org.janelia.it.venkman.config.rules.LEDStimulus;
import org.janelia.it.venkman.config.rules.PositionalVariable;
import org.janelia.it.venkman.config.rules.PositionalVariableFunction;
import org.janelia.it.venkman.data.LarvaFrameData;
import org.janelia.it.venkman.data.LarvaSkeleton;
import org.janelia.it.venkman.data.TrackerPoint;
import org.janelia.it.venkman.log.StandardOutLogger;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.janelia.it.venkman.config.rules.OutOfRangeErrorHandlingMethod.END_SESSION_FOR_MINIMUM_AND_MAXIMUM;

/**
 * Tests the {@link DefinedEnvironmentBasedUponOrientation} class.
 *
 * @author Eric Trautman
 */
public class DefinedEnvironmentBasedUponOrientationTest {

    // Test Setup/Design:
    //
    // arena: max of 8                    intensity:
    //
    //      0 1 2 3 4 5 6 7 8                 0 1 2 3 4 5 6 7 8
    //    -------------------               -------------------
    //  0 |       t-m-h                   0 | 0 0 0 0 0 0 0 0 0
    //  1 |                               1 | 1 1 1 1 1 1 1 1 1
    //  2 |                               2 | 2 2 2 2 2 2 2 2 2
    //  3 |                               3 | 3 3 3 3 3 3 3 3 3
    //  4 |         C                     4 | 4 4 4 4 4 4 4 4 4
    //  5 |                               5 | 5 5 5 5 5 5 5 5 5
    //  6 |                               6 | 6 6 6 6 6 6 6 6 6
    //  7 |                               7 | 7 7 7 7 7 7 7 7 7
    //  8 |                               8 | 8 8 8 8 8 8 8 8 8
    //
    // configured distance from center = 2
    // arena center = (4,4)
    // larva centroid == larva midpoint
    // actual position = h(5,0), m(4,0), t(3,0)
    // actual centroid distance from center = 4
    // rotated position = h(4,1), m(4,0), t(4,-1)
    // transformed position = h(4,3), m(4,2), t(4,1)
    //
    // intensity at transformed head (4,3): 3

    private static final double MAX_VALUE = 8.0;
    private static final double[][] VALUES = {
            {      0.0,       0.0},
            {MAX_VALUE, MAX_VALUE}
    };

    private static final LEDFlashPattern DURATION = new LEDFlashPattern("1000");

    private static final PositionalVariableFunction PV_FUNCTION =
            new PositionalVariableFunction(PositionalVariable.HEAD,
                                           MAX_VALUE,
                                           MAX_VALUE,
                                           END_SESSION_FOR_MINIMUM_AND_MAXIMUM,
                                           VALUES);

    private DefinedEnvironmentBasedUponOrientation rules;
    private LarvaSkeleton skeleton;
    private long orientationDerivationDuration;
    private double centroidDistanceFromArenaCenter;
    private double expectedIntensity;
    private long captureTime;
    private List<LarvaFrameData> frameHistory;

    @Before
    public void setUp() {

        orientationDerivationDuration = 50;
        centroidDistanceFromArenaCenter = 2;

        rules = new DefinedEnvironmentBasedUponOrientation(DURATION,
                                                           PV_FUNCTION,
                                                           0.0,
                                                           orientationDerivationDuration,
                                                           centroidDistanceFromArenaCenter,
                                                           0,
                                                           new BehaviorLimitedKinematicVariableFunctionList());
        rules.init(new StandardOutLogger());

        final TrackerPoint head = new TrackerPoint(5,0);
        final TrackerPoint midpointAndCentroid = new TrackerPoint(4,0);
        final TrackerPoint tail = new TrackerPoint(3,0);
        skeleton = new LarvaSkeleton(0,
                                     head,
                                     midpointAndCentroid,
                                     tail,
                                     2,
                                     midpointAndCentroid,
                                     0,
                                     180);
        expectedIntensity = 0;
        captureTime = -30;
        frameHistory = new ArrayList<LarvaFrameData>();
    }

    @Test
    public void testDetermineStimulus() {

        // expectedIntensity = 0
        LEDStimulus stimulus = addFrameAndGetStimulus();
        validateStimulus("invalid stimulus before derivation duration 1",
                         stimulus);

        stimulus = addFrameAndGetStimulus();
        validateStimulus("invalid stimulus before derivation duration 2",
                         stimulus);

        // after rotation/translation, intensity should always be 3
        expectedIntensity = 3;

        final double sqrtOfOneHalf = Math.sqrt(0.5);

        Object[][] tests = {
                // head, tail
                {
                        new TrackerPoint(5, 0),
                        new TrackerPoint(3, 0),
                        90.0
                },
                {
                        new TrackerPoint(4 + sqrtOfOneHalf, sqrtOfOneHalf),
                        new TrackerPoint(4 - sqrtOfOneHalf, -sqrtOfOneHalf),
                        45.0
                },
                {
                        new TrackerPoint(4, 1),
                        new TrackerPoint(4, -1),
                        0.0
                },
                {
                        new TrackerPoint(4 - sqrtOfOneHalf, sqrtOfOneHalf),
                        new TrackerPoint(4 + sqrtOfOneHalf, -sqrtOfOneHalf),
                        -45.0
                },
                {
                        new TrackerPoint(3, 0),
                        new TrackerPoint(5, 0),
                        -90.0
                },
                {
                        new TrackerPoint(4 - sqrtOfOneHalf, -sqrtOfOneHalf),
                        new TrackerPoint(4 + sqrtOfOneHalf, sqrtOfOneHalf),
                        -135.0
                },
                {
                        new TrackerPoint(4, -1),
                        new TrackerPoint(4, 1),
                        -180.0
                },
                {
                        new TrackerPoint(4 + sqrtOfOneHalf, -sqrtOfOneHalf),
                        new TrackerPoint(4 - sqrtOfOneHalf, sqrtOfOneHalf),
                        135.0
                },
                {
                        new TrackerPoint(5, 0),
                        new TrackerPoint(3, 0),
                        90.0
                },
        };

        TrackerPoint head;
        final TrackerPoint midpointAndCentroid = skeleton.getMidpoint();
        TrackerPoint tail;
        Double expectedRotationInDegrees;
        double orientationOffsetInDegrees = 0;
        for (Object testData[] : tests) {

            head = (TrackerPoint) testData[0];
            tail = (TrackerPoint) testData[1];
            expectedRotationInDegrees = (Double) testData[2];

            skeleton = new LarvaSkeleton(0,
                                         head,
                                         midpointAndCentroid,
                                         tail,
                                         2,
                                         midpointAndCentroid,
                                         0,
                                         0); // tail bearing does not matter

            TestUtilities.assertEquals(
                    "invalid distance from head " + head + " to tail " + tail,
                    2.0,
                    head.distance(tail));
            TestUtilities.assertEquals(
                    "invalid distance from midpoint " + midpointAndCentroid +
                    " to tail " + tail,
                    1.0,
                    midpointAndCentroid.distance(tail));

            // create a new rules instance so that rotation
            // is based on test coordinates
            rules = new DefinedEnvironmentBasedUponOrientation(DURATION,
                                                               PV_FUNCTION,
                                                               0.0,
                                                               orientationDerivationDuration,
                                                               centroidDistanceFromArenaCenter,
                                                               orientationOffsetInDegrees,
                                                               new BehaviorLimitedKinematicVariableFunctionList());
            rules.init(new StandardOutLogger());

            stimulus = addFrameAndGetStimulus();

            TestUtilities.assertEquals(
                    "invalid rotation for head " + head + " and tail " + tail,
                    expectedRotationInDegrees,
                    Math.toDegrees(rules.getRotationAngleInRadians()));

            validateStimulus("invalid derived stimulus for head " + head +
                             " and tail " + tail,
                             stimulus);
        }

    }

    @Test
    public void testOrientationOffsetAndRotation() {

        // add 2 frames to get past derivation duration
        addFrameAndGetStimulus();
        addFrameAndGetStimulus();

        // test with 90 degree offset:
        //   original head (5,0) should be rotated 180 (90+90) degrees to (0,5)
        //   and then transformed to (3,2)
        final double orientationOffsetInDegrees = 90;
        final double expectedRotationInDegrees = 90 + orientationOffsetInDegrees;
        final double expectedOriginalTransformedX = 3.0;
        final double expectedOriginalTransformedY = 2.0;

        rules = new DefinedEnvironmentBasedUponOrientation(DURATION,
                                                           PV_FUNCTION,
                                                           0.0,
                                                           orientationDerivationDuration,
                                                           centroidDistanceFromArenaCenter,
                                                           orientationOffsetInDegrees,
                                                           new BehaviorLimitedKinematicVariableFunctionList());
        rules.init(new StandardOutLogger());

        addFrameAndGetStimulus();

        TestUtilities.assertEquals(
                "invalid rotation for orientation offset of " +
                orientationOffsetInDegrees,
                expectedRotationInDegrees,
                Math.toDegrees(rules.getRotationAngleInRadians()));

        TestUtilities.assertEquals(
                "invalid transformed X for orientation offset of " +
                orientationOffsetInDegrees,
                expectedOriginalTransformedX,
                rules.getTransformedX());

        TestUtilities.assertEquals(
                "invalid transformed Y for orientation offset of " +
                orientationOffsetInDegrees,
                expectedOriginalTransformedY,
                rules.getTransformedY());

        final TrackerPoint originalHead = skeleton.getHead();
        final TrackerPoint originalMidpointAndCentroid = skeleton.getMidpoint();
        final TrackerPoint originalTail = skeleton.getTail();

        // ---------------------------------
        // verify rotation is relative to original (not current) centroid

        // next: head (6, 0), midpointAndCentroid (5,0), tail (4,0)
        final TrackerPoint nextHead =
                new TrackerPoint(originalHead.getX() + 1,
                                 originalHead.getY());
        final TrackerPoint nextMidpointAndCentroid =
                new TrackerPoint(originalMidpointAndCentroid.getX() + 1,
                                 originalMidpointAndCentroid.getY());
        final TrackerPoint nextTail =
                new TrackerPoint(originalTail.getX() + 1,
                                 originalTail.getY());

        skeleton = new LarvaSkeleton(skeleton.getCaptureTime(),
                                     nextHead,
                                     nextMidpointAndCentroid,
                                     nextTail,
                                     2,
                                     nextMidpointAndCentroid,
                                     0,
                                     0); // tail bearing does not matter

        addFrameAndGetStimulus();

        TestUtilities.assertEquals(
                "rotation angle should remain the same",
                expectedRotationInDegrees,
                Math.toDegrees(rules.getRotationAngleInRadians()));

        TestUtilities.assertEquals(
                "rotation center should remain the same",
                originalMidpointAndCentroid,
                rules.getRotationCenter());

        TestUtilities.assertEquals(
                "transformed X should decrease by one after 180 degree rotation " +
                "around the original centroid",
                expectedOriginalTransformedX - 1,
                rules.getTransformedX());

        TestUtilities.assertEquals(
                "transformed Y should remain the same",
                expectedOriginalTransformedY,
                rules.getTransformedY());
    }

    @Test
    public void testForDaeyeon() {

        // add 2 frames to get past derivation duration
        addFrameAndGetStimulus();
        addFrameAndGetStimulus();

        // test using coordinates from email (don't worry about stimulus values)

        final TrackerPoint head = new TrackerPoint(4, 10);
        final TrackerPoint midpoint = new TrackerPoint(5, 9);
        final TrackerPoint tail = new TrackerPoint(6, 7);
        final TrackerPoint centroid = new TrackerPoint(5, 8);
        skeleton = new LarvaSkeleton(0,
                                     head,
                                     midpoint,
                                     tail,
                                     0,  // length does not matter
                                     centroid,
                                     0,  // head angle does not matter
                                     0); // tail bearing does not matter

        final TrackerPoint arenaCenter = new TrackerPoint(20, 20);
        PositionalVariableFunction pvFunction =
                new PositionalVariableFunction(PositionalVariable.HEAD,
                                               arenaCenter.getX() * 2,
                                               arenaCenter.getY() * 2,
                                               END_SESSION_FOR_MINIMUM_AND_MAXIMUM,
                                               VALUES);
        centroidDistanceFromArenaCenter = 5;

        double[][] tests = {
                {  0, 17.8417, 18.2734},
                { 45, 16.3426, 19.0989},
                {-45, 18.3180, 16.6296}
        };

        final double expectedRotationWithoutOffsetInDegrees = -77.91;
        double orientationOffsetInDegrees;
        double expectedX;
        double expectedY;
        double expectedRotationInDegrees;
        for (double[] test : tests) {
            orientationOffsetInDegrees = test[0];
            expectedX = test[1];
            expectedY = test[2];

            rules = new DefinedEnvironmentBasedUponOrientation(DURATION,
                                                               pvFunction,
                                                               0.0,
                                                               orientationDerivationDuration,
                                                               centroidDistanceFromArenaCenter,
                                                               orientationOffsetInDegrees,
                                                               new BehaviorLimitedKinematicVariableFunctionList());
            rules.init(new StandardOutLogger());

            addFrameAndGetStimulus();

            expectedRotationInDegrees =
                    expectedRotationWithoutOffsetInDegrees +
                    orientationOffsetInDegrees;

            TestUtilities.assertEquals(
                    "invalid rotation for orientation offset of " +
                    orientationOffsetInDegrees,
                    expectedRotationInDegrees,
                    Math.toDegrees(rules.getRotationAngleInRadians()));

            TestUtilities.assertEquals(
                    "invalid transformed X for orientation offset of " +
                    orientationOffsetInDegrees,
                    expectedX,
                    rules.getTransformedX());

            TestUtilities.assertEquals(
                    "invalid transformed Y for orientation offset of " +
                    orientationOffsetInDegrees,
                    expectedY,
                    rules.getTransformedY());
        }
    }

    private void validateStimulus(String errorMessage,
                                  LEDStimulus stimulus) {
        final LEDStimulus expectedStimulus =
                DURATION.getStimulusList(expectedIntensity).get(0);
        Assert.assertEquals(errorMessage,
                            expectedStimulus, stimulus);
    }

    private LEDStimulus addFrameAndGetStimulus() {
        addFrame();
        @SuppressWarnings("unchecked")
        final List<LEDStimulus> stimulusList = (List<LEDStimulus>)
                rules.determineStimulus(frameHistory, null);
        return stimulusList.get(0);
    }

    private void addFrame() {
        captureTime += 30;
        final LarvaFrameData frameData =
                new LarvaFrameData(TestUtilities.getSkeleton(captureTime,
                                                             skeleton));
        frameHistory.add(0, frameData);
    }
}
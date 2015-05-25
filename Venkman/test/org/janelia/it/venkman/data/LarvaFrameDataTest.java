/*
 * Copyright (c) 2014 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.venkman.data;

import org.janelia.it.venkman.TestUtilities;
import org.janelia.it.venkman.config.LarvaBehaviorParameters;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.LinkedList;
import java.util.List;

import static org.janelia.it.venkman.TestUtilities.getScaledValue;

/**
 * Tests the {@link LarvaFrameData} class.
 *
 * @author Eric Trautman
 */
public class LarvaFrameDataTest {

    private LarvaBehaviorParameters parameters;
    private LarvaSkeleton currentSkeleton;
    private LarvaFrameData currentFrame;
    private LarvaFrameData previousFrame;
    private List<LarvaFrameData> frameHistory;
    private long currentTime;

    @Before
    public void setUp() throws Exception {

        parameters = new LarvaBehaviorParameters();
        parameters.setMinBodyAngleSpeedForTurns(1.0);
        parameters.setMinBodyAngleSpeedDuration(970);
        parameters.setMinHeadAngleToContinueTurning(0.25);
        parameters.setMinHeadAngleForCasting(2.0);
        parameters.setMinHeadAngleToContinueCasting(1.25);
        parameters.setMinHeadAngleSpeedToContinueCasting(1.25);
        parameters.setDotProductThresholdForStraightModes(3.0);
        parameters.setMinBehaviorModeDuration(0);
        parameters.setMinStopOrBackUpDuration(0);

        currentSkeleton =
                new LarvaSkeleton(1030,
                                  new TrackerPoint(0,2),  // head
                                  new TrackerPoint(0,1),  // midpoint
                                  new TrackerPoint(0,0),  // tail
                                  2,                      // length
                                  new TrackerPoint(0,1),  // centroid
                                  0,                      // headToBodyAngle
                                  90);                    // tailBearing
        currentFrame = new LarvaFrameData(currentSkeleton);

        LarvaSkeleton previousSkeleton =
                new LarvaSkeleton(1000,
                                  new TrackerPoint(1,2),  // head
                                  new TrackerPoint(1,1),  // midpoint
                                  new TrackerPoint(1,0),  // tail
                                  2,                      // length
                                  new TrackerPoint(1,1),  // centroid
                                  0,                      // headToBodyAngle
                                  90);                    // tailBearing
        previousFrame = new LarvaFrameData(previousSkeleton);

        frameHistory = new LinkedList<LarvaFrameData>();
        currentTime = 0;
    }

    @Test
    public void testJAXBWithLEDStimulus() throws Exception {

        final String xml =
                "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
                "<larvaFrameData derivedMaxLength=\"0.0\" timeSinceLastBehaviorModeChange=\"0\" smoothedTailSpeedDotBodyAngle=\"0.0\" tailSpeedDotBodyAngle=\"0.0\" centroidSpeed=\"0.0\" headSpeed=\"0.0\" midpointSpeed=\"0.0\" tailSpeed=\"0.0\" smoothedHeadAngleSpeed=\"0.0\" headAngleSpeed=\"0.0\" smoothedBodyAngleSpeed=\"0.0\" bodyAngleSpeed=\"0.0\" behaviorMode=\"STOP\">\n" +
                "    <skeleton tailBearing=\"-122.914875\" headToBodyAngle=\"-24.323909\" length=\"6.805315\" captureTime=\"0\">\n" +
                "        <head y=\"206.426326\" x=\"201.004671\"/>\n" +
                "        <midpoint y=\"203.703781\" x=\"201.626262\"/>\n" +
                "        <tail y=\"201.217655\" x=\"201.122126\"/>\n" +
                "        <centroid y=\"203.822857\" x=\"201.579341\"/>\n" +
                "    </skeleton>\n" +
                "    <ledStimulus>\n" +
                "        <intensityPercentage>0.0</intensityPercentage>\n" +
                "        <duration>1000</duration>\n" +
                "    </ledStimulus>\n" +
                "</larvaFrameData>\n";

        TestUtilities.validateJAXBMarshalling(xml, LarvaFrameData.class);
    }

    @Test
    public void testSetMotionState() throws Exception {

        // --------------------------------------
        // body angle speed exceeds threshold but previous frame is not casting
        // dot product exceeds threshold

        previousFrame.setValuesForTesting(LarvaBehaviorMode.RUN, 0.0);

        testAndValidateMode(
                1.5, // smoothedBodyAngleSpeed > minBodyAngleSpeedForTurns (1.0)
                0.0, // headAngle < minHeadAngleForCasting (2.0)
                true,
                0.0,
                3.5, // smoothedTailSpeedDotBodyAngle > dotProductThreshold (3.0)
                LarvaBehaviorMode.RUN);

        // --------------------------------------
        // body angle speed exceeds threshold and previous frame is casting
        // should turn in direction of previous cast

        previousFrame.setValuesForTesting(LarvaBehaviorMode.CAST_LEFT, 0.0);

        testAndValidateMode(
                -1.5, // smoothedBodyAngleSpeed > minBodyAngleSpeedForTurns (1.0)
                0.0,
                true,
                0.0,
                0.0,
                LarvaBehaviorMode.TURN_LEFT);

        // --------------------------------------
        // body angle speed exceeds threshold and previous frame is casting
        // should turn in direction of previous cast

        previousFrame.setValuesForTesting(LarvaBehaviorMode.CAST_RIGHT, 0.0);

        testAndValidateMode(
                1.5, // smoothedBodyAngleSpeed > minBodyAngleSpeedForTurns (1.0)
                0.0,
                false,
                0.0,
                0.0,
                LarvaBehaviorMode.TURN_RIGHT);

        previousFrame.setValuesForTesting(LarvaBehaviorMode.TURN_RIGHT, 0.0);

        // --------------------------------------
        // body angle speed exceeds threshold and previous frame is turning
        // should turn in direction of previous turn

        testAndValidateMode(
                1.5, // smoothedBodyAngleSpeed > minBodyAngleSpeedForTurns (1.0)
                0.5, // headAngle > minHeadAngleToContinueTurning (0.25)
                false,
                0.0,
                0.0,
                LarvaBehaviorMode.TURN_RIGHT);

        // --------------------------------------
        // head angle speed less than threshold and previous frame is turning
        // dot product less than negative threshold
        // should return from turn to back-up

        testAndValidateMode(
                1.5, // smoothedBodyAngleSpeed > minBodyAngleSpeedForTurns (1.0)
                0.0, // headAngle < minHeadAngleToContinueTurning (0.25)
                false,
                0.0,
                -3.5, // smoothedTailSpeedDotBodyAngle < -dotProductThreshold (-3.0)
                LarvaBehaviorMode.BACK_UP);

        // --------------------------------------
        // head angle greater than threshold
        // previous frame is not casting or turning
        // should cast in direction of head

        previousFrame.setValuesForTesting(LarvaBehaviorMode.RUN, 0.0);

        testAndValidateMode(
                0.5, // smoothedBodyAngleSpeed < minBodyAngleSpeedForTurns (1.0)
                2.5, // headAngle > minHeadAngleForCasting (2.0)
                true,
                0.0,
                0.0,
                LarvaBehaviorMode.CAST_LEFT);

        testAndValidateMode(
                0.5, // smoothedBodyAngleSpeed < minBodyAngleSpeedForTurns (1.0)
                2.5, // headAngle > minHeadAngleForCasting (2.0)
                false,
                0.0,
                0.0,
                LarvaBehaviorMode.CAST_RIGHT);

        previousFrame.setValuesForTesting(LarvaBehaviorMode.CAST_LEFT, 0.0);

        // --------------------------------------
        // head angle less than casting entry threshold but
        // greater than continue threshold when previous frame is casting
        // should continue cast in direction of previous cast

        testAndValidateMode(
                0.5, // smoothedBodyAngleSpeed < minBodyAngleSpeedForTurns (1.0)
                1.5, // headAngle < minHeadAngleForCasting (2.0)
                     // headAngle > minHeadAngleToContinueCasting (1.25)
                true,
                1.5, // smoothedHeadAngleSpeed > minHeadAngleSpeedToContinueCasting (1.25)
                0.0,
                LarvaBehaviorMode.CAST_LEFT);

        // --------------------------------------
        // head angle less than casting entry threshold and
        // less than continue threshold when previous frame is casting
        // smoothed HeadAngleSpeed also less than continue threshold
        // dot product in between positive and negative threshold
        // should return from cast to stop

        testAndValidateMode(
                0.5, // smoothedBodyAngleSpeed < minBodyAngleSpeedForTurns (1.0)
                1.0, // headAngle < minHeadAngleToContinueCasting (1.25)
                true,
                1.0, // smoothedHeadAngleSpeed < minHeadAngleSpeedToContinueCasting (1.25)
                2.5, // -dotProductThreshold (-3.0) < smoothedTailSpeedDotBodyAngle < dotProductThreshold (3.0)
                LarvaBehaviorMode.STOP);

        // --------------------------------------
        // normally, these inputs would result in CAST_LEFT
        // but previous frame mode should stick
        // since minimum duration is not exceeded

        parameters.setMinBehaviorModeDuration(60);
        previousFrame.setValuesForTesting(LarvaBehaviorMode.BACK_UP, 0.0);

        testAndValidateMode(
                0.5, // smoothedBodyAngleSpeed < minBodyAngleSpeedForTurns (1.0)
                2.5, // headAngle > minHeadAngleForCasting (2.0)
                true,
                0.0,
                0.0,
                LarvaBehaviorMode.BACK_UP);
    }

    @Test
    public void testCalculateDerivedDataWithNoHistory() throws Exception {

        currentFrame.calculateDerivedData(frameHistory, parameters);

        Assert.assertEquals("velocity should not be calculated without history",
                            getScaledValue(0.0),
                            getScaledValue(currentFrame.getTailSpeed()));

        Assert.assertEquals("invalid state returned",
                            LarvaBehaviorMode.STOP,
                            currentFrame.getBehaviorMode());
    }

    @Test
    public void testCalculateDerivedDataVelocities() throws Exception {

        frameHistory.add(previousFrame);

        // add another dummy frame to make sure older frames are ignored
        LarvaFrameData dummyFrame = new LarvaFrameData(buildEmptySkeleton(0));
        frameHistory.add(dummyFrame);

        currentFrame.calculateDerivedData(frameHistory, parameters);

        Assert.assertEquals("invalid tail velocity calculated",
                            getScaledValue(33.33),
                            getScaledValue(currentFrame.getTailSpeed()));

        Assert.assertEquals("invalid mid point velocity calculated",
                            getScaledValue(33.33),
                            getScaledValue(currentFrame.getMidpointSpeed()));

        Assert.assertEquals("invalid head velocity calculated",
                            getScaledValue(33.33),
                            getScaledValue(currentFrame.getHeadSpeed()));

        Assert.assertEquals("invalid centroid velocity calculated",
                            getScaledValue(33.33),
                            getScaledValue(currentFrame.getCentroidSpeed()));
    }

    @Test
    public void testDetectBackwardsMotion() throws Exception {

        parameters.setMinBodyAngleSpeedDuration(10);
        parameters.setDotProductThresholdForStraightModes(0.1);

        validateStartingRun();
        validateStationaryPosition(LarvaBehaviorMode.STOP);
        validateReverse(LarvaBehaviorMode.BACK_UP);

    }

    @Test
    public void testMinStopOrBackupDuration() throws Exception {

        parameters.setMinBodyAngleSpeedDuration(10);
        parameters.setDotProductThresholdForStraightModes(0.1);
        parameters.setMinStopOrBackUpDuration(10); // at least one frame

        validateStartingRun();
        validateStationaryPosition(LarvaBehaviorMode.RUN);

        // minimum stop duration now exceeded ...
        validateStationaryPosition(LarvaBehaviorMode.STOP);

        validateReverse(LarvaBehaviorMode.STOP);
    }

    @Test
    public void testSkipJumpFrame() throws Exception {

        currentFrame = new LarvaFrameData(currentSkeleton);

        currentFrame.calculateDerivedData(frameHistory, parameters);

        Assert.assertEquals("invalid mode returned for stop frame",
                            LarvaBehaviorMode.STOP,
                            currentFrame.getBehaviorMode());

        frameHistory.add(0, currentFrame);
        previousFrame = currentFrame;
        currentTime += 33;

        final int testJumpFramesCount = parameters.getMaxJumpFramesToSkip() + 1;
        for (int i = 1; i < testJumpFramesCount; i++) {
            validateJumpFrame("jump frame " + i,
                              i,
                              LarvaBehaviorMode.STOP);
        }

        validateJumpFrame("jump frame " + testJumpFramesCount,
                          null,
                          LarvaBehaviorMode.STOP);
    }

    @Test
    public void testMaxLengthDerivation() throws Exception {

        parameters.setMaxLengthDerivationDuration(70);
        currentTime = 0;

        addFrameAndValidateDerivedLengths(2, 2,  null);  //   0 ms
        addFrameAndValidateDerivedLengths(4, 4,  null);  //  33 ms
        addFrameAndValidateDerivedLengths(3, 4,  null);  //  66 ms
        addFrameAndValidateDerivedLengths(2, 4,  50.0);  //  99 ms
        addFrameAndValidateDerivedLengths(6, 4, 150.0);  // 122 ms
    }

    private void addFrameAndValidateDerivedLengths(double lengthForFrame,
                                                   double expectedMaxLength,
                                                   Double expectedPercentageOfMaxLength) {

        if (currentTime > 0) {
            frameHistory.add(0, currentFrame);
            previousFrame = currentFrame;
        } else {
            previousFrame = null;
        }

        final LarvaSkeleton previousSkeleton = currentSkeleton;
        currentSkeleton = new LarvaSkeleton(currentTime,
                                            previousSkeleton.getHead(),
                                            previousSkeleton.getMidpoint(),
                                            previousSkeleton.getTail(),
                                            lengthForFrame,
                                            previousSkeleton.getCentroid(),
                                            previousSkeleton.getHeadToBodyAngle(),
                                            previousSkeleton.getTailBearing());
        currentFrame = new LarvaFrameData(currentSkeleton);

        currentFrame.calculateDerivedData(frameHistory, parameters);

        TestUtilities.assertEquals("invalid derived max length for frame at " + currentTime + " ms",
                                   expectedMaxLength,
                                   currentFrame.getDerivedMaxLength());

        TestUtilities.assertEqualDoubles("invalid percentage of max length for frame at " + currentTime + " ms",
                                         expectedPercentageOfMaxLength,
                                         currentFrame.getPercentageOfMaxLength());

        currentTime += 33;
    }

    private void validateDerivedMode(String context,
                                     LarvaBehaviorMode expectedMode) {

        currentFrame = new LarvaFrameData(currentSkeleton);

        currentFrame.calculateDerivedData(frameHistory, parameters);

        Assert.assertEquals("invalid mode returned for " + context +
                            ": " + currentFrame,
                            expectedMode,
                            currentFrame.getBehaviorMode());

        frameHistory.add(0, currentFrame);
        previousFrame = currentFrame;
        currentTime += 33;
    }

    @Test
    public void testSmoothData() throws Exception {

        boolean isAllDerivedDataAvailable =
                currentFrame.smoothData(
                        frameHistory,
                        parameters.getMinBodyAngleSpeedDuration()); // 1000

        Assert.assertFalse("smoothed data available with no history",
                           isAllDerivedDataAvailable);

        frameHistory.add(previousFrame); // 30ms prior

        isAllDerivedDataAvailable =
                currentFrame.smoothData(
                        frameHistory,
                        parameters.getMinBodyAngleSpeedDuration()); // 1000

        Assert.assertFalse("smoothed data available with not enough history",
                           isAllDerivedDataAvailable);

        frameHistory.clear();

        currentSkeleton =
                new LarvaSkeleton(2000,
                                  new TrackerPoint(0,2),  // head
                                  new TrackerPoint(0,1),  // midpoint
                                  new TrackerPoint(0,0),  // tail
                                  2,                      // length
                                  new TrackerPoint(0,1),  // centroid
                                  0,                      // headToBodyAngle
                                  90);                    // tailBearing
        currentFrame = new LarvaFrameData(currentSkeleton);

        final double testBodyAngleSpeed = 10.0;
        currentFrame.setValuesForTesting(LarvaBehaviorMode.RUN,
                                         testBodyAngleSpeed);

        LarvaFrameData lfd;
        for (int i = 1; i < 34; i++) {
            lfd = new LarvaFrameData(
                    new LarvaSkeleton(
                            currentSkeleton.getCaptureTime() - (33 * i),
                            new TrackerPoint(i,2),  // head
                            new TrackerPoint(i,1),  // midpoint
                            new TrackerPoint(i,0),  // tail
                            2,                      // length
                            new TrackerPoint(i,1),  // centroid
                            0,                      // headToBodyAngle
                            90));                   // tailBearing
            lfd.setValuesForTesting(LarvaBehaviorMode.RUN,
                                    testBodyAngleSpeed);
            frameHistory.add(lfd);
        }

        // add one more frame with skewed values too far in the past
        // -- should be ignored
        lfd = new LarvaFrameData(
                new LarvaSkeleton(500,
                                  new TrackerPoint(97,2),  // head
                                  new TrackerPoint(98,1),  // midpoint
                                  new TrackerPoint(99,0),  // tail
                                  2,                       // length
                                  new TrackerPoint(98,1),  // centroid
                                  0,                       // headToBodyAngle
                                  270));                   // tailBearing
        lfd.setValuesForTesting(LarvaBehaviorMode.RUN, 220.0);
        frameHistory.add(lfd);

        isAllDerivedDataAvailable =
                currentFrame.smoothData(
                        frameHistory,
                        parameters.getMinBodyAngleSpeedDuration()); // 1000

        Assert.assertTrue("smoothed data not available with enough history",
                           isAllDerivedDataAvailable);

        double smoothedBodyAngleSpeed = currentFrame.getSmoothedBodyAngleSpeed();

        Assert.assertTrue("invalid body angle speed smoothed, value=" +
                          smoothedBodyAngleSpeed,
                           ((smoothedBodyAngleSpeed > 9.999) &&
                            (smoothedBodyAngleSpeed < 10.001)));
    }

    private void testAndValidateMode(double smoothedBodyAngleSpeed,
                                     double headAngle,
                                     boolean isHeadLeftOfBody,
                                     double smoothedHeadAngleSpeed,
                                     double smoothedTailSpeedDotBodyAngle,
                                     LarvaBehaviorMode expectedMode) {

        long elapsedMilliseconds = currentFrame.getTime() -
                                   previousFrame.getTime();
        if (elapsedMilliseconds < 0) {
            elapsedMilliseconds = 0;
        }

        currentFrame.setBehaviorMode(parameters,
                                     previousFrame,
                                     smoothedBodyAngleSpeed,
                                     headAngle,
                                     isHeadLeftOfBody,
                                     smoothedHeadAngleSpeed,
                                     smoothedTailSpeedDotBodyAngle,
                                     elapsedMilliseconds);

        Assert.assertEquals("invalid mode returned",
                            expectedMode,
                            currentFrame.getBehaviorMode());
    }

    private static LarvaSkeleton buildEmptySkeleton(long captureTime) {
            return new LarvaSkeleton(captureTime,
                                     new TrackerPoint(0,0),
                                     new TrackerPoint(0,0),
                                     new TrackerPoint(0,0),
                                     0,
                                     new TrackerPoint(0,0),
                                     0,
                                     0);
    }

    private void validateStartingRun() {
        LarvaBehaviorMode expectedMode = LarvaBehaviorMode.STOP;
        for (int i = 0; i < 5; i++) {
            currentSkeleton =
                    new LarvaSkeleton(
                            currentTime,
                            new TrackerPoint(i+2,i+2),  // head
                            new TrackerPoint(i+1,i+1),  // midpoint
                            new TrackerPoint(i,i),      // tail
                            2,                          // length
                            new TrackerPoint(i+1,i+1),  // centroid
                            0,                          // headToBodyAngle
                            -135);                      // tailBearing
            if (i > 0) {
                expectedMode = LarvaBehaviorMode.RUN;
            }
            validateDerivedMode("starting run frame " + i, expectedMode);
        }
    }

    private void validateStationaryPosition(LarvaBehaviorMode expectedMode) {
        currentSkeleton = new LarvaSkeleton(currentTime,
                                            currentSkeleton.getHead(),
                                            currentSkeleton.getMidpoint(),
                                            currentSkeleton.getTail(),
                                            currentSkeleton.getLength(),
                                            currentSkeleton.getCentroid(),
                                            currentSkeleton.getHeadToBodyAngle(),
                                            currentSkeleton.getTailBearing());
        validateDerivedMode("stationary frame", expectedMode);
    }

    private void validateReverse(LarvaBehaviorMode expectedFirstFrameMode) {
        LarvaBehaviorMode expectedMode = expectedFirstFrameMode;
        for (int i = 0; i < 4; i++) {
            currentSkeleton =
                    new LarvaSkeleton(
                            currentTime,
                            new TrackerPoint(5-i, 5-i), // head
                            new TrackerPoint(4-i, 4-i), // midpoint
                            new TrackerPoint(3-i, 3-i), // tail
                            2,                          // length
                            new TrackerPoint(4-i, 4-i), // centroid
                            0,                          // headToBodyAngle
                            -135);                      // tailBearing
            if (i > 0) {
                expectedMode = LarvaBehaviorMode.BACK_UP;
            }
            validateDerivedMode("reverse frame " + i, expectedMode);
        }
    }

    private void validateJumpFrame(String context,
                                   Integer expectedFramesSkipped,
                                   LarvaBehaviorMode expectedMode) {
        currentSkeleton = new LarvaSkeleton(currentTime,
                                            new TrackerPoint(999,2),  // head
                                            new TrackerPoint(999,1),  // midpoint
                                            new TrackerPoint(999,0),  // tail
                                            2,                        // length
                                            new TrackerPoint(999,1),  // centroid
                                            0,                        // headToBodyAngle
                                            90);                      // tailBearing

        validateDerivedMode(context, expectedMode);

        Assert.assertEquals("invalid skipped frame count after " + context,
                            expectedFramesSkipped,
                            currentFrame.getJumpFramesSkipped());

        if (expectedFramesSkipped == null) {
            Assert.assertNull("skipped skeleton should not exist for " + context,
                              currentFrame.getSkippedSkeleton());
        } else {
            Assert.assertNotNull("missing skipped skeleton after " + context,
                                 currentFrame.getSkippedSkeleton());

            TestUtilities.assertEquals("head speed for " + context,
                                       0, currentFrame.getHeadSpeed());
            TestUtilities.assertEquals("midpoint speed for " + context,
                                       0, currentFrame.getMidpointSpeed());
            TestUtilities.assertEquals("tail speed for " + context,
                                       0, currentFrame.getTailSpeed());
            TestUtilities.assertEquals("centroid speed for " + context,
                                       0, currentFrame.getCentroidSpeed());
        }

        frameHistory.add(0, currentFrame);
        previousFrame = currentFrame;
        currentTime += 33;
    }
}

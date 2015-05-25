/*
 * Copyright (c) 2014 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.venkman.data;

import org.janelia.it.venkman.config.LarvaBehaviorParameters;
import org.janelia.it.venkman.config.rules.LEDArrayStimulus;
import org.janelia.it.venkman.config.rules.LEDStimulus;
import org.janelia.it.venkman.config.rules.Stimulus;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.XmlElementRefs;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;

/**
 * This class encapsulates the calculated behavioral data elements and state
 * for a single larva video frame.
 *
 * @author Eric Trautman
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class LarvaFrameData {

    /** Interface for mapping frame data to a tracker point. */
    public interface PointMapper {
        public TrackerPoint getValue(LarvaFrameData frameData);
    }

    /** Interface for mapping frame data to a double value. */
    public interface ValueMapper {
        public double getValue(LarvaFrameData frameData);
    }

    /** The skeleton for this frame. */
    @XmlElement
    private LarvaSkeleton skeleton;

    /** The derived behavior mode for this frame. */
    @XmlAttribute
    private LarvaBehaviorMode behaviorMode;

    /**
     * The raw amount (degrees/second) the body angle has changed
     * between this frame and the previous frame.
     */
    @XmlAttribute
    private double bodyAngleSpeed;

    /**
     * The derived body angle speed for this frame.
     */
    @XmlAttribute
    private double smoothedBodyAngleSpeed;

    /**
     * The raw amount (degrees/second) the head angle has changed
     * between this frame and the previous frame.
     */
    @XmlAttribute
    private double headAngleSpeed;

    /**
     * The smoothed body angle speed for this frame.
     */
    @XmlAttribute
    private double smoothedHeadAngleSpeed;

    /**
     * The distance (millimeters/second) the tail has moved between
     * this frame and the previous frame.
     */
    @XmlAttribute
    private double tailSpeed;

    /**
     * The distance (millimeters/second) the midpoint has moved between
     * this frame and the previous frame.
     */
    @XmlAttribute
    private double midpointSpeed;

    /**
     * The distance (millimeters/second) the head has moved between
     * this frame and the previous frame.
     */
    @XmlAttribute
    private double headSpeed;

    /**
     * The distance (millimeters/second) the centroid has moved between
     * this frame and the previous frame.
     */
    @XmlAttribute
    private double centroidSpeed;

    /**
     * The dot product of the tail speed vector (starting at the previous
     * tail position extending to the current tail position)
     * and the current body angle unit vector.
     *
     * Larger positive values should identify forward movement.
     * Larger negative values should identify backward movement.
     * Values close to zero should identify no movement.
     */
    @XmlAttribute
    private double tailSpeedDotBodyAngle;

    /**
     * The smoothed tail speed body angle dot product for this frame.
     */
    @XmlAttribute
    private double smoothedTailSpeedDotBodyAngle;

    /**
     * The number of milliseconds since the behavior mode has changed.
     */
    @XmlAttribute
    private long timeSinceLastBehaviorModeChange;

    /**
     * The number of milliseconds the larva has been in the STOP mode.
     */
    @XmlAttribute
    private Long timeStopped;

    /**
     * The number of milliseconds the larva has been in the BACK_UP mode.
     */
    @XmlAttribute
    private Long timeBackingUp;

    /**
     * The original skeleton for this frame that was skipped because
     * its measurements were aberrant.
     */
    @XmlElement
    private LarvaSkeleton skippedSkeleton;

    /**
     * The number of skipped frames (including this frame) because tracker
     * coordinates have jumped more than a logical (configured) amount.
     */
    @XmlAttribute
    private Integer jumpFramesSkipped;

    /**
     * The maximum length of the larva derived during the beginning of each run.
     */
    @XmlAttribute
    private double derivedMaxLength;

    /**
     * The larva's current length relative to it's derived maximum length
     * expressed as a percentage (0.0 to 200.0).
     */
    @XmlAttribute
    private Double percentageOfMaxLength;

    /** The list of stimulus flashes issued for this frame. */
    @XmlElementRefs({
            @XmlElementRef(type = LEDStimulus.class),
            @XmlElementRef(type = LEDArrayStimulus.class)
    })
    private List<? extends Stimulus> stimulusList;

    /**
     * No-arg constructor needed for JAXB.
     */
    @SuppressWarnings({"UnusedDeclaration"})
    private LarvaFrameData() {
    }

    /**
     * Constructs an empty data object with the specified skeleton.
     *
     * @param  skeleton  skeleton provided by the tracker for this frame.
     */
    public LarvaFrameData(LarvaSkeleton skeleton) {
        this.skeleton = skeleton;
        this.behaviorMode = LarvaBehaviorMode.STOP;
        this.timeSinceLastBehaviorModeChange = 0;
        this.timeStopped = null;
        this.timeBackingUp = null;
    }

    /**
     * Constructs an empty data object with the specified skeleton and
     * defaults to the specified behavior mode.
     *
     * @param  skeleton      skeleton for this frame.
     * @param  behaviorMode  default behavior mode for this frame.
     */
    public LarvaFrameData(LarvaSkeleton skeleton,
                          LarvaBehaviorMode behaviorMode) {
        this(skeleton);
        this.behaviorMode = behaviorMode;
    }

    /**
     * Calculates the smoothed data for this frame's skeleton.
     *
     * @param  frameHistory  history of data derived for frames received
     *                       <u>PRIOR</u> to the current frame.
     *                       Data for the most recent prior frame is at the
     *                       beginning of the list.
     *
     * @param  parameters    the configurable parameters for frame data
     *                       calculations.
     */
    public void calculateDerivedData(List<LarvaFrameData> frameHistory,
                                     LarvaBehaviorParameters parameters) {

        if (frameHistory.size() > 0) {

            final long currentFrameTime = skeleton.getCaptureTime();
            final LarvaFrameData previousFrame = frameHistory.get(0);
            final LarvaSkeleton previousSkeleton = previousFrame.getSkeleton();
            final long elapsedMilliseconds = (currentFrameTime -
                                              previousFrame.getTime());
            final double elapsedSeconds = (double) elapsedMilliseconds / 1000.0;

            final TrackerPoint originalCentroid = skeleton.getCentroid();
            centroidSpeed =
                    originalCentroid.distance(previousSkeleton.getCentroid()) /
                    elapsedSeconds;

            final double absCentroidSpeed = Math.abs(centroidSpeed);

            // If the tracker coordinates have jumped unreasonably,
            // skip this frame by simply using the previous frame's
            // coordinates with the current frame's timestamp.
            if (absCentroidSpeed > parameters.getMinCentroidSpeedToFlagJump()) {
                Integer previousSkipped =
                        previousFrame.getJumpFramesSkipped();
                if (previousSkipped == null) {
                    previousSkipped = 0;
                }
                if (previousSkipped < parameters.getMaxJumpFramesToSkip()) {
                    skippedSkeleton = skeleton.getClone();
                    skeleton.overrideMeasurements(previousSkeleton);
                    jumpFramesSkipped = previousSkipped + 1;
                    centroidSpeed = 0;
                }
            }

            final double length = skeleton.getLength();
            if (currentFrameTime < parameters.getMaxLengthDerivationDuration()) {
                // beginning of experiment: derive max length (assumes first frame has time 0)
                if (length > previousFrame.derivedMaxLength) {
                    derivedMaxLength = length;
                } else {
                    derivedMaxLength = previousFrame.derivedMaxLength;
                }
            } else {
                derivedMaxLength = previousFrame.derivedMaxLength;
                percentageOfMaxLength = (length * 100.0) / derivedMaxLength;
            }

            final TrackerPoint head = skeleton.getHead();
            final TrackerPoint midpoint = skeleton.getMidpoint();
            final TrackerPoint tail = skeleton.getTail();
            final TrackerPoint previousTail = previousSkeleton.getTail();

            headSpeed = head.distance(previousSkeleton.getHead()) /
                        elapsedSeconds;
            midpointSpeed = midpoint.distance(previousSkeleton.getMidpoint()) /
                            elapsedSeconds;
            tailSpeed = tail.distance(previousTail) / elapsedSeconds;

            // -180 <= headAngle <= 180
            // assume headAngle won't cross branch point
            // (larva's head won't bend back across body)
            final double headAngleDelta =
                    skeleton.getHeadToBodyAngle() -
                    previousSkeleton.getHeadToBodyAngle();
            headAngleSpeed = headAngleDelta / elapsedSeconds;

            final double bodyAngle = skeleton.getTailBearing();
            final double previousBodyAngle = previousSkeleton.getTailBearing();
            double bodyAngleDelta = bodyAngle - previousBodyAngle;

            // if we've crossed the branch point (180), assume speed
            // is smaller change and use opposite direction body angle
            // (for example, if previous = -170, current = 170, opposite = -190:
            //   speed should be 20 instead of 340)
            if ((bodyAngleDelta > 179.999999) || (bodyAngleDelta < -179.999999)) {
                final double oppositeDirectionBodyAngle;
                if (bodyAngle > -0.0) {
                    oppositeDirectionBodyAngle = bodyAngle - 360;
                } else {
                    oppositeDirectionBodyAngle = 360 + bodyAngle;
                }
                bodyAngleDelta = oppositeDirectionBodyAngle -
                                 previousBodyAngle;
            }
            bodyAngleSpeed = bodyAngleDelta / elapsedSeconds;

            final TrackerPoint bodyAngleUnitVectorTerminal =
                    Calculator.getUnitVectorPointForTrackerAngle(bodyAngle);

            tailSpeedDotBodyAngle =
                    Calculator.getDotProduct(previousTail,
                                             tail,
                                             Calculator.ORIGIN,
                                             bodyAngleUnitVectorTerminal);

            final boolean isAllSmoothedDataAvailable =
                    smoothData(frameHistory,
                               parameters.getMinBodyAngleSpeedDuration());

            if (isAllSmoothedDataAvailable) {

                final boolean isHeadLeftOfBody =
                        Calculator.isCoordinateLeftOfVector(head,
                                                            tail,
                                                            midpoint);
                setBehaviorMode(parameters,
                                previousFrame,
                                smoothedBodyAngleSpeed,
                                skeleton.getHeadToBodyAngle(),
                                isHeadLeftOfBody,
                                smoothedHeadAngleSpeed,
                                smoothedTailSpeedDotBodyAngle,
                                elapsedMilliseconds);
            }
        } else {
            derivedMaxLength = skeleton.getLength();
        }

    }

    /**
     * @return skeleton for this frame.
     */
    public LarvaSkeleton getSkeleton() {
        return skeleton;
    }

    /**
     * @return time (in milliseconds) this frame was captured.
     */
    public long getTime() {
        return skeleton.getCaptureTime();
    }

    /**
     * @return the skeleton's tail bearing angle.
     */
    public double getBodyAngle() {
        return skeleton.getTailBearing();
    }

    /**
     * @return the skeleton's length.
     */
    public double getLength() {
        return skeleton.getLength();
    }

    /**
     * @return the amount (degrees/ms) the body angle has changed.
     */
    public double getBodyAngleSpeed() {
        return bodyAngleSpeed;
    }

    public double getSmoothedBodyAngleSpeed() {
        return smoothedBodyAngleSpeed;
    }

    /**
     * @return the skeleton's head to body angle.
     */
    public double getHeadAngle() {
        return skeleton.getHeadToBodyAngle();
    }

    /**
     * @return the amount (degrees/ms) the head angle has changed.
     */
    public double getHeadAngleSpeed() {
        return headAngleSpeed;
    }

    public double getSmoothedHeadAngleSpeed() {
        return smoothedHeadAngleSpeed;
    }

    /**
     * @return the distance the tail has moved
     *         between this frame and the previous frame.
     */
    public double getTailSpeed() {
        return tailSpeed;
    }

    /**
     * @return the distance the midpoint has moved
     *         between this frame and the previous frame.
     */
    public double getMidpointSpeed() {
        return midpointSpeed;
    }

    /**
     * @return the distance the head has moved
     *         between this frame and the previous frame.
     */
    public double getHeadSpeed() {
        return headSpeed;
    }

    /**
     * @return the distance the centroid has moved
     *         between this frame and the previous frame.
     */
    public double getCentroidSpeed() {
        return centroidSpeed;
    }

    public double getTailSpeedDotBodyAngle() {
        return tailSpeedDotBodyAngle;
    }

    public double getSmoothedTailSpeedDotBodyAngle() {
        return smoothedTailSpeedDotBodyAngle;
    }

    public long getTimeSinceLastBehaviorModeChange() {
        return timeSinceLastBehaviorModeChange;
    }

    public Long getTimeStopped() {
        return timeStopped;
    }

    public Long getTimeBackingUp() {
        return timeBackingUp;
    }

    public LarvaSkeleton getSkippedSkeleton() {
        return skippedSkeleton;
    }

    public Integer getJumpFramesSkipped() {
        return jumpFramesSkipped;
    }

    public double getDerivedMaxLength() {
        return derivedMaxLength;
    }

    public Double getPercentageOfMaxLength() {
        return percentageOfMaxLength;
    }

    public boolean isMaxLengthDerivationComplete() {
        return percentageOfMaxLength != null;
    }

    /**
     * @return the behavior mode of the larva for this frame.
     */
    public LarvaBehaviorMode getBehaviorMode() {
        return behaviorMode;
    }

    /**
     * @return true if the larva is running during this frame; otherwise false.
     */
    public boolean isRunning() {
        return (LarvaBehaviorMode.RUN == behaviorMode);
    }

    /**
     * @return true if the larva is turning during this frame; otherwise false.
     */
    public boolean isTurning() {
        return ((LarvaBehaviorMode.TURN_RIGHT == behaviorMode) ||
                (LarvaBehaviorMode.TURN_LEFT == behaviorMode));
    }

    /**
     * @return true if the larva is casting during this frame; otherwise false.
     */
    public boolean isCasting() {
        return ((LarvaBehaviorMode.CAST_RIGHT == behaviorMode) ||
                (LarvaBehaviorMode.CAST_LEFT == behaviorMode));
    }

    /**
     * @return the list of stimulus flashes issued for this frame.
     */
    public List<? extends Stimulus> getStimulusList() {
        return stimulusList;
    }

    public void setStimulusList(List<? extends Stimulus> stimulusList) {
        this.stimulusList = stimulusList;
    }

    @Override
    public String toString() {
        return "LarvaFrameData{" +
               "behaviorMode=" + behaviorMode +
               ", skeleton=" + skeleton +
               ", bodyAngleSpeed=" + bodyAngleSpeed +
               ", smoothedBodyAngleSpeed=" + smoothedBodyAngleSpeed +
               ", headAngleSpeed=" + headAngleSpeed +
               ", smoothedHeadAngleSpeed=" + smoothedHeadAngleSpeed +
               ", tailSpeed=" + tailSpeed +
               ", midpointSpeed=" + midpointSpeed +
               ", headSpeed=" + headSpeed +
               ", centroidSpeed=" + centroidSpeed +
               ", tailSpeedDotBodyAngle=" + tailSpeedDotBodyAngle +
               ", smoothedTailSpeedDotBodyAngle=" +
               smoothedTailSpeedDotBodyAngle +
               ", stimulusList=" + stimulusList +
               '}';
    }

    /**
     * Smooths frame to frame data values using a weighted average over time.
     *
     * @param  frameHistory  history of data derived for frames received
     *                       <u>PRIOR</u> to the current frame.
     *                       Data for the most recent prior frame is at the
     *                       beginning of the list.
     *
     * @param smoothingDuration   the number of milliseconds prior to the
     *                            current frame's timestamp to include
     *                            historical frame data for smoothing data.
     *
     * @return true if enough historical data exists to derive all speeds;
     *         otherwise false.
     */
    protected boolean smoothData(List<LarvaFrameData> frameHistory,
                                 long smoothingDuration) {

        long currentMilliseconds = skeleton.getCaptureTime();
        long previousMilliseconds;
        long elapsedMilliseconds;
        long totalElapsedMilliseconds = 0;

        int durationFrameCount = 1; // current frame
        boolean isEnoughHistoryAvailable = false;

        for (LarvaFrameData lfd : frameHistory) {
            previousMilliseconds = lfd.getTime();
            elapsedMilliseconds = currentMilliseconds - previousMilliseconds;
            totalElapsedMilliseconds += elapsedMilliseconds;
            currentMilliseconds = previousMilliseconds;

            if (totalElapsedMilliseconds > smoothingDuration) {
                isEnoughHistoryAvailable = true;
                break;
            }

            durationFrameCount++;
        }

        if (isEnoughHistoryAvailable) {

            int frameCountSeries = 0;
            for (int i = 1; i <= durationFrameCount; i++) {
                frameCountSeries += i;
            }

            int frameCount = durationFrameCount;
            double weight = (double) durationFrameCount /
                             (double) frameCountSeries;
            double currentBodyAngleSpeed = bodyAngleSpeed * weight;
            double bodyAngleSum = currentBodyAngleSpeed;
            double currentHeadAngleSpeed = headAngleSpeed * weight;
            double headAngleSum = currentHeadAngleSpeed;
            double currentDotProduct = tailSpeedDotBodyAngle * weight;
            double dotProductSum = currentDotProduct;

            for (LarvaFrameData lfd : frameHistory) {
                frameCount--;

                if (frameCount > 0) {
                    weight = (double) frameCount / (double) frameCountSeries;
                    currentBodyAngleSpeed = lfd.bodyAngleSpeed * weight;
                    bodyAngleSum += currentBodyAngleSpeed;
                    currentHeadAngleSpeed = lfd.headAngleSpeed * weight;
                    headAngleSum += currentHeadAngleSpeed;
                    currentDotProduct = lfd.tailSpeedDotBodyAngle * weight;
                    dotProductSum += currentDotProduct;
                } else {
                    break;
                }
            }

            smoothedBodyAngleSpeed = bodyAngleSum;
            smoothedHeadAngleSpeed = headAngleSum;
            smoothedTailSpeedDotBodyAngle = dotProductSum;
        }

        return isEnoughHistoryAvailable;
    }

    public void setValuesForTesting(LarvaBehaviorMode behaviorMode,
                                       double bodyAngleSpeed) {
        this.behaviorMode = behaviorMode;
        this.bodyAngleSpeed = bodyAngleSpeed;
    }

    /**
     * Determines and sets the motion state for this frame.
     * This method is protected (instead of private) to support testing.
     *
     * @param  parameters                  current experiment parameters.
     * @param  previousFrame               previous frame's data.
     * @param  smoothedBodyAngleSpeed      smoothed body angle speed.
     * @param  headAngle                   head angle for this frame.
     * @param  isHeadLeftOfBody            true if the head is to the left of
     *                                     the body (tail to midpoint segment).
     * @param  smoothedHeadAngleSpeed         smoothed head angle speed.
     * @param  smoothedTailSpeedDotBodyAngle  the smoothed tail speed body angle
     *                                        dot product for this frame.
     * @param  elapsedMilliseconds         number of milliseconds between this
     *                                     frame and the previous frame.
     */
    protected void setBehaviorMode(LarvaBehaviorParameters parameters,
                                   LarvaFrameData previousFrame,
                                   double smoothedBodyAngleSpeed,
                                   double headAngle,
                                   boolean isHeadLeftOfBody,
                                   double smoothedHeadAngleSpeed,
                                   double smoothedTailSpeedDotBodyAngle,
                                   long elapsedMilliseconds) {

        final LarvaBehaviorMode previousMode = previousFrame.getBehaviorMode();

        behaviorMode = null;

        timeSinceLastBehaviorModeChange =
                previousFrame.timeSinceLastBehaviorModeChange +
                elapsedMilliseconds;

        if (timeSinceLastBehaviorModeChange <
            parameters.getMinBehaviorModeDuration()) {

            behaviorMode = previousMode;

        } else if (previousFrame.isTurning()) {

            // leave turn if head angle changes sign
            // (crosses both positive and negative threshold)
            final double previousHeadAngle = previousFrame.getHeadAngle();
            if (previousHeadAngle < 0) {
                if (-headAngle >
                       parameters.getMinHeadAngleToContinueTurning()) {
                    behaviorMode = previousMode;
                }
            } else if (headAngle >
                       parameters.getMinHeadAngleToContinueTurning()) {
                behaviorMode = previousMode;
            }

        } else if (previousFrame.isCasting()) {

            if (Math.abs(smoothedBodyAngleSpeed) >
                   parameters.getMinBodyAngleSpeedForTurns() &&
                (Math.abs(smoothedHeadAngleSpeed) <
                 parameters.getMinHeadAngleSpeedToContinueCasting())) {

                if (LarvaBehaviorMode.CAST_LEFT.equals(previousMode)) {
                    behaviorMode = LarvaBehaviorMode.TURN_LEFT;
                } else {
                    behaviorMode = LarvaBehaviorMode.TURN_RIGHT;
                }

            } else if (((Math.abs(headAngle) >
                        parameters.getMinHeadAngleToContinueCasting() ||
                        (Math.abs(smoothedHeadAngleSpeed) >
                        parameters.getMinHeadAngleSpeedToContinueCasting())))) {

                if (isHeadLeftOfBody) {
                    behaviorMode = LarvaBehaviorMode.CAST_LEFT;
                } else {
                    behaviorMode = LarvaBehaviorMode.CAST_RIGHT;
                }

            }

        } else if (Math.abs(headAngle) >
                   parameters.getMinHeadAngleForCasting()) {

            if (isHeadLeftOfBody) {
                behaviorMode = LarvaBehaviorMode.CAST_LEFT;
            } else {
                behaviorMode = LarvaBehaviorMode.CAST_RIGHT;
            }

        }

        // straight modes
        if (behaviorMode == null) {

            final double dotProductThreshold =
                    parameters.getDotProductThresholdForStraightModes();

            if (smoothedTailSpeedDotBodyAngle > dotProductThreshold) {

                behaviorMode = LarvaBehaviorMode.RUN;

            } else if (smoothedTailSpeedDotBodyAngle < -dotProductThreshold) {

                behaviorMode = LarvaBehaviorMode.BACK_UP;

                if (previousFrame.timeBackingUp == null) {
                    timeBackingUp = 0L;
                } else {
                    timeBackingUp = previousFrame.timeBackingUp +
                                    elapsedMilliseconds;
                }

            } else {

                behaviorMode = LarvaBehaviorMode.STOP;

                if (previousFrame.timeStopped == null) {
                    timeStopped = 0L;
                } else {
                    timeStopped = previousFrame.timeStopped +
                                  elapsedMilliseconds;
                }
            }

        }

        if (timeBackingUp != null) {

            final long minDuration = parameters.getMinStopOrBackUpDuration();
            if (timeBackingUp < minDuration) {
                behaviorMode = previousMode;
            } else {
                timeSinceLastBehaviorModeChange = timeBackingUp - minDuration;
            }

        } else if (timeStopped != null) {

            final long minDuration = parameters.getMinStopOrBackUpDuration();
            if (timeStopped < minDuration) {
                behaviorMode = previousMode;
            } else {
                timeSinceLastBehaviorModeChange = timeStopped - minDuration;
            }

        } else if (! behaviorMode.equals(previousMode)) {

            timeSinceLastBehaviorModeChange = 0;

        }

    }

}

/*
 * Copyright (c) 2014 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.venkman.config;

import org.janelia.it.venkman.gui.parameter.annotation.VenkmanParameter;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * The raw parameters used to configure frame data calculations.
 *
 * @author Eric Trautman
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class LarvaBehaviorParameters {

    @VenkmanParameter(displayName = "Minimum Head Angle To Start Casting (degrees)",
                      minimum = "0.0",
                      maximum = "360.0")
    @XmlElement
    private double minHeadAngleForCasting;

    @VenkmanParameter(displayName = "Minimum Head Angle To Continue Casting (degrees)",
                      minimum = "0.0",
                      maximum = "360.0")
    @XmlElement
    private double minHeadAngleToContinueCasting;

    @VenkmanParameter(displayName = "Minimum Head Angle Derivative To Continue Casting (degrees/s)",
                      minimum = "0.0",
                      maximum = "360.0")
    @XmlElement
    private double minHeadAngleSpeedToContinueCasting;

    @VenkmanParameter(displayName = "Minimum Body Angle Derivative To Start Turns (degrees/s)",
                      minimum = "0.0",
                      maximum = "360.0")
    @XmlElement
    private double minBodyAngleSpeedForTurns;

    // TODO: rename minBodyAngleSpeedDuration once we've settled on an algorithm

    @VenkmanParameter(displayName = "Duration for Calculating Head and Body Angle Derivatives (ms)",
                      minimum = "0",
                      maximum = "5000")
    @XmlElement
    private long minBodyAngleSpeedDuration;

    @VenkmanParameter(displayName = "Minimum Head Angle To Continue Turning (degrees)",
                      minimum = "0.0",
                      maximum = "360.0")
    @XmlElement
    private double minHeadAngleToContinueTurning;

    // TODO: properly scope maximum and identify units in displayName

    @VenkmanParameter(displayName = "Dot Product Threshold For Determining Run, Stop, and Back-Up",
                      minimum = "0.0",
                      maximum = "10.0")
    @XmlElement
    private double dotProductThresholdForStraightModes;

    @VenkmanParameter(displayName = "Minimum Duration to Maintain a Behavioral Mode (ms)",
                      minimum = "0",
                      maximum = "5000")
    @XmlElement
    private long minBehaviorModeDuration;

    @VenkmanParameter(displayName = "Minimum Duration Before Assigning Stop or Back-Up (ms)",
                      minimum = "0",
                      maximum = "1000")
    @XmlElement
    private long minStopOrBackUpDuration;

    @VenkmanParameter(displayName = "Minimum Centroid Speed to Flag Tracker Jump (mm/frame)",
                      minimum = "0.0",
                      maximum = "1000.0")
    @XmlElement
    private double minCentroidSpeedToFlagJump;

    @VenkmanParameter(displayName = "Maximum Tracker Jump Frames to Skip",
                      minimum = "0",
                      maximum = "300")
    @XmlElement
    private int maxJumpFramesToSkip;

    @VenkmanParameter(displayName = "Maximum Length Derivation Duration (milliseconds)",
                      minimum = "0",
                      maximum = "10000")
    @XmlElement
    private long maxLengthDerivationDuration;

    public LarvaBehaviorParameters() {
        this.minCentroidSpeedToFlagJump = 100.0;
        this.maxJumpFramesToSkip = 5;
        this.maxLengthDerivationDuration = 1000;
    }

    public double getMinHeadAngleForCasting() {
        return minHeadAngleForCasting;
    }

    public void setMinHeadAngleForCasting(double minHeadAngleForCasting) {
        this.minHeadAngleForCasting = minHeadAngleForCasting;
    }

    public double getMinHeadAngleToContinueCasting() {
        return minHeadAngleToContinueCasting;
    }

    public void setMinHeadAngleToContinueCasting(double minHeadAngleToContinueCasting) {
        this.minHeadAngleToContinueCasting = minHeadAngleToContinueCasting;
    }

    public double getMinHeadAngleSpeedToContinueCasting() {
        return minHeadAngleSpeedToContinueCasting;
    }

    public void setMinHeadAngleSpeedToContinueCasting(double minHeadAngleSpeedToContinueCasting) {
        this.minHeadAngleSpeedToContinueCasting =
                minHeadAngleSpeedToContinueCasting;
    }

    public double getMinBodyAngleSpeedForTurns() {
        return minBodyAngleSpeedForTurns;
    }

    public void setMinBodyAngleSpeedForTurns(double minBodyAngleSpeedForTurns) {
        this.minBodyAngleSpeedForTurns = minBodyAngleSpeedForTurns;
    }

    public long getMinBodyAngleSpeedDuration() {
        return minBodyAngleSpeedDuration;
    }

    public void setMinBodyAngleSpeedDuration(long minBodyAngleSpeedDuration) {
        this.minBodyAngleSpeedDuration = minBodyAngleSpeedDuration;
    }

    public double getMinHeadAngleToContinueTurning() {
        return minHeadAngleToContinueTurning;
    }

    public void setMinHeadAngleToContinueTurning(double minHeadAngleToContinueTurning) {
        this.minHeadAngleToContinueTurning = minHeadAngleToContinueTurning;
    }

    public double getDotProductThresholdForStraightModes() {
        return dotProductThresholdForStraightModes;
    }

    public void setDotProductThresholdForStraightModes(double dotProductThresholdForStraightModes) {
        this.dotProductThresholdForStraightModes =
                dotProductThresholdForStraightModes;
    }

    public long getMinBehaviorModeDuration() {
        return minBehaviorModeDuration;
    }

    public void setMinBehaviorModeDuration(long minBehaviorModeDuration) {
        this.minBehaviorModeDuration = minBehaviorModeDuration;
    }

    public long getMinStopOrBackUpDuration() {
        return minStopOrBackUpDuration;
    }

    public void setMinStopOrBackUpDuration(long minStopOrBackUpDuration) {
        this.minStopOrBackUpDuration = minStopOrBackUpDuration;
    }

    public double getMinCentroidSpeedToFlagJump() {
        return minCentroidSpeedToFlagJump;
    }

    @SuppressWarnings("UnusedDeclaration")
    public void setMinCentroidSpeedToFlagJump(double minCentroidSpeedToFlagJump) {
        this.minCentroidSpeedToFlagJump = minCentroidSpeedToFlagJump;
    }

    public int getMaxJumpFramesToSkip() {
        return maxJumpFramesToSkip;
    }

    @SuppressWarnings("UnusedDeclaration")
    public void setMaxJumpFramesToSkip(int maxJumpFramesToSkip) {
        this.maxJumpFramesToSkip = maxJumpFramesToSkip;
    }

    public long getMaxLengthDerivationDuration() {
        return maxLengthDerivationDuration;
    }

    public void setMaxLengthDerivationDuration(long maxLengthDerivationDuration) {
        this.maxLengthDerivationDuration = maxLengthDerivationDuration;
    }

    @Override
    public String toString() {
        return "LarvaBehaviorParameters{" +
               "dotProductThresholdForStraightModes=" +
               dotProductThresholdForStraightModes +
               ", minHeadAngleForCasting=" + minHeadAngleForCasting +
               ", minHeadAngleToContinueCasting=" +
               minHeadAngleToContinueCasting +
               ", minHeadAngleSpeedToContinueCasting=" +
               minHeadAngleSpeedToContinueCasting +
               ", minBodyAngleSpeedForTurns=" + minBodyAngleSpeedForTurns +
               ", minBodyAngleSpeedDuration=" + minBodyAngleSpeedDuration +
               ", minHeadAngleToContinueTurning=" +
               minHeadAngleToContinueTurning +
               ", minBehaviorModeDuration=" + minBehaviorModeDuration +
               ", minStopOrBackUpDuration=" + minStopOrBackUpDuration +
               ", minCentroidSpeedToFlagJump=" + minCentroidSpeedToFlagJump +
               ", maxJumpFramesToSkip=" + maxJumpFramesToSkip +
               ", maxLengthDerivationDuration=" + maxLengthDerivationDuration +
               '}';
    }
}
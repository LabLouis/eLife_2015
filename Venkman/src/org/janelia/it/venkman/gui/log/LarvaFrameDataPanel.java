/*
 * Copyright (c) 2014 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.venkman.gui.log;

import org.janelia.it.venkman.config.LarvaBehaviorParameters;
import org.janelia.it.venkman.data.LarvaBehaviorMode;
import org.janelia.it.venkman.data.LarvaFrameData;
import org.janelia.it.venkman.data.LarvaSkeleton;
import org.janelia.it.venkman.data.TrackerPoint;
import org.janelia.it.venkman.rules.DefinedEnvironment;

import javax.swing.*;
import java.awt.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.math.BigDecimal;

/**
 * This panel displays the tracker measurements for a specific video frame
 * along with the derived behavior mode for the frame.  The rationale used
 * to derive the behavior mode is also displayed.
 *
 * @author Eric Trautman
 */
public class LarvaFrameDataPanel {

    private JPanel contentPanel;
    @SuppressWarnings("UnusedDeclaration")
    private JPanel dataPanel;

    private JTextArea behaviorModeRationaleTextArea;
    private JTextArea positionTextArea;

    private JLabel motionStateLabel;
    private JLabel skeletonLengthLabel;
    private JLabel percentageOfMaxLengthLabel;
    private JLabel headSpeedLabel;
    private JLabel midpointSpeedLabel;
    private JLabel tailSpeedLabel;
    private JLabel centroidSpeedLabel;
    private JLabel headAngleLabel;
    private JLabel headAngleSpeedLabel;
    private JLabel bodyAngleLabel;
    private JLabel bodyAngleSpeedLabel;
    private JLabel tailSpeedDotBodyAngleLabel;

    private JLabel smoothedHeadAngleSpeed;
    private JLabel smoothedBodyAngleSpeed;
    private JLabel smoothedTailSpeedDotBodyAngle;

    private JLabel previousMotionStateLabel;
    private JLabel previousSkeletonLengthLabel;
    private JLabel previousPercentageOfMaxLengthLabel;
    private JLabel previousHeadSpeedLabel;
    private JLabel previousMidpointSpeedLabel;
    private JLabel previousTailSpeedLabel;
    private JLabel previousCentroidSpeedLabel;
    private JLabel previousHeadAngleLabel;
    private JLabel previousHeadAngleSpeedLabel;
    private JLabel previousBodyAngleLabel;
    private JLabel previousBodyAngleSpeedLabel;
    private JLabel previousTailSpeedDotBodyAngleLabel;

    private JLabel previousSmoothedHeadAngleSpeed;
    private JLabel previousSmoothedBodyAngleSpeed;
    private JLabel previousSmoothedTailSpeedDotBodyAngle;

    private LogModel logModel;

    public LarvaFrameDataPanel(LogModel logModel) {
        this.logModel = logModel;
        this.logModel.addPropertyChangeListener(
                LogModel.CURRENT_FRAME_PROPERTY,
                new PropertyChangeListener() {
                    @Override
                    public void propertyChange(PropertyChangeEvent evt) {
                        updateCurrentFrame();
                    }
                });
    }

    public JPanel getContentPanel() {
        return contentPanel;
    }

    private void createUIComponents() {

        final Color currentColor = Color.BLUE;
        final Color previousColor = Color.GRAY;

        behaviorModeRationaleTextArea = new JTextArea();
        positionTextArea = new JTextArea();

        motionStateLabel = createLabelWithColor(currentColor);
        skeletonLengthLabel = createLabelWithColor(currentColor);
        percentageOfMaxLengthLabel = createLabelWithColor(currentColor);
        headSpeedLabel = createLabelWithColor(currentColor);
        midpointSpeedLabel = createLabelWithColor(currentColor);
        tailSpeedLabel = createLabelWithColor(currentColor);
        centroidSpeedLabel = createLabelWithColor(currentColor);
        headAngleLabel = createLabelWithColor(currentColor);
        headAngleSpeedLabel = createLabelWithColor(currentColor);
        smoothedHeadAngleSpeed = createLabelWithColor(currentColor);
        bodyAngleLabel = createLabelWithColor(currentColor);
        bodyAngleSpeedLabel = createLabelWithColor(currentColor);
        smoothedBodyAngleSpeed = createLabelWithColor(currentColor);
        tailSpeedDotBodyAngleLabel = createLabelWithColor(currentColor);
        smoothedTailSpeedDotBodyAngle = createLabelWithColor(currentColor);

        previousMotionStateLabel = createLabelWithColor(previousColor);
        previousSkeletonLengthLabel = createLabelWithColor(previousColor);
        previousPercentageOfMaxLengthLabel = createLabelWithColor(previousColor);
        previousHeadSpeedLabel = createLabelWithColor(previousColor);
        previousMidpointSpeedLabel = createLabelWithColor(previousColor);
        previousTailSpeedLabel = createLabelWithColor(previousColor);
        previousCentroidSpeedLabel = createLabelWithColor(previousColor);
        previousHeadAngleLabel = createLabelWithColor(previousColor);
        previousHeadAngleSpeedLabel = createLabelWithColor(previousColor);
        previousSmoothedHeadAngleSpeed = createLabelWithColor(previousColor);
        previousBodyAngleLabel = createLabelWithColor(previousColor);
        previousBodyAngleSpeedLabel = createLabelWithColor(previousColor);
        previousSmoothedBodyAngleSpeed = createLabelWithColor(previousColor);
        previousTailSpeedDotBodyAngleLabel = createLabelWithColor(previousColor);
        previousSmoothedTailSpeedDotBodyAngle = createLabelWithColor(previousColor);
    }

    public void updateCurrentFrame() {

        final LarvaFrameData currentFrame =
                logModel.getCurrentFrameData();
        final LarvaFrameData previousFrame =
                logModel.getPreviousFrameData();
        LarvaBehaviorParameters currentParameters =
                logModel.getCurrentParameters();

        updateCalculationLabels(currentFrame,
                                motionStateLabel,
                                skeletonLengthLabel,
                                percentageOfMaxLengthLabel,
                                headSpeedLabel,
                                midpointSpeedLabel,
                                tailSpeedLabel,
                                centroidSpeedLabel,
                                headAngleLabel,
                                headAngleSpeedLabel,
                                smoothedHeadAngleSpeed,
                                bodyAngleLabel,
                                bodyAngleSpeedLabel,
                                smoothedBodyAngleSpeed,
                                tailSpeedDotBodyAngleLabel,
                                smoothedTailSpeedDotBodyAngle);

        updateCalculationLabels(previousFrame,
                                previousMotionStateLabel,
                                previousSkeletonLengthLabel,
                                previousPercentageOfMaxLengthLabel,
                                previousHeadSpeedLabel,
                                previousMidpointSpeedLabel,
                                previousTailSpeedLabel,
                                previousCentroidSpeedLabel,
                                previousHeadAngleLabel,
                                previousHeadAngleSpeedLabel,
                                previousSmoothedHeadAngleSpeed,
                                previousBodyAngleLabel,
                                previousBodyAngleSpeedLabel,
                                previousSmoothedBodyAngleSpeed,
                                previousTailSpeedDotBodyAngleLabel,
                                previousSmoothedTailSpeedDotBodyAngle);

        if ((previousFrame != null) && (currentFrame != null)) {
            setBehaviorModeRationale(currentParameters,
                                     currentFrame,
                                     previousFrame,
                                     currentFrame.getSmoothedBodyAngleSpeed(),
                                     currentFrame.getHeadAngle(),
                                     currentFrame.getSmoothedHeadAngleSpeed(),
                                     currentFrame.getSmoothedTailSpeedDotBodyAngle());
        } else {
            behaviorModeRationaleTextArea.setText("");
        }

        if (currentFrame == null) {
            positionTextArea.setText("");
        } else {
            setPositionTextArea(currentFrame);
        }
    }

    private JLabel createLabelWithColor(Color color) {
        JLabel label = new JLabel(EMPTY_TEXT);
        label.setForeground(color);
        return label;
    }

    private void updateCalculationLabels(LarvaFrameData frameData,
                                         JLabel motionState,
                                         JLabel skeletonLength,
                                         JLabel percentageOfMaxLength,
                                         JLabel headSpeed,
                                         JLabel midpointSpeed,
                                         JLabel tailSpeed,
                                         JLabel centroidSpeed,
                                         JLabel headAngle,
                                         JLabel headAngleSpeed,
                                         JLabel smoothedHeadAngleSpeed,
                                         JLabel bodyAngle,
                                         JLabel bodyAngleSpeed,
                                         JLabel smoothedBodyAngleSpeed,
                                         JLabel tailSpeedDotBodyAngle,
                                         JLabel smoothedTailSpeedDotBodyAngle) {

        if (frameData == null) {

            motionState.setText(EMPTY_TEXT);
            skeletonLength.setText(EMPTY_TEXT);
            percentageOfMaxLength.setText(EMPTY_TEXT);
            headSpeed.setText(EMPTY_TEXT);
            midpointSpeed.setText(EMPTY_TEXT);
            tailSpeed.setText(EMPTY_TEXT);
            centroidSpeed.setText(EMPTY_TEXT);
            headAngle.setText(EMPTY_TEXT);
            headAngleSpeed.setText(EMPTY_TEXT);
            smoothedHeadAngleSpeed.setText(EMPTY_TEXT);
            bodyAngle.setText(EMPTY_TEXT);
            bodyAngleSpeed.setText(EMPTY_TEXT);
            smoothedBodyAngleSpeed.setText(EMPTY_TEXT);
            tailSpeedDotBodyAngle.setText(EMPTY_TEXT);
            smoothedTailSpeedDotBodyAngle.setText(EMPTY_TEXT);

        } else {

            LarvaSkeleton skeleton = frameData.getSkeleton();
            setLabelText(motionState, frameData.getBehaviorMode());
            setLabelText(skeletonLength, skeleton.getLength());
            final Double percentageOfMaxLengthValue = frameData.getPercentageOfMaxLength();
            if (percentageOfMaxLengthValue == null) {
                percentageOfMaxLength.setText(EMPTY_TEXT);
            } else {
                setLabelText(percentageOfMaxLength, percentageOfMaxLengthValue);
            }
            setLabelText(headSpeed, frameData.getHeadSpeed());
            setLabelText(midpointSpeed, frameData.getMidpointSpeed());
            setLabelText(tailSpeed, frameData.getTailSpeed());
            setLabelText(centroidSpeed, frameData.getCentroidSpeed());
            setLabelText(headAngle, skeleton.getHeadToBodyAngle());
            setLabelText(headAngleSpeed,
                         frameData.getHeadAngleSpeed());
            setLabelText(smoothedHeadAngleSpeed,
                         frameData.getSmoothedHeadAngleSpeed());
            setLabelText(bodyAngle, skeleton.getTailBearing());
            setLabelText(bodyAngleSpeed,
                         frameData.getBodyAngleSpeed());
            setLabelText(smoothedBodyAngleSpeed,
                         frameData.getSmoothedBodyAngleSpeed());
            setLabelText(tailSpeedDotBodyAngle,
                         frameData.getTailSpeedDotBodyAngle());
            setLabelText(smoothedTailSpeedDotBodyAngle,
                         frameData.getSmoothedTailSpeedDotBodyAngle());
        }
    }

    private void setBehaviorModeRationale(LarvaBehaviorParameters parameters,
                                          LarvaFrameData currentFrame,
                                          LarvaFrameData previousFrame,
                                          double smoothedBodyAngleSpeed,
                                          double headAngle,
                                          double smoothedHeadAngleSpeed,
                                          double smoothedTailSpeedDotBodyAngle) {

        StringBuilder modeRational = new StringBuilder(128);
        
        final long timeSinceLastBehaviorModeChange = 
                currentFrame.getTimeSinceLastBehaviorModeChange();

        if (currentFrame.getTime() < parameters.getMinBodyAngleSpeedDuration()) {

            modeRational.append("mode not derived since smoothed data is not available,\n");
            modeRational.append("specifically current time (");
            modeRational.append(currentFrame.getTime());
            modeRational.append(") < minimum body angle speed duration (");
            modeRational.append(parameters.getMinBodyAngleSpeedDuration());
            modeRational.append(")");

        } else if (timeSinceLastBehaviorModeChange < parameters.getMinBehaviorModeDuration()) {

            modeRational.append("time since last behavior mode change (");
            modeRational.append(timeSinceLastBehaviorModeChange);
            modeRational.append(") < minimum duration (");
            modeRational.append(parameters.getMinBehaviorModeDuration());
            modeRational.append(")");
 
        } else if (previousFrame.isTurning()) {

            // leave turn if head angle changes sign
            // (crosses both positive and negative threshold)
            final double previousHeadAngle = previousFrame.getHeadAngle();
            if (previousHeadAngle < 0) {
                if (-headAngle >
                    parameters.getMinHeadAngleToContinueTurning()) {
                    modeRational.append("previous frame was turning with a negative head angle (");
                    modeRational.append(getScaledValue(previousHeadAngle));
                    modeRational.append("),\nhead angle (");
                    modeRational.append(getScaledValue(headAngle));
                    modeRational.append(") < negative minimum to continue turning (");
                    modeRational.append(getScaledValue(-parameters.getMinHeadAngleToContinueTurning()));
                    modeRational.append(")");
                }
            } else if (headAngle >
                       parameters.getMinHeadAngleToContinueTurning()) {
                modeRational.append("previous frame was turning with a positive head angle (");
                modeRational.append(getScaledValue(previousHeadAngle));
                modeRational.append("),\nhead angle (");
                modeRational.append(getScaledValue(headAngle));
                modeRational.append(") > minimum to continue turning (");
                modeRational.append(getScaledValue(parameters.getMinHeadAngleToContinueTurning()));
                modeRational.append(")");
            }
            
        } else if (previousFrame.isCasting()) {

            final boolean test1 = 
                    Math.abs(smoothedBodyAngleSpeed) >
                    parameters.getMinBodyAngleSpeedForTurns();
            final boolean test2 = 
                    Math.abs(smoothedHeadAngleSpeed) <
                    parameters.getMinHeadAngleSpeedToContinueCasting();
            final boolean test3 = 
                    Math.abs(headAngle) >
                    parameters.getMinHeadAngleToContinueCasting();
            final boolean test4 =
                    Math.abs(smoothedHeadAngleSpeed) >
                    parameters.getMinHeadAngleSpeedToContinueCasting();
            
            if (test1 && test2) {

                modeRational.append("previous frame was casting,\nsmoothed body angle speed (");
                modeRational.append(getScaledValue(Math.abs(smoothedBodyAngleSpeed)));
                modeRational.append(") > minimum for turns (");
                modeRational.append(getScaledValue(parameters.getMinBodyAngleSpeedForTurns()));
                modeRational.append("),\nsmoothed head angle speed (");
                modeRational.append(getScaledValue(Math.abs(smoothedHeadAngleSpeed)));
                modeRational.append(") < minimum to continue casting (");
                modeRational.append(getScaledValue(parameters.getMinHeadAngleSpeedToContinueCasting()));
                modeRational.append(")");

            } else if (test3 || test4) {

                modeRational.append("previous frame was casting,\n");

                if (test1) { // indicates test2 failed above
                    modeRational.append("smoothed head angle speed (");
                    modeRational.append(getScaledValue(Math.abs(smoothedHeadAngleSpeed)));
                    modeRational.append(") >= minimum to continue casting (");
                    modeRational.append(getScaledValue(parameters.getMinHeadAngleSpeedToContinueCasting()));
                } else {
                    modeRational.append("smoothed body angle speed (");
                    modeRational.append(getScaledValue(Math.abs(smoothedBodyAngleSpeed)));
                    modeRational.append(") <= minimum for turns (");
                    modeRational.append(getScaledValue(parameters.getMinBodyAngleSpeedForTurns()));
                }

                if (test3) {
                    modeRational.append("),\nhead angle (");
                    modeRational.append(getScaledValue(Math.abs(headAngle)));
                    modeRational.append(") > minimum to continue casting (");
                    modeRational.append(getScaledValue(parameters.getMinHeadAngleToContinueCasting()));
                } else { // indicates test4 passed
                    modeRational.append("),\nsmoothed head angle speed (");
                    modeRational.append(getScaledValue(Math.abs(smoothedHeadAngleSpeed)));
                    modeRational.append(") > minimum to continue casting (");
                    modeRational.append(getScaledValue(parameters.getMinHeadAngleSpeedToContinueCasting()));
                }

                modeRational.append(")");
            }

        } else if (Math.abs(headAngle) >
                   parameters.getMinHeadAngleForCasting()) {

            modeRational.append("head angle (");
            modeRational.append(getScaledValue(Math.abs(headAngle)));
            modeRational.append(") > minimum for casting (");
            modeRational.append(getScaledValue(parameters.getMinHeadAngleForCasting()));
            modeRational.append(")");

        }

        // straight modes
        if (modeRational.length() == 0) {

            final double dotProductThreshold =
                    parameters.getDotProductThresholdForStraightModes();
            final double minStopOrBackUpDuration =
                    parameters.getMinStopOrBackUpDuration();

            StringBuilder overrideRationale = new StringBuilder(128);

            modeRational.append("cast and turn criteria not met\nsmoothed tail speed dot body angle (");
            modeRational.append(getScaledValue(smoothedTailSpeedDotBodyAngle));

            if (smoothedTailSpeedDotBodyAngle > dotProductThreshold) {

                modeRational.append(") > positive ");

            } else if (smoothedTailSpeedDotBodyAngle < -dotProductThreshold) {

                modeRational.append(") <  negative ");

                final Long timeBackingUp = currentFrame.getTimeBackingUp();
                if ((timeBackingUp != null) &&
                    (timeBackingUp < minStopOrBackUpDuration)) {
                    overrideRationale.append("\ntime backing up (");
                    overrideRationale.append(timeBackingUp);
                    overrideRationale.append(") < minimum stop or back-up duration (");
                    overrideRationale.append(minStopOrBackUpDuration);
                    overrideRationale.append(")");
                }

            } else {

                modeRational.append(") in between positive and negative ");

                final Long timeStopped = currentFrame.getTimeStopped();
                if ((timeStopped != null) &&
                    (timeStopped < minStopOrBackUpDuration)) {
                    overrideRationale.append("\ntime stopped (");
                    overrideRationale.append(timeStopped);
                    overrideRationale.append(") < minimum stop or back-up duration (");
                    overrideRationale.append(minStopOrBackUpDuration);
                    overrideRationale.append(")");
                }
            }
            
            modeRational.append("threshold for straight modes (");
            modeRational.append(getScaledValue(dotProductThreshold));
            modeRational.append(")");
            modeRational.append(overrideRationale);

        }

        behaviorModeRationaleTextArea.setText(modeRational.toString());
    }

    private void setPositionTextArea(LarvaFrameData currentFrame) {
        StringBuilder position = new StringBuilder(256);
        final LarvaSkeleton skeleton = currentFrame.getSkeleton();
        appendPoint("HEAD", skeleton.getHead(), position);
        position.append(", ");
        appendPoint("MIDPOINT", skeleton.getMidpoint(), position);
        position.append('\n');
        appendPoint("TAIL", skeleton.getTail(), position);
        position.append(", ");
        appendPoint("CENTROID", skeleton.getCentroid(), position);

        final DefinedEnvironment orientedRules = logModel.getOrientedRules();
        if (orientedRules != null) {
            final Long rotateTime = orientedRules.getRotateTime();
            if ((rotateTime != null) && (rotateTime <= currentFrame.getTime())) {
                final TrackerPoint rotationCenter = orientedRules.getRotationCenter();
                final TrackerPoint rotatedPoint = orientedRules.getRotatedPointForVariable(currentFrame);
                final TrackerPoint transformedPoint = orientedRules.getTransformedPoint(rotatedPoint);
                position.append("\nFor Stimulus:\n  rotated ");
                position.append(orientedRules.getPositionalVariable());
                position.append(' ');
                position.append(getScaledValue(
                        Math.toDegrees(orientedRules.getRotationAngleInRadians())));
                position.append('\u00b0');
                position.append(" around (");
                position.append(getScaledValue(rotationCenter.getX()));
                position.append(", ");
                position.append(getScaledValue(rotationCenter.getY()));
                position.append(") \n  to (");
                position.append(getScaledValue(rotatedPoint.getX()));
                position.append(", ");
                position.append(getScaledValue(rotatedPoint.getY()));
                position.append(") and transformed to (");
                position.append(getScaledValue(transformedPoint.getX()));
                position.append(", ");
                position.append(getScaledValue(transformedPoint.getY()));
                position.append(')');
            }
        }

        positionTextArea.setText(position.toString());
    }

    private void setLabelText(JLabel label,
                              LarvaBehaviorMode value) {
        if (value == null) {
            label.setText(EMPTY_TEXT);
        } else {
            label.setText(value.toString());
        }
    }

    private BigDecimal getScaledValue(double value) {
        final BigDecimal bd = new BigDecimal(value);
        return bd.setScale(4, BigDecimal.ROUND_HALF_UP);
    }

    private void setLabelText(JLabel label,
                              double value) {
        label.setText(String.format("% 6.4f", value));
    }

    private void appendPoint(String context,
                             TrackerPoint value,
                             StringBuilder sb) {
        final BigDecimal x = getScaledValue(value.getX());
        final BigDecimal y = getScaledValue(value.getY());
        sb.append(context);
        sb.append(": (");
        sb.append(x);
        sb.append(", ");
        sb.append(y);
        sb.append(')');
    }

    private static final String EMPTY_TEXT = "-";
}

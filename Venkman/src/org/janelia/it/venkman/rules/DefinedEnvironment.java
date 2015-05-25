/*
 * Copyright (c) 2014 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.venkman.rules;

import org.janelia.it.venkman.config.LarvaBehaviorParameters;
import org.janelia.it.venkman.config.rules.BehaviorLimitedKinematicVariableFunctionList;
import org.janelia.it.venkman.config.rules.LEDFlashPattern;
import org.janelia.it.venkman.config.rules.LEDStimulus;
import org.janelia.it.venkman.config.rules.NoiseGenerator;
import org.janelia.it.venkman.config.rules.PositionalVariable;
import org.janelia.it.venkman.config.rules.PositionalVariableFunction;
import org.janelia.it.venkman.config.rules.Stimulus;
import org.janelia.it.venkman.data.Calculator;
import org.janelia.it.venkman.data.LarvaFrameData;
import org.janelia.it.venkman.data.LarvaSkeleton;
import org.janelia.it.venkman.data.TrackerPoint;
import org.janelia.it.venkman.gui.parameter.annotation.VenkmanParameter;
import org.janelia.it.venkman.log.Logger;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import java.util.List;

/**
 * Stimulus rule implementation for:
 *
 * 1.1 Chemotaxis in response to virtual light gradients.
 *
 * Define a function I_{RUN}(x,y) that determined the LED intensity based
 * on the larva's position. Let the animal freely move in this artificial
 * gradient.
 *
 * Hypothesis to test:
 * for appropriate geometries, the light gradient elicits genuine
 * chemotaxis behavior.
 *
 * May 2013 update:
 * All defined environment rules now support orientation logic.
 * This means that the landscape (spatial intensity function) can be rotated and transformed
 * based upon the initial larval orientation.
 *
 * When orientation logic is enabled and a configured amount of time (e.g. 15 seconds)
 * has elapsed, this rule will record the angle required to rotate the larva's
 * tail-to-midpoint vector parallel to the centroid-to-arena-center vector in
 * the direction of the arena center.
 * It will also record the offsets required to reposition the larva's centroid a
 * configured distance from the arena center.
 * The rotation (relative to the centroid) and transformation parameters
 * are then applied for all subsequent frames to derive intensity values.
 *
 * May 2014 update:
 * Added intensity filters to all rules.
 *
 * NOTE: To support JAXB translation, each rule implementation class
 *       must be added to {@link StimulusRuleImplementations}.
 *
 * @author Eric Trautman
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class DefinedEnvironment
        extends LedActivationDurationRule implements TrackerArenaProvider {

    protected static final long DEFAULT_ORIENTATION_DERIVATION_DURATION = 15000;
    protected static final double DEFAULT_CENTROID_DISTANCE_FROM_CENTER = 0;
    protected static final double DEFAULT_ORIENTATION_OFFSET = 0;

    @VenkmanParameter(displayName = "Intensity Function")
    @XmlElement
    protected PositionalVariableFunction intensityFunction;

    // TODO: make noise generation configurable in ui (add to position variable function?)
    @XmlElement
    protected NoiseGenerator intensityNoiseGenerator;

    @VenkmanParameter(displayName = "Enable Orientation Logic")
    @XmlElement
    private boolean enableOrientationLogic;

    @VenkmanParameter(displayName = "Orientation Derivation Duration (milliseconds)",
                      minimum = "0",
                      maximum = "60000")
    @XmlElement
    private long orientationDerivationDuration;

    @VenkmanParameter(displayName = "Centroid Distance from Arena Center (millimeters)",
                      minimum = "0",
                      maximum = "400")
    @XmlElement
    private double centroidDistanceFromArenaCenter;

    @VenkmanParameter(displayName = "Centered Orientation Offset (degrees)",
                      minimum = "-180",
                      maximum = "180")
    @XmlElement
    private double centeredOrientationOffsetInDegrees;

    @VenkmanParameter(displayName = "Intensity Filter(s)",
                      displayOrder = 90, // force intensity filters to be displayed next to last
                      required = false,
                      listItemBaseName = "Filter")
    @XmlElement
    private BehaviorLimitedKinematicVariableFunctionList intensityFilterFunctionList;

    @VenkmanParameter(displayName = "Signal To Noise Ratio",
                      displayOrder = 95, // force SNR parameters to be displayed last
                      minimum = "0",
                      maximum = "100")
    @XmlElement
    private double signalToNoiseRatio;

    @XmlTransient
    TrackerPoint arenaCenter;

    @XmlTransient
    PositionalVariable positionalVariable;

    @XmlTransient
    private Long rotateTime;

    @XmlTransient
    private double rotationAngleInRadians;

    @XmlTransient
    private TrackerPoint rotationCenter;

    @XmlTransient
    private double xOffset;

    @XmlTransient
    private double yOffset;

    @XmlTransient
    private double transformedX;

    @XmlTransient
    private double transformedY;

    @XmlTransient
    private NoiseGenerator whiteNoiseGenerator;

    // no-arg constructor needed for JAXB and EditStimulusDialog
    @SuppressWarnings({"UnusedDeclaration"})
    public DefinedEnvironment() {
        this(DEFAULT_LED_ACTIVATION_DURATION,
             new PositionalVariableFunction(),
             0);
    }

    // legacy constructor (before addition of orientation logic) kept for tests
    public DefinedEnvironment(LEDFlashPattern ledActivationDuration,
                              PositionalVariableFunction intensityFunction,
                              double signalToNoiseRatio) {
        this(ledActivationDuration,
             intensityFunction,
             signalToNoiseRatio,
             false,
             DEFAULT_ORIENTATION_DERIVATION_DURATION,
             DEFAULT_CENTROID_DISTANCE_FROM_CENTER,
             DEFAULT_ORIENTATION_OFFSET,
             new BehaviorLimitedKinematicVariableFunctionList());
    }

    public DefinedEnvironment(LEDFlashPattern ledActivationDuration,
                              PositionalVariableFunction intensityFunction,
                              double signalToNoiseRatio,
                              boolean enableOrientationLogic,
                              long orientationDerivationDuration,
                              double centroidDistanceFromArenaCenter,
                              double centeredOrientationOffsetInDegrees,
                              BehaviorLimitedKinematicVariableFunctionList intensityFilterFunctionList) {
        super(ledActivationDuration);
        this.intensityFunction = intensityFunction;
        this.signalToNoiseRatio = signalToNoiseRatio;
        this.enableOrientationLogic = enableOrientationLogic;
        this.orientationDerivationDuration = orientationDerivationDuration;
        this.centroidDistanceFromArenaCenter = centroidDistanceFromArenaCenter;
        this.centeredOrientationOffsetInDegrees = centeredOrientationOffsetInDegrees;
        this.intensityFilterFunctionList = intensityFilterFunctionList;
        this.positionalVariable = null;
        this.rotateTime = null;
        this.whiteNoiseGenerator = new NoiseGenerator();
    }

    public PositionalVariableFunction getIntensityFunction() {
        return intensityFunction;
    }

    @Override
    public String getCode() {
        return "1.1";
    }

    @Override
    public String getDescription() {
        return "Chemotaxis in response to virtual light gradients.";
    }

    public boolean isEnableOrientationLogic() {
        return enableOrientationLogic;
    }

    @Override
    public void init(Logger logger) {

        super.init(logger);

        final double centerX = intensityFunction.getMaximumVariableX() / 2.0;
        final double centerY = intensityFunction.getMaximumVariableY() / 2.0;
        arenaCenter = new TrackerPoint(centerX, centerY);
    }

    @Override
    public List<? extends Stimulus> determineStimulus(List<LarvaFrameData> frameHistory,
                                                      LarvaBehaviorParameters behaviorParameters) {
        final List<LEDStimulus> stimulusList = determinePositionBasedStimulus(frameHistory);
        return applyIntensityFiltersAndWhiteNoise(frameHistory, stimulusList, 0.0);
    }

    protected List<LEDStimulus> determinePositionBasedStimulus(List<LarvaFrameData> frameHistory) {

        List<LEDStimulus> list;
        final LarvaFrameData frameData = frameHistory.get(0);

        if (enableOrientationLogic) {

            final long captureTime = frameData.getTime();

            if (captureTime < orientationDerivationDuration) {

                list = getDefaultStimulus();

            } else {

                final LarvaSkeleton skeleton = frameData.getSkeleton();

                if (positionalVariable == null) {
                    setTransformationParameters(captureTime, skeleton);
                }

                final TrackerPoint rotatedVariablePoint =
                        Calculator.getRotatedPoint(
                                positionalVariable.getValue(frameData), // originalPoint
                                rotationAngleInRadians,
                                rotationCenter);
                transformedX = rotatedVariablePoint.getX() + xOffset;
                transformedY = rotatedVariablePoint.getY() + yOffset;

                double value = intensityFunction.getValue(transformedX,
                                                          transformedY);
                if (intensityNoiseGenerator != null) {
                    value = value + intensityNoiseGenerator.getNoise();
                }

                list = getStimulusList(value);
            }

        } else {

            double value = intensityFunction.getValue(frameData);
            if (intensityNoiseGenerator != null) {
                value = value + intensityNoiseGenerator.getNoise();
            }
            list = getStimulusList(value);

        }

        return list;
    }

    protected List<LEDStimulus> applyIntensityFiltersAndWhiteNoise(List<LarvaFrameData> frameHistory,
                                                                   List<LEDStimulus> stimulusList,
                                                                   double minimum) {
        final LarvaFrameData frameData = frameHistory.get(0);
        intensityFilterFunctionList.applyValues(frameData, stimulusList, minimum);
        whiteNoiseGenerator.addNoiseUsingRatio(signalToNoiseRatio, stimulusList);
        return stimulusList;
    }

    /**
     * @param  width   the width of the arena.
     * @param  height  the height of the arena.
     *
     * @return matrix of intensity percentages for the tracker arena.
     */
    @Override
    public double[][] getArena(int width,
                               int height) {
        return intensityFunction.getArena(width, height);
    }

    public PositionalVariable getPositionalVariable() {
        return positionalVariable;
    }

    public Long getRotateTime() {
        return rotateTime;
    }

    public double getRotationAngleInRadians() {
        return rotationAngleInRadians;
    }

    public TrackerPoint getRotationCenter() {
        return rotationCenter;
    }

    public double getxOffset() {
        return xOffset;
    }

    public double getyOffset() {
        return yOffset;
    }

    /**
     * Restores the transient transformation parameters for this rule from the
     * specified data lists (parsed from a log file).
     *
     * @param  frameDataList  list of all frames for run
     *                        (needed in case rotation center was not logged).
     * @param  ruleDataList   list of all rule data for run.
     */
    public void restoreTransformationParameters(List<LarvaFrameData> frameDataList,
                                                List<RuleData> ruleDataList) {
        positionalVariable = intensityFunction.getVariable();
        for (RuleData ruleData : ruleDataList) {
            if (ROTATION_CENTER_DATA_NAME.equals(ruleData.getName())) {
                final String pair[] = ruleData.getValue().split(",");
                if (pair.length == 2) {
                    final String x = pair[0];
                    if ((x.length() > 1) && (x.charAt(0) == '[')) {
                        final double xValue = Double.parseDouble(x.substring(1));
                        final String y = pair[1];
                        final int lastChar = y.length() - 1;
                        if ((y.length() > 1) && (y.charAt(lastChar) == ']')) {
                            final double yValue = Double.parseDouble(y.substring(0, lastChar));
                            this.rotationCenter = new TrackerPoint(xValue, yValue);
                        }
                    }
                }
            } else if (ROTATION_ANGLE_IN_DEGREES_DATA_NAME.equals(ruleData.getName())) {
                final double angleInDegrees = Double.parseDouble(ruleData.getValue());
                this.rotationAngleInRadians = Math.toRadians(angleInDegrees);
                this.rotateTime = ruleData.getCaptureTime();
            } else if (X_OFFSET_DATA_NAME.equals(ruleData.getName())) {
                this.xOffset = Double.parseDouble(ruleData.getValue());
            } else if (Y_OFFSET_DATA_NAME.equals(ruleData.getName())) {
                this.yOffset = Double.parseDouble(ruleData.getValue());
            }
        }

        if (this.rotationCenter == null) {
            for (LarvaFrameData larvaFrameData : frameDataList) {
                if (larvaFrameData.getTime() == this.rotateTime) {
                    this.rotationCenter = larvaFrameData.getSkeleton().getCentroid();
                    break;
                }
            }
        }

    }

    /**
     * @param  frameData  frame containing skeleton point to rotate.
     *
     * @return skeleton point identified by this rule's positional variable
     *         that is rotated based upon this rule's rotation parameters.
     */
    public TrackerPoint getRotatedPointForVariable(LarvaFrameData frameData) {
        return getRotatedPoint(positionalVariable.getValue(frameData));
    }

    /**
     * @param  originalPoint  point to rotate.
     *
     * @return the specified point rotated based upon this rule's rotation parameters.
     */
    public TrackerPoint getRotatedPoint(TrackerPoint originalPoint) {
        TrackerPoint rotatedPoint = originalPoint;
        if (enableOrientationLogic) {
            rotatedPoint =
                    Calculator.getRotatedPoint(
                            originalPoint,
                            rotationAngleInRadians,
                            rotationCenter);
        }
        return rotatedPoint;
    }

    /**
     * @param  rotatedPoint  rotated point to transform.
     *
     * @return the specified point transformed based upon this rule's transformation parameters.
     */
    public TrackerPoint getTransformedPoint(TrackerPoint rotatedPoint) {
        TrackerPoint transformedPoint = rotatedPoint;
        if (enableOrientationLogic) {
            transformedPoint = new TrackerPoint(rotatedPoint.getX() + xOffset,
                                                rotatedPoint.getY() + yOffset);
        }
        return transformedPoint;
    }

    /**
     * @param  skeleton  skeleton to rotate and transform
     *
     * @return the specified skeleton with all of its points rotated and transformed
     *         based upon this rule's parameters.
     */
    public LarvaSkeleton getRotatedAndTransformedSkeleton(LarvaSkeleton skeleton) {
        LarvaSkeleton transformedSkeleton = skeleton;
        if (enableOrientationLogic) {

            final TrackerPoint rotatedHead = getRotatedPoint(skeleton.getHead());
            final TrackerPoint rotatedMidpoint = getRotatedPoint(skeleton.getMidpoint());
            final TrackerPoint rotatedTail = getRotatedPoint(skeleton.getTail());
            final TrackerPoint rotatedCentroid = getRotatedPoint(skeleton.getCentroid());

            final TrackerPoint transformedHead = getTransformedPoint(rotatedHead);
            final TrackerPoint transformedMidpoint = getTransformedPoint(rotatedMidpoint);
            final TrackerPoint transformedTail = getTransformedPoint(rotatedTail);
            final TrackerPoint transformedCentroid = getTransformedPoint(rotatedCentroid);

            transformedSkeleton =
                    new LarvaSkeleton(
                            skeleton.getCaptureTime(),
                            transformedHead,
                            transformedMidpoint,
                            transformedTail,
                            skeleton.getLength(),
                            transformedCentroid,
                            skeleton.getHeadToBodyAngle(),
                            // note: don't worry about adjusting tail bearing based on rotation
                            skeleton.getTailBearing());
        }
        return transformedSkeleton;
    }

    protected List<LEDStimulus> getDefaultStimulus() {
        return ZERO_INTENSITY_FOR_ONE_SECOND;
    }

    protected double getTransformedX() {
        return transformedX;
    }

    protected double getTransformedY() {
        return transformedY;
    }

    /**
     * @return true if orientation logic is NOT enabled or if orientation
     *         derivation is complete; otherwise false.
     */
    protected boolean isOriented() {
        return ((! enableOrientationLogic) || (positionalVariable != null));
    }

    private void setTransformationParameters(long captureTime,
                                             LarvaSkeleton skeleton) {

        positionalVariable = intensityFunction.getVariable();

        final TrackerPoint actualTail = skeleton.getTail();
        final TrackerPoint actualMidpoint = skeleton.getMidpoint();
        final TrackerPoint actualCentroid = skeleton.getCentroid();

        // determine rotation for angling the tail-to-midpoint vector
        // parallel to the centroid-to-arena center vector
        final double absoluteRotationAngleInRadians =
                Calculator.getAngleBetweenVectors(
                        actualTail,
                        actualMidpoint,
                        actualCentroid,
                        arenaCenter);

        // derived rotation angle is always positive,
        // transform tail-to-midpoint vector so that the transformed tail is
        // at the centroid location and then look at the transformed midpoint
        // location to determine whether angle sign needs to be flipped
        final double transformedTailXOffset =
                actualCentroid.getX() - actualTail.getX();
        final double transformedTailYOffset =
                actualCentroid.getY() - actualTail.getY();
        final TrackerPoint transformedMidpoint =
                new TrackerPoint(
                        (actualMidpoint.getX() + transformedTailXOffset),
                        (actualMidpoint.getY()) + transformedTailYOffset);

        if (Calculator.isCoordinateLeftOfVector(transformedMidpoint,
                                                actualCentroid,
                                                arenaCenter)) {
            rotationAngleInRadians = absoluteRotationAngleInRadians;
        } else {
            rotationAngleInRadians = -absoluteRotationAngleInRadians;
        }

        rotateTime = captureTime;
        rotationAngleInRadians +=
                Math.toRadians(centeredOrientationOffsetInDegrees);
        rotationCenter = actualCentroid;

        // calculate the x and y transformation offsets so that
        // the centroid is the correct distance from the arena center
        final TrackerPoint transformedCentroid =
                Calculator.getPointOnVector(
                        arenaCenter,
                        actualCentroid,
                        centroidDistanceFromArenaCenter);

        xOffset = transformedCentroid.getX() - actualCentroid.getX();
        yOffset = transformedCentroid.getY() - actualCentroid.getY();

        logRuleData(captureTime,
                    ROTATION_CENTER_DATA_NAME,
                    rotationCenter.toString());
        logRuleData(captureTime,
                    ROTATION_ANGLE_IN_DEGREES_DATA_NAME,
                    String.valueOf(
                            Math.toDegrees(rotationAngleInRadians)));
        logRuleData(captureTime,
                    X_OFFSET_DATA_NAME,
                    String.valueOf(xOffset));
        logRuleData(captureTime,
                    Y_OFFSET_DATA_NAME,
                    String.valueOf(yOffset));
    }

    private static final String ROTATION_ANGLE_IN_DEGREES_DATA_NAME = "rotationAngleInDegrees";
    private static final String ROTATION_CENTER_DATA_NAME = "rotationCenter";
    private static final String X_OFFSET_DATA_NAME = "xOffset";
    private static final String Y_OFFSET_DATA_NAME = "yOffset";

}

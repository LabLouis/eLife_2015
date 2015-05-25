/*
 * Copyright (c) 2014 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.venkman.config.rules;

import org.janelia.it.venkman.data.Calculator;
import org.janelia.it.venkman.data.LarvaFrameData;
import org.janelia.it.venkman.data.TrackerPoint;
import org.janelia.it.venkman.jaxb.DoubleMatrixAdapter;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

/**
 * Set of values indexed by a position.
 *
 * @author Eric Trautman
 */
@XmlRootElement
public class PositionalVariableFunction {

    /** The default width (in millimeters) of the tracker arena. */
    public static final int DEFAULT_TRACKER_ARENA_WIDTH = 400;

    /** The default height (in millimeters) of the tracker arena. */
    public static final int DEFAULT_TRACKER_ARENA_HEIGHT = 400;

    /** The positional variable for indexing into this function. */
    @XmlElement
    private PositionalVariable variable;

    /** The maximum x position for the selected variable. */
    @XmlElement
    private double maximumVariableX;

    /** The maximum y position for the selected variable. */
    @XmlElement
    private double maximumVariableY;

    /** The method for handling out of range position values. */
    @XmlElement
    private OutOfRangeErrorHandlingMethod positionRangeErrorHandlingMethod;

    /** The number of columns in this function's value matrix. */
    @XmlElement
    private int columnCount;

    /** The number of rows in this function's value matrix. */
    @XmlElement
    private int rowCount;

    /** The source values for this function. */
    @XmlJavaTypeAdapter(DoubleMatrixAdapter.class)
    @XmlElement
    private double[][] values;

    /**
     * The derived multiplicative factor to scale variable x values
     * to the range of column indices for this function.
     */
    @XmlElement
    private double factorX;

    /**
     * The derived multiplicative factor to scale variable y values
     * to the range of row indices for this function's values.
     */
    @XmlElement
    private double factorY;

    /**
     * Constructs and empty function that always returns zero.
     */
    public PositionalVariableFunction() {
        this(PositionalVariable.HEAD,
             DEFAULT_TRACKER_ARENA_WIDTH,
             DEFAULT_TRACKER_ARENA_HEIGHT,
             OutOfRangeErrorHandlingMethod.END_SESSION_FOR_MINIMUM_AND_MAXIMUM,
             new double[][] {{0}});
    }

    /**
     * Constructs a function with the specified output values.
     * The variable range is assumed to match the the range of values.
     *
     * @param  variable  the positional variable whose x,y value should
     *                   be used as this function's input.
     *
     * @param  values    the output values for this function.
     */
    public PositionalVariableFunction(PositionalVariable variable,
                                      double[][] values) {
        this(variable,
             values[0].length - 1,
             values.length - 1,
             OutOfRangeErrorHandlingMethod.END_SESSION_FOR_MINIMUM_AND_MAXIMUM,
             values);
    }

    /**
     * Constructs a function with the specified output values.
     * The variable input range is identified by the specified
     * maximum values, allowing inputs to be scaled.
     *
     * @param  variable                          the positional variable whose x,y value
     *                                           should be used as this function's input.
     * @param  maximumVariableX                  the maximum X input position.
     * @param  maximumVariableY                  the maximum Y input position.
     * @param  positionRangeErrorHandlingMethod  method for handling out of range position values.
     * @param  values                            the output values for this function.
     *
     * @throws IllegalArgumentException
     *   if the specified maximums are not greater than zero or
     *   if the variable or values are not specified.
     */
    public PositionalVariableFunction(PositionalVariable variable,
                                      double maximumVariableX,
                                      double maximumVariableY,
                                      OutOfRangeErrorHandlingMethod positionRangeErrorHandlingMethod,
                                      double[][] values)
            throws IllegalArgumentException {

        if (variable == null) {
            throw new IllegalArgumentException(
                    "variable not defined for function");
        }
        this.variable = variable;

        if ((values == null) || values.length == 0 || values[0].length == 0) {
            throw new IllegalArgumentException(
                    "values not defined for function");
        }
        this.values = values;
        this.rowCount = values.length;
        this.columnCount = values[0].length;

        final double practicallyZero = 0.00000001;
        if (maximumVariableX <= practicallyZero) {
            throw new IllegalArgumentException(
                    "maximum x (" + maximumVariableX +
                    ") must be greater than zero");
        }
        this.factorX = (double) (this.columnCount - 1) / maximumVariableX;
        this.maximumVariableX = maximumVariableX;

        if (maximumVariableY <= practicallyZero) {
            throw new IllegalArgumentException(
                    "maximum y (" + maximumVariableY +
                    ") must be greater than zero");
        }
        this.factorY = (double) (this.rowCount - 1) / maximumVariableY;
        this.maximumVariableY = maximumVariableY;

        this.positionRangeErrorHandlingMethod = positionRangeErrorHandlingMethod;
    }

    public PositionalVariable getVariable() {
        return variable;
    }

    public double getMaximumVariableX() {
        return maximumVariableX;
    }

    public double getMaximumVariableY() {
        return maximumVariableY;
    }

    public OutOfRangeErrorHandlingMethod getPositionRangeErrorHandlingMethod() {
        return positionRangeErrorHandlingMethod;
    }

    public double[][] getValues() {
        return values;
    }

    /**
     * @param  frameData  current frame data.
     *
     * @return this function's value for the specified frame data.
     *
     * @throws IllegalArgumentException
     *   if the frame data cannot be mapped by this function.
     */
    public double getValue(LarvaFrameData frameData)
            throws IllegalArgumentException {

        final TrackerPoint point = variable.getValue(frameData);
        return getBiLinearInterpolatedResult(point, factorX, factorY);
    }

    /**
     * @param  x  x tracker coordinate.
     * @param  y  y tracker coordinate.
     *
     * @return this function's value for the specified coordinates.
     *
     * @throws IllegalArgumentException
     *   if the coordinates cannot be mapped by this function.
     */
    public double getValue(double x,
                           double y)
            throws IllegalArgumentException {

        final TrackerPoint point = new TrackerPoint(x, y);
        return getBiLinearInterpolatedResult(point, factorX, factorY);
    }

    /**
     * @param  point    the current input coordinates.
     * @param  factorX  scaling factor for x values.
     * @param  factorY  scaling factor for y values.
     *
     * @return  this function's bi-linear interpolated output for the specified
     *          point using the specified scaling factors.
     *
     * @throws IllegalArgumentException
     *   if the specified point is out of range for this function
     *   (after scaling).
     *
     */
    public double getBiLinearInterpolatedResult(TrackerPoint point,
                                                double factorX,
                                                double factorY)
            throws IllegalArgumentException {


        double actualY = point.getY();
        if ((actualY < 0) && positionRangeErrorHandlingMethod.repeatMinimum()) {
            actualY = 0;
        } else if ((actualY > maximumVariableY) && positionRangeErrorHandlingMethod.repeatMaximum()) {
            actualY = maximumVariableY;
        }

        final double y = factorY * actualY;
        final int previousY = (int) y;
        int nextY = previousY + 1;

        if ((previousY < 0) || (previousY >= rowCount)) {
            throw new IllegalArgumentException(
                    "row index " + previousY + " for " + variable +
                    " y coordinate " + point.getY() +
                    " is out of function range (0 to " +
                    (rowCount - 1) + ")");
        }

        if (nextY == rowCount) {
            nextY = previousY;
        }

        double actualX = point.getX();
        if ((actualX < 0) && positionRangeErrorHandlingMethod.repeatMinimum()) {
            actualX = 0;
        } else if ((actualX > maximumVariableX) && positionRangeErrorHandlingMethod.repeatMaximum()) {
            actualX = maximumVariableX;
        }

        final double x = factorX * actualX;
        final int previousX = (int) x;
        int nextX = previousX + 1;

        if ((previousX < 0) || (previousX >= columnCount)) {
            throw new IllegalArgumentException(
                    "column index " + previousX + " for " + variable +
                    " x coordinate " + point.getX() +
                    " is out of function range (0 to " +
                    (columnCount - 1) + ")");
        }

        if (nextX == columnCount) {
            nextX = previousX;
        }

        final double interpolatedXForPreviousY =
                Calculator.getLinearInterpolation(
                        x,
                        previousX, values[previousY][previousX],
                        nextX, values[previousY][nextX]);

        final double interpolatedXForNextY =
                Calculator.getLinearInterpolation(
                        x,
                        previousX, values[nextY][previousX],
                        nextX, values[nextY][nextX]);

        return Calculator.getLinearInterpolation(
                y,
                previousY, interpolatedXForPreviousY,
                nextY, interpolatedXForNextY);
    }

    /**
     * @param  width   the width of the arena.
     * @param  height  the height of the arena.
     *
     * @return interpolated values scaled to the specified height and width.
     */
    public double[][] getArena(int width,
                               int height) {
        if (width < 1) {
            throw new IllegalArgumentException(
                    "arena width (" + width + ") must be greater than zero");
        }

        if (height < 1) {
            throw new IllegalArgumentException(
                    "arena height (" + height + ") must be greater than zero");
        }

        double[][] arena = new double[height][width];
        double columnCountMinusOne = this.columnCount - 1;
        double rowCountMinusOne = this.rowCount - 1;
        final double arenaFactorX = columnCountMinusOne / (width - 1);
        final double arenaFactorY = rowCountMinusOne / (height - 1);
        TrackerPoint point;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                point = new TrackerPoint(x, y);
                arena[y][x] = getBiLinearInterpolatedResult(point,
                                                            arenaFactorX,
                                                            arenaFactorY);
            }
        }

        return arena;
    }

}

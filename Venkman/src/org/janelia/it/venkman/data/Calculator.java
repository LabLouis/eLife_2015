/*
 * Copyright (c) 2013 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.venkman.data;

import java.util.List;

/**
 * Utility calculation methods.
 *
 * @author Eric Trautman
 */
public class Calculator {

    public static final TrackerPoint ORIGIN = new TrackerPoint(0.0, 0.0);

    /**
     * @param  angleInDegrees  the vector angle in radians.
     *
     * @return the terminal point of the unit vector with
     *         the specified direction.
     */
    public static TrackerPoint getUnitVectorPointForTrackerAngle(double angleInDegrees) {
        // Tracker Angles:
        //                 (0,-1) 90 deg
        //
        //
        //  (-1,0) 0 deg               (1,0) +-180 deg
        //
        //
        //                 (0,1) -90 deg

        // Java Angles:
        //                 (0,-1) -90 deg
        //
        //
        //  (-1,0) +-180 deg               (1,0) 0 deg
        //
        //
        //                 (0,1) 90 deg

        final double translatedAngleInDegrees;
        if (angleInDegrees < 0.0) {
            translatedAngleInDegrees = 180.0 + angleInDegrees;
        } else {
            translatedAngleInDegrees = angleInDegrees - 180.0;
        }
        final double angleInRadians = Math.toRadians(translatedAngleInDegrees);
        return new TrackerPoint(Math.cos(angleInRadians),
                                Math.sin(angleInRadians));
    }

    /**
     * @param  vectorAStart  initial point of first vector.
     * @param  vectorAStop   terminal point of first vector.
     * @param  vectorBStart  initial point of second vector.
     * @param  vectorBStop   terminal point of second vector.
     *
     * @return the dot product of the two specified vectors.
     */
    public static double getDotProduct(TrackerPoint vectorAStart,
                                       TrackerPoint vectorAStop,
                                       TrackerPoint vectorBStart,
                                       TrackerPoint vectorBStop) {

        final double vectorADeltaX = vectorAStop.getX() - vectorAStart.getX();
        final double vectorADeltaY = vectorAStop.getY() - vectorAStart.getY();

        final double vectorBDeltaX = vectorBStop.getX() - vectorBStart.getX();
        final double vectorBDeltaY = vectorBStop.getY() - vectorBStart.getY();

        return (vectorADeltaX * vectorBDeltaX) +
               (vectorADeltaY * vectorBDeltaY);
    }

    /**
     * @param  vectorAStart  initial point of first vector.
     * @param  vectorAStop   terminal point of first vector.
     * @param  vectorBStart  initial point of second vector.
     * @param  vectorBStop   terminal point of second vector.
     *
     * @return the angle (in radians) between the two specified vectors.
     */
    public static double getAngleBetweenVectors(TrackerPoint vectorAStart,
                                                TrackerPoint vectorAStop,
                                                TrackerPoint vectorBStart,
                                                TrackerPoint vectorBStop) {

        // a dot b =  |a| * |b| * cos theta
        // theta = acos ( (a dot b) / (|a| * |b|) )

        final double dotProduct = getDotProduct(vectorAStart,
                                                vectorAStop,
                                                vectorBStart,
                                                vectorBStop);

        final double vector1Length = vectorAStart.distance(vectorAStop);
        final double vector2Length = vectorBStart.distance(vectorBStop);

        return Math.acos(dotProduct / (vector1Length * vector2Length));
    }

    /**
     * @param  tail      tail coordinate.
     * @param  midpoint  midpoint coordinate.
     * @param  head      head coordinate.
     *
     * @return the (absolute/positive) size of the angle in radians
     *         at the midpoint vertex of the triangle comprised of
     *         the specified coordinates.
     */
    public static double getDotProductMidpointAngle(TrackerPoint tail,
                                                    TrackerPoint midpoint,
                                                    TrackerPoint head) {
        return getAngleBetweenVectors(midpoint,
                                      tail,
                                      midpoint,
                                      head);
    }

    /**
     * @param  tail      tail coordinate.
     * @param  midpoint  midpoint coordinate.
     * @param  head      head coordinate.
     *
     * @return the (absolute/positive) size of the angle in radians
     *         at the midpoint vertex of the triangle comprised of
     *         the specified coordinates.
     */
    public static double getCosineRuleMidpointAngle(TrackerPoint tail,
                                                    TrackerPoint midpoint,
                                                    TrackerPoint head) {
        // b^2 = a^2 + c^2 - (2ac * cos(B))

        final double a = midpoint.distance(head);
        final double b = tail.distance(head);
        final double c = tail.distance(midpoint);

        return Math.acos( ((a*a) + (c*c) - (b*b)) / (2*a*c) );
    }

    /**
     * @param  coordinate   coordinate to compare to vector.
     * @param  vectorStart  initial point of vector.
     * @param  vectorStop   terminal point of vector.
     *
     * @return true if the specified coordinate is to the left of
     *         the specified vector.  This assumes a coordinate system
     *         with the origin in the upper left corner, x increasing
     *         to the right, and y increasing down.
     */
    public static boolean isCoordinateLeftOfVector(TrackerPoint coordinate,
                                                   TrackerPoint vectorStart,
                                                   TrackerPoint vectorStop) {

        // from http://www.gamedev.net/community/forums/topic.asp?topic_id=542870&whichpage=1&#3500873

        final double Ax = vectorStart.getX();
        final double Ay = vectorStart.getY();
        final double Bx = vectorStop.getX();
        final double By = vectorStop.getY();
        final double product =
                ((Bx - Ax) * (coordinate.getY() - Ay)) -
                ((By - Ay) * (coordinate.getX() - Ax));

        return (product < 0.0);
    }

    /**
     * @param  line1Point1      one point on the first line.
     * @param  line1Point2      another (different) point on the first line.
     * @param  line2Point1      one point on the second line.
     * @param  line2Point2      another (different) point on the second line.
     *
     * @return the intersection point between the specified lines or null
     *         if the lines are parallel (including the same lines).
     */
    public static TrackerPoint getIntersectionOfLines(TrackerPoint line1Point1,
                                                      TrackerPoint line1Point2,
                                                      TrackerPoint line2Point1,
                                                      TrackerPoint line2Point2) {

        TrackerPoint intersectionPoint = null;

        final double line1Slope = getSlope(line1Point1, line1Point2);
        final double line2Slope = getSlope(line2Point1, line2Point2);

        if (line1Slope != line2Slope) {

            double intersectionX;
            double intersectionY;
            double line1Constant;
            double line2Constant;

            if (Double.isInfinite(line1Slope)) {

                if (! Double.isInfinite(line2Slope)) {

                    line2Constant = getLineConstant(line2Point1, line2Slope);
                    intersectionX = line1Point1.getX();
                    intersectionY =
                            (line2Slope * intersectionX) + line2Constant;

                    intersectionPoint = new TrackerPoint(intersectionX,
                                                         intersectionY);

                } // else lines are parallel, so leave intersection null

            } else if (Double.isInfinite(line2Slope)) {

                line1Constant = getLineConstant(line1Point1, line1Slope);
                intersectionX = line2Point1.getX();
                intersectionY = (line1Slope * intersectionX) + line1Constant;

                intersectionPoint = new TrackerPoint(intersectionX,
                                                     intersectionY);

            } else {

                line1Constant = getLineConstant(line1Point1, line1Slope);
                line2Constant = getLineConstant(line2Point1, line2Slope);
                // y = m1x + b1, y = m2x + b2
                // m1x - m2x = b2 - b1
                // x = (b2 - b1) / (m1 - m2)
                intersectionX = (line2Constant - line1Constant) /
                                (line1Slope - line2Slope);
                intersectionY = (line1Slope * intersectionX) + line1Constant;

                intersectionPoint = new TrackerPoint(intersectionX,
                                                     intersectionY);
            }

        } // else lines are parallel, so leave intersection null

        return intersectionPoint;
    }

    /**
     * @param  point         a point on the line.
     * @param  anotherPoint  another (different) point on the line.
     *
     * @return the slope of the line containing the specified points.
     */
    public static double getSlope(TrackerPoint point,
                                  TrackerPoint anotherPoint) {
        return (point.getY() - anotherPoint.getY()) /
               (point.getX() - anotherPoint.getX());
    }

    /**
     * @param  pointOnLine  a point on the line.
     * @param  slope        the slope of the line.
     *
     * @return the constant (b) in y = mx + b for the line that contains
     *         the specified point and has the specified slope.
     */
    public static double getLineConstant(TrackerPoint pointOnLine,
                                         double slope) {
        // y = mx + b  =>  b = y - mx
        double lineConstant;
        if (Double.isInfinite(slope)) {
            lineConstant = 0;
        } else {
            lineConstant = pointOnLine.getY() - (slope * pointOnLine.getX());
        }
        return lineConstant;
    }

//    /**
//     * @param  from  beginning point.
//     * @param  to    end point.
//     *
//     * @return the atan2 (radians) value for the specified coordinates.
//     *
//     * @see Math#atan2(double, double)
//     */
//    public static double atan2(TrackerPoint from,
//                               TrackerPoint to) {
//        //  9 o'clock:  0
//        // 12 o'clock:  pi/2
//        //  3 o'clock:  pi
//        //  6 o'clock: -pi/2
//        return (Math.atan2(from.getY() - to.getY(),
//                           from.getX() - to.getX()));
//    }

    /**
     * @param  perimeter  list of points defining the polygon perimeter.
     *
     * @return the area of the polygon defined by the specified list of
     *         perimeter points.
     */
    public static double getArea(List<TrackerPoint> perimeter) {

        // from http://local.wasp.uwa.edu.au/~pbourke/geometry/polyarea/source2.java
        
        double area = 0;

        if (perimeter != null) {

            final int size = perimeter.size();

            if (size > 1) {

                TrackerPoint pointA;
                TrackerPoint pointB;
                for (int i = 0; i < size; i++) {
                    pointA = perimeter.get(i);
                    pointB = perimeter.get((i + 1) % size);
                    area += pointA.getX() * pointB.getY();
                    area -= pointA.getY() * pointB.getX();
                }

                area = Math.abs(area / 2.0);
            }
            
        }

        return(area);
    }

    /**
     * Works best when previousInput <= actualInput <= nextInput.
     *
     * @param  actualInput      the actual input value.
     * @param  previousInput    the previous discrete (known) input value.
     * @param  previousOutput   the output value for the previous input.
     * @param  nextInput        the next discrete (known) input value.
     * @param  nextOutput       the output value for the next input.
     *
     * @return the linearly interpolated output value
     *         for the specified actual input value.
     */
    public static double getLinearInterpolation(double actualInput,
                                                double previousInput,
                                                double previousOutput,
                                                double nextInput,
                                                double nextOutput) {
        double interpolatedOutput;

        if (previousInput == nextInput) {

            interpolatedOutput = previousOutput;

        } else {

            final double discreteDelta = nextInput - previousInput;
            final double previousFactor =
                    (nextInput - actualInput) / discreteDelta;
            final double nextFactor =
                    (actualInput - previousInput) / discreteDelta;

            interpolatedOutput = (previousFactor * previousOutput) +
                                 (nextFactor * nextOutput);
        }

        return interpolatedOutput;
    }

    /**
     *
     * @param  originalPoint   point to rotate.
     * @param  angleInRadians  angle of rotation in radians.
     * @param  rotationCenter  point around which rotation should be performed.
     *
     * @return the original point rotated the specified angle (in radians)
     *         around the rotation point.
     */
    public static TrackerPoint getRotatedPoint(TrackerPoint originalPoint,
                                               double angleInRadians,
                                               TrackerPoint rotationCenter) {

        final double deltaX = originalPoint.getX() - rotationCenter.getX();
        final double deltaY = originalPoint.getY() - rotationCenter.getY();
        final double rotatedX = rotationCenter.getX() +
                                (Math.cos(angleInRadians) * deltaX) -
                                (Math.sin(angleInRadians) * deltaY);
        final double rotatedY = rotationCenter.getY() +
                                (Math.sin(angleInRadians) * deltaX) +
                                (Math.cos(angleInRadians) * deltaY);
        return new TrackerPoint(rotatedX, rotatedY);
    }

    /**
     * @param  vectorStart        vector start (initial) point.
     * @param  vectorStop         vector stop (terminal) point.
     * @param  distanceFromStart  distance (magnitude) for derived terminal
     *                            point.
     *
     * @return  the point along the specified vector that is the
     *          specified distance from the vector's start.
     */
    public static TrackerPoint getPointOnVector(TrackerPoint vectorStart,
                                                TrackerPoint vectorStop,
                                                double distanceFromStart) {
        final double vectorMagnitude = vectorStart.distance(vectorStop);
        final double ratio = distanceFromStart / vectorMagnitude;
        final double x = (ratio * vectorStop.getX()) +
                         ((1.0 - ratio) * vectorStart.getX());
        final double y = (ratio * vectorStop.getY()) +
                         ((1.0 - ratio) * vectorStart.getY());
        return new TrackerPoint(x, y);
    }

}
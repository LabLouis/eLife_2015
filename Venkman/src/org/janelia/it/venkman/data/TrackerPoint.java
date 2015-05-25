/*
 * Copyright (c) 2014 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.venkman.data;

import javax.xml.bind.annotation.XmlAttribute;
import java.io.Serializable;

/**
 * A point (xy coordinate pair) from the tracker.
 *
 * @author Eric Trautman
 */
public class TrackerPoint implements Serializable {

    @XmlAttribute
    private double x;
    @XmlAttribute
    private double y;

    // no-arg constructor required for JAXB
    @SuppressWarnings({"UnusedDeclaration"})
    public TrackerPoint() {
    }

    /**
     * Constructs a point with the specified coordinates.
     *
     * @param  x  the X coordinate.
     * @param  y  the Y coordinate.
     */
    public TrackerPoint(double x,
                        double y) {
        this.x = x;
        this.y = y;
    }

    /**
     * Constructs a point with the specified coordinates.
     *
     * @param  x  the X coordinate.
     * @param  y  the Y coordinate.
     *
     * @throws NumberFormatException
     *   if either coordinate cannot be parsed into a double value.
     */
    public TrackerPoint(String x,
                        String y)
            throws NumberFormatException {

        this(java.lang.Double.parseDouble(x),
             java.lang.Double.parseDouble(y));
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    @Override
    public boolean equals(Object o) {
        boolean isEqual = false;
        if (this == o) {
            isEqual = true;
        } else if (o instanceof TrackerPoint) {
            TrackerPoint that = (TrackerPoint) o;
            isEqual = (Double.compare(that.x, x) == 0) &&
                      (Double.compare(that.y, y) == 0);
        }

        return isEqual;
    }

    // auto generated
    @Override
    public int hashCode() {
        long temp = x != +0.0d ? Double.doubleToLongBits(x) : 0L;
        int result = (int) (temp ^ (temp >>> 32));
        temp = y != +0.0d ? Double.doubleToLongBits(y) : 0L;
        return 31 * result + (int) (temp ^ (temp >>> 32));
    }

    @Override
    public String toString() {
        return "[" + x + "," + y + "]";
    }

    /**
     * @param  b  another point.
     *
     * @return the distance between this point and the specified points.
     */
    public double distance(TrackerPoint b) {
        return distance(this, b);
    }

    /**
     * @param  a  the first point
     * @param  b  the second point.
     *
     * @return the distance between the two points.
     */
    public static double distance(TrackerPoint a,
                                  TrackerPoint b) {
        final double deltaX = a.x - b.x;
        final double deltaY = a.y - b.y;
        return Math.sqrt((deltaX * deltaX) + (deltaY * deltaY));
    }

}

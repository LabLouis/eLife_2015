/*
 * Copyright (c) 2014 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.venkman.data;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

/**
 * The key larva skeleton points and measurements from the tracker.
 *
 * @author Eric Trautman
 */
@XmlType(propOrder={"head", "midpoint", "tail", "centroid" })
@XmlAccessorType(XmlAccessType.FIELD)
public class LarvaSkeleton implements Serializable {

    @XmlAttribute
    private long captureTime;
    @XmlElement
    private TrackerPoint head;
    @XmlElement
    private TrackerPoint midpoint;
    @XmlElement
    private TrackerPoint tail;
    @XmlElement
    private TrackerPoint centroid;
    @XmlAttribute
    private double length;

    /**
     * The head to body angle is the angle (in degrees) between
     * the midpoint-head segment and the extension of the tail-midpoint segment
     * (angle A in the following diagram).
     *
     * <pre>
     *           ^    H(ead)
     *           | A /
     *           |  /
     *           | /
     *           |/
     *           M(idpoint)
     *           |
     *           |
     *           |
     *           |
     *           T(ail)
     * </pre>
     */
    @XmlAttribute
    private double headToBodyAngle;

    /**
     * The tail bearing is the angle (in degrees) between
     * the midpoint-tail segment and the horizontal axis of the arena
     * (angle B in the following diagram).
     *
     * <pre>
     *                H(ead)
     *              /
     *             /
     *            /
     *           M(idpoint)
     *           |
     *           |
     *           | B
     * ----------|-------- horizontal axis of arena
     *           T(ail)
     * </pre>
     */
    @XmlAttribute
    private double tailBearing;

    // no-arg constructor required for JAXB
    public LarvaSkeleton() {

    }

    /**
     * Constructs a skeleton with the specified values.
     *
     * @param  captureTime      time this data was captured by the tracker.
     * @param  head             head position.
     * @param  midpoint         midpoint position.
     * @param  tail             tail position.
     * @param  length           skeleton length.
     * @param  centroid         centroid position.
     * @param  headToBodyAngle  angle between the midpoint-head segment and
     *                          the extension of the tail-midpoint segment.
     *                          The value is expressed in degrees.
     * @param  tailBearing      angle between the midpoint-tail segment and
     *                          the horizontal axis of the arena.
     *                          The value is expressed in degrees.
     */
    public LarvaSkeleton(long captureTime,
                         TrackerPoint head,
                         TrackerPoint midpoint,
                         TrackerPoint tail,
                         double length,
                         TrackerPoint centroid,
                         double headToBodyAngle,
                         double tailBearing) {
        this();
        this.captureTime = captureTime;
        this.head = head;
        this.centroid = centroid;
        this.midpoint = midpoint;
        this.tail = tail;
        this.length = length;
        this.headToBodyAngle = headToBodyAngle;
        this.tailBearing = tailBearing;
    }

    /**
     * Constructs a skeleton with the specified values.
     *
     * @param  captureTime      time this data was captured by the tracker.
     * @param  headX            head x position.
     * @param  headY            head y position.
     * @param  midpointX        midpoint x position.
     * @param  midpointY        midpoint y position.
     * @param  tailX            tail x position.
     * @param  tailY            tail y position.
     * @param  length           skeleton length.
     * @param  centroidX        centroid x position.
     * @param  centroidY        centroid y position.
     * @param  headToBodyAngle  angle between the midpoint-head segment and
     *                          the extension of the tail-midpoint segment.
     *                          The value is expressed in degrees.
     * @param  tailBearing      angle between the midpoint-tail segment and
     *                          the horizontal axis of the arena.
     *                          The value is expressed in degrees.
     *
     * @throws NumberFormatException
     *   if any specified value strings cannot be parsed.
     */
    public LarvaSkeleton(String captureTime,
                         String headX,
                         String headY,
                         String midpointX,
                         String midpointY,
                         String tailX,
                         String tailY,
                         String length,
                         String centroidX,
                         String centroidY,
                         String headToBodyAngle,
                         String tailBearing)
        throws NumberFormatException {

        this(Long.parseLong(captureTime),
             new TrackerPoint(headX, headY),
             new TrackerPoint(midpointX, midpointY),
             new TrackerPoint(tailX, tailY),
             Double.parseDouble(length),
             new TrackerPoint(centroidX, centroidY),
             Double.parseDouble(headToBodyAngle),
             Double.parseDouble(tailBearing));
    }

    public long getCaptureTime() {
        return captureTime;
    }

    public TrackerPoint getHead() {
        return head;
    }

    public TrackerPoint getMidpoint() {
        return midpoint;
    }

    public TrackerPoint getTail() {
        return tail;
    }

    public TrackerPoint getCentroid() {
        return centroid;
    }

    public double getLength() {
        return length;
    }

    public double getHeadToBodyAngle() {
        return headToBodyAngle;
    }

    public double getTailBearing() {
        return tailBearing;
    }

    @Override
    public String toString() {
        return "LarvaSkeleton{" +
               "captureTime=" + captureTime +
               ", head=" + head +
               ", midpoint=" + midpoint +
               ", tail=" + tail +
               ", centroid=" + centroid +
               ", length=" + length +
               ", headToBodyAngle=" + headToBodyAngle +
               ", tailBearing=" + tailBearing +
               '}';
    }

    public List<TrackerPoint> getPoints() {
        return Arrays.asList(tail, midpoint, head);
    }

    /**
     * @return a clone (deep copy) of this skeleton.
     */
    public LarvaSkeleton getClone() {
        return new LarvaSkeleton(captureTime,
                                 head,
                                 midpoint,
                                 tail,
                                 length,
                                 centroid,
                                 headToBodyAngle,
                                 tailBearing);
    }

    /**
     * Copies all measurements from the specified previous skeleton to this
     * skeleton leaving the capture time alone.  This is intended to be used
     * when the tracker coordinates have wildly jumped from one frame to
     * the next and we want to skip processing of the aberrant coordinates.
     *
     * @param  previousSkeleton  skeleton from the previous "valid" frame.
     */
    public void overrideMeasurements(LarvaSkeleton previousSkeleton) {
        this.head = previousSkeleton.head;
        this.midpoint = previousSkeleton.midpoint;
        this.tail = previousSkeleton.tail;
        this.centroid = previousSkeleton.centroid;
        this.length = previousSkeleton.length;
        this.headToBodyAngle = previousSkeleton.headToBodyAngle;
        this.tailBearing = previousSkeleton.tailBearing;
    }
}

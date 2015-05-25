/*
 * Copyright (c) 2014 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.venkman;

import org.janelia.it.venkman.data.LarvaBehaviorMode;
import org.janelia.it.venkman.data.LarvaFrameData;
import org.janelia.it.venkman.data.LarvaSkeleton;
import org.janelia.it.venkman.data.TrackerPoint;
import org.junit.Assert;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import java.io.StringReader;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.util.Formatter;

/**
 * Common utility methods for simplifying tests.
 *
 * @author Eric Trautman
 */
public class TestUtilities {

    public static final TrackerPoint DEFAULT_POINT = new TrackerPoint(0, 0);
    /**
     * @param  value  test value to scale.
     *
     * @return normalized value scaled to 2 decimal places for test comparisons.
     */
    public static BigDecimal getScaledValue(Double value) {
        BigDecimal scaledValue = null;
        if (value != null) {
            scaledValue = new BigDecimal(value);
            scaledValue = scaledValue.setScale(2, BigDecimal.ROUND_HALF_UP);
        }
        return scaledValue;
    }

    /**
     * Asserts that two double values are equal.
     * Both values are scaled to 2 decimal places to simplify comparison.
     *
     * @param  message   error message if assertion fails.
     * @param  expected  expected value.
     * @param  actual    actual value.
     */
    public static void assertEquals(String message,
                                    double expected,
                                    double actual) {
        Assert.assertEquals(message,
                            getScaledValue(expected),
                            getScaledValue(actual));
    }

    /**
     * Asserts that two Double values are equal (handles null values).
     * If both values are not null, they are scaled to 2 decimal places to simplify comparison.
     *
     * @param  message   error message if assertion fails.
     * @param  expected  expected value.
     * @param  actual    actual value.
     */
    public static void assertEqualDoubles(String message,
                                          Double expected,
                                          Double actual) {
        if (expected == null) {
            Assert.assertNull(message, actual);
        } else if (actual == null) {
            Assert.assertEquals(message, expected, null);
        } else {
            assertEquals(message, expected, actual);
        }
    }

    /**
     * Asserts that two tracker points are equal.
     * All coordinates are scaled to 2 decimal places to simplify comparison.
     *
     * @param  message   error message if assertion fails.
     * @param  expected  expected value.
     * @param  actual    actual value.
     */
    public static void assertEquals(String message,
                                    TrackerPoint expected,
                                    TrackerPoint actual) {
        Assert.assertEquals(message + " - x values do not match",
                            getScaledValue(expected.getX()),
                            getScaledValue(actual.getX()));

        Assert.assertEquals(message + " - y values do not match",
                            getScaledValue(expected.getY()),
                            getScaledValue(actual.getY()));
    }

    /**
     * @param  captureTime  capture time.
     * @param  head         head coordinate.
     * @param  tailBearing  tail bearing.
     *
     * @return skeleton with the specified values.
     */
    public static LarvaSkeleton getSkeleton(long captureTime,
                                            TrackerPoint head,
                                            double tailBearing) {
        return getSkeleton(captureTime, 0, head, tailBearing);
    }

    /**
     * @param  captureTime  capture time.
     * @param  length       length.
     * @param  head         head coordinate.
     * @param  tailBearing  tail bearing.
     *
     * @return skeleton with the specified values.
     */
    public static LarvaSkeleton getSkeleton(long captureTime,
                                            double length,
                                            TrackerPoint head,
                                            double tailBearing) {
        return getSkeleton(captureTime, length, head, 0.0, tailBearing);
    }

    /**
     * @param  captureTime   capture time.
     * @param  copySkeleton  skeleton to copy.
     *
     * @return skeleton with the specified capture time and all other
     *         values from the copy skeleton.
     */
    public static LarvaSkeleton getSkeleton(long captureTime,
                                            LarvaSkeleton copySkeleton) {
        return new LarvaSkeleton(captureTime,
                                 copySkeleton.getHead(),
                                 copySkeleton.getMidpoint(),
                                 copySkeleton.getTail(),
                                 copySkeleton.getLength(),
                                 copySkeleton.getCentroid(),
                                 copySkeleton.getHeadToBodyAngle(),
                                 copySkeleton.getTailBearing());
    }

    /**
     * @param  captureTime  capture time.
     * @param  length       length.
     * @param  head         head coordinate.
     * @param  headAngle    head angle.
     * @param  tailBearing  tail bearing.
     *
     * @return skeleton with the specified values.
     */
    public static LarvaSkeleton getSkeleton(long captureTime,
                                            double length,
                                            TrackerPoint head,
                                            double headAngle,
                                            double tailBearing) {
        return new LarvaSkeleton(captureTime,
                                 head,
                                 DEFAULT_POINT,
                                 DEFAULT_POINT,
                                 length,
                                 DEFAULT_POINT,
                                 headAngle,
                                 tailBearing);
    }

    /**
     * @param  head  head coordinate.
     *
     * @return frame data containing skeleton with the specified
     *         head coordinate.
     */
    public static LarvaFrameData getFrameDataWithHead(TrackerPoint head) {
        return new LarvaFrameData(getSkeleton(0, head, 0.0));
    }

    /**
     * @param  captureTime   capture time.
     * @param  behaviorMode  behavior mode.
     *
     * @return frame data containing skeleton with the specified
     *         capture time.
     */
    public static LarvaFrameData getFrameDataWithTime(long captureTime,
                                                      LarvaBehaviorMode behaviorMode) {
        return new LarvaFrameData(getSkeleton(captureTime, DEFAULT_POINT, 0.0),
                                  behaviorMode);
    }

    /**
     * @param  tailBearing   tail bearing.
     *
     * @return frame data containing skeleton with the specified
     *         tail bearing.
     */
    public static LarvaFrameData getFrameDataWithTailBearing(double tailBearing) {
        return new LarvaFrameData(getSkeleton(0, DEFAULT_POINT, tailBearing));
    }

    public static void printMatrix(double[][] matrix) {
        System.out.println("\n");
        Formatter formatter = new Formatter(System.out);
        for (double[] row : matrix) {
            for (double v : row) {
                formatter.format("%10.2f", TestUtilities.getScaledValue(v));
            }
            System.out.println();
        }
        System.out.println("\n");
    }

    public static void validateJAXBMarshalling(String xml,
                                               Class clazz)
            throws Exception {

        JAXBContext ctx = JAXBContext.newInstance(clazz);
        Unmarshaller unm = ctx.createUnmarshaller();
        Object o = unm.unmarshal(new StringReader(xml));

        if (clazz.isInstance(o)) {
            Marshaller m = ctx.createMarshaller();
            m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            StringWriter writer = new StringWriter();
            m.marshal(o, writer);
            StringBuffer sb = writer.getBuffer();
            String marshallXml = sb.toString();
            Assert.assertEquals("input and output xml do not match",
                                xml, marshallXml);
        } else {
            Assert.fail("returned an object of type " +
                        o.getClass().getName() + " instead of type " +
                        clazz.getName());
        }

    }


}

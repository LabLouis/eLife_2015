/*
 * Copyright (c) 2011 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.venkman.jaxb;

import javax.xml.bind.annotation.adapters.XmlAdapter;
import java.util.regex.Pattern;

/**
 * Converts double arrays to and from a comma separated value string.
 *
 * @author Eric Trautman
 */
public class DoubleArrayAdapter extends XmlAdapter<String, double[]> {

    @Override
    public double[] unmarshal(String xmlString)
            throws Exception {
        double[] values;
        if (xmlString.length() == 0) {
            values = new double[0];
        } else {
            final String[] stringValues = CSV.split(xmlString);
            values = new double[stringValues.length];
            for (int i = 0; i < stringValues.length; i++) {
                values[i] = Double.parseDouble(stringValues[i]);
            }
        }
        return values;
    }

    @Override
    public String marshal(double[] values)
            throws Exception {
        StringBuilder xml = new StringBuilder(values.length * 5);
        if (values.length > 0) {
            xml.append(String.valueOf(values[0]));
            for (int i = 1; i < values.length; i++) {
                xml.append(',');
                xml.append(String.valueOf(values[i]));
            }
        }
        return xml.toString();
    }

    private static final Pattern CSV = Pattern.compile(",");
}

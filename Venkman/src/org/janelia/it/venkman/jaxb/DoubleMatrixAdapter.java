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
public class DoubleMatrixAdapter
        extends XmlAdapter<String, double[][]> {

    @Override
    public double[][] unmarshal(String xmlString)
            throws Exception {
        double[][] values;
        if (xmlString.length() == 0) {
            values = new double[0][0];
        } else {
            final String[] stringRows = PSV.split(xmlString);
            String[] stringCells = CSV.split(stringRows[0]);
            final int numColumns = stringCells.length;
            values = new double[stringRows.length][numColumns];

            for (int i = 0; i < stringRows.length; i++) {
                stringCells = CSV.split(stringRows[i]);
                for (int j = 0; j < numColumns; j++) {
                    values[i][j] = Double.parseDouble(stringCells[j]);
                }
            }
        }
        return values;
    }

    @Override
    public String marshal(double[][] values)
            throws Exception {
        String xml;
        if (values.length > 0) {
            StringBuilder sb =
                    new StringBuilder(values.length * values[0].length * 5);
            double[] row;
            for (int i = 0; i < values.length; i++) {
                if (i > 0) {
                    sb.append('|');
                }
                row = values[i];
                sb.append(String.valueOf(row[0]));
                for (int j = 1; j < row.length; j++) {
                    sb.append(',');
                    sb.append(String.valueOf(row[j]));
                }
            }
            xml = sb.toString();
        } else {
            xml = "";
        }
        return xml;
    }

    private static final Pattern CSV = Pattern.compile(",");
    private static final Pattern PSV = Pattern.compile("\\|");
}

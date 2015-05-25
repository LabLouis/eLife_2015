/*
 * Copyright (c) 2011 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.venkman.config.rules;

import org.apache.log4j.Logger;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.regex.Pattern;

/**
 * Parses values from an ASCII delimited matrix file
 * (likely generated from MatLab) into a simple two-dimensional array.
 *
 * @author Eric Trautman
 */
public class MatrixFile {

    private File file;

    private String delimiter;

    private double[][] values;

    private Integer numberOfColumns;

    private double minimumValue;

    private double maximumValue;

    /**
     * Parses the specified file using the default value delimiter expression
     * (white space and/or commas).
     *
     * @param  file       file to parse.
     *
     * @throws IllegalArgumentException
     *   if the specified file cannot be parsed or is not a valid matrix file.
     */
    public MatrixFile(File file)
            throws IllegalArgumentException {
        this(file, "[\\s,]+");
    }

    /**
     * Parses the specified file using the specified value delimiter expression.
     *
     * @param  file       file to parse.
     * @param  delimiter  the regular expression pattern {@link Pattern}
     *                    used the delimit values.
     *
     * @throws IllegalArgumentException
     *   if the specified file cannot be parsed or is not a valid matrix file.
     */
    public MatrixFile(File file,
                      String delimiter)
            throws IllegalArgumentException {

        this.file = file;
        this.delimiter = delimiter;
        this.minimumValue = Double.MAX_VALUE;
        this.maximumValue = -Double.MAX_VALUE;

        if (! file.exists())  {
            throw new IllegalArgumentException(
                    "The matrix file " + file.getAbsolutePath() +
                    " does not exist.");
        }

        if (! file.canRead()) {
            throw new IllegalArgumentException(
                    "You do not have permission to read the matrix file " +
                    file.getAbsolutePath() + ".");
        }

        BufferedReader in = null;
        try {

            String line;
            in = new BufferedReader(new FileReader(this.file));
            final Pattern elementDelimiter = Pattern.compile(this.delimiter);
            String elementStrings[];
            Integer columnCount = null;
            int rowNumber = 1;
            double value;
            double elementValues[];
            ArrayList<double[]> lineElements = new ArrayList<double[]>();

            while ((line = in.readLine()) != null) {

                elementStrings = elementDelimiter.split(line.trim());

                if (elementStrings.length > 0) {

                    if (columnCount == null) {
                        columnCount = elementStrings.length;
                    } else if (columnCount != elementStrings.length) {
                        throw new IllegalArgumentException(
                                "Row " + rowNumber +  " contains " +
                                elementStrings.length +
                                " elements while all previous rows contain " +
                                columnCount +
                                " elements for the matrix file " +
                                file.getAbsolutePath() + ".");
                    }

                    elementValues = new double[columnCount];
                    for (int i = 0; i < columnCount; i++) {
                        try {
                            value = Double.parseDouble(elementStrings[i]);
                        } catch (NumberFormatException e) {
                            throw new IllegalArgumentException(
                                    "Row " + rowNumber + " column " + (i+1) +
                                    " contains invalid value '" +
                                    elementStrings[i] +
                                    "' in the matrix file " +
                                    file.getAbsolutePath() + ".", e);
                        }
                        elementValues[i] = value;
                        if (value < this.minimumValue) {
                            this.minimumValue = value;
                        }
                        if (value > this.maximumValue) {
                            this.maximumValue = value;
                        }
                    }

                    lineElements.add(elementValues);
                    rowNumber++;

                }

            }

            if (lineElements.size() == 0) {
                throw new IllegalArgumentException(
                        "The matrix file " + file.getAbsolutePath() +
                        " does not contain any values.");
            }

            rowNumber = 0;
            this.values = new double[lineElements.size()][];
            for (double[] row : lineElements) {
                this.values[rowNumber] = row;
                rowNumber++;
            }

            this.numberOfColumns = columnCount;

        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "Failed to parse the matrix file " +
                    file.getAbsolutePath() + ".", e);
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    LOG.warn("Failed to close " + file.getAbsolutePath() +
                             " - ignoring exception.", e);
                }
            }

        }

    }

    /**
     * @return the source file for this matrix.
     */
    public File getFile() {
        return file;
    }

    /**
     * @return the delimiter expression used to parse the matrix file.
     */
    public String getDelimiter() {
        return delimiter;
    }

    /**
     * @return the values parsed from the matrix file.
     */
    public double[][] getValues() {
        return values;
    }

    /**
     * @return the number of parsed value rows.
     */
    public int getNumberOfRows() {
        return values.length;
    }

    /**
     * @return the number of parsed value columns.
     */
    public int getNumberOfColumns() {
        return numberOfColumns;
    }

    /**
     * @return the minimum parsed value.
     */
    public double getMinimumValue() {
        return minimumValue;
    }

    /**
     * @return the maximum parsed value.
     */
    public double getMaximumValue() {
        return maximumValue;
    }

    @Override
    public String toString() {
        return "MatrixFile{" +
               "file=" + getFile().getAbsolutePath() +
               ", delimiter='" + getDelimiter() +
               "', numberOfRows=" + getNumberOfRows() +
               ", numberOfColumns=" + getNumberOfColumns() +
               ", minimumValue=" + getMinimumValue() +
               ", maximumValue=" + getMaximumValue() +
               '}';
    }

    private static final Logger LOG = Logger.getLogger(MatrixFile.class);
}

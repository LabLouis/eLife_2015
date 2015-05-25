/*
 * Copyright (c) 2011 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.venkman.config.rules;

import org.janelia.it.venkman.TestUtilities;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;

/**
 * Tests the {@link MatrixFile} class.
 *
 * @author Eric Trautman
 */
public class MatrixFileTest {

    @Test
    public void testLoadFile()
            throws Exception {

        final File[] files = {
                new File("test/matlab-10x10-windows.txt"),
                new File("test/matlab-10x10-unix.txt")
        };

        final int size = 10;

        for (File asciiFile : files) {

            final MatrixFile matrixFile = new MatrixFile(asciiFile);
            final double[][] values = matrixFile.getValues();

            Assert.assertEquals("invalid number of rows for " + matrixFile,
                                size,
                                matrixFile.getNumberOfRows());

            Assert.assertEquals("invalid values array length for " + matrixFile,
                                size,
                                values.length);

            Assert.assertEquals("invalid number of columns for " + matrixFile,
                                size,
                                matrixFile.getNumberOfColumns());

            for (int i = 0; i < values.length; i++) {
                Assert.assertEquals("invalid row " + i + " values length for " +
                                    matrixFile,
                                    size,
                                    values[i].length);
            }

            TestUtilities.assertEquals("invalid minimum for " + matrixFile,
                                       0.0,
                                       matrixFile.getMinimumValue());

            TestUtilities.assertEquals("invalid maximum for " + matrixFile,
                                       100.0,
                                       matrixFile.getMaximumValue());
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testMissingFile() {
        new MatrixFile(new File("missing-file.txt"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testEmptyFile() {
        new MatrixFile(new File("test/matlab-empty.txt"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvalidValue() {
        new MatrixFile(new File("test/matlab-invalid-value.txt"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testVariedNumberOfColumns() {
        new MatrixFile(new File("test/matlab-varied-num-columns.txt"));
    }

}

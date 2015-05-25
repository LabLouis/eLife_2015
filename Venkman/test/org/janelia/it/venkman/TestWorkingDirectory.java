/*
 * Copyright (c) 2011 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.venkman;

import junit.framework.Assert;
import org.janelia.it.venkman.config.ConfigurationManager;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Utility to create a working directory for testing that can
 * be easily cleaned up when testing completes.
 *
 * @author Eric Trautman
 */
public class TestWorkingDirectory {

    private File directory;
    private ConfigurationManager manager;

    public TestWorkingDirectory() {
        final SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_hhmmss_SSS");
        final String testDirectoryName =
                sdf.format(new Date()) + "_test_config";
        directory = new File(testDirectoryName);
        final boolean createdNewDirectory = directory.mkdirs();
        Assert.assertTrue("test work directory already exists: " +
                          directory.getAbsolutePath(),
                          createdNewDirectory);
        manager = new ConfigurationManager(directory);
    }

    public ConfigurationManager getManager() {
        return manager;
    }

    public void delete() {
        ConfigurationManager.recursiveDelete(directory);
        manager = null;
    }
}

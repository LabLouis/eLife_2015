/*
 * Copyright (c) 2011 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.venkman.log;

import java.text.SimpleDateFormat;

/**
 * Utility objects for formatting dates in logs.
 *
 * @author Eric Trautman
 */
public class LogDateFormats {

    /**
     * Default format is: yyyy-MM-dd HH:mm:ss.SSS
     */
    public static final SimpleDateFormat DEFAULT =
            new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

    /**
     * File name format is: yyyyMMdd-HHmmssSSS
     */
    public static final SimpleDateFormat FILE_NAME =
            new SimpleDateFormat("yyyyMMdd-HHmmssSSS");

    /**
     * Time only format is: HH:mm:ss.SSS
     */
    public static final SimpleDateFormat TIME_ONLY =
            new SimpleDateFormat("HH:mm:ss.SSS");
}

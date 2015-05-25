/*
 * Copyright (c) 2012 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.venkman.log;

/**
 * Overrides file based logging so that test log messages are sent to
 * standard out.
 *
 * @author Eric Trautman
 */
public class StandardOutLogger extends Logger {

    @Override
    public synchronized void log(Object logObject)
            throws IllegalStateException {
        System.out.println("venkman-log: " + logObject);
    }

}

/*
 * Copyright (c) 2011 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.venkman.log;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.File;

/**
 * Utility for reading a log file and parse its contents into
 * the plugin object model. 
 *
 * @author Eric Trautman
 */
public class LogReader {

    private File logFile;
    private LogSession session;

    public LogReader(File logFile) {
        this.logFile = logFile;
        this.session = null;
    }

    public void read() throws JAXBException {
        JAXBContext context = JAXBContext.newInstance(LogSession.class);
        Unmarshaller unmarshaller = context.createUnmarshaller();
        Object o = unmarshaller.unmarshal(logFile);
        if (o instanceof LogSession) {
             session = (LogSession) o;
        }
    }

    public LogSession getSession() {
        return session;
    }
}

/*
 * Copyright (c) 2011 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.venkman.log;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlValue;

/**
 * Simple container for time stamped log messages.
 *
 * @author Eric Trautman
 */
@XmlRootElement
public class LogMessage {

    @XmlAttribute
    private String time;

    @XmlValue
    private String message;

    /**
     * No-arg constructor needed for JAXB.
     */
    @SuppressWarnings({"UnusedDeclaration"})
    private LogMessage() {
    }

    public LogMessage(String message) {
        this.time = LogDateFormats.DEFAULT.format(System.currentTimeMillis());
        this.message = message;
    }

    @Override
    public String toString() {
        return "LogMessage{" +
               "time='" + time + '\'' +
               ", message='" + message + '\'' +
               '}';
    }
}
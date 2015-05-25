/*
 * Copyright (c) 2014 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.venkman.rules;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * This class encapsulates a named data value associated with a
 * frame capture time.
 * It can be used to log additional rule information.
 *
 * @author Eric Trautman
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class RuleData {

    // function type names for rule data logging
    public static final String FORWARD_FUNCTION_NAME = "forward function";
    public static final String INTENSITY_FUNCTION_NAME = "intensity function";
    public static final String MULTIPLICATIVE_FUNCTION_NAME = "multiplicative function";
    public static final String SAMPLING_FUNCTION_NAME = "sampling function";

    // function instance names for rule data logging
    public static final String PRIMARY_NAME = "primary";
    public static final String ALTERNATE_NAME = "alternate";

    @XmlAttribute
    private String value;

    @XmlAttribute
    private String name;

    @XmlAttribute
    private long captureTime;

    /**
     * No-arg constructor required for JAXB.
     */
    @SuppressWarnings("UnusedDeclaration")
    private RuleData() {
    }

    public RuleData(long captureTime,
                    String name,
                    String value) {
        this.captureTime = captureTime;
        this.name = name;
        this.value = value;
    }

    public long getCaptureTime() {
        return captureTime;
    }

    public String getName() {
        return name;
    }

    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return "RuleData{captureTime=" + captureTime +
               ", name='" + name +
               "', value='" + value +
               "'}";
    }
}

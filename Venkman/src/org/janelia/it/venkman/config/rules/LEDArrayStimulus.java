/*
 * Copyright (c) 2014 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.venkman.config.rules;

import org.janelia.it.venkman.gui.parameter.annotation.VenkmanParameter;
import org.janelia.it.venkman.message.Message;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.List;

/**
 * The attributes for an LED Array stimulus.
 *
 * @author Eric Trautman
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class LEDArrayStimulus implements Stimulus {

    @XmlAttribute
    private double x;

    @XmlAttribute
    private double y;

    @XmlAttribute
    private double width;

    @XmlAttribute
    private double height;

    @VenkmanParameter(displayName = "LED Intensity Data")
    @XmlElementRef
    private List<LEDStimulus> stimulusList;

    // no-arg constructor needed for JAXB
    @SuppressWarnings({"UnusedDeclaration"})
    private LEDArrayStimulus() {
    }

    public LEDArrayStimulus(double x,
                            double y,
                            double width,
                            double height,
                            List<LEDStimulus> stimulusList) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.stimulusList = stimulusList;
    }

    @Override
    public void addFieldsToMessage(Message message) {
        final StringBuilder sb = new StringBuilder(128);

        sb.append(x);
        sb.append(STIMULUS_DATA_SEPARATOR);

        sb.append(y);
        sb.append(STIMULUS_DATA_SEPARATOR);

        sb.append(width);
        sb.append(STIMULUS_DATA_SEPARATOR);

        sb.append(height);

        for (LEDStimulus stimulus : stimulusList) {
            sb.append(STIMULUS_DATA_SEPARATOR);
            sb.append(String.valueOf(stimulus.getIntensityPercentage()));
            sb.append(STIMULUS_DATA_SEPARATOR);
            sb.append(String.valueOf(stimulus.getDuration()));
        }

        message.addField(sb.toString());
    }

    public List<LEDArrayStimulus> toList() {
        final List<LEDArrayStimulus> list = new ArrayList<LEDArrayStimulus>();
        list.add(this);
        return list;
    }

    public LEDStimulus getLedStimulus(int index) {
        LEDStimulus ledStimulus = null;
        if (stimulusList.size() > index) {
            ledStimulus = stimulusList.get(index);
        }
        return ledStimulus;
    }

    @Override
    public String toString() {
        return "LEDArrayStimulus{" +
               "x=" + x +
               ", y=" + y +
               ", width=" + width +
               ", height=" + height +
               ", stimulusList=" + stimulusList +
               '}';
    }

    private static final char STIMULUS_DATA_SEPARATOR = '|';
}
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
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * The attributes for an LED stimulus pulse.
 *
 * @author Eric Trautman
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class LEDStimulus implements Stimulus {

    @VenkmanParameter(displayName = "LED Intensity Percentage",
                      minimum = "0.0",
                      maximum = "100.0")
    @XmlElement
    private double intensityPercentage;

    @VenkmanParameter(displayName = "LED Duration",
                      minimum = "0",
                      maximum = "5000")
    @XmlElement
    private long duration;

     // no-arg constructor needed for JAXB
    @SuppressWarnings({"UnusedDeclaration"})
    private LEDStimulus() {
    }

    public LEDStimulus(double intensityPercentage,
                       long duration) {

        setIntensityPercentage(intensityPercentage);
        if (duration < 0) {
            this.duration = 0;
        } else {
            this.duration = duration;
        }
    }

    public long getDuration() {
        return duration;
    }

    public double getIntensityPercentage() {
        return intensityPercentage;
    }

    public void scaleIntensityPercentage(double factor) {
        setIntensityPercentage(intensityPercentage * factor);
    }

    public void scaleIntensityPercentageWithFloor(double factor,
                                                  double minimum) {
        final double scaledValue = intensityPercentage * factor;
        if (scaledValue < minimum) {
            setIntensityPercentage(minimum);
        } else {
            setIntensityPercentage(scaledValue);
        }
    }

    public void addIntensityPercentage(double addend) {
        setIntensityPercentage(intensityPercentage + addend);
    }

    public List<LEDStimulus> toList() {
        List<LEDStimulus> list = new ArrayList<LEDStimulus>();
        list.add(this);
        return list;
    }

    @Override
    public void addFieldsToMessage(Message message) {
        message.addField(String.valueOf(intensityPercentage));
        message.addField(String.valueOf(duration));
    }

    // excludes time and rounds percentages to 4 decimals
    // (this is slow and should only be used for testing)
    @Override
    public boolean equals(Object o) {
        boolean isEqual = false;
        if (this == o) {
            isEqual = true;
        } else if (o instanceof LEDStimulus) {
            final LEDStimulus that = (LEDStimulus) o;
            isEqual = ((duration == that.duration) &&
                       isEssentiallyEqual(that.intensityPercentage,
                                          intensityPercentage));
        }
        return isEqual;
    }

    // auto generated (excludes time)
    @Override
    public int hashCode() {
        long temp = intensityPercentage != +0.0d ?
                    Double.doubleToLongBits(intensityPercentage) : 0L;
        int result = (int) (temp ^ (temp >>> 32));
        result = 31 * result + (int) (duration ^ (duration >>> 32));
        return result;
    }

    @Override
    public String toString() {
        return "LEDStimulus{" +
               "intensityPercentage=" + intensityPercentage +
               ", duration=" + duration +
               '}';
    }

    private void setIntensityPercentage(double intensityPercentage) {
        if (intensityPercentage > 100.0) {
            this.intensityPercentage = 100.0;
        } else if (intensityPercentage < 0.0) {
            this.intensityPercentage = 0.0;
        } else {
            this.intensityPercentage = intensityPercentage;
        }
    }

    private boolean isEssentiallyEqual(double l,
                                       double r) {
        BigDecimal lbd = new BigDecimal(l).setScale(4,
                                                    BigDecimal.ROUND_HALF_UP);
        BigDecimal rbd = new BigDecimal(r).setScale(4,
                                                    BigDecimal.ROUND_HALF_UP);
        return lbd.equals(rbd);
    }

}
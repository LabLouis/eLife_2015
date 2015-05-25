/*
 * Copyright (c) 2013 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.venkman.config.rules;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlValue;
import java.util.ArrayList;
import java.util.List;

/**
 * A sequence of on and off millisecond flash durations.
 *
 * @author Eric Trautman
 */
@XmlAccessorType(XmlAccessType.FIELD)
public class LEDFlashPattern {

    /**
     * The default LED activation duration
     * (greater than frame rate to reduce chance of flicker).
     */
    public static final long DEFAULT_LED_ACTIVATION_DURATION = 60;

    @XmlValue
    private String flashPattern;

    @XmlTransient
    private int stimulusListSize;

    @XmlTransient
    private int[] onOffDurations;

    public LEDFlashPattern() {
        this(String.valueOf(DEFAULT_LED_ACTIVATION_DURATION));
    }

    /**
     * Constructs a value with the specified flash pattern.
     *
     * @param  flashPattern  comma separated list of on and off
     *                       millisecond times.
     */
    public LEDFlashPattern(String flashPattern) throws IllegalArgumentException {
        this.flashPattern = flashPattern;
        rebuildStimulusLists();
    }

    public String getFlashPattern() {
        return flashPattern;
    }

    @Override
    public boolean equals(Object o) {
        boolean isEqual = false;
        if (this == o) {
            isEqual = true;
        } else if (o instanceof LEDFlashPattern) {
            final LEDFlashPattern that = (LEDFlashPattern) o;
            if (flashPattern == null) {
                isEqual = (that.flashPattern == null);
            } else {
                isEqual = flashPattern.equals(that.flashPattern);
            }
        }
        return isEqual;
    }

    @Override
    public int hashCode() {
        return flashPattern != null ? flashPattern.hashCode() : 0;
    }

    public List<LEDStimulus> getStimulusList(double intensityPercentage) {
        List<LEDStimulus> list = new ArrayList<LEDStimulus>(stimulusListSize);
        for (int i = 0; i < stimulusListSize; i++) {
            if ((i % 2) == 0) {
                list.add(new LEDStimulus(intensityPercentage,
                                         onOffDurations[i]));
            } else {
                list.add(new LEDStimulus(0,
                                         onOffDurations[i]));
            }
        }
        return list;
    }

    public void rebuildStimulusLists() throws IllegalArgumentException {

        if ((flashPattern == null) || (flashPattern.length() == 0)) {
            throw new IllegalArgumentException(
                    "A flash pattern must be specified.");
        }

        StringBuilder trimmedPattern = new StringBuilder();
        ArrayList<Integer> onOffList = new ArrayList<Integer>();

        final String[] tokens = flashPattern.split(",");
        Integer duration;
        try {
            for (String token : tokens) {
                duration = Integer.valueOf(token);
                if ((duration < 0) || (duration > 1000)) {
                    throw new IllegalArgumentException(
                            "An invalid flash pattern '" + flashPattern +
                            "' was specified.  All millisecond durations " +
                            "must be between 0 and 1000.");
                }
                onOffList.add(duration);
                trimmedPattern.append(token);
                trimmedPattern.append(',');
            }

        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                    "An invalid flash pattern '" + flashPattern +
                    "' was specified.  The value should be a comma " +
                    "separated list of on and off millisecond duration " +
                    "values between 1 and 1000 (e.g. '5,3,5,3').");
        }

        // remove last comma
        trimmedPattern.setLength(trimmedPattern.length() - 1);
        flashPattern = trimmedPattern.toString();
        stimulusListSize = onOffList.size();
        onOffDurations = new int[stimulusListSize];
        for (int i = 0; i < stimulusListSize; i++) {
            onOffDurations[i] = onOffList.get(i);
        }
    }

}

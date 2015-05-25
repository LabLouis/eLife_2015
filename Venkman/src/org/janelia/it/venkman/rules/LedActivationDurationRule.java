/*
 * Copyright (c) 2014 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.venkman.rules;

import org.janelia.it.venkman.config.LarvaBehaviorParameters;
import org.janelia.it.venkman.config.rules.LEDFlashPattern;
import org.janelia.it.venkman.config.rules.LEDStimulus;
import org.janelia.it.venkman.gui.parameter.annotation.VenkmanParameter;
import org.janelia.it.venkman.log.Logger;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlTransient;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Base class for LED stimulation rules that defines the duration of the
 * LED activation.  Uninterrupted stimulation can be implemented by setting
 * the duration to be longer than the expected tracker frame rate.
 *
 * @author Eric Trautman
 */
@XmlAccessorType(XmlAccessType.FIELD)
public abstract class LedActivationDurationRule
        implements LarvaStimulusRules {

    /** The default LED activation duration flash pattern. */
    public static final LEDFlashPattern DEFAULT_LED_ACTIVATION_DURATION =
            new LEDFlashPattern();

    /** Ensures LED will be turned off for the current frame. */
    public static final List<LEDStimulus> ZERO_INTENSITY_FOR_ONE_SECOND =
            Collections.unmodifiableList(new LEDStimulus(0, 1000).toList());

    /**
     * Ensures LED will continue with previous intensity
     * (if previous duration extends long enough).
     */
    public static final List<LEDStimulus> IGNORE_INTENSITY_FOR_FRAME =
            Collections.unmodifiableList(new ArrayList<LEDStimulus>(0));


    @VenkmanParameter(displayName = "LED Flash Pattern (ms-on,ms-off,ms-on,...)")
    @XmlElement
    private LEDFlashPattern ledActivationDuration;

    @XmlTransient
    private Logger logger;

     // no-arg constructor needed for JAXB
    @SuppressWarnings({"UnusedDeclaration"})
    private LedActivationDurationRule() {
        this(DEFAULT_LED_ACTIVATION_DURATION);
    }

    public LedActivationDurationRule(LEDFlashPattern ledActivationDuration) {
        this.ledActivationDuration = ledActivationDuration;
    }

    @Override
    public boolean supportsVersion(String version) {
        return "1".equals(version);
    }

    public LEDFlashPattern getLedActivationDuration() {
        return ledActivationDuration;
    }

    public void logRuleData(long captureTime,
                            String name,
                            String value) {
        logger.log(new RuleData(captureTime, name, value));
    }

    public List<LEDStimulus> getStimulusList(double intensityPercentage) {
        return ledActivationDuration.getStimulusList(intensityPercentage);
    }

    @Override
    public void init(Logger logger) {
        this.logger = logger;
        this.ledActivationDuration.rebuildStimulusLists();
    }

    /**
     * @param  behaviorParameters  the configured behavior parameters.
     *
     * @return the specified parameters unmodified.
     */
    @Override
    public LarvaBehaviorParameters overrideBehaviorParameters(LarvaBehaviorParameters behaviorParameters) {
        return behaviorParameters;
    }

}

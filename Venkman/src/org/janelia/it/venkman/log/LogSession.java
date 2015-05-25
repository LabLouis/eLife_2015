/*
 * Copyright (c) 2014 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.venkman.log;

import org.janelia.it.venkman.config.Configuration;
import org.janelia.it.venkman.config.LarvaBehaviorParameters;
import org.janelia.it.venkman.config.rules.LEDStimulus;
import org.janelia.it.venkman.data.LarvaFrameData;
import org.janelia.it.venkman.rules.DefinedEnvironment;
import org.janelia.it.venkman.rules.DefinedEnvironmentBasedUponOrientation;
import org.janelia.it.venkman.rules.DefinedEnvironmentForMaximumLengthWithAdditiveFunction;
import org.janelia.it.venkman.rules.ImportedStimulus;
import org.janelia.it.venkman.rules.LarvaStimulusRules;
import org.janelia.it.venkman.rules.RuleData;
import org.janelia.it.venkman.rules.ScaledRunIntensity;
import org.janelia.it.venkman.rules.ScaledRunIntensityWithRandomDelay;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.XmlElementRefs;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.List;

/**
 * Container for session log data to be used for analysis.
 *
 * @author Eric Trautman
 */
@XmlRootElement
public class LogSession {

    @XmlElement
    private Configuration configuration;

    @XmlElement
    private LarvaBehaviorParameters larvaBehaviorParameters;

    // TODO: find a way to build this list from StimulusRuleImplementations
    @XmlElementRefs({

            @XmlElementRef(type = DefinedEnvironment.class),
            @XmlElementRef(type = DefinedEnvironmentBasedUponOrientation.class),
            @XmlElementRef(type = DefinedEnvironmentForMaximumLengthWithAdditiveFunction.class),
            @XmlElementRef(type = ImportedStimulus.class),
            @XmlElementRef(type = ScaledRunIntensity.class),
            @XmlElementRef(type = ScaledRunIntensityWithRandomDelay.class),
                    })
    private LarvaStimulusRules larvaStimulusRules;
    
    @XmlElement
    private List<LarvaFrameData> larvaFrameData;

    @XmlElement
    private List<LEDStimulus> ledStimulus;

    @XmlElement
    private List<RuleData> ruleData;

    public LogSession() {
        this.configuration = null;
        this.larvaBehaviorParameters = null;
        this.larvaStimulusRules = null;
        this.larvaFrameData = new ArrayList<LarvaFrameData>();
        this.ledStimulus = new ArrayList<LEDStimulus>();
        this.ruleData = new ArrayList<RuleData>();
    }

    public Configuration getConfiguration() {
        return configuration;
    }

    public LarvaBehaviorParameters getLarvaBehaviorParameters() {
        return larvaBehaviorParameters;
    }

    public LarvaStimulusRules getLarvaStimulusRules() {
        return larvaStimulusRules;
    }

    public List<LarvaFrameData> getFrameDataList() {
        return larvaFrameData;
    }

    public List<LEDStimulus> getStimulusList() {
        return ledStimulus;
    }

    public List<RuleData> getRuleDataList() {
        return ruleData;
    }
}

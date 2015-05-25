/*
 * Copyright (c) 2014 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.venkman.rules;

import org.janelia.it.venkman.config.LarvaBehaviorParameters;
import org.janelia.it.venkman.config.rules.LEDStimulus;
import org.janelia.it.venkman.config.rules.Stimulus;
import org.janelia.it.venkman.data.LarvaFrameData;
import org.janelia.it.venkman.log.Logger;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import java.util.ArrayList;
import java.util.List;

/**
 * Simply spits out stimulus imported from another file.
 *
 * For earlier (pre March 2013) runs, this assumes only one stimulus was
 * issued per frame and does not support cases where a sequence of multiple
 * stimuli were issued for a single frame.
 *
 * NOTE: To support JAXB translation, each rule implementation class
 *       must be added to {@link org.janelia.it.venkman.rules.StimulusRuleImplementations}.
 *
 * @author Eric Trautman
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class ImportedStimulus implements LarvaStimulusRules {

    @SuppressWarnings({"FieldCanBeLocal", "UnusedDeclaration"})
    @XmlElement
    private String logFilePath;

    /**
     * Frame data elements contain stimulus lists for runs after March 2013.
     */
    @XmlTransient
    private List<LarvaFrameData> importedFrameData;

    /**
     * Stimulus elements were logged independently from frames in early runs.
     */
    @XmlTransient
    private List<LEDStimulus> importedLedStimulus;

     // no-arg constructor needed for JAXB and EditStimulusDialog
    @SuppressWarnings({"UnusedDeclaration"})
    public ImportedStimulus() {
        this(null, null, null);
    }

    public ImportedStimulus(String logFilePath,
                            List<LarvaFrameData> importedFrameData,
                            List<LEDStimulus> importedLedStimulus) {
        this.logFilePath = logFilePath;

        this.importedFrameData = importedFrameData;

        if (importedLedStimulus == null) {
            this.importedLedStimulus = new ArrayList<LEDStimulus>();
        } else {
            this.importedLedStimulus = importedLedStimulus;
        }
    }

    @Override
    public String getCode() {
        return "import";
    }

    @Override
    public String getDescription() {
        return "Explicit stimulus imported from a prior run.";
    }

    @Override
    public boolean supportsVersion(String version) {
        return true ;
    }

    @Override
    public void init(Logger logger) {
    }

    @Override
    public LarvaBehaviorParameters overrideBehaviorParameters(LarvaBehaviorParameters behaviorParameters) {
        return behaviorParameters;
    }

    @Override
    public List<? extends Stimulus> determineStimulus(List<LarvaFrameData> frameHistory,
                                                      LarvaBehaviorParameters behaviorParameters) {

        List<? extends Stimulus> frameStimulusList = null;

        final int frameIndex = frameHistory.size() - 1;
        if (frameIndex > -1) {
            if (frameIndex < importedLedStimulus.size()) {
                frameStimulusList = importedLedStimulus.get(frameIndex).toList();
            } else if (frameIndex < importedFrameData.size()) {
                LarvaFrameData frameData = importedFrameData.get(frameIndex);
                frameStimulusList = frameData.getStimulusList();
            }
        }

        return frameStimulusList;
    }
}

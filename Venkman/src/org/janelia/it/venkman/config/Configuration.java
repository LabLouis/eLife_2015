/*
 * Copyright (c) 2011 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.venkman.config;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

/**
 * A configuration identifies a specific set behavior parameters to be
 * used in conjunction with a specific set of configured stimulus rules.
 *
 * @author Eric Trautman
 */
@XmlRootElement
@XmlType(propOrder={"id", "behaviorParametersId", "stimulusParametersId"})
public class Configuration {

    private ParameterCollectionId id;
    private ParameterCollectionId behaviorParametersId;
    private ParameterCollectionId stimulusParametersId;

    @SuppressWarnings({"UnusedDeclaration"})
    private Configuration() {
    }

    public Configuration(ParameterCollectionId id,
                         ParameterCollectionId behaviorParametersId,
                         ParameterCollectionId stimulusParametersId) {
        this.id = id;
        this.behaviorParametersId = behaviorParametersId;
        this.stimulusParametersId = stimulusParametersId;
    }

    @XmlElement
    public ParameterCollectionId getId() {
        return id;
    }

    @SuppressWarnings({"UnusedDeclaration"})
    public void setId(ParameterCollectionId id) {
        this.id = id;
    }

    @XmlElement
    public ParameterCollectionId getBehaviorParametersId() {
        return behaviorParametersId;
    }

    @SuppressWarnings({"UnusedDeclaration"})
    public void setBehaviorParametersId(ParameterCollectionId behaviorParametersId) {
        this.behaviorParametersId = behaviorParametersId;
    }

    @XmlElement
    public ParameterCollectionId getStimulusParametersId() {
        return stimulusParametersId;
    }

    @SuppressWarnings({"UnusedDeclaration"})
    public void setStimulusParametersId(ParameterCollectionId stimulusParametersId) {
        this.stimulusParametersId = stimulusParametersId;
    }
}

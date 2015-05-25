/*
 * Copyright (c) 2014 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.venkman.rules;

import org.janelia.it.venkman.config.LarvaBehaviorParameters;
import org.janelia.it.venkman.config.rules.Stimulus;
import org.janelia.it.venkman.data.LarvaFrameData;
import org.janelia.it.venkman.log.Logger;

import java.util.List;

/**
 * Interface for all stimulus rules implementations.
 *
 * @author Eric Trautman
 */
public interface LarvaStimulusRules {

    /**
     * @return the code for this rule.
     */
    public String getCode();

    /**
     * @return the description for this rule.
     */
    public String getDescription();

    /**
     * @param  version  rules version to check.
     *
     * @return true if this rule supports the specified version; otherwise false.
     */
    public boolean supportsVersion(String version);

    /**
     * Initialize any state for this rule.
     *
     * @param  logger  the logger for the current session.
     */
    public void init(Logger logger);

    /**
     * Allows this rule to override the configured behavior parameters
     * (e.g. to handle backwards compatibility for rule parameters that get "promoted" to behavior parameters).
     *
     * @param  behaviorParameters  the configured behavior parameters.
     *
     * @return the specified behavior parameters with modifications if necessary.
     */
    public LarvaBehaviorParameters overrideBehaviorParameters(LarvaBehaviorParameters behaviorParameters);

    /**
     * Determine the appropriate stimulus for the specified frame data and parameters.
     *
     * @param  frameHistory        history of all tracker frames.
     * @param  behaviorParameters  behavior parameters for the current session.
     *
     * @return list of stimuli.
     */
    public List<? extends Stimulus> determineStimulus(List<LarvaFrameData> frameHistory,
                                                      LarvaBehaviorParameters behaviorParameters);

}

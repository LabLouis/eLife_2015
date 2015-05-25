/*
 * Copyright (c) 2012 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.venkman.gui.log;

import org.janelia.it.venkman.config.rules.LEDStimulus;

import java.util.ArrayList;
import java.util.List;

/**
 * {@link HistogramPanel} for LED stimulus.
 *
 * @author Eric Trautman
 */
public class LEDStimulusHistogramPanel
        extends HistogramPanel {

    @Override
    public List<Double> getThresholdValues() {
        return new ArrayList<Double>();
    }

    @Override
    public double getValue(int index) {
        final LogModel logModel = getLogModel();
        final LEDStimulus stimulus = logModel.getStimulus(index);
        if (stimulus == null) {
            return 0;
        } else {
            return stimulus.getIntensityPercentage();
        }
    }

}

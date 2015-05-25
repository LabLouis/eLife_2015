/*
 * Copyright (c) 2012 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.venkman.gui.log;

import org.janelia.it.venkman.config.LarvaBehaviorParameters;
import org.janelia.it.venkman.data.LarvaFrameData;

import java.util.ArrayList;
import java.util.List;

/**
 * {@link HistogramPanel} for smoothedHeadAngleSpeed.
 *
 * @author Eric Trautman
 */
public class SmoothedHeadAngleSpeedHistogramPanel
        extends HistogramPanel {

    @Override
    public List<Double> getThresholdValues() {
        List<Double> thresholdValues = new ArrayList<Double>();
        final LogModel logModel = getLogModel();
        final LarvaBehaviorParameters parameters =
                logModel.getCurrentParameters();
        if (parameters != null) {
            thresholdValues.add(
                    parameters.getMinHeadAngleSpeedToContinueCasting());
        }
        return thresholdValues;
    }

    @Override
    public double getValue(int index) {
        final LogModel logModel = getLogModel();
        final LarvaFrameData data = logModel.getFrameData(index);
        return Math.abs(data.getSmoothedHeadAngleSpeed());
    }

}

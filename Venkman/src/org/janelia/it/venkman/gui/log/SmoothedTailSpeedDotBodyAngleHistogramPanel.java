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
 * {@link HistogramPanel} for smoothedTailSpeedDotBodyAngle.
 *
 * @author Eric Trautman
 */
public class SmoothedTailSpeedDotBodyAngleHistogramPanel
        extends HistogramPanel {

    @Override
    public List<Double> getThresholdValues() {
        List<Double> thresholdValues = new ArrayList<Double>();
        final LogModel logModel = getLogModel();
        final LarvaBehaviorParameters parameters =
                logModel.getCurrentParameters();
        if (parameters != null) {
            final double t =
                    parameters.getDotProductThresholdForStraightModes();
            thresholdValues.add(t);
            thresholdValues.add(-t);
            thresholdValues.add(0.0);
        }
        return thresholdValues;
    }

    @Override
    public double getValue(int index) {
        final LogModel logModel = getLogModel();
        final LarvaFrameData data = logModel.getFrameData(index);
        return data.getSmoothedTailSpeedDotBodyAngle();
    }

}

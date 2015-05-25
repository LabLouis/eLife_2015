/*
 * Copyright (c) 2012 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.venkman.gui.log;

import javax.swing.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

/**
 * This panel displays historical graphs for data elements used to
 * derive behavior mode.  It is intended to assist in tuning behavior
 * mode configuration parameters and thresholds.
 *
 * @author Eric Trautman
 */
public class HistoryPanel {

    private JPanel contentPanel;

    @SuppressWarnings("UnusedDeclaration")
    private BehaviorModePanel behaviorModePanel;
    @SuppressWarnings("UnusedDeclaration")
    private JPanel behaviorModeControlPanel;
    @SuppressWarnings("FieldCanBeLocal")
    private BehaviorModeControlPanel behaviorModeControls;

    @SuppressWarnings("UnusedDeclaration")
    private HeadAngleHistogramPanel headAnglePanel;
    @SuppressWarnings("UnusedDeclaration")
    private SmoothedBodyAngleSpeedHistogramPanel bodyAngleSpeedPanel;
    @SuppressWarnings("UnusedDeclaration")
    private SmoothedHeadAngleSpeedHistogramPanel headAngleSpeedPanel;
    @SuppressWarnings("UnusedDeclaration")
    private SmoothedTailSpeedDotBodyAngleHistogramPanel dotProductPanel;
    @SuppressWarnings("UnusedDeclaration")
    private JPanel dataPanel;

    public HistoryPanel(LogModel logModel) {
        headAnglePanel.setLogModel(logModel);
        headAngleSpeedPanel.setLogModel(logModel);
        bodyAngleSpeedPanel.setLogModel(logModel);
        dotProductPanel.setLogModel(logModel);

        BehaviorModeComparisonModel model =
                new BehaviorModeComparisonModel(logModel);
        behaviorModePanel.setModel(model);
        behaviorModeControls.setModel(model);

        final PropertyChangeListener repaintBehaviorModePanel =
                new PropertyChangeListener() {
                    @Override
                    public void propertyChange(PropertyChangeEvent evt) {
                        behaviorModePanel.repaint();
                    }
                };

        model.addPropertyChangeListener(
                BehaviorModeComparisonModel.LOG_MODELS_PROPERTY,
                repaintBehaviorModePanel);

        model.addPropertyChangeListener(
                BehaviorModeComparisonModel.FILTER_PROPERTY,
                repaintBehaviorModePanel);
    }

    private void createUIComponents() {
        behaviorModePanel = new BehaviorModePanel();
        behaviorModeControls = new BehaviorModeControlPanel();
        behaviorModeControlPanel = behaviorModeControls.getControlPanel();
        headAnglePanel = new HeadAngleHistogramPanel();
        headAngleSpeedPanel = new SmoothedHeadAngleSpeedHistogramPanel();
        bodyAngleSpeedPanel = new SmoothedBodyAngleSpeedHistogramPanel();
        dotProductPanel = new SmoothedTailSpeedDotBodyAngleHistogramPanel();
    }

    public JPanel getContentPanel() {
        return contentPanel;
    }
}
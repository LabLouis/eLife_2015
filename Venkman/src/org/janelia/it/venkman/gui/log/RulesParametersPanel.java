/*
 * Copyright (c) 2012 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.venkman.gui.log;

import org.janelia.it.venkman.config.LarvaBehaviorParameters;
import org.janelia.it.venkman.gui.parameter.ParameterGroup;
import org.janelia.it.venkman.rules.LarvaStimulusRules;

import javax.swing.*;
import java.awt.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

/**
 * This panel displays the configured parameters for the rules.
 *
 * @author Eric Trautman
 */
public class RulesParametersPanel {
    private JPanel contentPanel;
    private JPanel behaviorPanel;
    private JPanel stimulusPanel;

    private LogModel logModel;

    public RulesParametersPanel(LogModel logModel) {
        this.logModel = logModel;
        this.logModel.addPropertyChangeListener(
                LogModel.LOG_FILE_PROPERTY,
                new PropertyChangeListener() {
                    @Override
                    public void propertyChange(PropertyChangeEvent evt) {
                        updateLogFile();
                    }
                });

        updateLogFile();
    }

    public JPanel getContentPanel() {
        return contentPanel;
    }

    private void updateLogFile() {

        behaviorPanel.removeAll();

        final LarvaBehaviorParameters currentParameters =
                logModel.getCurrentParameters();

        if (currentParameters != null) {
            ParameterGroup<LarvaBehaviorParameters> behaviorParameterGroup =
                    new ParameterGroup<LarvaBehaviorParameters>(currentParameters);

            behaviorPanel.add(behaviorParameterGroup.getContentPanel(false),
                              BorderLayout.WEST);
        } else {
            behaviorPanel.add(getNoParametersDefinedLabel(),
                              BorderLayout.WEST);
        }

        stimulusPanel.removeAll();

        final LarvaStimulusRules currentRules =
                logModel.getCurrentRules();

        if (currentRules != null) {
            stimulusPanel.add(getRulesTitleLabel(currentRules),
                              BorderLayout.NORTH);
            ParameterGroup<LarvaStimulusRules> stimulusParameterGroup =
                    new ParameterGroup<LarvaStimulusRules>(currentRules);
            stimulusPanel.add(stimulusParameterGroup.getContentPanel(false),
                              BorderLayout.WEST);
        } else {
            stimulusPanel.add(getNoParametersDefinedLabel(),
                              BorderLayout.WEST);
        }
    }

    private JLabel getNoParametersDefinedLabel() {
        JLabel label = new JLabel("No parameters defined.");
        label.setEnabled(false);
        return label;
    }

    private JLabel getRulesTitleLabel(LarvaStimulusRules rules) {
        return new JLabel(
                "<html><b><font color='blue'>Rules Group " + rules.getCode() +
                ": " + rules.getDescription() + "</font></b><br><br></html>");

    }
}

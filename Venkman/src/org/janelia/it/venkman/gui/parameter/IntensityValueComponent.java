/*
 * Copyright (c) 2011 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.venkman.gui.parameter;

import javax.swing.*;
import java.awt.event.ActionEvent;

/**
 * User interface component for intensity value parameters.
 *
 * @author Eric Trautman
 */
public class IntensityValueComponent {
    private JTextField intensityValueField;
    private JPanel contentPanel;
    private JCheckBox addNoiseCheckBox;
    private JTextField meanTextField;
    private JTextField standardDeviationTextField;
    private JLabel meanLabel;
    private JLabel standardDeviationLabel;

    private DecimalParameter percentage;
    private boolean isNoiseEnabled;
    private DecimalParameter noiseMean;
    private DecimalParameter noiseStandardDeviation;

    public IntensityValueComponent(DecimalParameter percentage,
                                   boolean isNoiseEnabled,
                                   DecimalParameter noiseMean,
                                   DecimalParameter noiseStandardDeviation) {
        this.percentage = percentage;
        this.isNoiseEnabled = isNoiseEnabled;
        this.noiseMean = noiseMean;
        this.noiseStandardDeviation = noiseStandardDeviation;

        this.addNoiseCheckBox.setSelected(isNoiseEnabled);
        this.addNoiseCheckBox.addActionListener(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                setEnabledForNoiseComponents(addNoiseCheckBox.isSelected());
            }
        });

        setEnabledForNoiseComponents(isNoiseEnabled);
    }

    public JPanel getContentPanel() {
        return contentPanel;
    }

    public String getIntensityText() {
        return intensityValueField.getText();
    }

    public boolean isNoiseEnabled() {
        return addNoiseCheckBox.isSelected();
    }

    public String getNoiseMeanText() {
        return meanTextField.getText();
    }

    public String getNoiseStandardDeviationText() {
        return standardDeviationTextField.getText();
    }

    private void setEnabledForNoiseComponents(boolean isEnabled) {
        meanLabel.setEnabled(isEnabled);
        meanTextField.setEnabled(isEnabled);
        standardDeviationLabel.setEnabled(isEnabled);
        standardDeviationTextField.setEnabled(isEnabled);
    }

    private void createUIComponents() {
        intensityValueField = (JTextField)
                percentage.getComponent();
        meanTextField = (JTextField)
                noiseMean.getComponent();
        standardDeviationTextField = (JTextField)
                noiseStandardDeviation.getComponent();
    }
}

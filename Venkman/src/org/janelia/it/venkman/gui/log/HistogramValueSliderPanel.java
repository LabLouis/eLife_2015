/*
 * Copyright (c) 2012 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.venkman.gui.log;

import javax.swing.*;
import javax.swing.event.ChangeListener;

/**
 * TODO: add javadoc
 *
 * @author Eric Trautman
 */
public class HistogramValueSliderPanel {

    private static final int MAX_VALUE = 1000;

    private JPanel contentPanel;
    private JSlider maxSlider;
    private JSlider minSlider;

    public HistogramValueSliderPanel() {
        maxSlider.setMaximum(MAX_VALUE);
        minSlider.setMaximum(MAX_VALUE);

        maxSlider.setValue(MAX_VALUE);
        minSlider.setValue(0);

        setEnabled(false);
    }

    public JPanel getContentPanel() {
        return contentPanel;
    }

    public void addMaxChangeListener(ChangeListener listener) {
        maxSlider.addChangeListener(listener);
    }

    public void addMinChangeListener(ChangeListener listener) {
        minSlider.addChangeListener(listener);
    }

    public double getMaxPercentage() {
        final double val = maxSlider.getValue();
        return val / MAX_VALUE;
    }

    public double getMinPercentage() {
        final double val = minSlider.getValue();
        final double max = MAX_VALUE;
        return (max - val) / max;
    }

    public void setEnabled(boolean enabled) {
        maxSlider.setEnabled(enabled);
        minSlider.setEnabled(enabled);
    }
}


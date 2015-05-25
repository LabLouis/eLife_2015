/*
 * Copyright (c) 2012 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.venkman.gui.log;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.math.BigDecimal;

/**
 * TODO: add javadoc
 *
 * @author Eric Trautman
 */
public class HistogramTimeSliderPanel {
    private JPanel contentPanel;
    private JLabel timeWindowLabel;
    private JLabel timeWindowValueLabel;
    private JSlider timeSlider;

    private double graphWidth;
    private double millisecondsPerFrame;

    public HistogramTimeSliderPanel(int graphWidth,
                                    double millisecondsPerFrame) {
        this.graphWidth = graphWidth;
        this.millisecondsPerFrame = millisecondsPerFrame;

        timeSlider.setMinimum(1);
        timeSlider.setMaximum(6);
        timeSlider.setSnapToTicks(true);

        addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                timeWindowValueLabel.setText(getTimeWindowSeconds());
            }
        });

        timeSlider.setValue(1);
        setEnabled(false);
    }

    public JPanel getContentPanel() {
        return contentPanel;
    }

    public void addChangeListener(ChangeListener listener) {
        timeSlider.addChangeListener(listener);
    }

    public int getValue() {
        return timeSlider.getValue();
    }

    public String getTimeWindowSeconds() {
        final double seconds =
                (getValue() * graphWidth * millisecondsPerFrame) / 1000.0;
        BigDecimal bd = new BigDecimal(seconds);
        bd = bd.setScale(0, BigDecimal.ROUND_HALF_UP);
        return bd.toString() + "s";
    }

    public void setEnabled(boolean enabled) {
        timeWindowLabel.setEnabled(enabled);
        timeWindowValueLabel.setEnabled(enabled);
        timeSlider.setEnabled(enabled);
    }
}

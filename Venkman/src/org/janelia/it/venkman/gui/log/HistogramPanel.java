/*
 * Copyright (c) 2013 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.venkman.gui.log;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * A reusable panel for displaying data points with thresholds over time.
 *
 * @author Eric Trautman
 */
public abstract class HistogramPanel extends JPanel {

    private static final int TOP_MARGIN = 10;
    private static final int BOTTOM_MARGIN = 40;
    private static final int RIGHT_MARGIN = 80;

    private Dimension size = new Dimension(300, 150);
    private Dimension sizeWithBorder =
            new Dimension(size.width + RIGHT_MARGIN,
                          size.height + TOP_MARGIN + BOTTOM_MARGIN);
    private int maxY = TOP_MARGIN + size.height;

    private LogModel logModel;
    private PropertyChangeListener logFileListener;
    private PropertyChangeListener currentFrameListener;

    private Double minimumValue;
    private Double maximumValue;
    private Double minimumThreshold;
    private Double maximumThreshold;

    private double currentMinimumValue;
    private String currentMinimumString;
    private String currentMaximumString;
    private double currentScalingFactor;

    private List<Threshold> thresholds;

    private HistogramValueSliderPanel valuesSliderPanel;
    private HistogramTimeSliderPanel timeSliderPanel;

    public HistogramPanel() {

        setBackground(Color.WHITE);

        setLayout(new BorderLayout());

        valuesSliderPanel = new HistogramValueSliderPanel();
        final ChangeListener listener = new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                setCurrentValueScale(true);
            }
        };

        valuesSliderPanel.addMaxChangeListener(listener);
        valuesSliderPanel.addMinChangeListener(listener);
        add(valuesSliderPanel.getContentPanel(), BorderLayout.EAST);

        timeSliderPanel = new HistogramTimeSliderPanel(size.width,
                                                       33.3333);
        add(timeSliderPanel.getContentPanel(), BorderLayout.SOUTH);
        timeSliderPanel.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                repaint();
            }
        });

        this.thresholds = new ArrayList<Threshold>();

        this.logFileListener = new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                updateLogFile();
            }
        };

        this.currentFrameListener = new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                updateCurrentFrame();
            }
        };
    }

    public LogModel getLogModel() {
        return logModel;
    }

    public void setLogModel(LogModel logModel) {

        if (this.logModel != null) {
            this.logModel.removePropertyChangeListener(
                    LogModel.LOG_FILE_PROPERTY,
                    logFileListener);
            this.logModel.removePropertyChangeListener(
                    LogModel.CURRENT_FRAME_PROPERTY,
                    currentFrameListener);
        }

        this.logModel = logModel;

        logModel.addPropertyChangeListener(LogModel.LOG_FILE_PROPERTY,
                                           logFileListener);

        logModel.addPropertyChangeListener(LogModel.CURRENT_FRAME_PROPERTY,
                                           currentFrameListener);
    }

    private void updateLogFile() {

        minimumValue = null;
        maximumValue = null;
        minimumThreshold = null;
        maximumThreshold = null;
        thresholds.clear();

        if (logModel.hasFrames()) {
            setMinAndMaxValues();
            setThresholds();
            setCurrentValueScale(false);
            timeSliderPanel.setEnabled(true);
        } else {
            valuesSliderPanel.setEnabled(false);
            timeSliderPanel.setEnabled(true);
        }
    }

    private void setMinAndMaxValues() {
        double value;
        for (int i = 0; i < logModel.getFrameCount(); i++) {
            value = getValue(i);
            if ((minimumValue == null) || (value < minimumValue)) {
                minimumValue = value;
            }
            if ((maximumValue == null) || (value > maximumValue)) {
                maximumValue = value;
            }
        }

        if ((maximumValue - minimumValue) < 0.000001) {
            minimumValue = (double) minimumValue.intValue();
            maximumValue = maximumValue.intValue() + 1.0;
            valuesSliderPanel.setEnabled(false);
        } else {
            valuesSliderPanel.setEnabled(true);
        }
    }

    private void setThresholds() {

        // add in draw order (give first threshold precedence by adding last)

        double value;
        final Color[] colors = { Color.red, Color.green, Color.orange };
        final List<Double> thresholdValues = getThresholdValues();
        Color color;
        Threshold threshold;
        for (int i = thresholdValues.size() - 1; i >= 0; i-- ) {
            if (i < colors.length) {
                color = colors[i];
            } else {
                color = Color.black;
            }
            threshold = new Threshold(thresholdValues.get(i), color);
            thresholds.add(threshold);

            if (threshold.isDefined()) {
                value = threshold.getValue();
                if ((minimumThreshold == null) ||
                    (value < minimumThreshold)) {
                    minimumThreshold = value;
                }
                if ((maximumThreshold == null) ||
                    (value > maximumThreshold)) {
                    maximumThreshold = value;
                }
            }
        }

        if (minimumThreshold == null) {
            minimumThreshold = minimumValue;
        } else if (minimumThreshold < minimumValue) {
            minimumValue = minimumThreshold;
        }

        if (maximumThreshold == null) {
            maximumThreshold = maximumValue;
        } else if (maximumThreshold > maximumValue) {
            maximumValue = maximumThreshold;
        }

        final double thresholdOffset = (maximumValue - minimumValue) / 100.0;
        if ((maximumThreshold - minimumThreshold) < 0.000001) {
            minimumThreshold = minimumThreshold - thresholdOffset;
            maximumThreshold = maximumThreshold + thresholdOffset;
        }
    }

    public void updateCurrentFrame() {
        repaint();
    }

    @Override
    public Dimension getMaximumSize() {
        return sizeWithBorder;
    }

    @Override
    public Dimension getMinimumSize() {
        return sizeWithBorder;
    }

    @Override
    public Dimension getPreferredSize() {
        return sizeWithBorder;
    }

    public abstract List<Double> getThresholdValues();
    public abstract double getValue(int index);

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);

        if (logModel.hasFrames()) {

            final int currentFrame = logModel.getCurrentFrame();
            final int currentModeStartFrame = logModel.getCurrentModeStart();
            final int currentModeStopFrame = logModel.getCurrentModeStop();
            final int frameCount = logModel.getFrameCount();

            Graphics2D g2d = (Graphics2D) g.create();

            final int widthDividedByTwo = size.width / 2;
            final int minY = TOP_MARGIN;
            final int timeIndex = timeSliderPanel.getValue();

            double currentLine = (double) currentFrame / (double) timeIndex;
            double currentOffset = 0;
            if (currentLine > widthDividedByTwo) {
                currentLine = widthDividedByTwo;
                currentOffset = widthDividedByTwo -
                                ((double) currentFrame / ((double) timeIndex));
            }

            double modeStart = (currentModeStartFrame / (double) timeIndex) +
                               currentOffset;
            double modeWidth =
                    (double) (currentModeStopFrame - currentModeStartFrame) /
                    (double) timeIndex;

            if (modeStart < 0) {
                modeWidth = modeWidth + modeStart;
                modeStart = 0;
            }

            if ((modeStart + modeWidth) > size.width) {
                modeWidth = size.width - modeStart;
            }

            g2d.setPaint(Color.yellow);
            g2d.fillRect((int) modeStart, minY, (int) modeWidth, size.height);

            g2d.setPaint(Color.lightGray);

            g2d.drawLine(0, minY, size.width, minY);
            g2d.drawLine(0, maxY, size.width, maxY);


            g2d.setPaint(Color.blue);
            g2d.drawLine((int) currentLine, minY,
                         (int) currentLine, maxY);

            for (Threshold threshold : thresholds) {
                threshold.draw(g2d);
            }

            int startIndex =
                    (currentFrame - (widthDividedByTwo * timeIndex)) + 1;
            if (startIndex < timeIndex) {
                startIndex = timeIndex;
            }

            g2d.setPaint(Color.black);

            g2d.drawString(currentMinimumString,
                           (size.width + 2), (maxY + 5));
            g2d.drawString(currentMaximumString,
                           (size.width + 2), (minY + 5));

            g2d.clipRect(0, minY, size.width, maxY);

            if (startIndex < frameCount) {
                int stopIndex = startIndex + (size.width * timeIndex);
                if (stopIndex > frameCount) {
                    stopIndex = frameCount;
                }

                int x2 = 1;
                for (int i = startIndex; i < stopIndex; i = i + timeIndex) {
                    g2d.drawLine((x2 - 1),
                                 (maxY - getScaledValue(i - timeIndex)),
                                 x2,
                                 (maxY - getScaledValue(i)));
                    x2++;
                }
            }
        }

    }

    private int getScaledValue(int index) {
        return getScaledValue(getValue(index));
    }

    private int getScaledValue(double value) {
        return (int) ((value - currentMinimumValue) * currentScalingFactor);
    }

    private void setCurrentValueScale(boolean repaint) {

        if (logModel.hasFrames()) {
            final double minPercentage = valuesSliderPanel.getMinPercentage();
            final double fullDeltaForMinimum = minimumThreshold - minimumValue;
            currentMinimumValue =
                    minimumThreshold - (minPercentage * fullDeltaForMinimum);

            final double maxPercentage = valuesSliderPanel.getMaxPercentage();
            final double fullDeltaForMaximum = maximumValue - maximumThreshold;
            double currentMaximumValue =
                    maximumThreshold + (maxPercentage * fullDeltaForMaximum);

            int formatScale = 4;
            final double currentRange = currentMaximumValue - currentMinimumValue;
            if (currentRange > 50) {
                formatScale = 0;
            } else if (currentRange > 10) {
                formatScale = 1;
            } else if (currentRange > 5) {
                formatScale = 2;
            }

            currentMinimumString = formatStringValue(currentMinimumValue,
                                                     formatScale);
            currentMaximumString = formatStringValue(currentMaximumValue,
                                                     formatScale);

            final double delta = currentMaximumValue - currentMinimumValue;
            if (delta > 0) {
                currentScalingFactor = (double) size.height / delta;
            } else {
                currentScalingFactor = 0;
            }

            if (repaint) {
                repaint();
            }
        }
    }

    private static String formatStringValue(Double value,
                                            int scale) {
        String valueString;
        if (value == null) {
            valueString = "";
        } else {
            BigDecimal bd = new BigDecimal(value);
            bd = bd.setScale(scale, BigDecimal.ROUND_HALF_UP);
            valueString = bd.toString();
            if (valueString.indexOf('.') > -1) {
                int trimLength = valueString.length();
                char c;
                for (int i = valueString.length() - 1; i >=0; i--) {
                    c = valueString.charAt(i);
                    if (c == '0') {
                        trimLength--;
                    } else {
                        if (c == '.') {
                            trimLength--;
                        }
                        break;
                    }
                }
                valueString = valueString.substring(0, trimLength);
            }
        }
        return valueString;
    }

    private class Threshold {

        private boolean defined;
        private Double value;
        private String stringValue;
        private Color color;

        private Threshold(Double value,
                          Color color) {
            if (value == null) {
                this.defined = false;
            } else {
                this.defined = true;
                this.value = value;
                this.stringValue = formatStringValue(value, 4);
                this.color = color;
            }
        }

        public boolean isDefined() {
            return defined;
        }

        public Double getValue() {
            return value;
        }

        @Override
        public String toString() {
            return stringValue;
        }

        public void draw(Graphics2D g2d) {
            if (defined) {
                g2d.setPaint(color);
                final int scaledValue = maxY - getScaledValue(value);
                g2d.drawLine(0, scaledValue, size.width, scaledValue);
                g2d.drawString(stringValue,
                               (size.width + 2), (scaledValue + 5));
            }
        }

    }
}


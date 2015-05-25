/*
 * Copyright (c) 2012 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.venkman.gui.log;

import org.janelia.it.venkman.data.LarvaBehaviorMode;
import org.janelia.it.venkman.data.LarvaFrameData;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This panel displays actual and expected behavior modes over time.
 *
 * @author Eric Trautman
 */
public class BehaviorModePanel
        extends JPanel {

    private static final int TOP_MARGIN = 10;
    private static final int BOTTOM_MARGIN = 40;
    private static final int RIGHT_MARGIN = 80;

    private Dimension size = new Dimension(300, 150);
    private Dimension sizeWithBorder =
            new Dimension(size.width + RIGHT_MARGIN,
                          size.height + TOP_MARGIN + BOTTOM_MARGIN);
    private int maxY = TOP_MARGIN + size.height;

    private HistogramTimeSliderPanel timeSliderPanel;

    private BehaviorModeComparisonModel model;
    private PropertyChangeListener logFileListener;
    private PropertyChangeListener currentFrameListener;

    public BehaviorModePanel() {

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

        setBackground(Color.WHITE);

        setLayout(new BorderLayout());

        this.timeSliderPanel = new HistogramTimeSliderPanel(size.width,
                                                            33.3333);
        add(timeSliderPanel.getContentPanel(), BorderLayout.SOUTH);
        timeSliderPanel.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                repaint();
            }
        });
    }

    public void setModel(BehaviorModeComparisonModel model) {
        LogModel actualModel;
        if (this.model != null) {
            actualModel = this.model.getActualModel();
            if (actualModel != null) {
                actualModel.removePropertyChangeListener(
                        LogModel.LOG_FILE_PROPERTY,
                        logFileListener);
                actualModel.removePropertyChangeListener(
                        LogModel.CURRENT_FRAME_PROPERTY,
                        currentFrameListener);
            }
        }

        this.model = model;
        actualModel = this.model.getActualModel();

        actualModel.addPropertyChangeListener(
                LogModel.LOG_FILE_PROPERTY,
                logFileListener);
        actualModel.addPropertyChangeListener(
                LogModel.CURRENT_FRAME_PROPERTY,
                currentFrameListener);
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

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);

        if (model.hasActualFrames()) {

            final int currentFrame = model.getCurrentFrame();

            Graphics2D g2d = (Graphics2D) g.create();

            final int widthDividedByTwo = size.width / 2;
            final int minY = TOP_MARGIN;
            final int timeIndex = timeSliderPanel.getValue();

            double currentLine = (double) currentFrame / (double) timeIndex;
            if (currentLine > widthDividedByTwo) {
                currentLine = widthDividedByTwo;
            }

            g2d.setPaint(Color.gray);

            g2d.drawLine(0, minY, size.width, minY);
            g2d.drawLine(0, maxY, size.width, maxY);


            g2d.setPaint(Color.blue);
            g2d.drawLine((int) currentLine, minY,
                         (int) currentLine, maxY);

            int startIndex =
                    (currentFrame - (widthDividedByTwo * timeIndex)) + 1;
            if (startIndex < timeIndex) {
                startIndex = timeIndex;
            }

            final int barHeight = 20;
            final int actualFrameCount = model.getActualFrameCount();
            if (startIndex < actualFrameCount) {
                int stopIndex = startIndex + (size.width * timeIndex);
                if (stopIndex > actualFrameCount) {
                    stopIndex = actualFrameCount;
                }

                paintModeBar(g2d, model.getActualModel(), timeIndex,
                             startIndex, stopIndex,
                             1,
                             minY + 25,
                             barHeight,
                             "actual",
                             false);
            }

            final int expectedFrameCount = model.getExpectedFrameCount();
            if (startIndex < expectedFrameCount) {
                int stopIndex = startIndex + (size.width * timeIndex);
                if (stopIndex > expectedFrameCount) {
                    stopIndex = expectedFrameCount;
                }

                paintModeBar(g2d, model.getExpectedModel(), timeIndex,
                             startIndex, stopIndex,
                             1,
                             minY + 90,
                             barHeight,
                             "expected",
                             model.isFilterEnabled());
            }
        }


    }

    private void updateLogFile() {
        final LogModel actualModel = model.getActualModel();
        timeSliderPanel.setEnabled(actualModel.hasFrames());
    }

    private void updateCurrentFrame() {
        repaint();
    }

    private LarvaBehaviorMode getDefinedMode(LarvaFrameData frameData) {
        LarvaBehaviorMode mode = frameData.getBehaviorMode();
        if (mode == null) {
            mode = LarvaBehaviorMode.IGNORE;
        }
        return mode;
    }

    private void paintModeBar(Graphics2D g2d,
                              LogModel logModel,
                              int timeIndex,
                              int startIndex,
                              int stopIndex,
                              int barX,
                              int barMinY,
                              int barHeight,
                              String barLabelText,
                              boolean filterSimilarFrames) {
        
        LarvaBehaviorMode mode;
        int barWidth;
        int directionLineY;

        if (filterSimilarFrames) {
            while ((startIndex < stopIndex) &&
                   (! model.hasDifference(startIndex))) {
                startIndex = startIndex + timeIndex;
                barX++;
            }
        }

        final List<LarvaFrameData> dataList = logModel.getFrameDataList();
        LarvaFrameData frameData;
        for (int i = startIndex; i < stopIndex; i = i + timeIndex) {
            
            mode = getDefinedMode(dataList.get(i));
            barWidth = 1;

            for (int j = i + timeIndex; j < stopIndex; j = j + timeIndex) {
                frameData = dataList.get(j);
                if (((! filterSimilarFrames) ||
                     model.hasDifference(j)) &&
                    mode.equals(frameData.getBehaviorMode())) {
                    barWidth++;
                    i = j;
                } else {
                    break;
                }            
            }

            if ((! filterSimilarFrames) || model.hasDifference(i)) {
                g2d.setPaint(MODE_TO_COLOR_MAP.get(mode));
                g2d.fillRect(barX, barMinY, barWidth, barHeight);
                
                if (LarvaBehaviorMode.CAST_LEFT.equals(mode) || 
                    LarvaBehaviorMode.TURN_LEFT.equals(mode)) {
                    g2d.setPaint(Color.BLACK);
                    directionLineY = barMinY + 4;
                    g2d.drawLine(barX, directionLineY,
                                 barX + barWidth, directionLineY);
                    directionLineY = directionLineY + 2;
                    g2d.drawLine(barX, directionLineY,
                                 barX + barWidth, directionLineY);
                } else if (LarvaBehaviorMode.CAST_RIGHT.equals(mode) ||
                           LarvaBehaviorMode.TURN_RIGHT.equals(mode)) {
                    g2d.setPaint(Color.BLACK);
                    directionLineY = barMinY + barHeight - 4;
                    g2d.drawLine(barX, directionLineY,
                                 barX + barWidth, directionLineY);
                    directionLineY = directionLineY - 2;
                    g2d.drawLine(barX, directionLineY,
                                 barX + barWidth, directionLineY);
                }
            }

            barX = barX + barWidth;
        }

        g2d.setPaint(Color.BLACK);

        final int labelX = 5;
        final int labelY = barMinY - 5;
        g2d.drawString(barLabelText, labelX, labelY);

        final int modeX = size.width + 5;
        final int modeY = barMinY + (barHeight / 2) + 5;
        mode = getDefinedMode(dataList.get(model.getCurrentFrame()));
        g2d.drawString(mode.getName(), modeX, modeY);
    }

    public static Color getColor(LarvaBehaviorMode mode) {
        return MODE_TO_COLOR_MAP.get(mode);
    }

    private static final Map<LarvaBehaviorMode, Color> MODE_TO_COLOR_MAP;
    static {
        Map<LarvaBehaviorMode, Color> map = 
                new HashMap<LarvaBehaviorMode, Color>();
        map.put(LarvaBehaviorMode.RUN, Color.GREEN);
        map.put(LarvaBehaviorMode.BACK_UP, Color.BLACK);
        map.put(LarvaBehaviorMode.STOP, Color.RED);
        map.put(LarvaBehaviorMode.IGNORE, Color.LIGHT_GRAY);
        map.put(LarvaBehaviorMode.SAMPLING, Color.PINK);
        map.put(LarvaBehaviorMode.TURN_RIGHT, Color.CYAN);
        map.put(LarvaBehaviorMode.CAST_RIGHT, Color.ORANGE);
        map.put(LarvaBehaviorMode.TURN_LEFT, Color.CYAN);
        map.put(LarvaBehaviorMode.CAST_LEFT, Color.ORANGE);
        MODE_TO_COLOR_MAP = map;
    }
    
}

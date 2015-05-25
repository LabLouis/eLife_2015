/*
 * Copyright (c) 2013 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.venkman.gui.log;

import org.apache.log4j.Logger;
import org.janelia.it.venkman.data.LarvaFrameData;
import org.janelia.it.venkman.log.LogDateFormats;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.net.URL;
import java.util.Calendar;

/**
 * A panel that displays buttons and sliders for moving around the
 * frames in a log file.
 *
 * @author Eric Trautman
 */
public class PlaybackControlsPanel {

    private JPanel contentPanel;

    @SuppressWarnings("UnusedDeclaration")
    private JButton beginButton;
    @SuppressWarnings("UnusedDeclaration")
    private JButton previousButton;
    private JButton playButton;
    @SuppressWarnings("UnusedDeclaration")
    private JButton nextButton;
    @SuppressWarnings("UnusedDeclaration")
    private JButton endButton;

    private JSlider timestampSlider;

    @SuppressWarnings("UnusedDeclaration")
    private JPanel textPanel;
    private JLabel frameLabel;
    private JLabel timestampLabel;

    private LogModel logModel;
    private ImageIcon playIcon;
    private ImageIcon stopIcon;

    private boolean isPlaying;
    private boolean handleSliderChangeEvents;

    public PlaybackControlsPanel(LogModel logModel) {

        this.logModel = logModel;

        this.logModel.addPropertyChangeListener(
                LogModel.LOG_FILE_PROPERTY,
                new PropertyChangeListener() {
                    @Override
                    public void propertyChange(PropertyChangeEvent evt) {
                        updateLogFile();
                    }
                });

        this.logModel.addPropertyChangeListener(
                LogModel.CURRENT_FRAME_PROPERTY,
                new PropertyChangeListener() {
                    @Override
                    public void propertyChange(PropertyChangeEvent evt) {
                        updateCurrentFrame();
                    }
                });

        this.playIcon = loadIcon("/images/play.png");
        this.stopIcon = loadIcon("/images/stop.png");

        this.playButton.addActionListener(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (isPlaying) {
                    stopPlaying();
                } else {
                    startPlaying();
                }
            }
        });
        this.playButton.setEnabled(false);

        this.timestampSlider.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                if (handleSliderChangeEvents &&
                    (! timestampSlider.getValueIsAdjusting())) {
                    slideCurrentFrame();
                }
            }
        });
        this.timestampSlider.setEnabled(false);
        this.handleSliderChangeEvents = false;

    }

    public JPanel getContentPanel() {
        return contentPanel;
    }

    public void startPlaying() {
        if ((! isPlaying) && (logModel.hasFrames())) {
            isPlaying = true;
            playButton.setIcon(stopIcon);
            playButton.setToolTipText("Stop");
            invokePlayNextLater(System.currentTimeMillis());
        }
    }

    public void stopPlaying() {
        if ((isPlaying) && (logModel.hasFrames())) {
            isPlaying = false;
            playButton.setIcon(playIcon);
            playButton.setToolTipText("Play");
        }
    }

    private void createUIComponents() {

        final Color currentColor = Color.BLUE;

        frameLabel = new JLabel("-");
        frameLabel.setForeground(currentColor);

        timestampLabel = new JLabel("-");
        timestampLabel.setForeground(currentColor);

        beginButton = createButton(
                new AbstractAction("First") {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        logModel.moveToFirstFrame();
                    }
                });

        previousButton = createButton(
                new AbstractAction("Previous") {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        logModel.moveRelativeToCurrentFrame(-1);
                    }
                });

        nextButton = createButton(
                new AbstractAction("Next") {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        logModel.moveRelativeToCurrentFrame(1);
                    }
                });

        endButton = createButton(
                new AbstractAction("Last") {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        logModel.moveToLastFrame();
                    }
                });
    }

    private JButton createButton(Action action) {

        JButton button = new JButton(action);

//        InputMap inMap = button.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
//        ActionMap actionMap = button.getActionMap();
//        final KeyStroke keyStroke = KeyStroke.getKeyStroke(keyStrokeCode, 0);
//        inMap.put(keyStroke, action.getValue(Action.NAME));
//        actionMap.put(action.getValue(Action.NAME), action);

        button.setEnabled(false);

        return button;
    }

    private ImageIcon loadIcon(String filename) {
        ImageIcon icon = null;
        Class clazz = getClass();
        URL resource = clazz.getResource(filename);

        if (resource == null) {
            LOG.warn("loadIcon: failed to find " + filename);
        }  else {
            try {
                Image img = ImageIO.read(resource);
                icon = new ImageIcon(img);
            } catch (IOException e) {
                LOG.warn("loadIcon: failed to load " + filename, e);
            }
        }
        return icon;
    }

    private void updateLogFile() {
        final int frameCount = logModel.getFrameCount();

        handleSliderChangeEvents = false;

        if (frameCount > 0) {
            timestampSlider.setMinimum(0);
            timestampSlider.setMaximum(frameCount - 1);
            timestampSlider.setValue(0);
            final int majorTickSpacing = frameCount / 4;
            timestampSlider.setMajorTickSpacing(majorTickSpacing);
            timestampSlider.setPaintTicks(true);
            timestampSlider.setEnabled(true);
        }
    }

    private void updateCurrentFrame() {
        final boolean isFirstFrame = logModel.isFirstFrame();
        beginButton.setEnabled(! isFirstFrame);
        previousButton.setEnabled(! isFirstFrame);

        final boolean isLastFrame = logModel.isLastFrame();
        nextButton.setEnabled(! isLastFrame);
        endButton.setEnabled(! isLastFrame);

        final int currentFrame = logModel.getCurrentFrame();
        handleSliderChangeEvents = false;
        timestampSlider.setValue(currentFrame);
        handleSliderChangeEvents = true;

        playButton.setEnabled(true);

        frameLabel.setText((currentFrame + 1) + "/" +
                           logModel.getFrameCount());

        final LarvaFrameData currentFrameData =
                logModel.getCurrentFrameData();
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(currentFrameData.getTime());
        c.add(Calendar.HOUR, 5);
        final String time = LogDateFormats.TIME_ONLY.format(c.getTime());
        timestampLabel.setText(time);
    }

    private void invokePlayNextLater(final long requestTime) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                playNext(requestTime);
            }
        });
    }

    private void playNext(final long requestTime) {
        if (isPlaying) {
            final long frameElapsed =
                    logModel.getIntervalBetweenCurrentAndNextFrame();
            if (frameElapsed > 0) {
                long actualElapsed = System.currentTimeMillis() - requestTime;
                if (actualElapsed < frameElapsed) {
                    invokePlayNextLater(requestTime);
                } else {
                    final int delta = (int) (actualElapsed / frameElapsed);
                    if (delta > 5) {
                        LOG.warn("playNext: large playback delta:" + delta);
                    }
                    logModel.moveRelativeToCurrentFrame(delta);
                    invokePlayNextLater(System.currentTimeMillis());
                }
            } else {
                stopPlaying();
            }
        }
    }

    private void slideCurrentFrame() {
        handleSliderChangeEvents = false;
        logModel.moveToFrame(timestampSlider.getValue());
        handleSliderChangeEvents = true;
    }

    private static final Logger LOG =
            Logger.getLogger(PlaybackControlsPanel.class);
}

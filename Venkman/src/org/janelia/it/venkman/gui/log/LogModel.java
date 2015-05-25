/*
 * Copyright (c) 2014 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.venkman.gui.log;

import org.apache.log4j.Logger;
import org.janelia.it.venkman.config.LarvaBehaviorParameters;
import org.janelia.it.venkman.config.rules.LEDArrayStimulus;
import org.janelia.it.venkman.config.rules.LEDStimulus;
import org.janelia.it.venkman.config.rules.Stimulus;
import org.janelia.it.venkman.data.LarvaBehaviorMode;
import org.janelia.it.venkman.data.LarvaFrameData;
import org.janelia.it.venkman.data.LarvaSkeleton;
import org.janelia.it.venkman.data.TrackerPoint;
import org.janelia.it.venkman.log.LogReader;
import org.janelia.it.venkman.log.LogSession;
import org.janelia.it.venkman.rules.DefinedEnvironment;
import org.janelia.it.venkman.rules.LarvaStimulusRules;
import org.janelia.it.venkman.rules.RuleData;

import java.io.File;
import java.util.List;

/**
 * The data model read from a venkman log file.
 *
 * @author Eric Trautman
 */
public class LogModel
        extends PropertyChangeSupporter {

    public static final String LOG_FILE_PROPERTY =
            "log-file";
    public static final String CURRENT_FRAME_PROPERTY =
            "current-frame";

    private File logFileDirectory;
    private File logFile;
    private LarvaBehaviorParameters currentParameters;
    private LarvaStimulusRules currentRules;
    private int currentFrame;
    private LarvaFrameData currentFrameData;
    private List<LarvaFrameData> frameDataList;
    private List<LEDStimulus> stimulusList;
    private List<RuleData> ruleDataList;
    private TrackerPoint minimumPosition;
    private TrackerPoint maximumPosition;

    private DefinedEnvironment orientedRules;

    private int currentModeStart;
    private int currentModeStop;

    public LogModel() {
        this.logFileDirectory = new File(".");
        resetData();
    }

    public File getLogFileDirectory() {
        return logFileDirectory;
    }

    public void setLogFileDirectory(File logFileDirectory) {
        this.logFileDirectory = logFileDirectory;
    }

    public File getLogFile() {
        return logFile;
    }

//    public TrackerPoint getMinimumPosition() {
//        return minimumPosition;
//    }

    public TrackerPoint getMaximumPosition() {
        return maximumPosition;
    }

    public int getCurrentFrame() {
        return currentFrame;
    }

    public LarvaFrameData getCurrentFrameData() {
        return currentFrameData;
    }

    public LarvaFrameData getPreviousFrameData() {
        LarvaFrameData previousFrameData = null;
        final int previousFrame = currentFrame - 1;
        if ((currentFrame > 0) && hasFrames()) {
            previousFrameData = frameDataList.get(previousFrame);

        }
        return previousFrameData;
    }

    public LarvaFrameData getFrameData(int index) {
        LarvaFrameData frameData = null;
        if (hasFrames() && (index < frameDataList.size())) {
            frameData = frameDataList.get(index);
        }
        return frameData;
    }

    public List<LarvaFrameData> getFrameDataList() {
        return frameDataList;
    }

    public LarvaBehaviorParameters getCurrentParameters() {
        return currentParameters;
    }

    public LarvaStimulusRules getCurrentRules() {
        return currentRules;
    }

    public LEDStimulus getStimulus(int index) {
        // TODO: provide access to more than just the first stimulus flash
        LEDStimulus ledStimulus = null;
        if (hasFrames()) {
            if (stimulusList != null) {
                if (index < stimulusList.size()) {
                    ledStimulus = stimulusList.get(index);
                }
            } else if (index < frameDataList.size()) {
                final LarvaFrameData frameData = frameDataList.get(index);
                final List<? extends Stimulus> frameStimulusList = frameData.getStimulusList();
                if ((frameStimulusList != null) && (frameStimulusList.size() > 0)) {
                    final Stimulus stimulus = frameStimulusList.get(0);
                    if (stimulus instanceof LEDStimulus) {
                        ledStimulus = (LEDStimulus) stimulus;
                    } else if (stimulus instanceof LEDArrayStimulus) {
                        ledStimulus = ((LEDArrayStimulus) stimulus).getLedStimulus(0);
                    }
                }
            }
        }
        return ledStimulus;
    }

    public DefinedEnvironment getOrientedRules() {
        return orientedRules;
    }

    public long getLastFrameTime() {
        long lastTime = 0;
        if ((frameDataList != null) && (frameDataList.size() > 0)) {
            final LarvaFrameData lastFrameData =
                    frameDataList.get(frameDataList.size() - 1);
            lastTime = lastFrameData.getTime();
        }
        return lastTime;
    }

    public int getCurrentModeStart() {
        return currentModeStart;
    }

    public int getCurrentModeStop() {
        return currentModeStop;
    }

    public void loadLogFile(File selectedLogFile) throws IllegalArgumentException {

        if (selectedLogFile != null) {

            // TODO: move load off of event dispatch thread

            logFileDirectory = selectedLogFile.getParentFile();

            LogReader reader = new LogReader(selectedLogFile);

            try {

                // TODO: handle large files in some reasonable manner

                reader.read();
                final LogSession logSession = reader.getSession();
                List<LarvaFrameData> importedFrameDataList =
                        logSession.getFrameDataList();

                if ((importedFrameDataList != null) &&
                    (importedFrameDataList.size() > 0)) {

                    final File previousLogFile = logFile;

                    resetData();

                    logFile = selectedLogFile;
                    frameDataList = importedFrameDataList;
                    currentParameters = logSession.getLarvaBehaviorParameters();
                    currentRules = logSession.getLarvaStimulusRules();
                    if (currentRules != null) {
                        currentRules.overrideBehaviorParameters(currentParameters);
                    }
                    stimulusList = logSession.getStimulusList();
                    ruleDataList = logSession.getRuleDataList();

                    // if stimulus list is empty, reset it to null
                    if ((stimulusList != null) && (stimulusList.size() == 0)) {
                        stimulusList = null;
                    }

                    if (currentRules instanceof DefinedEnvironment) {
                        DefinedEnvironment deRule = (DefinedEnvironment) currentRules;
                        if (deRule.isEnableOrientationLogic()) {
                            orientedRules = deRule;
                            orientedRules.restoreTransformationParameters(frameDataList,
                                                                          ruleDataList);
                        }
                    }

                    setMinAndMaxPositions();

                    if ((minimumPosition.getX() < 0) || (minimumPosition.getY() < 0)) {
                        LOG.warn("loadLogFile: minimum (likely rotated) position for " +
                                 selectedLogFile.getAbsolutePath() +
                                 " is less than zero, value is " + minimumPosition);
                    }

                    firePropertyChange(LOG_FILE_PROPERTY,
                                       previousLogFile,
                                       logFile);

                    moveToFirstFrame();
                }
            } catch (Exception e) {
                final String message =
                        "Load of " + selectedLogFile.getAbsolutePath() +
                        " failed with error:\n\n" +
                        e.getMessage();
                throw new IllegalArgumentException(message, e);
            }
        }
    }

    public void moveToFirstFrame() {
        setCurrentFrame(0);
    }

    public void moveRelativeToCurrentFrame(int delta) {
        setCurrentFrame(currentFrame + delta);
    }

    public void moveToLastFrame() {
        setCurrentFrame(Integer.MAX_VALUE);
    }

    public void moveToFrame(int frameNumber) {
        setCurrentFrame(frameNumber);
    }

//    /**
//     * Moves the current frame as close as possible to the specified time.
//     * If the specified time is before or after the entire run,
//     * the current frame will be set to the beginning or end of the run
//     * respectively.
//     *
//     * @param  time  the run time in milliseconds.
//     */
//    public void moveToTime(long time) {
//        if (frameDataList != null) {
//            final int size = frameDataList.size();
//            if (size > 0) {
//                final int lastIndex = size - 1;
//                final LarvaFrameData lastFrame = frameDataList.get(lastIndex);
//                int estimatedIndex = 0;
//                if (time > 0) {
//                    estimatedIndex = (int)
//                            ((lastIndex * time) / lastFrame.getTime());
//                    if (estimatedIndex > lastIndex) {
//                        estimatedIndex = lastIndex;
//                    }
//                }
//
//                LarvaFrameData data = frameDataList.get(estimatedIndex);
//                if (time < data.getTime()) {
//                    for (int i = estimatedIndex; i >= 0; i--) {
//                        data = frameDataList.get(i);
//                        if (time >= data.getTime()) {
//                            setCurrentFrame(i);
//                            break;
//                        }
//                    }
//                } else {
//                    for (int i = estimatedIndex; i < size; i++) {
//                        data = frameDataList.get(i);
//                        if (time <= data.getTime()) {
//                            setCurrentFrame(i);
//                            break;
//                        }
//                    }
//                }
//            }
//        }
//    }

    public long getIntervalBetweenCurrentAndNextFrame() {
        final int nextFrame = currentFrame + 1;
        long interval = 0;
        if ((frameDataList != null) && (nextFrame < frameDataList.size())) {
            final LarvaFrameData next = frameDataList.get(currentFrame + 1);
            interval = next.getTime() - currentFrameData.getTime();
        }
        return interval;
    }

    public int getFrameCount() {
        int frameCount = 0;
        if (frameDataList != null) {
            frameCount = frameDataList.size();
        }
        return frameCount;
    }

    public boolean hasFrames() {
        return ((frameDataList != null) && (frameDataList.size() > 0));
    }

    public boolean isFirstFrame() {
        return currentFrame == 0;
    }

    public boolean isLastFrame() {
        return currentFrame == (frameDataList.size() - 1);
    }

    private void resetData() {
        frameDataList = null;
        stimulusList = null;
        ruleDataList = null;
//        minimumPosition = null;
        maximumPosition = null;
        currentParameters = null;
        currentRules = null;
        orientedRules = null;
        currentFrame = -1;
        currentFrameData = null;
        currentModeStart = Integer.MAX_VALUE;
        currentModeStop = -1;
    }

    private void setMinAndMaxPositions() {

        Long rotateTime = null;
        if (orientedRules != null) {
            rotateTime = orientedRules.getRotateTime();
        }

        double minX = Integer.MAX_VALUE;
        double minY = Integer.MAX_VALUE;
        double maxX = 0;
        double maxY = 0;
        double x;
        double y;
        TrackerPoint rotatedPoint;
        TrackerPoint transformedPoint;
        for (LarvaFrameData frame : frameDataList) {
            LarvaSkeleton s = frame.getSkeleton();
            for (TrackerPoint p : s.getPoints()) {
                x = p.getX();
                y = p.getY();
                if (x < minX) {
                    minX = x;
                }
                if (x > maxX) {
                    maxX = x;
                }
                if (y < minY) {
                    minY = y;
                }
                if (y > maxY) {
                    maxY = y;
                }

                // include rotated coordinates in min/max derivation
                if ((rotateTime != null) && (rotateTime <= frame.getTime())) {
                    rotatedPoint = orientedRules.getRotatedPoint(p);
                    transformedPoint = orientedRules.getTransformedPoint(rotatedPoint);
                    x = transformedPoint.getX();
                    y = transformedPoint.getY();
                    if (x < minX) {
                        minX = x;
                    }
                    if (x > maxX) {
                        maxX = x;
                    }
                    if (y < minY) {
                        minY = y;
                    }
                    if (y > maxY) {
                        maxY = y;
                    }
                }
            }
        }

        minimumPosition = new TrackerPoint(minX, minY);
        maximumPosition = new TrackerPoint(maxX, maxY);
    }

    private void setCurrentFrame(int currentFrame) {

        if (frameDataList != null) {

            final int lastFrame = frameDataList.size() - 1;

            if (lastFrame > 0) {

                if (currentFrame > lastFrame) {
                    currentFrame = lastFrame;
                } else if (currentFrame < 0) {
                    currentFrame = 0;
                }

                if (currentFrame != this.currentFrame) {
                    int previousFrame = this.currentFrame;
                    this.currentFrame = currentFrame;
                    this.currentFrameData = frameDataList.get(currentFrame);

                    setCurrentModeStartAndStop();

                    firePropertyChange(CURRENT_FRAME_PROPERTY,
                                       previousFrame,
                                       currentFrame);
                }

            }
        }
    }

    private void setCurrentModeStartAndStop() {

        if ((currentFrame < currentModeStart) ||
            (currentFrame > currentModeStop)) {

            final LarvaBehaviorMode mode = currentFrameData.getBehaviorMode();
            int start = currentFrame;
            LarvaBehaviorMode listMode;
            for (int i = (currentFrame - 1); i >= 0; i--) {
                listMode = frameDataList.get(i).getBehaviorMode();
                if (LarvaBehaviorMode.isSameMode(mode, listMode)) {
                    start--;
                } else {
                    break;
                }
            }
            currentModeStart = start;

            final int size = frameDataList.size();
            int stop = currentFrame;
            for (int i = (currentFrame + 1); i < size; i++) {
                listMode = frameDataList.get(i).getBehaviorMode();
                if (LarvaBehaviorMode.isSameMode(mode, listMode)) {
                    stop++;
                } else {
                    break;
                }
            }
            currentModeStop = stop;
        }
    }

    private static final Logger LOG = Logger.getLogger(LogModel.class);
}

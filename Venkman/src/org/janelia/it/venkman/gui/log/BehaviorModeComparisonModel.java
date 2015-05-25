/*
 * Copyright (c) 2012 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.venkman.gui.log;

import org.janelia.it.venkman.data.LarvaBehaviorMode;
import org.janelia.it.venkman.data.LarvaFrameData;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The data model for comparing the behavior modes between two imported logs.
 *
 * @author Eric Trautman
 */
public class BehaviorModeComparisonModel
        extends PropertyChangeSupporter {

    public static final String LOG_MODELS_PROPERTY = "log-models";
    public static final String FILTER_PROPERTY = "filter";

    private LogModel actualModel;
    private LogModel expectedModel;
    private Set<Integer> differentFrameIndexes;
    private double similarityPercentage;
    private boolean filterEnabled;

    public BehaviorModeComparisonModel(LogModel actualModel) {
        this.actualModel = actualModel;

        this.actualModel.addPropertyChangeListener(
                LogModel.LOG_FILE_PROPERTY,
                new PropertyChangeListener() {
                    @Override
                    public void propertyChange(PropertyChangeEvent evt) {
                        initExpectedLogFileDirectory();
                        calculateDifferences();
                    }
                });

        this.actualModel.addPropertyChangeListener(
                LogModel.CURRENT_FRAME_PROPERTY,
                new PropertyChangeListener() {
                    @Override
                    public void propertyChange(PropertyChangeEvent evt) {
                        synchronizeExpectedWithActual();
                    }
                });

        this.expectedModel = new LogModel();

        this.expectedModel.addPropertyChangeListener(
                LogModel.LOG_FILE_PROPERTY,
                new PropertyChangeListener() {
                    @Override
                    public void propertyChange(PropertyChangeEvent evt) {
                        calculateDifferences();
                    }
                });

        this.differentFrameIndexes = new HashSet<Integer>();
        this.similarityPercentage = -1;
        this.filterEnabled = false;
    }

    public LogModel getActualModel() {
        return actualModel;
    }

    public LogModel getExpectedModel() {
        return expectedModel;
    }

    public double getSimilarityPercentage() {
        return similarityPercentage;
    }

    public boolean isFilterEnabled() {
        return filterEnabled;
    }

    public void setFilterEnabled(boolean filterEnabled) {
        if (filterEnabled != this.filterEnabled) {
            this.filterEnabled = filterEnabled;
            firePropertyChange(FILTER_PROPERTY,
                               ! filterEnabled,
                               filterEnabled);
        }
    }

    public File getExpectedLogFileDirectory() {
        return expectedModel.getLogFileDirectory();
    }

    public File getExpectedLogFile() {
        return expectedModel.getLogFile();
    }

    public int getActualFrameCount() {
        return actualModel.getFrameCount();
    }

    public int getExpectedFrameCount() {
        return expectedModel.getFrameCount();
    }

    public void loadExpectedLogFile(File selectedLogFile)
            throws IllegalArgumentException {
        expectedModel.loadLogFile(selectedLogFile);
    }

    public boolean hasActualFrames() {
        return actualModel.hasFrames();
    }

    public boolean hasExpectedFrames() {
        return expectedModel.hasFrames();
    }

    public int getCurrentFrame() {
        return actualModel.getCurrentFrame();
    }

    public boolean hasDifference(int frameIndex) {
        return differentFrameIndexes.contains(frameIndex);
    }

    public boolean moveToNextDifference() {

        Integer nextFrame = null;

        int frame = actualModel.getCurrentFrame();

        if (differentFrameIndexes.contains(frame)) {
            frame = getEndOfCurrentDifference(frame);
        }

        // move to next difference
        final int expectedFrameCount = expectedModel.getFrameCount();
        for (int i = frame + 1; i < expectedFrameCount; i++) {
            if (differentFrameIndexes.contains(i)) {
                nextFrame = i;
                break;
            }
        }

        boolean nextDifferenceFound = (nextFrame != null);

        if (nextDifferenceFound) {
            actualModel.moveToFrame(nextFrame);
        }

        return nextDifferenceFound;
    }

    public boolean moveToPreviousDifference() {

        Integer previousFrame = null;

        int frame = actualModel.getCurrentFrame();

        if (differentFrameIndexes.contains(frame)) {
            frame = getBeginningOfCurrentDifference(frame);
        }

        // move to previous difference
        for (int i = frame - 1; i > -1; i--) {
            if (differentFrameIndexes.contains(i)) {
                previousFrame = i;
                break;
            }
        }

        boolean previousDifferenceFound = (previousFrame != null);

        if (previousDifferenceFound) {
            previousFrame = getBeginningOfCurrentDifference(previousFrame);
            actualModel.moveToFrame(previousFrame);
        }

        return previousDifferenceFound;
    }

    private LarvaBehaviorMode getDefinedMode(LarvaFrameData frameData) {
        LarvaBehaviorMode mode = frameData.getBehaviorMode();
        if (mode == null) {
            mode = LarvaBehaviorMode.IGNORE;
        }
        return mode;
    }

    private void initExpectedLogFileDirectory() {
        // if an expected model has not already been loaded,
        // init the log directory to the actual model's log directory
        if (! expectedModel.hasFrames()) {
            expectedModel.setLogFileDirectory(
                    actualModel.getLogFileDirectory());
        }
    }

    private void calculateDifferences() {

        similarityPercentage = -1;

        final List<LarvaFrameData> expectedList =
                expectedModel.getFrameDataList();
        final List<LarvaFrameData> actualList =
                actualModel.getFrameDataList();

        if ((expectedList != null) && (actualList != null)) {

            final int expectedSize = expectedList.size();
            final int actualSize = actualList.size();

            differentFrameIndexes = new HashSet<Integer>(expectedSize);

            if ((expectedSize > 0) && (actualSize > 0)) {
                LarvaBehaviorMode expectedMode;
                LarvaBehaviorMode actualMode;
                for (int i = 0; i < expectedSize; i++) {
                    if (i < actualSize) {
                        expectedMode = getDefinedMode(expectedList.get(i));
                        actualMode = getDefinedMode(actualList.get(i));
                        if ((! expectedMode.isEquivalent(actualMode) &&
                             (! LarvaBehaviorMode.IGNORE.equals(actualMode)))) {
                            differentFrameIndexes.add(i);
                        }
                    }
                }
            }

            if (actualSize > 0) {
                final double difference =
                        (double) differentFrameIndexes.size() / actualSize;
                similarityPercentage = 100.0 - (difference * 100.0);
            }

        } else {
            differentFrameIndexes = new HashSet<Integer>();
        }

        firePropertyChange(LOG_MODELS_PROPERTY, null, this);
    }

    private LarvaFrameData getData(LogModel model,
                                   int index) {
        return model.getFrameData(index);
    }

    private LarvaFrameData getActualData(int index) {
        return getData(actualModel, index);
    }

    private LarvaFrameData getExpectedData(int index) {
        return getData(expectedModel, index);
    }

    private boolean modesDiffer(int frameIndex,
                                LarvaBehaviorMode currentExpectedMode,
                                LarvaBehaviorMode currentActualMode) {
        final LarvaBehaviorMode expectedMode =
                getDefinedMode(getExpectedData(frameIndex));
        final LarvaBehaviorMode actualMode =
                getDefinedMode(getActualData(frameIndex));
        return (! expectedMode.isEquivalent(currentExpectedMode)) ||
               (! actualMode.isEquivalent(currentActualMode));
    }

    private int getBeginningOfCurrentDifference(int differenceFrame) {

        final LarvaBehaviorMode expectedMode =
                getDefinedMode(getExpectedData(differenceFrame));
        final LarvaBehaviorMode actualMode =
                getDefinedMode(getActualData(differenceFrame));

        for (int i = differenceFrame - 1; i > -1; i--) {
            if (modesDiffer(i, expectedMode, actualMode)) {
                differenceFrame = i + 1;
                break;
            }
        }

        return differenceFrame;
    }

    private int getEndOfCurrentDifference(int differenceFrame) {

        final int expectedFrameCount = expectedModel.getFrameCount();

        final LarvaBehaviorMode expectedMode =
                getDefinedMode(getExpectedData(differenceFrame));
        final LarvaBehaviorMode actualMode =
                getDefinedMode(getActualData(differenceFrame));

        for (int i = differenceFrame + 1; i < expectedFrameCount; i++) {
            if (modesDiffer(i, expectedMode, actualMode)) {
                differenceFrame = i - 1;
                break;
            }
        }

        return differenceFrame;
    }

    private void synchronizeExpectedWithActual() {
        expectedModel.moveToFrame(actualModel.getCurrentFrame());
    }
}

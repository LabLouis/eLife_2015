/*
 * Copyright (c) 2012 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.venkman.gui.log;

import org.janelia.it.venkman.data.LarvaBehaviorMode;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.math.BigDecimal;

/**
 * A panel component that manages loading expected/annotated data for
 * comparison with actual experimental data.
 *
 * @author Eric Trautman
 */
public class BehaviorModeControlPanel {

    private JCheckBox onlyShowDifferencesCheckBox;

    private JPanel controlPanel;

    private JLabel standardFileName;

    @SuppressWarnings("UnusedDeclaration")
    private JButton selectFileButton;
    @SuppressWarnings("UnusedDeclaration")
    private JButton previousButton;
    @SuppressWarnings("UnusedDeclaration")
    private JButton nextButton;

    @SuppressWarnings("UnusedDeclaration")
    private JPanel modeLegendPanel;
    @SuppressWarnings("UnusedDeclaration")
    private JLabel runLegendLabel;
    @SuppressWarnings("UnusedDeclaration")
    private JLabel stopLegendLabel;
    @SuppressWarnings("UnusedDeclaration")
    private JLabel backLegendLabel;
    @SuppressWarnings("UnusedDeclaration")
    private JLabel castLegendLabel;
    @SuppressWarnings("UnusedDeclaration")
    private JLabel turnLegendLabel;
    @SuppressWarnings("UnusedDeclaration")
    private JLabel sampleLegendLabel;
    @SuppressWarnings("UnusedDeclaration")
    private JLabel ignoreLegendLabel;
    private JLabel similarityLabel;

    private PropertyChangeListener modelChangedListener;
    private BehaviorModeComparisonModel model;

    public BehaviorModeControlPanel() {
        this.modelChangedListener = new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                updateDifferences();
            }
        };

        onlyShowDifferencesCheckBox.setSelected(false);
        standardFileName.setText("");
        similarityLabel.setText("-");
    }

    public void setModel(BehaviorModeComparisonModel model) {
        if (this.model != null) {
            this.model.removePropertyChangeListener(
                    BehaviorModeComparisonModel.LOG_MODELS_PROPERTY,
                    modelChangedListener);
        }

        this.model = model;
        this.model.addPropertyChangeListener(
                BehaviorModeComparisonModel.LOG_MODELS_PROPERTY,
                modelChangedListener);
    }

    private void createUIComponents() {

        selectFileButton = new JButton(
                new AbstractAction("Select File") {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        selectFile();
                    }
                });

        previousButton = new JButton(
                new AbstractAction("Previous") {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        model.moveToPreviousDifference();
                    }
                });
        previousButton.setEnabled(false);

        nextButton = new JButton(
                new AbstractAction("Next") {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        model.moveToNextDifference();
                    }
                });
        nextButton.setEnabled(false);

        onlyShowDifferencesCheckBox = new JCheckBox(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                model.setFilterEnabled(
                        onlyShowDifferencesCheckBox.isSelected());
            }
        });
        onlyShowDifferencesCheckBox.setEnabled(false);

        runLegendLabel = getModeLegendLabel(LarvaBehaviorMode.RUN);
        stopLegendLabel = getModeLegendLabel(LarvaBehaviorMode.STOP);
        backLegendLabel = getModeLegendLabel(LarvaBehaviorMode.BACK_UP);
        castLegendLabel = getModeLegendLabel(LarvaBehaviorMode.CAST_LEFT);
        turnLegendLabel = getModeLegendLabel(LarvaBehaviorMode.TURN_LEFT);
        sampleLegendLabel = getModeLegendLabel(LarvaBehaviorMode.SAMPLING);
        ignoreLegendLabel = getModeLegendLabel(LarvaBehaviorMode.IGNORE);
    }

    public JPanel getControlPanel() {
        return controlPanel;
    }

    private void updateDifferences() {

        final boolean hasExpectedFrames = model.hasExpectedFrames();
        previousButton.setEnabled(hasExpectedFrames);
        nextButton.setEnabled(hasExpectedFrames);
        onlyShowDifferencesCheckBox.setEnabled(hasExpectedFrames);

        final File expectedLogFile = model.getExpectedLogFile();

        if (expectedLogFile != null) {
            standardFileName.setText(expectedLogFile.getName());
        } else {
            standardFileName.setText("");
        }

        final double similarityPercentage = model.getSimilarityPercentage();
        String score;
        if (similarityPercentage < 0) {
            score = "-";
        } else {
            BigDecimal value = new BigDecimal(similarityPercentage);
            value = value.setScale(2, BigDecimal.ROUND_HALF_UP);
            score = value + "%";
        }

        similarityLabel.setText(score);
    }

    private JLabel getModeLegendLabel(LarvaBehaviorMode mode) {
        JLabel label = new JLabel();
        final Color color = BehaviorModePanel.getColor(mode);
        label.setBackground(color);
        label.setOpaque(true);
        if (Color.BLACK.equals(color) ||
            Color.BLUE.equals(color) ||
            Color.DARK_GRAY.equals(color)) {
            label.setForeground(Color.WHITE);
        }
        label.setBorder(new EmptyBorder(3, 3, 3, 3));
        return label;
    }
    
    private void selectFile() {
        JFileChooser fileChooser =
                new JFileChooser(model.getExpectedLogFileDirectory());
        int choice =
                fileChooser.showDialog(SwingUtilities.getRoot(controlPanel),
                                       "Select Standard Log File");

        if (choice == JFileChooser.APPROVE_OPTION) {
            final File selectedFile = fileChooser.getSelectedFile();
            if (selectedFile != null) {
                model.loadExpectedLogFile(selectedFile);
            }
        }
    }
}

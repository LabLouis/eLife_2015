/*
 * Copyright (c) 2014 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.venkman.gui.parameter;

import org.janelia.it.venkman.config.rules.MatrixFile;
import org.janelia.it.venkman.config.rules.OutOfRangeErrorHandlingMethod;
import org.janelia.it.venkman.config.rules.PositionalVariable;
import org.janelia.it.venkman.config.rules.PositionalVariableFunction;
import org.janelia.it.venkman.gui.NarrowOptionPane;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.io.File;
import java.math.BigDecimal;

/**
 * User interface component for positional variable function parameters.
 *
 * @author Eric Trautman
 */
public class PositionalVariableFunctionComponent {
    private JPanel contentPanel;
    @SuppressWarnings("UnusedDeclaration")
    private JLabel maxY;
    @SuppressWarnings("UnusedDeclaration")
    private JLabel maxX;
    @SuppressWarnings("UnusedDeclaration")
    private JPanel viewPanel;
    private ValuesPanel valuesPanel;
    @SuppressWarnings("UnusedDeclaration")
    private JPanel xRangePanel;
    @SuppressWarnings("UnusedDeclaration")
    private JPanel yRangePanel;
    private JButton loadMatrixFileButton;
    @SuppressWarnings("UnusedDeclaration")
    private JPanel loadPanel;
    @SuppressWarnings("UnusedDeclaration")
    private JPanel parameterPanel;
    private JComboBox variableComboBox;
    private JTextField maxXTextField;
    private JTextField maxYTextField;
    private JPanel intensityRangePanel;
    @SuppressWarnings("UnusedDeclaration")
    private JPanel intensityRangeParentPanel;
    @SuppressWarnings("UnusedDeclaration")
    private JLabel positionalRangeErrorHandlingMethodLabel;
    private JComboBox positionalRangeErrorHandlingMethodComboBox;

    private PositionalVariableFunction originalFunction;
    private double[][] modifiedValues;

    public PositionalVariableFunctionComponent(PositionalVariableFunction originalFunction) {

        variableComboBox.setModel(
                new DefaultComboBoxModel(PositionalVariable.getValuesVector()));
        variableComboBox.setSelectedItem(originalFunction.getVariable());

        this.originalFunction = originalFunction;

        maxXTextField.setText(
                String.valueOf((int) originalFunction.getMaximumVariableX()));
        maxYTextField.setText(
                String.valueOf((int) originalFunction.getMaximumVariableY()));

        valuesPanel.updateView(getPositionalVariableFunction());

        positionalRangeErrorHandlingMethodComboBox.setModel(
                new DefaultComboBoxModel(OutOfRangeErrorHandlingMethod.getValuesVector()));
        OutOfRangeErrorHandlingMethod errorHandlingMethod = originalFunction.getPositionRangeErrorHandlingMethod();
        positionalRangeErrorHandlingMethodComboBox.setSelectedItem(errorHandlingMethod);
        positionalRangeErrorHandlingMethodComboBox.setToolTipText(errorHandlingMethod.getDescription());
        positionalRangeErrorHandlingMethodComboBox.addActionListener(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                updatePositionalRangeErrorHandlingMethod();
            }
        });

        loadMatrixFileButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                loadMatrixFile();
            }
        });
    }

    public JPanel getContentPanel() {
        return contentPanel;
    }

    public void setEditable(boolean editable) {
        positionalRangeErrorHandlingMethodComboBox.setEnabled(editable);
        variableComboBox.setEnabled(editable);
        maxXTextField.setEditable(editable);
        maxYTextField.setEditable(editable);
        loadMatrixFileButton.setVisible(editable);
    }

    private int getIntegerValue(JTextField field,
                                int defaultValue) {
        int value;
        try {
            value = Integer.parseInt(field.getText());
        } catch (NumberFormatException e1) {
            NarrowOptionPane.showMessageDialog(
                    contentPanel,
                    "Non-integer value '" + field.getText() +
                    "' specified, resetting to default value of " +
                    defaultValue + ".",
                    "Invalid Parameter",
                    JOptionPane.ERROR_MESSAGE);
            value = defaultValue;
            field.setText(String.valueOf(value));
        }
        return value;
    }

    public PositionalVariableFunction getPositionalVariableFunction() {
        final PositionalVariable variable = (PositionalVariable)
                variableComboBox.getSelectedItem();
        final int maxX =
                getIntegerValue(maxXTextField,
                                PositionalVariableFunction.DEFAULT_TRACKER_ARENA_WIDTH);
        final int maxY =
                getIntegerValue(maxYTextField,
                                PositionalVariableFunction.DEFAULT_TRACKER_ARENA_HEIGHT);
        double[][] values;

        if (modifiedValues == null) {
            values = originalFunction.getValues();
        } else {
            values = modifiedValues;
        }
        return new PositionalVariableFunction(variable,
                                              maxX,
                                              maxY,
                                              originalFunction.getPositionRangeErrorHandlingMethod(),
                                              values);
    }

    private void updatePositionalRangeErrorHandlingMethod() {

        final OutOfRangeErrorHandlingMethod errorHandlingMethod =
                (OutOfRangeErrorHandlingMethod) positionalRangeErrorHandlingMethodComboBox.getSelectedItem();

        if (! errorHandlingMethod.equals(originalFunction.getPositionRangeErrorHandlingMethod())) {
            positionalRangeErrorHandlingMethodComboBox.setToolTipText(errorHandlingMethod.getDescription());
            originalFunction = new PositionalVariableFunction(originalFunction.getVariable(),
                                                              originalFunction.getMaximumVariableX(),
                                                              originalFunction.getMaximumVariableY(),
                                                              errorHandlingMethod,
                                                              originalFunction.getValues());
        }
    }

    private void loadMatrixFile() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileSelectionMode(
                JFileChooser.FILES_ONLY);

        fileChooser.showDialog(contentPanel, "Select Matrix File");
        File selectedFile = fileChooser.getSelectedFile();
        if (selectedFile != null) {
            MatrixFile matrixFile = null;
            try {
                matrixFile = new MatrixFile(selectedFile);
            } catch (IllegalArgumentException e) {
                NarrowOptionPane.showMessageDialog(
                        contentPanel,
                        "The matrix cannot be loaded. " +
                        e.getMessage(),
                        "Failed to Load Matrix",
                        JOptionPane.ERROR_MESSAGE);
            }

            if (matrixFile != null) {
                String errorMessage = null;
                if (matrixFile.getMinimumValue() < -0.0) {
                    errorMessage = "less than 0 (" +
                                   matrixFile.getMinimumValue() + ").  ";
                }else if (matrixFile.getMaximumValue() > 100.0000000001) {
                    errorMessage = "greater than 100 (" +
                                   matrixFile.getMaximumValue() + ").  ";
                }

                if (errorMessage == null) {
                    modifiedValues = matrixFile.getValues();
                    valuesPanel.updateView(getPositionalVariableFunction());
                } else {
                    NarrowOptionPane.showMessageDialog(
                            contentPanel,
                            "The matrix file contains at least one value " +
                            errorMessage +
                            "Please regenerate the matrix so that it only " +
                            "contains values between 0 and 100 inclusive.",
                            "Invalid Matrix Value",
                            JOptionPane.ERROR_MESSAGE);
                }
            }
        }
    }

    private class ValuesPanel
            extends JPanel {

        private Dimension gradientSize;
        private Color[][] gradient;

        public ValuesPanel(Dimension gradientSize,
                           boolean displayToolTips) {
            this.gradientSize = gradientSize;
            setBackground(Color.BLACK);
            if (displayToolTips) {
                setToolTipText("");
            }
        }

        public void updateView(PositionalVariableFunction pvFunction) {
            final double[][] arena =  pvFunction.getArena(gradientSize.width,
                                                          gradientSize.height);
            gradient = new Color[gradientSize.height][gradientSize.width];
            int blue;
            for (int y = 0; y < gradientSize.height; y++) {
                for (int x = 0; x < gradientSize.width; x++) {
                    blue = (int) ((arena[y][x] / 100) * 255);
                    gradient[y][x] = new Color(0, 0, blue);
                }
            }
            repaint();
        }

        @Override
        public Dimension getPreferredSize() {
            return gradientSize;
        }

        @Override
        public Dimension getMinimumSize() {
            return gradientSize;
        }

        @Override
        public Dimension getMaximumSize() {
            return gradientSize;
        }

        @Override
        public String getToolTipText(MouseEvent event) {
            final Point point = event.getPoint();
            final PositionalVariableFunction pvFunction =
                    getPositionalVariableFunction();
            final double x = (point.x * pvFunction.getMaximumVariableX()) /
                             (double) gradientSize.width;
            final double y = (point.y * pvFunction.getMaximumVariableY()) /
                             (double) gradientSize.height;
            final double v = pvFunction.getValue(x, y);
            return "(" + getScaledValue(x) + ", " + getScaledValue(y) +
                   ") has intensity percentage " + getScaledValue(v);
        }

        public void paintComponent(Graphics g) {
            super.paintComponent(g);

            if (gradient != null) {
                Graphics2D g2d = (Graphics2D) g.create();
                for (int y = 0; y < gradientSize.height; y++) {
                    for (int x = 0; x < gradientSize.width; x++) {
                        g2d.setPaint(gradient[y][x]);
                        g2d.drawRect(x, y, 0, 0);
                    }
                }
            }
        }

        private String getScaledValue(double value) {
            BigDecimal bd = new BigDecimal(value);
            bd = bd.setScale(2, BigDecimal.ROUND_HALF_UP);
            return bd.toString();
        }
    }

    private void createUIComponents() {
        valuesPanel = new ValuesPanel(new Dimension(240, 240), true);
        viewPanel = valuesPanel;
        intensityRangePanel = new ValuesPanel(
                new Dimension((int) INTENSITY_RANGE_FUNCTION.getMaximumVariableX(),
                              (int) INTENSITY_RANGE_FUNCTION.getMaximumVariableY()),
                false);
        ((ValuesPanel) intensityRangePanel).updateView(INTENSITY_RANGE_FUNCTION);
    }

    private static final PositionalVariableFunction INTENSITY_RANGE_FUNCTION =
            new PositionalVariableFunction(PositionalVariable.HEAD,
                                           20,
                                           80,
                                           OutOfRangeErrorHandlingMethod.END_SESSION_FOR_MINIMUM_AND_MAXIMUM,
                                           new double[][] {{100},{0}});

}
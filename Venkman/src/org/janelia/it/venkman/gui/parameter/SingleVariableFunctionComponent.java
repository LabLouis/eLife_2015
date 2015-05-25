/*
 * Copyright (c) 2014 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.venkman.gui.parameter;

import org.janelia.it.venkman.config.rules.BehaviorLimitedKinematicVariableFunction;
import org.janelia.it.venkman.config.rules.KinematicVariable;
import org.janelia.it.venkman.config.rules.KinematicVariableFunction;
import org.janelia.it.venkman.config.rules.MatrixFile;
import org.janelia.it.venkman.config.rules.OutOfRangeErrorHandlingMethod;
import org.janelia.it.venkman.config.rules.SingleVariableFunction;
import org.janelia.it.venkman.data.LarvaBehaviorMode;
import org.janelia.it.venkman.gui.NarrowOptionPane;
import org.janelia.it.venkman.gui.VerticalLabelUI;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.io.File;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

/**
 * User interface component for single variable function parameters.
 *
 * @author Eric Trautman
 */
public class SingleVariableFunctionComponent {
    private JPanel contentPanel;
    @SuppressWarnings("UnusedDeclaration")
    private JPanel viewPanel;
    private ValuesPanel valuesPanel;
    @SuppressWarnings("UnusedDeclaration")
    private JPanel inputRangePanel;
    private JLabel inputRangeLabel;
    @SuppressWarnings("UnusedDeclaration")
    private JTextField minInputTextField;
    @SuppressWarnings("UnusedDeclaration")
    private JTextField maxInputTextField;
    private JLabel inputUnitsLabel;
    private JLabel maxOutputLabel;
    private JLabel minOutputLabel;
    @SuppressWarnings("UnusedDeclaration")
    private JPanel outputRangePanel;
    @SuppressWarnings("UnusedDeclaration")
    private JPanel parameterPanel;
    private JLabel outputUnitsLabel;
    private JComboBox variableComboBox;
    private JLabel variableLabel;
    private JButton updateRangeButton;
    private JLabel minInputLabel;
    private JLabel maxInputLabel;
    @SuppressWarnings("UnusedDeclaration")
    private JPanel loadValuesPanel;
    private JButton loadOutputValuesButton;
    private JButton toggleDetailsButton;
    @SuppressWarnings("UnusedDeclaration")
    private JLabel inputRangeErrorHandlingMethodLabel;
    private JComboBox inputRangeErrorHandlingMethodComboBox;
    private JLabel behaviorModesLabel;
    private JPanel behaviorModesPanel;
    @SuppressWarnings("UnusedDeclaration")
    private JCheckBox runCheckBox;
    @SuppressWarnings("UnusedDeclaration")
    private JCheckBox backUpCheckBox;
    @SuppressWarnings("UnusedDeclaration")
    private JCheckBox turnRightCheckBox;
    @SuppressWarnings("UnusedDeclaration")
    private JCheckBox castRightCheckBox;
    @SuppressWarnings("UnusedDeclaration")
    private JCheckBox turnLeftCheckBox;
    @SuppressWarnings("UnusedDeclaration")
    private JCheckBox castLeftCheckBox;
    @SuppressWarnings("UnusedDeclaration")
    private JCheckBox stopCheckBox;
    private JComboBox applyResultComboBox;
    private java.util.List<ModeCheckBox> behaviorModeCheckBoxes;

    private SingleVariableFunction svFunction;
    private DecimalParameter minimumInputParameter;
    private DecimalParameter maximumInputParameter;
    private BigDecimal minimumOutputValue;
    private BigDecimal maximumOutputValue;
    private boolean detailedViewDisplayed;

    public SingleVariableFunctionComponent(SingleVariableFunction originalFunction,
                                           String inputUnits,
                                           DecimalParameter minimumInputParameter,
                                           DecimalParameter maximumInputParameter,
                                           String outputUnits,
                                           BigDecimal minimumOutputValue,
                                           BigDecimal maximumOutputValue) {
        this.svFunction = originalFunction;
        this.minimumInputParameter = minimumInputParameter;
        this.maximumInputParameter = maximumInputParameter;
        this.minimumOutputValue = minimumOutputValue;
        this.maximumOutputValue = maximumOutputValue;
        this.detailedViewDisplayed = false;

        // createUIComponents called here (just before first call to managed component)

        behaviorModesLabel.setVisible(false);
        behaviorModesPanel.setVisible(false);
        variableLabel.setVisible(false);
        variableComboBox.setVisible(false);
        applyResultComboBox.setVisible(false);

        setInputUnits(inputUnits);

        inputRangeErrorHandlingMethodComboBox.setModel(
                new DefaultComboBoxModel(OutOfRangeErrorHandlingMethod.getValuesVector()));
        OutOfRangeErrorHandlingMethod errorHandlingMethod = originalFunction.getInputRangeErrorHandlingMethod();
        inputRangeErrorHandlingMethodComboBox.setSelectedItem(errorHandlingMethod);
        inputRangeErrorHandlingMethodComboBox.setToolTipText(errorHandlingMethod.getDescription());
        inputRangeErrorHandlingMethodComboBox.addActionListener(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                updateInputRangeErrorHandlingMethod();
            }
        });

        outputUnitsLabel.setText(outputUnits);
        outputUnitsLabel.setUI(new VerticalLabelUI(false));

        loadOutputValuesButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                loadOutputValues();
            }
        });

        updateRangeButton.addActionListener(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                updateInputRange();
            }
        });

        updateFunctionView();

        toggleDetailsButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                toggleDetails();
            }
        });
    }

    private void createUIComponents() {
        valuesPanel = new ValuesPanel(DEFAULT_SIZE, true);
        viewPanel = valuesPanel;
        minInputTextField = (JTextField) minimumInputParameter.getComponent();
        maxInputTextField = (JTextField) maximumInputParameter.getComponent();

        behaviorModesPanel = new JPanel();

        behaviorModeCheckBoxes = new ArrayList<ModeCheckBox>();
        runCheckBox = createModeCheckBox(LarvaBehaviorMode.RUN);
        backUpCheckBox = createModeCheckBox(LarvaBehaviorMode.BACK_UP);
        stopCheckBox = createModeCheckBox(LarvaBehaviorMode.STOP);
        turnRightCheckBox = createModeCheckBox(LarvaBehaviorMode.TURN_RIGHT);
        castRightCheckBox = createModeCheckBox(LarvaBehaviorMode.CAST_RIGHT);
        turnLeftCheckBox = createModeCheckBox(LarvaBehaviorMode.TURN_LEFT);
        castLeftCheckBox = createModeCheckBox(LarvaBehaviorMode.CAST_LEFT);

        applyResultComboBox = new JComboBox(new String[] {APPLY_AS_FACTOR, APPLY_AS_ADDEND});
    }

    public SingleVariableFunctionComponent(SingleVariableFunction originalFunction,
                                           String inputUnits,
                                           DecimalParameter minimumInputParameter,
                                           DecimalParameter maximumInputParameter,
                                           String outputUnits,
                                           BigDecimal minimumOutputValue,
                                           BigDecimal maximumOutputValue,
                                           KinematicVariable variable) {
        this(originalFunction,
             inputUnits,
             minimumInputParameter,
             maximumInputParameter,
             outputUnits,
             minimumOutputValue,
             maximumOutputValue);

        variableComboBox.setModel(
                new DefaultComboBoxModel(KinematicVariable.getValuesVector()));
        variableComboBox.setSelectedItem(variable);
        variableComboBox.addActionListener(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                updateVariable(true);
            }
        });

        // probably should find a cleaner way to determine
        // if the function is new or already exists (with a defined range)
        final boolean resetInputRange =
                (svFunction.getMinimumInputValue() ==
                 svFunction.getMaximumInputValue()) &&
                (svFunction.getMinimumOutputValue() ==
                 svFunction.getMaximumOutputValue());
        updateVariable(resetInputRange);

        variableLabel.setVisible(true);
        variableComboBox.setVisible(true);
    }

    public SingleVariableFunctionComponent(SingleVariableFunction originalFunction,
                                           String inputUnits,
                                           DecimalParameter minimumInputParameter,
                                           DecimalParameter maximumInputParameter,
                                           String outputUnits,
                                           BigDecimal minimumOutputValue,
                                           BigDecimal maximumOutputValue,
                                           KinematicVariable variable,
                                           Set<LarvaBehaviorMode> behaviorModes,
                                           boolean isAdditive) {
        this(originalFunction,
             inputUnits,
             minimumInputParameter,
             maximumInputParameter,
             outputUnits,
             minimumOutputValue,
             maximumOutputValue,
             variable);

        if (behaviorModes != null) {
            for (ModeCheckBox checkBox : behaviorModeCheckBoxes) {
                checkBox.setSelected(behaviorModes.contains(checkBox.getMode()));
            }
         }

        this.behaviorModesLabel.setVisible(true);
        this.behaviorModesPanel.setVisible(true);

        if (isAdditive) {
            this.applyResultComboBox.setSelectedItem(APPLY_AS_ADDEND);
        } else {
            this.applyResultComboBox.setSelectedItem(APPLY_AS_FACTOR);
        }

        this.applyResultComboBox.setVisible(true);
    }

    public JPanel getContentPanel() {
        return contentPanel;
    }

    public SingleVariableFunction getSingleVariableFunction() {
        return svFunction;
    }

    public KinematicVariableFunction getKinematicVariableFunction() {
        final KinematicVariable variable = (KinematicVariable) variableComboBox.getSelectedItem();
        return new KinematicVariableFunction(variable,
                                             svFunction.getMinimumInputValue(),
                                             svFunction.getMaximumInputValue(),
                                             svFunction.getInputRangeErrorHandlingMethod(),
                                             svFunction.getValues());
    }

    public BehaviorLimitedKinematicVariableFunction getBehaviorLimitedKinematicVariableFunction() {
        Set<LarvaBehaviorMode> behaviorModes = new HashSet<LarvaBehaviorMode>();
        for (ModeCheckBox checkBox : behaviorModeCheckBoxes) {
            if (checkBox.isSelected()) {
                behaviorModes.add(checkBox.getMode());
            }
        }
        final KinematicVariable variable = (KinematicVariable) variableComboBox.getSelectedItem();
        final boolean isAdditive = APPLY_AS_ADDEND.equals(applyResultComboBox.getSelectedItem());
        return new BehaviorLimitedKinematicVariableFunction(behaviorModes,
                                                            isAdditive,
                                                            variable,
                                                            svFunction.getMinimumInputValue(),
                                                            svFunction.getMaximumInputValue(),
                                                            svFunction.getInputRangeErrorHandlingMethod(),
                                                            svFunction.getValues());
    }

    public void setEditable(boolean editable) {
        minInputTextField.setEditable(editable);
        maxInputTextField.setEditable(editable);
        inputRangeErrorHandlingMethodComboBox.setEnabled(editable);
        if (behaviorModeCheckBoxes != null) {
            for (ModeCheckBox checkBox : behaviorModeCheckBoxes) {
                checkBox.setEnabled(editable);
            }
        }
        variableComboBox.setEnabled(editable);
        updateRangeButton.setVisible(editable);
        loadOutputValuesButton.setVisible(editable);
    }

    private void updateFunctionView() {
        final double minInput = svFunction.getMinimumInputValue();
        minimumInputParameter.setDoubleValue(minInput);
        minInputLabel.setText(String.valueOf(minInput));

        final double maxInput = svFunction.getMaximumInputValue();
        maximumInputParameter.setDoubleValue(maxInput);
        maxInputLabel.setText(String.valueOf(maxInput));

        final double minOutput = svFunction.getMinimumOutputValue();
        minOutputLabel.setText(String.valueOf(minOutput));

        final double maxOutput = svFunction.getMaximumOutputValue();
        maxOutputLabel.setText(String.valueOf(maxOutput));

        valuesPanel.updateView();
    }

    private void updateInputRange() {

        final double previousMin = svFunction.getMinimumInputValue();
        final double currentMin = getParameterValue(minimumInputParameter,
                                                    previousMin);
        final boolean isMinChanged = (previousMin != currentMin);

        final double previousMax = svFunction.getMaximumInputValue();
        final double currentMax = getParameterValue(maximumInputParameter,
                                                    previousMax);
        final boolean isMaxChanged = (previousMax != currentMax);

        if (isMinChanged || isMaxChanged) {

            if (currentMax < currentMin) {

                NarrowOptionPane.showMessageDialog(
                        contentPanel,
                        "Minimum input value must be less than " +
                        "or equal to maximum.  " +
                        "Restoring previous values.",
                        "Invalid Parameter",
                        JOptionPane.ERROR_MESSAGE
                );
                minimumInputParameter.setDoubleValue(previousMin);
                maximumInputParameter.setDoubleValue(previousMax);

            } else {

                svFunction = new SingleVariableFunction(currentMin,
                                                        currentMax,
                                                        svFunction.getInputRangeErrorHandlingMethod(),
                                                        svFunction.getValues());
                updateFunctionView();
            }
        }

    }

    private double getParameterValue(DecimalParameter parameter,
                                     double defaultValue) {
        double value;
        try {
            parameter.validate();
            value = parameter.getDoubleValue();
        } catch (Exception e) {
            NarrowOptionPane.showMessageDialog(
                    contentPanel,
                    e.getMessage() +
                    "  Resetting value to " + defaultValue + ".",
                    "Invalid Parameter",
                    JOptionPane.ERROR_MESSAGE
            );
            parameter.setDoubleValue(defaultValue);
            value = defaultValue;
        }
        return value;
    }

    private void loadOutputValues() {
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
                        JOptionPane.ERROR_MESSAGE
                );
            }

            if (matrixFile != null) {
                String errorMessage = null;
                double[][] fileValues = matrixFile.getValues();
                double[] modifiedValues = null;
                if (matrixFile.getNumberOfColumns() == 1) {
                    modifiedValues = new double[fileValues.length];
                    for (int i = 0; i < fileValues.length; i++) {
                        modifiedValues[i] = fileValues[i][0];
                    }
                } else if (matrixFile.getNumberOfRows() == 1) {
                    modifiedValues = fileValues[0];
                } else {
                    errorMessage = "The matrix file contains " +
                                   matrixFile.getNumberOfRows() + " rows and " +
                                   matrixFile.getNumberOfColumns() +
                                   " columns.  Please regenerate the matrix " +
                                   "so that it only contains 1 row or " +
                                   "1 column.";
                }

                if (errorMessage == null) {
                    checkLoadedOutputRange(matrixFile);
                }

                if (modifiedValues != null) {

                    svFunction = new SingleVariableFunction(
                            svFunction.getMinimumInputValue(),
                            svFunction.getMaximumInputValue(),
                            svFunction.getInputRangeErrorHandlingMethod(),
                            modifiedValues);
                    updateFunctionView();

                } else {

                    NarrowOptionPane.showMessageDialog(
                            contentPanel,
                            errorMessage,
                            "Invalid Matrix Value",
                            JOptionPane.ERROR_MESSAGE);
                }
            }
        }
    }

    private void checkLoadedOutputRange(MatrixFile matrixFile) {

        StringBuilder sb = new StringBuilder();

        if (minimumOutputValue != null) {
            if (matrixFile.getMinimumValue() <
                minimumOutputValue.doubleValue()) {
                sb.append("The loaded minimum output value ");
                sb.append(matrixFile.getMinimumValue());
                sb.append(" is less than the expected minimum of ");
                sb.append(minimumOutputValue);
                sb.append(".  ");
            }
        }

        if (maximumOutputValue != null) {
            if (matrixFile.getMaximumValue() >
                maximumOutputValue.doubleValue()) {
                sb.append("The loaded maximum output value ");
                sb.append(matrixFile.getMaximumValue());
                sb.append(" is greater than the expected maximum of ");
                sb.append(maximumOutputValue);
                sb.append(".  ");
            }
        }

        if (sb.length() > 0) {
            sb.append("Please confirm the values are accurate before saving.");
            NarrowOptionPane.showMessageDialog(
                    contentPanel,
                    sb.toString(),
                    "Loaded Values Outside Expected Range",
                    JOptionPane.WARNING_MESSAGE);
        }

    }

    private void setInputUnits(String inputUnits) {
        inputUnitsLabel.setText(inputUnits);
        if ((inputUnits == null) || (inputUnits.length() == 0)) {
            inputRangeLabel.setText("Input Range:");
        } else {
            inputRangeLabel.setText("Input Range (" + inputUnits + "):");
        }
    }

    private void updateInputRangeErrorHandlingMethod() {

        final OutOfRangeErrorHandlingMethod errorHandlingMethod =
                (OutOfRangeErrorHandlingMethod) inputRangeErrorHandlingMethodComboBox.getSelectedItem();

        if (!errorHandlingMethod.equals(svFunction.getInputRangeErrorHandlingMethod())) {
            inputRangeErrorHandlingMethodComboBox.setToolTipText(errorHandlingMethod.getDescription());
            svFunction = new SingleVariableFunction(svFunction.getMinimumInputValue(),
                                                    svFunction.getMaximumInputValue(),
                                                    errorHandlingMethod,
                                                    svFunction.getValues());
        }
    }

    private void updateVariable(boolean resetRange) {
        KinematicVariable variable = (KinematicVariable)
                variableComboBox.getSelectedItem();
        setInputUnits(variable.getUnits());
        if (resetRange) {
            minimumInputParameter.setDoubleValue(variable.getDefaultMinimum());
            maximumInputParameter.setDoubleValue(variable.getDefaultMaximum());
            updateInputRange();
        }
    }

    private class ValuesPanel
            extends JPanel {

        private Dimension viewSize;
        private double[] viewOutputValues;

        public ValuesPanel(Dimension viewSize,
                           boolean displayToolTips) {
            this.viewSize = viewSize;
            setBackground(Color.WHITE);
            if (displayToolTips) {
                setToolTipText("");
            }

//            this.addMouseListener(new MouseAdapter() {
//                @Override
//                public void mouseClicked(MouseEvent e) {
//                    super.mouseClicked(e);
//                    if (e.getClickCount() == 2) {
//                        displayDetails();
//                    }
//                }
//            });
        }

        public void updateView() {
            SingleVariableFunction viewInputFunction =
                    new SingleVariableFunction(0,
                                               viewSize.width,
                                               svFunction.getInputRangeErrorHandlingMethod(),
                                               svFunction.getValues());
            final double outputMin = svFunction.getMinimumOutputValue();
            final double outputRange =
                    svFunction.getMaximumOutputValue() - outputMin;
            viewOutputValues = new double[viewSize.width];

            double viewOutput;
            if (outputRange > 0) {

                double actualOutput;
                for (int x = 0; x < viewOutputValues.length; x++) {
                    actualOutput = viewInputFunction.getValue(x);
                    viewOutput = ((actualOutput - outputMin) / outputRange) *
                                 viewSize.height;
                    // flip so that 0 is on bottom
                    viewOutputValues[x] = viewSize.height - viewOutput;
                }

            } else {
                // draw straight line across middle of view
                viewOutput = (double) viewSize.height / (double) 2;
                for (int x = 0; x < viewOutputValues.length; x++) {
                    viewOutputValues[x] = viewOutput;
                }

            }

            repaint();
        }

        @Override
        public Dimension getPreferredSize() {
            return viewSize;
        }

        @Override
        public Dimension getMinimumSize() {
            return viewSize;
        }

        @Override
        public Dimension getMaximumSize() {
            return viewSize;
        }

        @Override
        public String getToolTipText(MouseEvent event) {
            final Point point = event.getPoint();
            final double actualInputMin = svFunction.getMinimumInputValue();
            final double viewScalingFactor =
                    (svFunction.getMaximumInputValue() - actualInputMin) /
                    viewSize.width;
            final double actualInputValue =
                    actualInputMin + ((double) point.x * viewScalingFactor);
            final double actualOutputValue =
                    svFunction.getValue(actualInputValue);
            return "input value " + formatValue(actualInputValue) +
                   " has result " + formatValue(actualOutputValue);
        }

        public void paintComponent(Graphics g) {
            super.paintComponent(g);

            if (viewOutputValues != null) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setPaint(Color.BLUE);
                for (int x = 1; x < viewSize.width; x++) {
                    g2d.drawLine(x - 1, (int) viewOutputValues[x - 1],
                                 x, (int) viewOutputValues[x]);
                }
            }
        }

        private String formatValue(double value) {
            BigDecimal bd = new BigDecimal(value);
            bd = bd.setScale(2, BigDecimal.ROUND_HALF_UP);
            return bd.toString();
        }
    }

    private ModeCheckBox createModeCheckBox(LarvaBehaviorMode mode) {
        ModeCheckBox checkBox = new ModeCheckBox(mode);
        this.behaviorModeCheckBoxes.add(checkBox);
        this.behaviorModesPanel.add(checkBox);
        return checkBox;
    }

    private class ModeCheckBox
            extends JCheckBox {

        private LarvaBehaviorMode mode;

        private ModeCheckBox(LarvaBehaviorMode mode) {
            super(mode.getName());
            this.mode = mode;
        }

        public LarvaBehaviorMode getMode() {
            return mode;
        }
    }

    private void toggleDetails() {

        if (detailedViewDisplayed) {

            detailedViewDisplayed = false;
            valuesPanel.viewSize = DEFAULT_SIZE;
            valuesPanel.updateView();
            parameterPanel.setVisible(true);
            toggleDetailsButton.setText("+");
            toggleDetailsButton.setToolTipText("expand function view (hides range parameters)");

        } else {

            detailedViewDisplayed = true;

            final int detailsWidth = DEFAULT_SIZE.width + parameterPanel.getWidth() -
                                     (toggleDetailsButton.getWidth() * 2);

            valuesPanel.viewSize = new Dimension(detailsWidth, DEFAULT_SIZE.height);
            valuesPanel.updateView();
            parameterPanel.setVisible(false);
            toggleDetailsButton.setText("-");
            toggleDetailsButton.setToolTipText("contract function view (shows range parameters)");


        }

    }

    private static final Dimension DEFAULT_SIZE = new Dimension(240, 240);
    private static final String APPLY_AS_ADDEND = "addend";
    private static final String APPLY_AS_FACTOR = "factor";
}

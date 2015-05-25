/*
 * Copyright (c) 2012 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.venkman.gui.parameter;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.lang.reflect.Field;

/**
 * User interface for boolean parameters.
 *
 * @author Eric Trautman
 */
public class BooleanParameter extends AbstractExperimentParameter {

    private Boolean value;
    private JPanel component;
    private JRadioButton yesButton;
    private JRadioButton noButton;


    public BooleanParameter(String displayName,
                            boolean required,
                            Field dataField) {

        super(displayName, required, dataField);

        this.component = new JPanel(new FlowLayout(FlowLayout.LEFT));

        this.yesButton = new JRadioButton(new AbstractAction("Yes") {
            @Override
            public void actionPerformed(ActionEvent e) {
                setValueAfterClick(true);
            }
        });
        this.yesButton.setMnemonic(KeyEvent.VK_Y);
        this.component.add(this.yesButton);

        this.noButton = new JRadioButton(new AbstractAction("No") {
            @Override
            public void actionPerformed(ActionEvent e) {
                setValueAfterClick(false);
            }
        });
        this.noButton.setMnemonic(KeyEvent.VK_N);
        this.component.add(this.noButton);

        ButtonGroup group = new ButtonGroup();
        group.add(this.yesButton);
        group.add(this.noButton);
    }

    @Override
    public Component getReadOnlyComponent() {
        String displayValue;
        if (value == null)  {
            displayValue = "";
        } else {
            displayValue = value.toString();
        }
        return new JLabel(displayValue);
    }

    @Override
    public Component getComponent() {
        return component;
    }

    @Override
    public void validate() throws IllegalArgumentException {
        if (isRequired() && (value == null)) {
            throw new IllegalArgumentException(
                    "The " + getDisplayName() + " parameter must be defined.");
        }
    }

    @Override
    public void applyValue(Object object)
            throws IllegalArgumentException, IllegalAccessException {
        validate();
        Field dataField = getDataField();
        dataField.setBoolean(object, getValue());
    }

    public Boolean getValue() {
        return value;
    }

    public void setValue(Boolean value) {
        this.value = value;
        if (Boolean.TRUE.equals(value)) {
            this.yesButton.setSelected(true);
        } else {
            this.noButton.setSelected(true);
        }
    }
    private void setValueAfterClick(boolean flag) {
        this.value = flag;
    }
}
/*
 * Copyright (c) 2012 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.venkman.gui.parameter;

import javax.swing.*;
import java.awt.*;
import java.lang.reflect.Field;

/**
 * Base user interface for parameters that can be required.
 *
 * @author Eric Trautman
 */
public class VerifiedParameter extends AbstractExperimentParameter {

    private JTextField textField;

    public VerifiedParameter(String displayName,
                             boolean required,
                             Field dataField) {

        super(displayName, required, dataField);
        this.textField = new JTextField();
        final Dimension preferredSize = this.textField.getPreferredSize();
        this.textField.setPreferredSize(new Dimension(120,
                                                      preferredSize.height));
        this.textField.setHorizontalAlignment(JTextField.RIGHT);
    }

    @Override
    public Component getReadOnlyComponent() {
        return new JLabel(getValue());
    }

    @Override
    public Component getComponent() {
        return textField;
    }

    @Override
    public void validate() throws IllegalArgumentException {
        if (isRequired()) {
            String value = getValue();
            if ((value == null) || (value.trim().length() == 0)) {
                throw new IllegalArgumentException(
                        getErrorMessagePrefix() + "must be defined.");
            }
        }
    }

    @Override
    public void applyValue(Object object)
            throws IllegalArgumentException, IllegalAccessException {
        validate();
        Field dataField = getDataField();
        dataField.set(object, getValue());
    }

    public String getValue() {
        return this.textField.getText();
    }

    public void setValue(String value)
            throws IllegalArgumentException {
        this.textField.setText(value);
    }

    public String getErrorMessagePrefix() {
        return "The " + getDisplayName() + " parameter ";
    }
}

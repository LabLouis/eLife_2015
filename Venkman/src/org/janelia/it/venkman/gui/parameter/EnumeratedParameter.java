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
import java.util.HashMap;
import java.util.Map;

/**
 * Experiment parameter constrained by an enumerated list of values.
 *
 * @author Eric Trautman
 */
public class EnumeratedParameter
        extends AbstractExperimentParameter {

    private JComboBox comboBox;
    private Map<String, Object> valueToEnumConstantMap;

    public EnumeratedParameter(String displayName,
                               boolean required,
                               Field dataField) {

        super(displayName, required, dataField);

        this.valueToEnumConstantMap = new HashMap<String, Object>();

        final Class type = dataField.getType();
        final Object[] constants = type.getEnumConstants();
        final int numValues = constants.length;
        String[] values = new String[numValues];
        for (int i = 0; i < numValues; i++) {
            values[i] = String.valueOf(constants[i]);
            this.valueToEnumConstantMap.put(values[i], constants[i]);
        }
        this.comboBox = new JComboBox(values);
    }

    @Override
    public Component getReadOnlyComponent() {
        return new JLabel(getValue());
    }

    @Override
    public Component getComponent() {
        return comboBox;
    }

    @Override
    public void validate() throws IllegalArgumentException {
        // nothing to validate
    }

    @Override
    public void applyValue(Object object)
            throws IllegalArgumentException, IllegalAccessException {
        validate();
        final String value = getValue();
        final Object enumConstant = valueToEnumConstantMap.get(value);
        Field dataField = getDataField();
        dataField.set(object, enumConstant);
    }

    public String getValue() {
        return (String) comboBox.getSelectedItem();
    }

    public void setValue(String value) {
        comboBox.setSelectedItem(value);
    }
}
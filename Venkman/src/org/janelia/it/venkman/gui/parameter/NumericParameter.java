/*
 * Copyright (c) 2011 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.venkman.gui.parameter;

import java.lang.reflect.Field;

/**
 * User interface for numeric (long) parameters.
 *
 * @author Eric Trautman
 */
public class NumericParameter
        extends RangeParameter<Long> {

    public NumericParameter(String displayName,
                            boolean required,
                            Field dataField,
                            Long minimum,
                            Long maximum) {
        super(displayName, required, dataField, minimum, maximum);
    }

    @Override
    public void applyValue(Object object)
            throws IllegalArgumentException, IllegalAccessException {
        validate();
        Field dataField = getDataField();
        final Class type = dataField.getType();
        final Long longValue = getLongValue();
        if ((type == long.class) || (type == Long.class)) {
            dataField.setLong(object, longValue);

        } else if ((type == int.class) || (type == Integer.class)) {
            dataField.setInt(object, longValue.intValue());
        }
    }

    @Override
    public Long getValueOf(String valueStr)
            throws IllegalArgumentException {

        Long value = null;

        if (valueStr != null) {

            String trimmedValueStr = valueStr.trim();
            if (trimmedValueStr.length() > 0) {

                try {
                    value = Long.parseLong(trimmedValueStr);
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException(
                            getErrorMessagePrefix() + "should contain " +
                            getValueName(), e);
                }
            }
        }

        return value;
    }

    @Override
    public String getValueName() {
        return "a numeric value";
    }

    public Long getLongValue() {
        return getValueOf(getValue());
    }

}
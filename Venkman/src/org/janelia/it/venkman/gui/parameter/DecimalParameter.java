/*
 * Copyright (c) 2011 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.venkman.gui.parameter;

import java.lang.reflect.Field;
import java.math.BigDecimal;

/**
 * User interface for decimal (double) parameters.
 *
 * @author Eric Trautman
 */
public class DecimalParameter
        extends RangeParameter<BigDecimal> {

    public DecimalParameter(String displayName,
                            boolean required,
                            Field dataField,
                            BigDecimal minimum,
                            BigDecimal maximum) {
        super(displayName, required, dataField, minimum, maximum);
    }

    @Override
    public void applyValue(Object object)
            throws IllegalArgumentException, IllegalAccessException {
        validate();
        Field dataField = getDataField();
        dataField.setDouble(object, getDoubleValue());
    }

    @Override
    public BigDecimal getValueOf(String valueStr)
            throws IllegalArgumentException {

        BigDecimal value = null;

        if (valueStr != null) {

            String trimmedValueStr = valueStr.trim();
            if (trimmedValueStr.length() > 0) {

                try {
                    value = new BigDecimal(valueStr);
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
        return "a decimal value";
    }

    public Double getDoubleValue() throws IllegalArgumentException {
        Double dValue = null;
        BigDecimal bdValue = getValueOf(getValue());
        if (bdValue != null) {
            dValue = bdValue.doubleValue();
        }
        return dValue;
    }

    public void setDoubleValue(double value) {
        super.setValue(String.valueOf(value));
    }

}
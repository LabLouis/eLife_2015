/*
 * Copyright (c) 2011 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.venkman.gui.parameter;

import java.lang.reflect.Field;

/**
 * Base user interface for parameters scoped with a range.
 *
 * @author Eric Trautman
 */
public abstract class RangeParameter<T extends Comparable<T>>
        extends VerifiedParameter {

    private T minimum;
    private T maximum;

    protected RangeParameter(String displayName,
                             boolean required,
                             Field dataField,
                             T minimum,
                             T maximum) {
        super(displayName, required, dataField);
        this.minimum = minimum;
        this.maximum = maximum;
    }

    @Override
    public void validate()
            throws IllegalArgumentException {

        super.validate();

        String valueString = getValue();
        if (valueString != null) {

            valueString = valueString.trim();
            if (valueString.length() > 0) {

                T value = getValueOf(valueString);
                if ((minimum != null) && (value.compareTo(minimum) < 0)) {
                    throwMinMaxError();
                } else if ((maximum != null) &&
                           (value.compareTo(maximum) > 0)) {
                    throwMinMaxError();
                }

            }
        }

    }

    public T getMinimum() {
        return minimum;
    }

    public T getMaximum() {
        return maximum;
    }

    /**
     * Converts the specified value string into a object than can be
     * compared to range constraints.
     *
     * @param  valueStr  string to convert.
     *
     * @return an object that can be compared to range constraints.
     *
     * @throws IllegalArgumentException
     *   if the value string cannot be converted.
     */
    public abstract T getValueOf(String valueStr)
            throws IllegalArgumentException;

    /**
     * @return a name (e.g. "an integer value") for the type of values
     *         verified by this model.  This name is used in verfication
     *         error messages.
     */
    public abstract String getValueName();

    private void throwMinMaxError() throws IllegalArgumentException {
        String msg = null;

        if (minimum != null) {
            if (maximum != null) {
                msg = getErrorMessagePrefix() + "should contain " +
                      getValueName() + " that is between " +
                      minimum + " and " + maximum + ".";
            } else {
                msg = getErrorMessagePrefix() + "should contain " +
                      getValueName() + " that is greater than or equal to " +
                      minimum + ".";
            }
        } else if (maximum != null) {
            msg = getErrorMessagePrefix() + "should contain " +
                  getValueName() + " that is less than or equal to " +
                  maximum + ".";
        }

        throw new IllegalArgumentException(msg);
    }
}
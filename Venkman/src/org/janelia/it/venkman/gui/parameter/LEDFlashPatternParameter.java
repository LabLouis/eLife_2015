/*
 * Copyright (c) 2013 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.venkman.gui.parameter;

import org.janelia.it.venkman.config.rules.LEDFlashPattern;

import java.lang.reflect.Field;

/**
 * User interface for {@link LEDFlashPattern} parameters.
 *
 * @author Eric Trautman
 */
public class LEDFlashPatternParameter
        extends VerifiedParameter {

    private LEDFlashPattern validatedPattern;

    public LEDFlashPatternParameter(String displayName,
                                    boolean required,
                                    Field dataField) {
        super(displayName, required, dataField);
    }

    @Override
    public void validate() throws IllegalArgumentException {
        super.validate();
        validatedPattern = new LEDFlashPattern(getValue());
    }

    @Override
    public void applyValue(Object object)
            throws IllegalArgumentException, IllegalAccessException {
        validate();
        Field dataField = getDataField();
        dataField.set(object, validatedPattern);
    }
}
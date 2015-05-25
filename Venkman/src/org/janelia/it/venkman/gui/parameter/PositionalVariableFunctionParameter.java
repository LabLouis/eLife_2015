/*
 * Copyright (c) 2012 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.venkman.gui.parameter;

import org.janelia.it.venkman.config.rules.PositionalVariableFunction;

import java.awt.*;
import java.lang.reflect.Field;

/**
 * User interface for positional variable function parameters.
 *
 * @author Eric Trautman
 */
public class PositionalVariableFunctionParameter
        extends AbstractExperimentParameter {

    private PositionalVariableFunctionComponent component;

    public PositionalVariableFunctionParameter(String displayName,
                                               boolean required,
                                               Field dataField,
                                               PositionalVariableFunction originalFunction) {

        super(displayName, required, dataField);

        this.component =
                new PositionalVariableFunctionComponent(originalFunction);
    }

    @Override
    public Component getReadOnlyComponent() {
        component.setEditable(false);
        return getComponent();
    }

    @Override
    public Component getComponent() {
        return component.getContentPanel();
    }

    @Override
    public void validate() throws IllegalArgumentException {
        // nothing to check
    }

    @Override
    public void applyValue(Object object)
            throws IllegalArgumentException, IllegalAccessException {
        validate();
        Field dataField = getDataField();
        dataField.set(object, component.getPositionalVariableFunction());
    }

}
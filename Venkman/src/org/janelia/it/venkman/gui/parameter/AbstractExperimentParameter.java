/*
 * Copyright (c) 2011 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.venkman.gui.parameter;

import java.lang.reflect.Field;

/**
 * Provides common implementation for {@link ExperimentParameter} instances.
 *
 * @author Eric Trautman
 */
public abstract class AbstractExperimentParameter
        implements ExperimentParameter {

    private String displayName;
    private boolean isRequired;
    private Field dataField;

    public AbstractExperimentParameter(String displayName,
                                       boolean required,
                                       Field dataField) {
        this.displayName = displayName;
        this.isRequired = required;
        this.dataField = dataField;
    }

    @Override
    public String getDisplayName() {
        return displayName;
    }

    @Override
    public boolean isRequired() {
        return isRequired;
    }

    @Override
    public Field getDataField() {
        return dataField;
    }
}
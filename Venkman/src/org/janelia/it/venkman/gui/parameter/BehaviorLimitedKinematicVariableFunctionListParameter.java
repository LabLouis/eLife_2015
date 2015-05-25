/*
 * Copyright (c) 2014 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.venkman.gui.parameter;

import org.janelia.it.venkman.config.rules.BehaviorLimitedKinematicVariableFunctionList;

import java.awt.*;
import java.lang.reflect.Field;

/**
 * User interface for {@link BehaviorLimitedKinematicVariableFunctionList} objects.
 *
 * @author Eric Trautman
 */
public class BehaviorLimitedKinematicVariableFunctionListParameter
        extends AbstractExperimentParameter {

    private BehaviorLimitedKinematicVariableFunctionListComponent component;

    /**
     * Constructs a parameter.
     *
     * @param  displayName         the display name for the list.
     * @param  required            indicates whether the list must have at least one element.
     * @param  dataField           the annotated data field for the list.
     * @param  listItemBaseName    base name (e.g. 'Filter') for each element in the list.
     *                             Used by UI component to name elements (e.g. 'Filter 1', 'Filter 2', ...).
     * @param  originalList        the original list to display.
     */
    public BehaviorLimitedKinematicVariableFunctionListParameter(String displayName,
                                                                 boolean required,
                                                                 Field dataField,
                                                                 String listItemBaseName,
                                                                 BehaviorLimitedKinematicVariableFunctionList originalList) {

        super(displayName, required, dataField);

        this.component = new BehaviorLimitedKinematicVariableFunctionListComponent(listItemBaseName,
                                                                                   originalList);
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
        if (isRequired() && (! component.hasAtLeastOneConfiguredFunction())) {
            throw new IllegalArgumentException(
                    "The " + getDisplayName() + " parameter must contain at least one function.");
        }
        component.validateFunctions(getDisplayName());
    }

    @Override
    public void applyValue(Object object)
            throws IllegalArgumentException, IllegalAccessException {
        validate();
        Field dataField = getDataField();
        Object value = component.getModifiedList();
        dataField.set(object, value);
    }

}
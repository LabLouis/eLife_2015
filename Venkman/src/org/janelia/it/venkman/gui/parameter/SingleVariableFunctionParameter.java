/*
 * Copyright (c) 2014 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.venkman.gui.parameter;

import org.janelia.it.venkman.config.rules.BehaviorLimitedKinematicVariableFunction;
import org.janelia.it.venkman.config.rules.KinematicVariableFunction;
import org.janelia.it.venkman.config.rules.SingleVariableFunction;

import java.awt.*;
import java.lang.reflect.Field;
import java.math.BigDecimal;

/**
 * User interface for {@link SingleVariableFunction} objects.
 *
 * @author Eric Trautman
 */
public class SingleVariableFunctionParameter
        extends AbstractExperimentParameter {

    private Class functionClass;
    private SingleVariableFunctionComponent component;

    public SingleVariableFunctionParameter(String displayName,
                                           boolean required,
                                           Field dataField,
                                           SingleVariableFunction originalFunction,
                                           String inputUnits,
                                           BigDecimal minimumInputValue,
                                           BigDecimal maximumInputValue,
                                           String outputUnits,
                                           BigDecimal minimumOutputValue,
                                           BigDecimal maximumOutputValue) {

        super(displayName, required, dataField);

        DecimalParameter min = new DecimalParameter("Minimum Input Value",
                                                    true,
                                                    null, // don't call apply!
                                                    minimumInputValue,
                                                    maximumInputValue);

        DecimalParameter max = new DecimalParameter("Maximum Input Value",
                                                    true,
                                                    null, // don't call apply!
                                                    minimumInputValue,
                                                    maximumInputValue);

        if (originalFunction instanceof BehaviorLimitedKinematicVariableFunction) {
            this.functionClass = BehaviorLimitedKinematicVariableFunction.class;
            final BehaviorLimitedKinematicVariableFunction f =
                    (BehaviorLimitedKinematicVariableFunction) originalFunction;
            this.component = new SingleVariableFunctionComponent(originalFunction,
                                                                 inputUnits,
                                                                 min,
                                                                 max,
                                                                 outputUnits,
                                                                 minimumOutputValue,
                                                                 maximumOutputValue,
                                                                 f.getVariable(),
                                                                 f.getBehaviorModes(),
                                                                 f.isAdditive());

        } else if (originalFunction instanceof KinematicVariableFunction) {
            this.functionClass = KinematicVariableFunction.class;
            final KinematicVariableFunction f = (KinematicVariableFunction) originalFunction;
            this.component = new SingleVariableFunctionComponent(originalFunction,
                                                                 inputUnits,
                                                                 min,
                                                                 max,
                                                                 outputUnits,
                                                                 minimumOutputValue,
                                                                 maximumOutputValue,
                                                                 f.getVariable());

        } else {
            this.functionClass = SingleVariableFunction.class;
            this.component = new SingleVariableFunctionComponent(originalFunction,
                                                                 inputUnits,
                                                                 min,
                                                                 max,
                                                                 outputUnits,
                                                                 minimumOutputValue,
                                                                 maximumOutputValue);
        }
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
        Object value;
        if (this.functionClass.equals(BehaviorLimitedKinematicVariableFunction.class)) {
            value = component.getBehaviorLimitedKinematicVariableFunction();
        } else if (this.functionClass.equals(KinematicVariableFunction.class)) {
            value = component.getKinematicVariableFunction();
        } else {
            value = component.getSingleVariableFunction();
        }
        dataField.set(object, value);
    }

}
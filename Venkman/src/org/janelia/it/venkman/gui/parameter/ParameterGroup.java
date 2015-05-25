/*
 * Copyright (c) 2014 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.venkman.gui.parameter;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import org.janelia.it.venkman.config.rules.BehaviorLimitedKinematicVariableFunction;
import org.janelia.it.venkman.config.rules.BehaviorLimitedKinematicVariableFunctionList;
import org.janelia.it.venkman.config.rules.IntensityValue;
import org.janelia.it.venkman.config.rules.KinematicVariableFunction;
import org.janelia.it.venkman.config.rules.LEDFlashPattern;
import org.janelia.it.venkman.config.rules.PositionalVariableFunction;
import org.janelia.it.venkman.config.rules.SingleVariableFunction;
import org.janelia.it.venkman.gui.NarrowOptionPane;
import org.janelia.it.venkman.gui.parameter.annotation.VenkmanParameter;
import org.janelia.it.venkman.gui.parameter.annotation.VenkmanParameterFilter;

import javax.swing.*;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Group of user interface parameters for a behavior or stimulus configuration.
 * Examines the behavior/stimulus class implementation's annotations to build
 * the appropriate user interface.
 *
 * @author Eric Trautman
 */
public class ParameterGroup<T> {

    private List<ExperimentParameter> parameterList;
    private T data;
    private JPanel contentPanel;

    public ParameterGroup(T data)
            throws IllegalArgumentException {

        this.parameterList = new ArrayList<ExperimentParameter>();
        this.data = data;

        Set<String> hiddenParameters = new HashSet<String>();
        VenkmanParameterFilter filter;
        Class<?> clazz = data.getClass();
        List<Class<?>> classList = new ArrayList<Class<?>>();
        while (clazz != null ) {

            filter = clazz.getAnnotation(VenkmanParameterFilter.class);
            if (filter != null) {
                hiddenParameters.addAll(
                        Arrays.asList(filter.hiddenParameters()));
            }

            classList.add(clazz);
            clazz = clazz.getSuperclass();
        }

        List<SortableAnnotationField> annotationFieldList = new ArrayList<SortableAnnotationField>();
        VenkmanParameter annotation;
        // reverse loop order so that super class parameters are added first by default
        for (int i = classList.size(); i > 0; i--) {
            clazz = classList.get(i-1);
            for (Field f : clazz.getDeclaredFields()) {
                annotation = f.getAnnotation(VenkmanParameter.class);
                if ((annotation != null) && (! hiddenParameters.contains(f.getName()))) {
                    annotationFieldList.add(new SortableAnnotationField(annotation, f));
                }
            }
        }

        // sort list by annotated display order (to override default class order as needed)
        Collections.sort(annotationFieldList);

        // add each field (in the desired order) to the user interface
        for (SortableAnnotationField annotationField : annotationFieldList) {
            try {
                addParameter(annotationField.field, annotationField.annotation);
            } catch (Exception e) {
                final String fieldName = annotationField.field.getName();
                final String className = annotationField.field.getClass().getName();
                throw new IllegalArgumentException(
                        "Failed to add parameter for field " +fieldName + " in class " + className,
                        e);
            }
         }
    }

    public T getData() {
        return data;
    }

    public JPanel getEditableContentPanel() {
        return getContentPanel(true);
    }

    public JPanel getContentPanel(boolean isEditable) {
        if (contentPanel == null) {
            FormLayout layout = new FormLayout(
                    "left:max(40dlu;pref), 4dlu, left:pref:none",  // column layout
                    "");                                           // row layout
            DefaultFormBuilder builder = new DefaultFormBuilder(layout);
//            builder.setDefaultRowSpec(RowSpec.decode("top:pref:none"));

            builder.setVAlignment(CellConstraints.TOP);

            if (parameterList.size() > 0) {

                for (ExperimentParameter parameter : parameterList) {
                    if (isEditable) {
                        builder.append(parameter.getDisplayName() + ":",
                                       parameter.getComponent());
                    } else {
                        builder.append(parameter.getDisplayName() + ":",
                                       parameter.getReadOnlyComponent());
                    }
                }
                builder.nextLine();

            } else {
                builder.append("There are no parameters to configure.");
            }

            contentPanel = builder.getPanel();
        }
        return contentPanel;
    }

    public boolean validate() {

        boolean allParametersValid = false;

        ExperimentParameter parameter = null;
        try {

            for (ExperimentParameter p : parameterList) {
                parameter = p;
                parameter.validate();
            }

            allParametersValid = true;

        } catch (IllegalArgumentException e) {
            if (parameter != null) {
                NarrowOptionPane.showMessageDialog(contentPanel,
                                                   e.getMessage(),
                                                   "Invalid Parameter",
                                                   JOptionPane.ERROR_MESSAGE);
                parameter.getComponent().requestFocus();
            }
        }

        return allParametersValid;
    }

    public void applyParameters()
            throws IllegalArgumentException, IllegalAccessException {
        for (ExperimentParameter parameter : parameterList) {
            parameter.applyValue(data);
        }
    }

    protected List<ExperimentParameter> getParameterList() {
        return parameterList;
    }

    private void addParameter(Field field,
                              VenkmanParameter annotation)
            throws IllegalAccessException, NumberFormatException {

        field.setAccessible(true);

        ExperimentParameter parameter = null;

        final Class type = field.getType();
        if ((type == double.class) || (type == Double.class)) {

            DecimalParameter p = new DecimalParameter(
                    annotation.displayName(),
                    annotation.required(),
                    field,
                    getBigDecimalValue(annotation.minimum()),
                    getBigDecimalValue(annotation.maximum()));
            double v = field.getDouble(data);
            p.setDoubleValue(v);
            parameter = p;

        } else if ((type == long.class) || (type == Long.class) ||
                   (type == int.class) || (type == Integer.class)) {

            NumericParameter p = new NumericParameter(
                    annotation.displayName(),
                    annotation.required(),
                    field,
                    getLongValue(annotation.minimum()),
                    getLongValue(annotation.maximum()));
            long v = field.getLong(data);
            p.setValue(String.valueOf(v));
            parameter = p;

        } else if (type == boolean.class) {

            BooleanParameter p = new BooleanParameter(
                    annotation.displayName(),
                    annotation.required(),
                    field);
            boolean v = field.getBoolean(data);
            p.setValue(v);
            parameter = p;

        } else if (type.isEnum()) {

            EnumeratedParameter p = new EnumeratedParameter(
                    annotation.displayName(),
                    annotation.required(),
                    field);
            String v = String.valueOf(field.get(data));
            p.setValue(v);
            parameter = p;

        } else if (type == String.class) {

            VerifiedParameter p = new VerifiedParameter(
                    annotation.displayName(),
                    annotation.required(),
                    field);
            String v = (String) field.get(data);
            p.setValue(v);
            parameter = p;

        } else if (type == LEDFlashPattern.class) {

            LEDFlashPatternParameter p = new LEDFlashPatternParameter(
                    annotation.displayName(),
                    annotation.required(),
                    field);
            LEDFlashPattern v = (LEDFlashPattern) field.get(data);
            p.setValue(v.getFlashPattern());
            parameter = p;

        } else if (type == IntensityValue.class) {

            final IntensityValue originalValue =
                    (IntensityValue) field.get(data);

            parameter = new IntensityValueParameter(
                    annotation.displayName(),
                    annotation.required(),
                    field,
                    originalValue);

        } else if (type == PositionalVariableFunction.class) {

            final PositionalVariableFunction originalFunction =
                    (PositionalVariableFunction)
                            field.get(data);

            parameter = new PositionalVariableFunctionParameter(
                    annotation.displayName(),
                    annotation.required(),
                    field,
                    originalFunction);

        } else if ((type == SingleVariableFunction.class) ||
                   (type == KinematicVariableFunction.class) ||
                   (type == BehaviorLimitedKinematicVariableFunction.class)) {

            final SingleVariableFunction originalFunction = (SingleVariableFunction) field.get(data);

            parameter = new SingleVariableFunctionParameter(annotation.displayName(),
                                                            annotation.required(),
                                                            field,
                                                            originalFunction,
                                                            annotation.inputUnits(),
                                                            getBigDecimalValue(annotation.minimum()),
                                                            getBigDecimalValue(annotation.maximum()),
                                                            annotation.outputUnits(),
                                                            getBigDecimalValue(annotation.minimumOutput()),
                                                            getBigDecimalValue(annotation.maximumOutput()));

        } else if (type == BehaviorLimitedKinematicVariableFunctionList.class) {

            final BehaviorLimitedKinematicVariableFunctionList originalList =
                    (BehaviorLimitedKinematicVariableFunctionList) field.get(data);

            parameter = new BehaviorLimitedKinematicVariableFunctionListParameter(annotation.displayName(),
                                                                                  annotation.required(),
                                                                                  field,
                                                                                  annotation.listItemBaseName(),
                                                                                  originalList);
        }

        if (parameter != null) {
            parameterList.add(parameter);
        }
    }

    public static BigDecimal getBigDecimalValue(String valueString) {
        BigDecimal value = null;
        if ((valueString != null) && (valueString.length() > 0)) {
            value = new BigDecimal(valueString);
        }
        return value;
    }

    public static Long getLongValue(String valueString) {
        Long value = null;
        if ((valueString != null) && (valueString.length() > 0)) {
            value = new Long(valueString);
        }
        return value;
    }

    private class SortableAnnotationField
            implements Comparable<SortableAnnotationField> {

        public VenkmanParameter annotation;
        public Field field;

        private SortableAnnotationField(VenkmanParameter annotation,
                                        Field field) {
            this.annotation = annotation;
            this.field = field;
        }

        @SuppressWarnings("NullableProblems")
        @Override
        public int compareTo(SortableAnnotationField o) {
            return annotation.displayOrder() - o.annotation.displayOrder();
        }
    }
}

/*
 * Copyright (c) 2014 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.venkman.gui.parameter.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Identifies parameters that should be configured through the user interface.
 *
 * @author Eric Trautman
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface VenkmanParameter {

    /**
     * @return the display name for this parameter.
     */
    String displayName();

    /**
     * @return the order in which to display this parameter (lesser values are displayed above greater values).
     */
    int displayOrder() default 0;

    /**
     * @return true if this parameter must be specified; otherwise false.
     */
    boolean required() default true;

    /**
     * @return the minimum value (or minimum input value) for this parameter.
     *         This should be a string representation of a number.
     */
    String minimum() default "";

    /**
     * @return the maximum value (or maximum input value) for this parameter.
     *         This should be a string representation of a number.
     */
    String maximum() default "";

    /**
     * @return the name of the input units for this parameter
     *         (used for function parameters).
     */
    String inputUnits() default "";

    /**
     * @return the minimum output value for this parameter
     *         (used for function parameters).
     *         This should be a string representation of a number.
     */
    String minimumOutput() default "";

    /**
     * @return the maximum output value for this parameter
     *         (used for function parameters).
     *         This should be a string representation of a number.
     */
    String maximumOutput() default "";

    /**
     * @return the name of the output units for this parameter
     *         (used for function parameters).
     */
    String outputUnits() default "";

    /**
     * @return the base name to use for each item within a list
     *         (used for function list parameters).
     */
    String listItemBaseName() default "";
}

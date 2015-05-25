/*
 * Copyright (c) 2012 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.venkman.gui.parameter;

import java.awt.*;
import java.lang.reflect.Field;

/**
 * The common interface for managing experiment parameters
 * in the user interface.
 *
 * @author Eric Trautman
 */
public interface ExperimentParameter {

    /**
     * @return the display name for this parameter.
     */
    public String getDisplayName();

    /**
     * @return true if this parameter must be specified; otherwise false.
     */
    public boolean isRequired();

    /**
     * @return the mapped data field for this parameter.
     */
    public Field getDataField();

    /**
     * @return a read-only user interface component for this parameter.
     */
    public Component getReadOnlyComponent();

    /**
     * @return a user interface component for rendering/editing this parameter.
     */
    public Component getComponent();

    /**
     * Confirms that the value entered via the user interface
     * for this parameter is valid.
     *
     * @throws IllegalArgumentException
     *   if the entered value is invalid.
     */
    public void validate() throws IllegalArgumentException;

    /**
     * Applies the user interface value for this parameter to the
     * specified container object if the entered value is valid.
     *
     * @param  object  container object to which value should be applied.
     *
     * @throws IllegalArgumentException
     *   if the entered value is invalid.
     *
     * @throws IllegalAccessException
     *   if the container object field may not be modified (should not happen).
     */
    public void applyValue(Object object)
            throws IllegalArgumentException, IllegalAccessException;
}

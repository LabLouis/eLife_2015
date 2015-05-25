/*
 * Copyright (c) 2012 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.venkman.gui.log;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

/**
 * Copied change support code from {@link java.awt.Component} so that
 * it could be reused by IntelliJ GUI form classes.
 *
 * @author Eric Trautman
 */
public class PropertyChangeSupporter {

    private PropertyChangeSupport changeSupport;

    /**
     * Adds a PropertyChangeListener to the listener list. The listener is
     * registered for all bound properties of this class.
     * <p>
     * If <code>listener</code> is <code>null</code>,
     * no exception is thrown and no action is performed.
     *
     * @param    listener  the property change listener to be added
     */
    public void addPropertyChangeListener(PropertyChangeListener listener) {
        synchronized (getObjectLock()) {
            if (listener == null) {
                return;
            }
            if (changeSupport == null) {
                changeSupport = new PropertyChangeSupport(this);
            }
            changeSupport.addPropertyChangeListener(listener);
        }
    }

    /**
     * Adds a PropertyChangeListener to the listener list for a specific
     * property. The specified property may be user-defined.
     * <p>
     * If <code>propertyName</code> or <code>listener</code> is <code>null</code>,
     * no exception is thrown and no action is taken.
     *
     * @param propertyName one of the property names listed above
     * @param listener the property change listener to be added
     */
    public void addPropertyChangeListener(String propertyName,
                                          PropertyChangeListener listener) {
        synchronized (getObjectLock()) {
            if (listener == null) {
                return;
            }
            if (changeSupport == null) {
                changeSupport = new PropertyChangeSupport(this);
            }
            changeSupport.addPropertyChangeListener(propertyName, listener);
        }
    }

    /**
     * Removes a <code>PropertyChangeListener</code> from the listener
     * list for a specific property. This method should be used to remove
     * <code>PropertyChangeListener</code>s
     * that were registered for a specific bound property.
     * <p>
     * If <code>propertyName</code> or <code>listener</code> is <code>null</code>,
     * no exception is thrown and no action is taken.
     *
     * @param propertyName a valid property name
     * @param listener the PropertyChangeListener to be removed
     */
    public void removePropertyChangeListener(String propertyName,
                                             PropertyChangeListener listener) {
        synchronized (getObjectLock()) {
            if (listener == null || changeSupport == null) {
                return;
            }
            changeSupport.removePropertyChangeListener(propertyName, listener);
        }
    }

    /**
     * Support for reporting bound property changes for Object properties.
     * This method can be called when a bound property has changed and it will
     * send the appropriate PropertyChangeEvent to any registered
     * PropertyChangeListeners.
     *
     * @param propertyName the property whose value has changed
     * @param oldValue the property's previous value
     * @param newValue the property's new value
     */
    protected void firePropertyChange(String propertyName,
                                      Object oldValue,
                                      Object newValue) {
        PropertyChangeSupport changeSupport;
        synchronized (getObjectLock()) {
            changeSupport = this.changeSupport;
        }
        if (changeSupport == null ||
            (oldValue != null && newValue != null && oldValue.equals(newValue))) {
            return;
        }
        changeSupport.firePropertyChange(propertyName, oldValue, newValue);
    }

    /**
     * Support for reporting bound property changes for integer properties.
     * This method can be called when a bound property has changed and it will
     * send the appropriate PropertyChangeEvent to any registered
     * PropertyChangeListeners.
     *
     * @param propertyName the property whose value has changed
     * @param oldValue the property's previous value
     * @param newValue the property's new value
     */
    protected void firePropertyChange(String propertyName,
                                      int oldValue,
                                      int newValue) {
        PropertyChangeSupport changeSupport = this.changeSupport;
        if (changeSupport == null || oldValue == newValue) {
            return;
        }
        changeSupport.firePropertyChange(propertyName, oldValue, newValue);
    }

    /**
     * Copied from {@link java.awt.Component#getObjectLock}.
     */
    private transient Object objectLock = new Object();
    private Object getObjectLock() {
        return objectLock;
    }

}

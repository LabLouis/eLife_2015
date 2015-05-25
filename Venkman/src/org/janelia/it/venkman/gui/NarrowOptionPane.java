/*
 * Copyright (c) 2011 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.venkman.gui;

import javax.swing.*;
import java.awt.*;

/**
 * This class supports the creation of option panes that sensibly wrap
 * text messages instead of displaying long messages on a single line.
 * The idea for this class was taken from
 * <a href="http://java.sun.com/new2java/supplements/2005/July05.html">
 * http://java.sun.com/new2java/supplements/2005/July05.html
 * </a>.
 *
 * @author Eric Trautman
 */
public class NarrowOptionPane extends JOptionPane {

    public static final int DEFAULT_MAX_CHARACTERS = 100;

    private int maxCharactersPerLineCount;

    /**
     * Constructs an option pane with the specified count.
     *
     * @param  maxCharactersPerLineCount  the maximum number of message
     *                                    characters to place on a line
     *                                    in the dialog (or null to use
     *                                    {@link #DEFAULT_MAX_CHARACTERS}).
     */
    public NarrowOptionPane(Integer maxCharactersPerLineCount) {
        if (maxCharactersPerLineCount != null) {
            this.maxCharactersPerLineCount = maxCharactersPerLineCount;
        } else {
            this.maxCharactersPerLineCount = DEFAULT_MAX_CHARACTERS;
        }
    }

    /**
     * @return the maximum number of characters on a line.
     */
    @Override
    public int getMaxCharactersPerLineCount() {
        return maxCharactersPerLineCount;
    }

    /**
     * Brings up a dialog displaying a message, specifying all parameters.
     *
     * @param  parentComponent  determines the <code>Frame</code> in which the
     *                          dialog is displayed; if <code>null</code>,
     *                          or if the <code>parentComponent</code> has no
     *                          <code>Frame</code>, a default <code>Frame</code>
     *                          is used.
     *
     * @param  message          the <code>Object</code> to display.
     *
     * @param  title            the title string for the dialog.
     *
     * @param  messageType      the type of message to be displayed:
     *                          {@link #ERROR_MESSAGE},
     *                          {@link #INFORMATION_MESSAGE},
     *                          {@link #WARNING_MESSAGE},
     *                          {@link #QUESTION_MESSAGE}, or
     *                          {@link #PLAIN_MESSAGE}.
     *
     * @exception java.awt.HeadlessException
     *   if <code>GraphicsEnvironment.isHeadless</code>
     *   returns <code>true</code>.
     *
     * @see java.awt.GraphicsEnvironment#isHeadless
     */
    public static void showMessageDialog(Component parentComponent,
                                         String message,
                                         String title,
                                         int messageType)
        throws HeadlessException {
        showMessageDialog(parentComponent,
                          message,
                          title,
                          messageType,
                          DEFAULT_MAX_CHARACTERS);
    }

    /**
     * Brings up a dialog displaying a message, specifying all parameters.
     *
     * @param  parentComponent  determines the <code>Frame</code> in which the
     *                          dialog is displayed; if <code>null</code>,
     *                          or if the <code>parentComponent</code> has no
     *                          <code>Frame</code>, a default <code>Frame</code>
     *                          is used.
     *
     * @param  message          the <code>Object</code> to display.
     *
     * @param  title            the title string for the dialog.
     *
     * @param  messageType      the type of message to be displayed:
     *                          {@link #ERROR_MESSAGE},
     *                          {@link #INFORMATION_MESSAGE},
     *                          {@link #WARNING_MESSAGE},
     *                          {@link #QUESTION_MESSAGE}, or
     *                          {@link #PLAIN_MESSAGE}.
     *
     * @param  maxCharactersPerLineCount  the maximum number of message
     *                                    characters to place on a line
     *                                    in the dialog (or null to use
     *                                    {@link #DEFAULT_MAX_CHARACTERS}).
     *
     * @exception HeadlessException
     *   if <code>GraphicsEnvironment.isHeadless</code>
     *   returns <code>true</code>.
     *
     * @see java.awt.GraphicsEnvironment#isHeadless
     */
    public static void showMessageDialog(Component parentComponent,
                                         String message,
                                         String title,
                                         int messageType,
                                         Integer maxCharactersPerLineCount)
        throws HeadlessException {

        showNarrowOptionDialog(parentComponent,
                               message,
                               title,
                               DEFAULT_OPTION,
                               null,
                               messageType,
                               null,
                               null,
                               maxCharactersPerLineCount);
    }

    /**
     * Brings up a dialog where the number of choices is determined
     * by the <code>optionType</code> parameter.
     *
     * @param  parentComponent  determines the <code>Frame</code> in which the
     *                          dialog is displayed; if <code>null</code>,
     *                          or if the <code>parentComponent</code> has no
     *                          <code>Frame</code>, a default <code>Frame</code>
     *                          is used.
     *
     * @param  message          the <code>Object</code> to display.
     * @param  title            the title string for the dialog.
     * @param  optionType       designates the available options.
     *
     * @return the option selected by the user.
     *
     * @throws HeadlessException
     *   if <code>GraphicsEnvironment.isHeadless</code> returns true.
     */
    public static int showConfirmDialog(Component parentComponent,
                                        Object message,
                                        String title,
                                        int optionType)
            throws HeadlessException {

        return showNarrowOptionDialog(parentComponent,
                                      message,
                                      title,
                                      optionType,
                                      null,
                                      QUESTION_MESSAGE,
                                      null,
                                      null,
                                      DEFAULT_MAX_CHARACTERS);
    }

    /**
     * Brings up a dialog with a specified icon, where the initial
     * choice is determined by the <code>initialValue</code> parameter and
     * the number of choices is determined by the <code>optionType</code>
     * parameter.
     *
     * @param  parentComponent frame in which the dialog is displayed.
     * @param  message         the <code>Object</code> to display.
     * @param  title           the title string for the dialog.
     * @param  optionType      an integer designating the options available.
     * @param  messageType     an integer designating the kind of message.
     * @param  icon            the icon to display in the dialog.
     * @param  options         possible choices the user can make.
     * @param  initialValue    default selection for the dialog.
     * @param  maxCharactersPerLineCount  the maximum number of message
     *                                    characters to place on a line
     *                                    in the dialog (or null to use
     *                                    {@link #DEFAULT_MAX_CHARACTERS}).
     *
     * @return the option chosen by the user.
     *
     * @exception HeadlessException if
     *   <code>GraphicsEnvironment.isHeadless</code> returns <code>true</code>
     */
    public static int showNarrowOptionDialog(Component parentComponent,
                                             Object message,
                                             String title,
                                             int optionType,
                                             Icon icon,
                                             int messageType,
                                             Object[] options,
                                             Object initialValue,
                                             Integer maxCharactersPerLineCount)
            throws HeadlessException {

        NarrowOptionPane pane =
                new NarrowOptionPane(maxCharactersPerLineCount);
        pane.setMessage(message);
        pane.setMessageType(messageType);
        pane.setOptionType(optionType);
        pane.setIcon(icon);
        pane.setOptions(options);
        pane.setInitialValue(initialValue);
        Component parent = (parentComponent == null) ?
                getRootFrame() : parentComponent;
        pane.setComponentOrientation(parent.getComponentOrientation());
        pane.selectInitialValue();

        JDialog dialog = pane.createDialog(parentComponent, title);
        dialog.setVisible(true);
        dialog.dispose();

        Object selectedValue = pane.getValue();

        int option = CLOSED_OPTION;
        if (selectedValue != null) {
            if (options == null) {
                if(selectedValue instanceof Integer) {
                    option = (Integer) selectedValue;
                }
            } else {
                int maxCounter = options.length;
                for (int counter = 0; counter < maxCounter; counter++) {
                    if(options[counter].equals(selectedValue)) {
                        option = counter;
                        break;
                    }
                }
            }
        }
        return option;
    }
}

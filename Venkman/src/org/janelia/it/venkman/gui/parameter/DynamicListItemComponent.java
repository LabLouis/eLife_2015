/*
 * Copyright (c) 2014 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.venkman.gui.parameter;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ActionEvent;

/**
 * User interface component that wraps another component so that it can be used within
 * a list that allows additions, removals, and ordering.
 *
 * @author Eric Trautman
 */
public class DynamicListItemComponent {

    private JPanel contentPanel;
    private JPanel elementPanel;
    private JPanel buttonPanel;
    @SuppressWarnings("UnusedDeclaration")
    private JButton addButton;
    @SuppressWarnings("UnusedDeclaration")
    private JButton removeButton;
    private JButton moveUpButton;
    private JButton moveDownButton;

    private TitledBorder contentPanelBorder;

    private String itemBaseName;
    private Component component;
    private ListButtonHandler buttonHandler;
    private int index;

    public DynamicListItemComponent(final String itemBaseName,
                                    Component component,
                                    int index,
                                    int listSize,
                                    ListButtonHandler buttonHandler) {
        this.itemBaseName = itemBaseName;
        this.component = component;
        this.buttonHandler = buttonHandler;

        // createUIComponents called here ...

        updatePositionOrListSize(index, listSize);
    }

    private void createUIComponents() {
        this.contentPanel = new JPanel(new BorderLayout());
        this.contentPanelBorder = BorderFactory.createTitledBorder(itemBaseName);
        this.contentPanel.setBorder(this.contentPanelBorder);

        this.elementPanel = new JPanel(new BorderLayout());
        this.elementPanel.add(this.component, BorderLayout.LINE_START);

        final DynamicListItemComponent listComponent = this;

        this.addButton = new JButton(
                new AbstractAction("Add " + itemBaseName) {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        buttonHandler.addDynamicListItem(listComponent);
                    }
                }
        );

        this.removeButton = new JButton(
                new AbstractAction("Remove " + itemBaseName) {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        buttonHandler.removeDynamicListItem(listComponent);
                    }
                }
        );

        this.moveUpButton = new JButton(
                new AbstractAction("Move " + itemBaseName + " Up") {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        buttonHandler.moveDynamicListItemUp(listComponent);
                    }
                }
        );

        this.moveDownButton = new JButton(
                new AbstractAction("Move " + itemBaseName + " Down") {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        buttonHandler.moveDynamicListItemDown(listComponent);
                    }
                }
        );
    }

    /**
     * @return the primary content panel for this list element.
     */
    public JPanel getContentPanel() {
        return contentPanel;
    }

    /**
     * @return the index of this element within the list.
     */
    public int getIndex() {
        return index;
    }

    /**
     * Indicates whether the component should allow editing or is simply read-only.
     *
     * @param  editable  true if the component should allow editing;
     *                   false if it is read-only.
     */
    public void setEditable(boolean editable) {
        buttonPanel.setVisible(editable);
    }

    /**
     * This should be called when this element's position in the list has changed
     * or the list size has changed.
     *
     * @param  toIndex   the new index for this element in the list.
     * @param  listSize  the current size of the list.
     */
    public void updatePositionOrListSize(int toIndex,
                                         int listSize) {
        this.index = toIndex;

        this.contentPanelBorder.setTitle(getItemName());

        moveUpButton.setVisible(toIndex > 0);
        moveDownButton.setVisible(toIndex < (listSize - 1));
    }

    /**
     * @return the full name for this item in the list.
     */
    public String getItemName() {
        return itemBaseName + " " + (index + 1);
    }

    /**
     * Interface for handling list manipulation button events.
     */
    public interface ListButtonHandler {

        /**
         * Add an element after the specified component.
         *
         * @param  listComponent  component after which new element should be added.
         */
        public void addDynamicListItem(DynamicListItemComponent listComponent);

        /**
         * Remove the specified component for the list.
         *
         * @param  listComponent  component to remove.
         */
        public void removeDynamicListItem(DynamicListItemComponent listComponent);

        /**
         * Move the specified component up one spot in the ordering of the list.
         *
         * @param  listComponent  component to move.
         */
        public void moveDynamicListItemUp(DynamicListItemComponent listComponent);

        /**
         * Move the specified component down one spot in the ordering of the list.
         *
         * @param  listComponent  component to move.
         */
        public void moveDynamicListItemDown(DynamicListItemComponent listComponent);
    }
}

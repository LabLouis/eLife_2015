/*
 * Copyright (c) 2014 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.venkman.gui.parameter;

import org.janelia.it.venkman.config.rules.BehaviorLimitedKinematicVariableFunction;
import org.janelia.it.venkman.config.rules.BehaviorLimitedKinematicVariableFunctionList;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * User interface component for {@link BehaviorLimitedKinematicVariableFunctionListParameter} objects.
 *
 * @author Eric Trautman
 */
public class BehaviorLimitedKinematicVariableFunctionListComponent {
    private JPanel contentPanel;
    private JPanel emptyListPanel;
    private JButton addFirstButton;

    private String itemBaseName;
    private BehaviorLimitedKinematicVariableFunctionList originalFunctionList;
    private List<ComponentTuple> listComponents;
    private DynamicListItemComponent.ListButtonHandler buttonHandler;

    /**
     * Constructs a user interface component for the specified function list.
     *
     * @param  itemBaseName        base name to display for each list function.
     * @param  functionList        list of functions to display.
     */
    public BehaviorLimitedKinematicVariableFunctionListComponent(String itemBaseName,
                                                                 BehaviorLimitedKinematicVariableFunctionList functionList) {
        this.itemBaseName = itemBaseName;
        this.originalFunctionList = functionList;

        // createUIComponents called here ...
    }

    private void createUIComponents() {
        contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));

        buttonHandler = new DynamicListItemComponent.ListButtonHandler() {
            @Override
            public void addDynamicListItem(DynamicListItemComponent listComponent) {
                insertFunctionAfter(listComponent.getIndex(), null);
            }

            @Override
            public void removeDynamicListItem(DynamicListItemComponent listComponent) {
                removeFunctionAt(listComponent.getIndex());
            }

            @Override
            public void moveDynamicListItemUp(DynamicListItemComponent listComponent) {
                moveFunctionUpFrom(listComponent.getIndex());
            }

            @Override
            public void moveDynamicListItemDown(DynamicListItemComponent listComponent) {
                moveFunctionDownFrom(listComponent.getIndex());
            }
        };

        // create emptyListPanel first so that updateContentPanel logic can be reused when adding list components
        emptyListPanel = new JPanel();
        emptyListPanel.setLayout(new FlowLayout(FlowLayout.LEADING, 0, 0));
        emptyListPanel.add(new JLabel("none defined  "));
        addFirstButton = new JButton(new AbstractAction("Add " + itemBaseName) {
            @Override
            public void actionPerformed(ActionEvent e) {
                appendFunction(new BehaviorLimitedKinematicVariableFunction());
            }
        });
        emptyListPanel.add(addFirstButton);

        // add component for each function in list
        listComponents = new ArrayList<ComponentTuple>();
        for (int i = 0; i < originalFunctionList.size(); i++) {
            appendFunction(originalFunctionList.get(i));
        }

        // add empty list panel last so that same index can be used for listComponents and contentPanel
        contentPanel.add(emptyListPanel);
    }

    public JPanel getContentPanel() {
        return contentPanel;
    }

    /**
     * Indicates whether the component should allow editing or is simply read-only.
     *
     * @param  editable  true if the component should allow editing;
     *                   false if it is read-only.
     */
    public void setEditable(boolean editable) {
        for (ComponentTuple listComponent : listComponents) {
            listComponent.setEditable(editable);
        }
        addFirstButton.setVisible(editable);
    }

    /**
     * @return true if at least one function has been configured;
     *         otherwise false.
     */
    public boolean hasAtLeastOneConfiguredFunction() {
        return listComponents.size() > 0;
    }

    /**
     * Validates minimal requirements for each function in the list.
     *
     * @param  displayName  display name for the function list.
     *
     * @throws IllegalArgumentException
     *   if any of the list functions have invalid configurations.
     */
    public void validateFunctions(String displayName) throws IllegalArgumentException {
        for (ComponentTuple listComponent : listComponents) {
            listComponent.validate(displayName);
        }
    }

    /**
     * @return the modified/edited list of functions.
     */
    public BehaviorLimitedKinematicVariableFunctionList getModifiedList() {
        BehaviorLimitedKinematicVariableFunctionList modifiedList = new BehaviorLimitedKinematicVariableFunctionList();
        for (ComponentTuple listComponent : listComponents) {
            modifiedList.append(listComponent.getFunction());
        }
        return modifiedList;
    }

    private void updateContentPanel() {
        final boolean isListEmpty = (listComponents.size() == 0);
        emptyListPanel.setVisible(isListEmpty);
        ComponentTuple listComponent;
        for (int index = 0; index < listComponents.size(); index++) {
            listComponent = listComponents.get(index);
            listComponent.updatePositionOrListSize(index);
        }
        contentPanel.validate(); // to pickup function order changes
    }

    private void appendFunction(BehaviorLimitedKinematicVariableFunction function) {
        insertFunctionAfter((listComponents.size() - 1), function);
    }

    private void insertFunctionAfter(int index,
                                     BehaviorLimitedKinematicVariableFunction function) {

        final int insertionIndex = index + 1;
        if (function == null) {
            function = new BehaviorLimitedKinematicVariableFunction();
        }
        ComponentTuple componentTuple = new ComponentTuple(function, insertionIndex);
        listComponents.add(insertionIndex, componentTuple);
        contentPanel.add(componentTuple.getlistWrapperPanel(), insertionIndex);
        updateContentPanel();
    }

    private void removeFunctionAt(int index) {
        ComponentTuple componentTuple = listComponents.remove(index);
        contentPanel.remove(componentTuple.getlistWrapperPanel());
        updateContentPanel();
    }

    private void moveFunctionUpFrom(int fromIndex) {
        if (fromIndex > 0) {
            final int toIndex = fromIndex - 1;
            final ComponentTuple componentTuple = listComponents.get(fromIndex);
            final JPanel listWrapperPanel = componentTuple.getlistWrapperPanel();
            Collections.swap(listComponents, fromIndex, toIndex);

            contentPanel.remove(listWrapperPanel);
            contentPanel.add(listWrapperPanel, toIndex);
            updateContentPanel();
        }
    }

    private void moveFunctionDownFrom(int fromIndex) {
        final int indexToMoveUp = fromIndex + 1; // move down is same as moving function below up
        if (indexToMoveUp < listComponents.size()) {
            moveFunctionUpFrom(indexToMoveUp);
        }
    }

    /**
     * Container class for coupling function and list wrapper components to simplify list management.
     */
    private class ComponentTuple {

        private SingleVariableFunctionComponent functionComponent;
        private DynamicListItemComponent listWrapperComponent;

        public ComponentTuple(BehaviorLimitedKinematicVariableFunction function,
                              int index) {

            final DecimalParameter minimumInputValue = getStubRequiredDecimalParameter("Minimum Input Value");
            final DecimalParameter maximumInputValue = getStubRequiredDecimalParameter("Maximum Input Value");

            functionComponent = new SingleVariableFunctionComponent(function,
                                                                    "",
                                                                    minimumInputValue,
                                                                    maximumInputValue,
                                                                    "intensity factor or addend",
                                                                    null,
                                                                    null,
                                                                    function.getVariable(),
                                                                    function.getBehaviorModes(),
                                                                    function.isAdditive());

            listWrapperComponent = new DynamicListItemComponent(itemBaseName,
                                                                functionComponent.getContentPanel(),
                                                                index,
                                                                listComponents.size(),
                                                                buttonHandler);
        }

        public JPanel getlistWrapperPanel() {
            return listWrapperComponent.getContentPanel();
        }

        public void setEditable(boolean editable) {
            functionComponent.setEditable(editable);
            listWrapperComponent.setEditable(editable);
        }

        public void updatePositionOrListSize(int index) {
            listWrapperComponent.updatePositionOrListSize(index, listComponents.size());
        }

        public BehaviorLimitedKinematicVariableFunction getFunction() {
            return functionComponent.getBehaviorLimitedKinematicVariableFunction();
        }

        public void validate(String displayName) throws IllegalArgumentException {
            final BehaviorLimitedKinematicVariableFunction function = getFunction();
            if (! function.hasBehaviorModes()) {
                throw new IllegalArgumentException(
                        listWrapperComponent.getItemName() + " in the " + displayName +
                        " parameter must have at least one behavior mode checked.");
            }
        }

        private DecimalParameter getStubRequiredDecimalParameter(String displayName) {
            return new DecimalParameter(displayName,
                                        true,
                                        null, // don't call apply!
                                        null,
                                        null);
        }
    }

}

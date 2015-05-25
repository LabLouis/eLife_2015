/*
 * Copyright (c) 2011 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.venkman.gui.collection;

import org.janelia.it.venkman.config.Configuration;
import org.janelia.it.venkman.config.ConfigurationManager;
import org.janelia.it.venkman.config.ParameterCollectionCategory;
import org.janelia.it.venkman.config.ParameterCollectionId;
import org.janelia.it.venkman.gui.NarrowOptionPane;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.List;
import java.util.Vector;

public class EditConfigurationDialog
        extends JDialog {
    private JPanel contentPane;
    private JButton saveButton;
    private JButton cancelButton;
    private JComboBox configurationGroupComboBox;
    private JTextField configurationNameTextField;
    private JComboBox behaviorComboBox;
    private JComboBox stimulusComboBox;

    private ConfigurationManager configurationManager;
    private Configuration configuration;

    public EditConfigurationDialog(ConfigurationManager configurationManager,
                                   Configuration configuration)
            throws IllegalStateException {

        this.configurationManager = configurationManager;
        this.configuration = configuration;
        if (isAdd()) {
            setTitle("Add Configuration");
        } else {
            setTitle("Edit Configuration");
        }

        setContentPane(contentPane);
        setModal(true);
        getRootPane().setDefaultButton(saveButton);

        saveButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onSave();
            }
        });

        cancelButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        });

// call onCancel() when cross is clicked
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                onCancel();
            }
        });

// call onCancel() on ESCAPE
        contentPane.registerKeyboardAction(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        }, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
    }

    private void onSave() {
        try {
            Configuration enteredConfiguration = getEnteredConfiguration();
            ParameterCollectionId enteredId = enteredConfiguration.getId();

            if (isAdd()) {
                if (configurationManager.isExistingCollection(enteredId)) {
                    throw new IllegalArgumentException(
                            "The configuration " + enteredId.getFullName() +
                            " already exists.  Please choose another name if " +
                            "you wish add a new configuration.");
                }
            } else if (!enteredId.equals(configuration.getId())) {
                configurationManager.renameCollection(configuration.getId(),
                                                      enteredId);
            }

            configurationManager.saveCollection(enteredId,
                                                enteredConfiguration);
            dispose();

        } catch (IllegalArgumentException e) {
            NarrowOptionPane.showMessageDialog(
                    this,
                    e.getMessage(),
                    "Edit Configuration Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void onCancel() {
        dispose();
    }

    private Configuration getEnteredConfiguration()
            throws IllegalArgumentException {

        final String groupName = (String)
                configurationGroupComboBox.getSelectedItem();
        if (groupName == null) {
            throw new IllegalArgumentException(
                    "Please specify a group for the configuration.");
        }

        final String name = configurationNameTextField.getText().trim();
        if (name == null) {
            throw new IllegalArgumentException(
                    "Please specify a name for the configuration.");
        }

        final ParameterCollectionId id =
                new ParameterCollectionId(
                        ParameterCollectionCategory.CONFIGURATION,
                        groupName,
                        name);

        final String behavior = (String) behaviorComboBox.getSelectedItem();
        if (behavior == null) {
            throw new IllegalArgumentException(
                    "Please specify the behavior parameters for the configuration.");
        }

        final ParameterCollectionId behaviorId =
                ParameterCollectionId.getBehaviorId(behavior);

        ParameterCollectionId stimulusId = null;
        final String stimulus = (String) stimulusComboBox.getSelectedItem();
        if (!NO_STIMULUS.equals(stimulus)) {
            stimulusId = ParameterCollectionId.getStimulusId(stimulus);
        }

        return new Configuration(id, behaviorId, stimulusId);
    }

    private void verifyNamesExist(String messageContext,
                                  List<String> names)
            throws IllegalStateException {
        if (names.size() == 0) {
            throw new IllegalStateException(
                    "Please add at least one " + messageContext +
                    " before creating a configuration instance.");
        }
    }

    private boolean isAdd() {
        return (configuration == null);
    }

    private static final String NO_STIMULUS = "No Stimulus";

    private void createUIComponents() {
        List<String> names =
                configurationManager.getGroupNamesInCategory(
                        ParameterCollectionCategory.CONFIGURATION);
        verifyNamesExist("configuration group", names);
        configurationGroupComboBox =
                new JComboBox(new Vector<String>(names));
        configurationNameTextField = new JTextField();

        names = configurationManager.getBehaviorConfigurationNames();
        verifyNamesExist("behavior instance", names);

        behaviorComboBox = new JComboBox(new Vector<String>(names));

        names = configurationManager.getStimulusConfigurationNames();
        names.add(0, NO_STIMULUS); // allow  no stimulus as an option
        stimulusComboBox = new JComboBox(new Vector<String>(names));

        if (configuration == null) {
            configurationGroupComboBox.setSelectedItem(null);
            behaviorComboBox.setSelectedItem(null);
            stimulusComboBox.setSelectedItem(0); // no stimulus
        } else {
            ParameterCollectionId id = configuration.getId();
            configurationGroupComboBox.setSelectedItem(id.getGroupName());
            configurationNameTextField.setText(id.getName());

            id = configuration.getBehaviorParametersId();
            behaviorComboBox.setSelectedItem(id.getFullName());

            id = configuration.getStimulusParametersId();
            if (id == null) {
                stimulusComboBox.setSelectedItem(null);
            } else {
                stimulusComboBox.setSelectedItem(id.getFullName());
            }
        }
    }

}

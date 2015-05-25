/*
 * Copyright (c) 2014 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.venkman.gui.collection;

import org.apache.log4j.Logger;
import org.janelia.it.venkman.config.ConfigurationManager;
import org.janelia.it.venkman.config.ParameterCollectionCategory;
import org.janelia.it.venkman.config.ParameterCollectionId;
import org.janelia.it.venkman.gui.NarrowOptionPane;
import org.janelia.it.venkman.gui.parameter.ParameterGroup;
import org.janelia.it.venkman.rules.LarvaStimulusRules;
import org.janelia.it.venkman.rules.StimulusRuleImplementations;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.lang.reflect.Constructor;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

public class EditStimulusDialog
        extends JDialog {
    private JPanel contentPane;
    private JButton buttonOK;
    private JButton buttonCancel;
    private JComboBox stimulusGroupComboBox;
    private JTextField nameTextField;
    private JPanel parameterPanel;
    private JComboBox rulesTypeComboBox;
    private JLabel rulesTypeDescriptionLabel;
    @SuppressWarnings({"UnusedDeclaration"})
    private JScrollPane parameterPanelScrollPane;

    private ConfigurationManager configurationManager;
    private ParameterCollectionId originalId;
    private CardLayout parameterPanelLayout;

    private Map<String, ParameterGroup<LarvaStimulusRules>> codeToGroupMap;

    public EditStimulusDialog(ConfigurationManager configurationManager,
                              ParameterCollectionId originalId,
                              LarvaStimulusRules stimulusRules)
            throws IllegalAccessException {

        this.parameterPanelLayout = (CardLayout)
                this.parameterPanel.getLayout();

        this.configurationManager = configurationManager;
        this.originalId = originalId;

        List<String> names =
                configurationManager.getGroupNamesInCategory(
                        ParameterCollectionCategory.STIMULUS);
        stimulusGroupComboBox.setModel(
                new DefaultComboBoxModel(new Vector<String>(names)));

        if (originalId != null) {
            final String groupName = originalId.getGroupName();
            if (groupName != null) {
                stimulusGroupComboBox.setSelectedItem(groupName);
            }
        }

        if (! isAdd()) {
            nameTextField.setText(originalId.getName());
        }

        codeToGroupMap =
                new HashMap<String, ParameterGroup<LarvaStimulusRules>>();

        Vector<String> ruleCodes = new Vector<String>();
        String rulesCode;
        LarvaStimulusRules rules;
        ParameterGroup<LarvaStimulusRules> group;

        for (Class<?> clazz : StimulusRuleImplementations.getClasses()) {
            try {
                if ((stimulusRules != null) &&
                    (stimulusRules.getClass().equals(clazz))) {

                    rules = stimulusRules;

                } else {

                    Constructor constructor = clazz.getConstructor();
                    constructor.setAccessible(true);
                    rules = (LarvaStimulusRules) constructor.newInstance();

                }

                rulesCode = rules.getCode();
                ruleCodes.add(rulesCode);
                group = new ParameterGroup<LarvaStimulusRules>(rules);
                codeToGroupMap.put(rulesCode, group);
                parameterPanel.add(group.getEditableContentPanel(),
                                   rulesCode);


            } catch (Exception e) {
                LOG.error("failed to load stimulus rules instances", e);
            }
        }
        Collections.sort(ruleCodes);
        rulesTypeComboBox.setModel(new DefaultComboBoxModel(ruleCodes));

        rulesTypeComboBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                changeSelectedRule();
            }
        });

        if (isAdd()) {
            rulesTypeComboBox.setSelectedIndex(0);
        } else {
            rulesTypeComboBox.setSelectedItem(stimulusRules.getCode());
        }

        setTitle("Edit Stimulus Parameters");

        setContentPane(contentPane);
        setModal(true);
        getRootPane().setDefaultButton(buttonOK);

        final Dimension contentPaneSize = contentPane.getPreferredSize();
        final Dimension shorterWindowSize =
                new Dimension(contentPaneSize.width, 800);
        contentPane.setPreferredSize(shorterWindowSize);

        buttonOK.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onOK();
            }
        });

        buttonCancel.addActionListener(new ActionListener() {
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

    private void onOK() {

        try {
            final String groupName = (String)
                    stimulusGroupComboBox.getSelectedItem();
            final String name = nameTextField.getText();
            final String selectedCode = (String)
                    rulesTypeComboBox.getSelectedItem();
            ParameterGroup<LarvaStimulusRules> selectedGroup =
                    codeToGroupMap.get(selectedCode);

            selectedGroup.applyParameters();

            final LarvaStimulusRules selectedRules = selectedGroup.getData();

            configurationManager.saveCollection(
                    originalId,
                    ParameterCollectionCategory.STIMULUS,
                    groupName,
                    name,
                    selectedRules);

            dispose();

        } catch (Exception e) {
            NarrowOptionPane.showMessageDialog(
                    this,
                    e.getMessage(),
                    "Edit Stimulus Parameters Error",
                    JOptionPane.ERROR_MESSAGE);
        }

    }

    private void onCancel() {
        dispose();
    }

    private void changeSelectedRule() {

        final String selectedCode = (String)
                rulesTypeComboBox.getSelectedItem();
        final ParameterGroup<LarvaStimulusRules> selectedGroup =
                codeToGroupMap.get(selectedCode);
        final LarvaStimulusRules selectedRules = selectedGroup.getData();

        parameterPanelLayout.show(parameterPanel, selectedCode);

        rulesTypeDescriptionLabel.setText(
                "<html><p>" + selectedRules.getDescription() + "</p></html>");

    }

    private boolean isAdd() {
        return (originalId == null);
    }

    private static final Logger LOG =
            Logger.getLogger(EditStimulusDialog.class);

}

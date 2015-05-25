/*
 * Copyright (c) 2011 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.venkman.gui.collection;

import org.janelia.it.venkman.config.ConfigurationManager;
import org.janelia.it.venkman.config.LarvaBehaviorParameters;
import org.janelia.it.venkman.config.ParameterCollectionCategory;
import org.janelia.it.venkman.config.ParameterCollectionId;
import org.janelia.it.venkman.gui.NarrowOptionPane;
import org.janelia.it.venkman.gui.parameter.ParameterGroup;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.List;
import java.util.Vector;

public class EditBehaviorDialog
        extends JDialog {
    private JPanel contentPane;
    private JButton buttonOK;
    private JButton buttonCancel;
    private JComboBox groupComboBox;
    private JTextField nameTextField;
    private JPanel parameterPanel;

    private ConfigurationManager configurationManager;
    private ParameterCollectionId originalId;
    private LarvaBehaviorParameters behaviorParameters;
    private ParameterGroup<LarvaBehaviorParameters> parameterGroup;

    public EditBehaviorDialog(ConfigurationManager configurationManager,
                              ParameterCollectionId originalId,
                              LarvaBehaviorParameters behaviorParameters)
            throws IllegalAccessException {

        this.configurationManager = configurationManager;
        this.originalId = originalId;
        if (behaviorParameters == null) {
            this.behaviorParameters = new LarvaBehaviorParameters();
        } else {
            this.behaviorParameters = behaviorParameters;
        }

        List<String> names =
                configurationManager.getGroupNamesInCategory(
                        ParameterCollectionCategory.BEHAVIOR);
        groupComboBox.setModel(
                new DefaultComboBoxModel(new Vector<String>(names)));

        if (! isAdd()) {
            nameTextField.setText(originalId.getName());
        }

        this.parameterGroup =
                new ParameterGroup<LarvaBehaviorParameters>(
                        this.behaviorParameters);
        parameterPanel.add(parameterGroup.getEditableContentPanel(),
                           BorderLayout.CENTER);

        setTitle("Edit Behavior Parameters");

        setContentPane(contentPane);
        setModal(true);
        getRootPane().setDefaultButton(buttonOK);

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
            final String groupName = (String) groupComboBox.getSelectedItem();
            final String name = nameTextField.getText();

            parameterGroup.applyParameters();

            configurationManager.saveCollection(
                    originalId,
                    ParameterCollectionCategory.BEHAVIOR,
                    groupName,
                    name,
                    behaviorParameters);

            dispose();

        } catch (Exception e) {
            NarrowOptionPane.showMessageDialog(
                    this,
                    e.getMessage(),
                    "Edit Behavior Parameters Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void onCancel() {
        dispose();
    }

    private boolean isAdd() {
        return (originalId == null);
    }

}

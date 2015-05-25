/*
 * Copyright (c) 2011 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.venkman.gui.group;

import org.janelia.it.venkman.config.ConfigurationManager;
import org.janelia.it.venkman.config.ParameterCollectionCategory;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.List;
import java.util.Vector;

public class RenameGroupDialog
        extends JDialog {
    private JPanel contentPane;
    private JButton buttonOK;
    private JButton buttonCancel;
    private JComboBox groupNameComboBox;
    private JTextField toGroupNameTextField;

    private ConfigurationManager configurationManager;
    private ParameterCollectionCategory category;

    public RenameGroupDialog(ConfigurationManager configurationManager,
                             ParameterCollectionCategory category) {

        this.configurationManager = configurationManager;
        this.category = category;
        setTitle("Rename " + category.getDisplayName() + " Group");

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
        final String fromGroupName = (String)
                groupNameComboBox.getSelectedItem();
        if (fromGroupName != null) {
            final String toGroupName = toGroupNameTextField.getText().trim();
            if (toGroupName.length() > 0) {
                configurationManager.renameGroup(category,
                                                 fromGroupName,
                                                 toGroupName);
            }
        }
        dispose();
    }

    private void onCancel() {
        dispose();
    }

    private void createUIComponents() {
        final List<String> groupNames =
                configurationManager.getGroupNamesInCategory(category);
        groupNameComboBox = new JComboBox(new Vector<String>(groupNames));
        groupNameComboBox.setSelectedItem(null);
    }

}

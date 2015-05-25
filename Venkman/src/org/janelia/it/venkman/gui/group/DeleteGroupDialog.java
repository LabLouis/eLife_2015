/*
 * Copyright (c) 2011 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.venkman.gui.group;

import org.janelia.it.venkman.config.ConfigurationManager;
import org.janelia.it.venkman.config.ParameterCollectionCategory;
import org.janelia.it.venkman.gui.NarrowOptionPane;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.List;
import java.util.Vector;

public class DeleteGroupDialog
        extends JDialog {
    private JPanel contentPane;
    private JButton buttonOK;
    private JButton buttonCancel;
    private JComboBox groupNameComboBox;

    private ConfigurationManager configurationManager;
    private ParameterCollectionCategory category;

    public DeleteGroupDialog(ConfigurationManager configurationManager,
                             ParameterCollectionCategory category) {

        this.configurationManager = configurationManager;
        this.category = category;
        setTitle("Delete " + category.getDisplayName() + " Group");

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
        final String groupName = (String) groupNameComboBox.getSelectedItem();
        if (groupName != null) {
            final List<String> configNames =
                    configurationManager.getCollectionNamesInGroup(category,
                                                                   groupName);
            boolean deleteGroup = true;
            if (configNames.size() > 0) {
                StringBuilder message = new StringBuilder(128);
                message.append("The ").append(groupName).append(" group ");
                message.append("contains the following configurations:\n");
                ConfigurationManager.appendNames(configNames, message);
                message.append("Deleting this group will delete all of ");
                message.append("its configurations.\n");
                message.append("Are you sure you wish to continue?");
                final int option = NarrowOptionPane.showConfirmDialog(
                        getContentPane(),
                        message,
                        "Confirm Deletion of Non-empty Group",
                        JOptionPane.YES_NO_OPTION);
                deleteGroup = (option == JOptionPane.YES_OPTION);
            }

            if (deleteGroup) {
                try {
                    configurationManager.deleteGroup(category, groupName);
                } catch (IllegalArgumentException e) {
                    NarrowOptionPane.showMessageDialog(
                            getContentPane(),
                            e.getMessage(),
                            "Group Delete Failed",
                            JOptionPane.ERROR_MESSAGE);
                }
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

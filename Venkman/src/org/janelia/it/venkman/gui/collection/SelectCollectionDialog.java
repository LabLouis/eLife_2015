/*
 * Copyright (c) 2011 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.venkman.gui.collection;

import org.janelia.it.venkman.config.Configuration;
import org.janelia.it.venkman.config.ConfigurationManager;
import org.janelia.it.venkman.config.LarvaBehaviorParameters;
import org.janelia.it.venkman.config.ParameterCollectionCategory;
import org.janelia.it.venkman.config.ParameterCollectionId;
import org.janelia.it.venkman.gui.NarrowOptionPane;
import org.janelia.it.venkman.rules.LarvaStimulusRules;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.List;
import java.util.Vector;

public class SelectCollectionDialog
        extends JDialog {
    private JPanel contentPane;
    private JButton buttonEdit;
    private JButton buttonCancel;
    private JComboBox collectionComboBox;
    private JButton buttonDelete;

    private ConfigurationManager configurationManager;
    private ParameterCollectionCategory category;

    public SelectCollectionDialog(ConfigurationManager configurationManager,
                                  ParameterCollectionCategory category,
                                  String title) {

        this.configurationManager = configurationManager;
        this.category = category;
        setTitle(title);

        final List<String> collectionNames =
                configurationManager.getCollectionNames(category);
        collectionComboBox.setModel(
                new DefaultComboBoxModel(new Vector<String>(collectionNames)));

        setContentPane(contentPane);
        setModal(true);
        getRootPane().setDefaultButton(buttonEdit);

        buttonEdit.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onEdit();
            }
        });

        buttonDelete.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onDelete();
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

    private void onEdit() {
        final String collectionName = (String)
                collectionComboBox.getSelectedItem();
        if (collectionName != null) {
            ParameterCollectionId id = new ParameterCollectionId(category,
                                                                 collectionName);
            try {

                switch (category) {
                    case CONFIGURATION:
                        final Configuration configuration =
                                configurationManager.getConfiguration(
                                        collectionName);
                        displayEditDialog(
                                new EditConfigurationDialog(configurationManager,
                                                            configuration));
                        break;

                    case BEHAVIOR:
                        final LarvaBehaviorParameters parameters =
                                configurationManager.getBehaviorParameters(id);
                        displayEditDialog(
                                new EditBehaviorDialog(configurationManager,
                                                       id,
                                                       parameters));
                        break;

                    case STIMULUS:
                        final LarvaStimulusRules stimulusRules =
                                configurationManager.getStimulusRules(id);
                        displayEditDialog(
                                new EditStimulusDialog(configurationManager,
                                                       id,
                                                       stimulusRules));
                        break;
                }

            } catch (Exception e) {
                NarrowOptionPane.showMessageDialog(
                        this,
                        e.getMessage(),
                        category.getDisplayName() + " Edit Failure",
                        JOptionPane.ERROR_MESSAGE);
                e.printStackTrace();
            }

        }
    }

    private void onDelete() {
        String collectionName = (String) collectionComboBox.getSelectedItem();
        if (collectionName != null) {
            try {
                configurationManager.deleteCollection(category, collectionName);
            } catch (IllegalArgumentException e) {
                NarrowOptionPane.showMessageDialog(
                        getContentPane(),
                        e.getMessage(),
                        "Delete Failed",
                        JOptionPane.ERROR_MESSAGE);
            }
        }
        dispose();
    }

    private void onCancel() {
        dispose();
    }

    private void displayEditDialog(JDialog dialog) {
        dialog.setLocation(this.getLocation());
        dialog.pack();
        dispose();
        dialog.setVisible(true);
    }
}

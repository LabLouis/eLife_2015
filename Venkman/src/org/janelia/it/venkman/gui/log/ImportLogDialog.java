/*
 * Copyright (c) 2012 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.venkman.gui.log;

import org.janelia.it.venkman.ImportLogWorker;
import org.janelia.it.venkman.config.ConfigurationManager;
import org.janelia.it.venkman.config.ParameterCollectionCategory;
import org.janelia.it.venkman.gui.NarrowOptionPane;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.List;
import java.util.Vector;

/**
 * Dialog to select a log file for importing and re-process it's
 * tracker skeleton data using a different rules configuration.
 *
 * @author Eric Trautman
 */
public class ImportLogDialog
        extends JDialog {
    private JPanel contentPane;
    private JButton cancelButton;
    private JTextField logFileTextField;
    private JButton logFileChooserButton;
    private JComboBox configurationComboBox;
    private JButton importButton;
    private JCheckBox manuallyEditBehaviorParametersCheckBox;
    private JLabel rulesConfigurationLabel;

    private ConfigurationManager configurationManager;
    private File logFileDirectory;

    public ImportLogDialog(ConfigurationManager configurationManager) {
        this.configurationManager = configurationManager;
        this.logFileDirectory = configurationManager.getLogDirectory();

        setTitle("Import Log");

        final List<String> collectionNames =
                configurationManager.getCollectionNames(
                        ParameterCollectionCategory.CONFIGURATION);
        configurationComboBox.setModel(
                new DefaultComboBoxModel(new Vector<String>(collectionNames)));

        setContentPane(contentPane);
        setModal(true);

        logFileChooserButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                selectLogFile();
            }
        });

        manuallyEditBehaviorParametersCheckBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                handleCheckBoxEvent();
            }
        });
        
        importButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                importLogFile();
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
        contentPane.registerKeyboardAction(
                new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        onCancel();
                    }
                },
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
    }

    private void handleCheckBoxEvent() {
        if (manuallyEditBehaviorParametersCheckBox.isSelected()) {
            rulesConfigurationLabel.setEnabled(false);
            configurationComboBox.setEnabled(false);
        } else {
            rulesConfigurationLabel.setEnabled(true);
            configurationComboBox.setEnabled(true);
        }        
    }
    
    private void selectLogFile() {
        JFileChooser fileChooser = new JFileChooser(logFileDirectory);
        int choice = fileChooser.showDialog(this, "Select Log File");

        if (choice == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            if (selectedFile != null) {
                logFileDirectory = selectedFile.getParentFile();
                logFileTextField.setText(selectedFile.getAbsolutePath());
            }
        }
    }

    private void importLogFile() {

        ImportLogWorker importWorker = null;
        
        final String configurationName = (String)
                configurationComboBox.getSelectedItem();

        File logFile = new File(logFileTextField.getText());

        if ((! logFile.canRead()) || (! logFile.getName().endsWith(".xml"))) {

            NarrowOptionPane.showMessageDialog(
                    this,
                    "Please specify a valid Venkman log file for import.",
                    "Invalid Log File",
                    JOptionPane.ERROR_MESSAGE);

        } else if (manuallyEditBehaviorParametersCheckBox.isSelected()) {

            importWorker = new ImportLogWorker(configurationManager,
                                               logFile,
                                               this.getLocation());

        } else if (configurationName == null) {

            NarrowOptionPane.showMessageDialog(
                    this,
                    "Please select a configuration.",
                    "Missing Configuration",
                    JOptionPane.ERROR_MESSAGE);

        } else {

            importWorker = new ImportLogWorker(configurationManager,
                                               configurationName,
                                               logFile);
        }
        
        if (importWorker != null) {
            importWorker.execute(); // import in background thread
            dispose();
        }
    }

    private void onCancel() {
        dispose();
    }

}

/*
 * Copyright (c) 2012 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.venkman.gui.collection;

import org.janelia.it.venkman.config.LarvaBehaviorParameters;
import org.janelia.it.venkman.gui.NarrowOptionPane;
import org.janelia.it.venkman.gui.parameter.ParameterGroup;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class ImportBehaviorDialog
        extends JDialog {
    private JPanel contentPane;
    private JButton buttonOK;
    private JButton buttonCancel;
    private JPanel parameterPanel;

    private LarvaBehaviorParameters behaviorParameters;
    private ParameterGroup<LarvaBehaviorParameters> parameterGroup;

    public ImportBehaviorDialog(LarvaBehaviorParameters behaviorParameters) {

        if (behaviorParameters == null) {
            this.behaviorParameters = new LarvaBehaviorParameters();
        } else {
            this.behaviorParameters = behaviorParameters;
        }

        this.parameterGroup =
                new ParameterGroup<LarvaBehaviorParameters>(
                        this.behaviorParameters);
        parameterPanel.add(parameterGroup.getEditableContentPanel(),
                           BorderLayout.CENTER);

        setTitle("Edit Import Behavior Parameters");

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
        contentPane.registerKeyboardAction(
                new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        onCancel();
                    }
                },
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
    }

    public LarvaBehaviorParameters getBehaviorParameters() {
        return behaviorParameters;
    }

    private void onOK() {

        try {
            parameterGroup.applyParameters();
            dispose();
        } catch (Exception e) {
            NarrowOptionPane.showMessageDialog(
                    this,
                    e.getMessage(),
                    "Edit Import Behavior Parameters Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void onCancel() {
        behaviorParameters = null; // let caller know cancel was clicked
        dispose();
    }
    
}

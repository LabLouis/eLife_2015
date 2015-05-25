/*
 * Copyright (c) 2012 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.venkman.gui;

import org.apache.log4j.Logger;
import org.janelia.it.venkman.config.Configuration;
import org.janelia.it.venkman.config.ConfigurationManager;
import org.janelia.it.venkman.config.LarvaBehaviorParameters;
import org.janelia.it.venkman.config.ParameterCollectionCategory;
import org.janelia.it.venkman.config.ParameterCollectionId;
import org.janelia.it.venkman.gui.collection.EditBehaviorDialog;
import org.janelia.it.venkman.gui.collection.EditConfigurationDialog;
import org.janelia.it.venkman.gui.collection.EditStimulusDialog;
import org.janelia.it.venkman.gui.collection.SelectCollectionDialog;
import org.janelia.it.venkman.gui.group.AddGroupDialog;
import org.janelia.it.venkman.gui.group.DeleteGroupDialog;
import org.janelia.it.venkman.gui.group.RenameGroupDialog;
import org.janelia.it.venkman.gui.log.ImportLogDialog;
import org.janelia.it.venkman.gui.log.VenkmanLogAnalyzer;
import org.janelia.it.venkman.rules.LarvaStimulusRules;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;

/**
 * User interface for the rules configuration management dashboard.
 *
 * @author Eric Trautman
 */
public class Dashboard {

    private static JFrame dashboardFrame = new JFrame("Venkman Manager");

    public static JFrame getDashboardFrame() {
        return dashboardFrame;
    }

    public static void main(String[] args) {

        // force dashboard L&F to system L&F
        try {
            String lookAndFeel = UIManager.getSystemLookAndFeelClassName();
            UIManager.setLookAndFeel(lookAndFeel);
        } catch( Exception e ) {
            LOG.warn("failed to change UI to system look and feel", e);
        }

        String workingDirectoryName = ".";
        if (args.length > 0) {
            workingDirectoryName = args[0];
        }
        final File workingDirectory = new File(workingDirectoryName);

        final ConfigurationManager manager =
                new ConfigurationManager(workingDirectory);
        Dashboard dashboard = new Dashboard(manager);

        dashboardFrame.setContentPane(dashboard.appPanel);
        dashboardFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        dashboardFrame.pack();

        // size the window
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        Dimension frameSize = dashboardFrame.getSize();

        final double frameSizeFactor = 0.6;
        frameSize.height = (int) (screenSize.height * frameSizeFactor);
        frameSize.width = (int) (screenSize.width * frameSizeFactor);

        // hack for dual screens
        final int maxWidth = (int) (1920 * frameSizeFactor);
        if (frameSize.width > maxWidth) {
            frameSize.width = maxWidth;
        }

        dashboardFrame.setSize(frameSize.width, frameSize.height);
        dashboardFrame.setPreferredSize(frameSize);

        dashboardFrame.setVisible(true);
    }

    private JMenuBar menuBar;
    @SuppressWarnings("UnusedDeclaration")
    private JPanel dashboardPanel;
    private JPanel appPanel;
    private JTable dashboardTable;
    @SuppressWarnings("UnusedDeclaration")
    private JScrollPane dashboardTableScrollPane;

    private ConfigurationManager configurationManager;

    public Dashboard(ConfigurationManager manager) {
        configurationManager = manager;
        dashboardTable.setModel(new DashboardTableModel(configurationManager));

        dashboardTable.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                handleTableClick(e);
            }
        });

        configurationManager.addChangeListener(new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                DashboardTableModel
                        updatedModel =
                        new DashboardTableModel(configurationManager);
                dashboardTable.setModel(updatedModel);
            }
        });
    }

    private void handleTableClick(MouseEvent e) {
        if (e.getComponent().isEnabled() &&
            e.getButton() == MouseEvent.BUTTON1 &&
            e.getClickCount() == 2) {
            Point p = e.getPoint();
            int row = dashboardTable.rowAtPoint(p);
            int column = dashboardTable.columnAtPoint(p);
            DashboardTableModel model = (DashboardTableModel)
                    dashboardTable.getModel();
            ParameterCollectionId id = model.getIdAt(row, column);

            // TODO: find better way to share this code with SelectCollectionDialog
            if (id != null) {
                final ParameterCollectionCategory category = id.getCategory();
                try {
                    JDialog dialog = null;
                    switch (category) {
                        case CONFIGURATION:
                            final Configuration configuration =
                                    configurationManager.getConfiguration(
                                            id.getFullName());
                            dialog = new EditConfigurationDialog(
                                    configurationManager, configuration);
                            break;

                        case BEHAVIOR:
                            final LarvaBehaviorParameters parameters =
                                    configurationManager.getBehaviorParameters(id);
                            dialog = new EditBehaviorDialog(
                                    configurationManager, id, parameters);
                            break;

                        case STIMULUS:
                            final LarvaStimulusRules stimulusRules =
                                    configurationManager.getStimulusRules(id);
                            dialog = new EditStimulusDialog(
                                    configurationManager, id, stimulusRules);
                            break;
                    }

                    dialog.setLocation(appPanel.getLocation());
                    dialog.pack();
                    dialog.setVisible(true);

                } catch (Exception e1) {
                    NarrowOptionPane.showMessageDialog(
                            this.appPanel,
                            e1.getMessage(),
                            category.getDisplayName() + " Edit Failure",
                            JOptionPane.ERROR_MESSAGE);
                    e1.printStackTrace();
                }

            }
        }
    }

    private void createMenuBar() {

        menuBar = new JMenuBar();

        Action addInstanceAction = new AbstractAction("Add Configuration") {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    final EditConfigurationDialog dialog =
                            new EditConfigurationDialog(configurationManager,
                                                        null);
                    showDialog(dialog);
                } catch (IllegalStateException exception) {
                    NarrowOptionPane.showMessageDialog(
                            menuBar,
                            exception.getMessage(),
                            "Configuration Dependencies Missing",
                            JOptionPane.ERROR_MESSAGE);
                }

            }
        };
        menuBar.add(
                createCategoryMenu(ParameterCollectionCategory.CONFIGURATION,
                                   KeyEvent.VK_C,
                                   addInstanceAction,
                                   "Edit or Delete Configuration"));

        addInstanceAction = new AbstractAction("Add Behavior Parameters") {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    EditBehaviorDialog dialog =
                            new EditBehaviorDialog(configurationManager,
                                                   null,
                                                   null);
                    showDialog(dialog);
                } catch (Exception e1) {
                    NarrowOptionPane.showMessageDialog(
                            dashboardFrame,
                            e1.getMessage(),
                            "Add Behavior Parameters Failure",
                            JOptionPane.ERROR_MESSAGE);
                    e1.printStackTrace();
                }
            }
        };
        menuBar.add(
                createCategoryMenu(ParameterCollectionCategory.BEHAVIOR,
                                   KeyEvent.VK_B,
                                   addInstanceAction,
                                   "Edit or Delete Behavior Parameters"));
        addInstanceAction = new AbstractAction("Add Stimulus Parameters") {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    EditStimulusDialog dialog =
                            new EditStimulusDialog(configurationManager,
                                                   null,
                                                   null);
                    showDialog(dialog);
                } catch (Exception e1) {
                    NarrowOptionPane.showMessageDialog(
                            dashboardFrame,
                            e1.getMessage(),
                            "Add Stimulus Parameters Failure",
                            JOptionPane.ERROR_MESSAGE);
                    e1.printStackTrace();
                }
            }
        };
        menuBar.add(
                createCategoryMenu(ParameterCollectionCategory.STIMULUS,
                                   KeyEvent.VK_S,
                                   addInstanceAction,
                                   "Edit or Delete Stimulus Parameters"));

        menuBar.add(createLogToolsMenu());
    }

    private JMenu createCategoryMenu(final ParameterCollectionCategory category,
                                     int keyCode,
                                     Action addInstanceAction,
                                     String editText) {
        JMenu menu = new JMenu(category.getDisplayName());
        menu.setMnemonic(keyCode);

        final JMenuItem addInstanceItem = new JMenuItem(addInstanceAction);
        menu.add(addInstanceItem);

        final String editTitle = editText;
        final JMenuItem deleteInstanceItem =
                new JMenuItem(new AbstractAction(editText) {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        final SelectCollectionDialog dialog =
                                new SelectCollectionDialog(configurationManager,
                                                           category,
                                                           editTitle);
                        showDialog(dialog);
                    }
                });

        menu.add(deleteInstanceItem);
        menu.addSeparator();

        final JMenuItem addGroupItem =
                new JMenuItem(new AbstractAction("Add Group") {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        final AddGroupDialog dialog =
                                new AddGroupDialog(configurationManager,
                                                   category);
                        showDialog(dialog);
                    }
                });

        menu.add(addGroupItem);

        final JMenuItem renameGroupItem =
                new JMenuItem(new AbstractAction("Rename Group") {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        final RenameGroupDialog dialog =
                                new RenameGroupDialog(configurationManager,
                                                      category);
                        showDialog(dialog);
                    }
                });

        menu.add(renameGroupItem);

        final JMenuItem deleteGroupItem =
                new JMenuItem(new AbstractAction("Delete Group") {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        final DeleteGroupDialog dialog =
                                new DeleteGroupDialog(configurationManager,
                                                      category);
                        showDialog(dialog);
                    }
                });

        menu.add(deleteGroupItem);

        return menu;
    }

    private void showDialog(Dialog dialog) {
        final Point loc = menuBar.getLocationOnScreen();
        dialog.setLocation(loc.x, loc.y);
        dialog.pack();
        dialog.setVisible(true);
    }

    private JMenu createLogToolsMenu() {
        JMenu menu = new JMenu("Log Tools");
        menu.setMnemonic(KeyEvent.VK_L);

        final JMenuItem analyzerItem =
                new JMenuItem(new AbstractAction("Log Analyzer") {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        VenkmanLogAnalyzer.display(configurationManager);
                    }
                });

        menu.add(analyzerItem);

        final JMenuItem importItem =
                new JMenuItem(new AbstractAction("Import Log") {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        final ImportLogDialog dialog =
                                new ImportLogDialog(configurationManager);
                        showDialog(dialog);
                    }
                });

        menu.add(importItem);

        return menu;
    }

    private void createUIComponents() {
        createMenuBar();
    }

    private static final Logger LOG = Logger.getLogger(Dashboard.class);
}

/*
 * Copyright (c) 2014 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.venkman.gui.log;

import org.apache.log4j.Logger;
import org.janelia.it.venkman.ImportLogWorker;
import org.janelia.it.venkman.config.ConfigurationManager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyVetoException;
import java.io.File;

/**
 * A desktop frame for loading and analyzing venkman log data.
 *
 * @author Eric Trautman
 */
public class LogAnalyzer extends JFrame {

    private JDesktopPane desktop;
    private PlaybackControlsPanel playbackPanel;
//    private KeyAdapter playbackKeyAdapter;

    private JInternalFrame arenaFrame;
    private JInternalFrame frameDataFrame;
    private JInternalFrame rulesParametersFrame;
    private JInternalFrame behaviorDataFrame;
    private JInternalFrame stimulusFrame;

    private JMenuItem reImportLogMenuItem;
    private JMenu navigateMenu;


    private ConfigurationManager configurationManager;
    private LogModel logModel;

    public LogAnalyzer() {

        super("Venkman Log Analyzer");

        this.logModel = new LogModel();

        this.logModel.addPropertyChangeListener(
                LogModel.LOG_FILE_PROPERTY,
                new PropertyChangeListener() {
                    @Override
                    public void propertyChange(PropertyChangeEvent evt) {
                        updateLogFile();
                    }
                });

        //Make sure we have nice window decorations.
        JFrame.setDefaultLookAndFeelDecorated(true);

        // close application if analyzer window is the last window closed
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        desktop = new JDesktopPane();
        desktop.setBackground(new Color(136, 152, 193));

//        playbackKeyAdapter = new KeyAdapter() {
//            @Override
//            public void keyPressed(KeyEvent e) {
//                final int code = e.getKeyCode();
//                if (code == KeyEvent.VK_LEFT) {
//                    logModel.moveRelativeToCurrentFrame(-1);
//                } else if (code == KeyEvent.VK_RIGHT) {
//                    logModel.moveRelativeToCurrentFrame(1);
//                }
//            }
//        };

        createDesktop();
        sizeDesktop();

        setContentPane(desktop);
        setJMenuBar(createMenuBar());

        //Make dragging a little faster but perhaps uglier.
//        desktop.setDragMode(JDesktopPane.OUTLINE_DRAG_MODE);
        captureAllArrowKeyEventsForPlayback();
    }

    public void setConfigurationManager(ConfigurationManager configurationManager) {
        this.configurationManager = configurationManager;
        this.logModel.setLogFileDirectory(
                configurationManager.getLogDirectory());
    }

    public void loadLogFile(File logFile) {
        this.logModel.loadLogFile(logFile);
    }

    /**
     * Create desktop with the following basic layout:
     *
     * <pre>
     *     ------------------            --------------  ---------------------
     *     | playback panel |            | frame data |  | behavior analysis |
     *     ------------------            |            |  |                   |
     *     ----------------------------  |            |  |                   |
     *     | arena                    |  |            |  |                   |
     *     |                          |  --------------  |                   |
     *     |                          |  --------------  |                   |
     *     |                          |  | rules      |  |                   |
     *     |                          |  | parameters |  |                   |
     *     |                          |  --------------  |                   |
     *     |                          |  --------------  |                   |
     *     |                          |  | stimulus   |  |                   |
     *     |                          |  |            |  |                   |
     *     ----------------------------  --------------  ---------------------
     * </pre>
     */
    private void createDesktop() {

        // ---------------------------------
        playbackPanel = new PlaybackControlsPanel(logModel);
        JPanel pcContentPanel = playbackPanel.getContentPanel();
        pcContentPanel.setVisible(true);
        desktop.add(pcContentPanel);

        // note set bounds call is important for panel to be displayed
        final Dimension ps = pcContentPanel.getPreferredSize();
        pcContentPanel.setBounds(12, 10, ps.width, ps.height);

        // ---------------------------------
        int dataFrameX = 0;
        int dataFrameY = pcContentPanel.getY() + pcContentPanel.getHeight() +
                         10;

        ArenaPanel arenaPanel = new ArenaPanel(logModel);
        arenaFrame = createDataFrame("Arena",
                                     arenaPanel.getContentPanel(),
                                     new Point(dataFrameX, dataFrameY));
        desktop.add(arenaFrame);

        // ---------------------------------
        dataFrameX = arenaFrame.getWidth();
        dataFrameY = 0;
        LarvaFrameDataPanel larvaFrameDataPanel =
                new LarvaFrameDataPanel(logModel);

        frameDataFrame = createDataFrame("Frame Data",
                                         larvaFrameDataPanel.getContentPanel(),
                                         new Point(dataFrameX, dataFrameY));
        desktop.add(frameDataFrame);

        // ---------------------------------
        dataFrameY = frameDataFrame.getHeight();
        RulesParametersPanel rulesParametersPanel =
                new RulesParametersPanel(logModel);

        rulesParametersFrame =
                createDataFrame("Rules Parameters",
                                rulesParametersPanel.getContentPanel(),
                                new Point(dataFrameX, dataFrameY));
        desktop.add(rulesParametersFrame);

        // ---------------------------------
        dataFrameY = dataFrameY + rulesParametersFrame.getHeight();
        StimulusPanel stimulusPanel = new StimulusPanel(logModel);
        stimulusFrame = createDataFrame("Stimulus",
                                        stimulusPanel.getContentPanel(),
                                        new Point(dataFrameX, dataFrameY));
        desktop.add(stimulusFrame);

        // ---------------------------------
        dataFrameX = dataFrameX + rulesParametersFrame.getWidth();
        dataFrameY = 0;
        HistoryPanel historyPanel =
                new HistoryPanel(logModel);

        behaviorDataFrame = createDataFrame("Behavior Mode Analysis",
                                            historyPanel.getContentPanel(),
                                            new Point(dataFrameX, dataFrameY));
        desktop.add(behaviorDataFrame);
    }

    /**
     * Set the desktop bounds and resize data frames if desktop size is
     * too small for ideal layout.
     *
     * NOTE: Hoped to iconify frames by using internalFrame.setIcon(true) or
     * desktopManager.iconifyFrame(internalFrame).
     * This did not work well on Mac - icons were either missing
     * (only appearing after desktop resize) or could not be clicked.
     */
    private void sizeDesktop() {

        // indent big window 50 pixels from each edge of the screen
        final int desktopInset = 50;
        final Dimension screenSize =
                Toolkit.getDefaultToolkit().getScreenSize();
        final int maxDesktopWidth = screenSize.width  - (desktopInset * 2);
        final int maxDesktopHeight = screenSize.height - (desktopInset * 2);

        final int optimalHeight = stimulusFrame.getY() +
                                  stimulusFrame.getHeight();
        final int optimalWidth = behaviorDataFrame.getX() +
                                 behaviorDataFrame.getWidth();

        int desktopHeight = optimalHeight + 120;
        int desktopWidth = optimalWidth + 10;

        final int maxHeight = maxDesktopHeight - 100; // leave room for icons
        if (maxDesktopHeight < optimalHeight) {
            for (JInternalFrame frame : getOrderedDataFrames(true)) {
                if (frame.getHeight() > maxHeight) {
                    frame.setSize(frame.getWidth(), maxHeight);
                }
                frame.setLocation(50,0);
            }
            desktopHeight = maxDesktopHeight;
        }

        if (maxDesktopWidth < optimalWidth) {
            // keep behavior data frame as far right as possible
            // note: -20 makes things look better on Windows 7
            int x = maxDesktopWidth - behaviorDataFrame.getWidth() - 20;
            if (x < 0) {
                x = 0;
            }
            behaviorDataFrame.setLocation(x, behaviorDataFrame.getY());
            desktopWidth = maxDesktopWidth;
        }

        setBounds(desktopInset,
                  desktopInset,
                  desktopWidth,
                  desktopHeight);

        // move each frame to front in reverse order to properly layer frames
        // (note: move to back does not seem to work as well)
        for (JInternalFrame frame : getOrderedDataFrames(false)) {
            frame.moveToFront();
        }
    }

    private void captureAllArrowKeyEventsForPlayback() {
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(
                new KeyEventDispatcher() {
                    public boolean dispatchKeyEvent(KeyEvent e) {
                        boolean discardEvent = false;
                        final int code = e.getKeyCode();
                        if (code == KeyEvent.VK_RIGHT) {
                            if (e.getID() == KeyEvent.KEY_PRESSED) {
                                logModel.moveRelativeToCurrentFrame(1);
                                discardEvent = true;
                            }
                        } else if (code == KeyEvent.VK_LEFT) {
                            if (e.getID() == KeyEvent.KEY_PRESSED) {
                                logModel.moveRelativeToCurrentFrame(-1);
                                discardEvent = true;
                            }
                        }
                        return discardEvent;
                    }
                });
    }

    private JInternalFrame createDataFrame(String title,
                                           Container contentPane,
                                           Point location) {
        JInternalFrame dataFrame = new JInternalFrame(title,
                                                      true,
                                                      false,
                                                      true,
                                                      true);
//        dataFrame.addKeyListener(playbackKeyAdapter);

        dataFrame.setContentPane(contentPane);
//        dataFrame.setFocusable(true);
        dataFrame.setVisible(true);
        dataFrame.setLocation(location);
        dataFrame.pack();

        final int minWidth = 300;
        final int minHeight = 70;
        boolean changeSize = false;
        Dimension size = dataFrame.getPreferredSize();
        if (size.width < minWidth) {
            size.width = minWidth;
            changeSize = true;
        }
        if (size.height < minHeight) {
            size.height = minHeight;
            changeSize = true;
        }
        if (changeSize) {
            dataFrame.setSize(size);
        }

        return dataFrame;
    }

    private JInternalFrame[] getOrderedDataFrames(boolean topToBottom) {
        JInternalFrame[] frames = new JInternalFrame[]  {
                arenaFrame, behaviorDataFrame, frameDataFrame,
                rulesParametersFrame, stimulusFrame
        };
        if (! topToBottom) {
            JInternalFrame[] bottomToTop = new JInternalFrame[frames.length];
            int j = 0;
            for (int i = frames.length - 1; i >=0; i--) {
                bottomToTop[j] = frames[i];
                j++;
            }
            frames = bottomToTop;
        }
        return frames;
    }

    private JMenuBar createMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        menuBar.add(createFileMenu());
        navigateMenu = createNavigateMenu();
        navigateMenu.setEnabled(logModel.hasFrames());
        menuBar.add(navigateMenu);
        menuBar.add(createWindowMenu());
        return menuBar;
    }

    private JMenu createFileMenu() {
        JMenu menu = new JMenu("File");
        menu.setMnemonic(KeyEvent.VK_F);

        JMenuItem menuItem = new JMenuItem(new AbstractAction("Open Log File") {
            @Override
            public void actionPerformed(ActionEvent e) {
                selectFile();
            }
        });
        menuItem.setMnemonic(KeyEvent.VK_O);
        menuItem.setAccelerator(
                KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent.ALT_MASK));
        menu.add(menuItem);

        reImportLogMenuItem =
                new JMenuItem(new AbstractAction("Re-Import Log File") {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        reImportFile();
                    }
                });
        reImportLogMenuItem.setMnemonic(KeyEvent.VK_R);
        reImportLogMenuItem.setAccelerator(
                KeyStroke.getKeyStroke(KeyEvent.VK_R, InputEvent.ALT_MASK));
        reImportLogMenuItem.setEnabled(false);
        menu.add(reImportLogMenuItem);

        menu.add(new JSeparator());

        menuItem = new JMenuItem(new AbstractAction("Quit") {
            @Override
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });
        menuItem.setMnemonic(KeyEvent.VK_Q);
        menuItem.setAccelerator(
                KeyStroke.getKeyStroke(KeyEvent.VK_Q, InputEvent.ALT_MASK));
        menu.add(menuItem);

        return menu;
    }

    private JMenu createNavigateMenu() {
        JMenu menu = new JMenu("Navigate");
        menu.setMnemonic(KeyEvent.VK_N);

        JMenuItem menuItem = new JMenuItem(new AbstractAction("Previous Frame") {
            @Override
            public void actionPerformed(ActionEvent e) {
                logModel.moveRelativeToCurrentFrame(-1);
            }
        });

        menuItem.setMnemonic(KeyEvent.VK_LEFT);
        menuItem.setAccelerator(
                KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0));
        menu.add(menuItem);

        menuItem = new JMenuItem(new AbstractAction("Next Frame") {
            @Override
            public void actionPerformed(ActionEvent e) {
                logModel.moveRelativeToCurrentFrame(1);
            }
        });

        menuItem.setMnemonic(KeyEvent.VK_RIGHT);
        menuItem.setAccelerator(
                KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0));
        menu.add(menuItem);

        menu.addSeparator();

        menuItem = new JMenuItem(new AbstractAction("First Frame") {
            @Override
            public void actionPerformed(ActionEvent e) {
                logModel.moveToFirstFrame();
            }
        });

        menuItem.setMnemonic(KeyEvent.VK_F);
        menu.add(menuItem);

        menuItem = new JMenuItem(new AbstractAction("Last Frame") {
            @Override
            public void actionPerformed(ActionEvent e) {
                logModel.moveToLastFrame();
            }
        });

        menuItem.setMnemonic(KeyEvent.VK_L);
        menu.add(menuItem);

        menu.addSeparator();

        menuItem = new JMenuItem(new AbstractAction("Play") {
            @Override
            public void actionPerformed(ActionEvent e) {
                playbackPanel.startPlaying();
            }
        });

        menuItem.setMnemonic(KeyEvent.VK_P);
        menu.add(menuItem);

        menuItem = new JMenuItem(new AbstractAction("Stop") {
            @Override
            public void actionPerformed(ActionEvent e) {
                playbackPanel.stopPlaying();
            }
        });

        menuItem.setMnemonic(KeyEvent.VK_S);
        menu.add(menuItem);

        return menu;
    }

    private void selectFile() {
        JFileChooser fileChooser =
                new JFileChooser(logModel.getLogFileDirectory());
        int choice = fileChooser.showDialog(desktop, "Select Log File");

        if (choice == JFileChooser.APPROVE_OPTION) {
            logModel.loadLogFile(fileChooser.getSelectedFile());
        }
    }

    private void reImportFile() {
        ImportLogWorker importWorker =
                new ImportLogWorker(configurationManager,
                                    logModel.getLogFile(),
                                    desktop.getLocation());
        importWorker.execute(); // import in background thread
    }

    private JMenu createWindowMenu() {
        JMenu menu = new JMenu("Window");
        menu.setMnemonic(KeyEvent.VK_W);

        final JInternalFrame[] orderedFrames = getOrderedDataFrames(true);
        for (JInternalFrame frame : orderedFrames) {
            menu.add(new DataWindow(frame).getMenu());
        }

        menu.addSeparator();

        menu.add(new JMenuItem(new AbstractAction("Show All") {
            @Override
            public void actionPerformed(ActionEvent e) {
                // note: reverse order affects window overlays
                for (JInternalFrame frame : getOrderedDataFrames(false)) {
                    showOrHideFrame(frame, true);
                }
            }
        }));

        menu.add(new JMenuItem(new AbstractAction("Hide All") {
            @Override
            public void actionPerformed(ActionEvent e) {
                for (JInternalFrame frame : orderedFrames) {
                    showOrHideFrame(frame, false);
                }
            }
        }));

        return menu;
    }

    private class DataWindow {
        private JInternalFrame frame;
        private JMenu menu;

        public DataWindow(JInternalFrame frame) {
            this.frame = frame;
            JMenuItem showMenuItem = new JMenuItem(
                    new AbstractAction("Show") {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            showOrHide(true);
                        }
                    });
            JMenuItem hideMenuItem = new JMenuItem(
                    new AbstractAction("Hide") {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            showOrHide(false);
                        }
                    });
            this.menu = new JMenu(frame.getTitle());
            this.menu.add(showMenuItem);
            this.menu.add(hideMenuItem);
        }

        public JMenu getMenu() {
            return menu;
        }

        private void showOrHide(boolean show) {
            showOrHideFrame(frame, show);
        }

    }

    private static void showOrHideFrame(JInternalFrame frame,
                                        boolean show) {
        try {
            if (show && (! frame.isIcon())) {
                frame.moveToFront();
            } else {
                frame.setIcon(! show);
            }

            // move frames whose title bars are hidden beneath desktop menu
            if (show) {
                final Point location = frame.getLocation();
                if (location.getY() < 0) {
                    frame.setLocation((int) location.getX(), 0);
                }
            }
        } catch (PropertyVetoException e) {
            LOG.warn("ignoring error", e);
        }
    }

    private void updateLogFile() {
        final File logFile = logModel.getLogFile();
        if (logFile != null) {
            setTitle(logFile.getName());
        } else {
            setTitle("Venkman Log Analyzer");
        }
        reImportLogMenuItem.setEnabled(logModel.hasFrames());
        navigateMenu.setEnabled(logModel.hasFrames());
    }

    private static final Logger LOG = Logger.getLogger(LogAnalyzer.class);
}

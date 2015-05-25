/*
 * Copyright (c) 2012 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.venkman.gui.log;

import org.apache.log4j.Logger;
import org.janelia.it.venkman.config.ConfigurationManager;

import javax.swing.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;

/**
 * Simple application to launch the log analyzer.
 *
 * @author Eric Trautman
 */
public class VenkmanLogAnalyzer
        extends JFrame {

    private static LogAnalyzer instance = null;

    public static synchronized LogAnalyzer getInstance() {
        if (instance == null) {
            instance = new LogAnalyzer();
        }
        return instance;
    }

    public static void display(ConfigurationManager configurationManager) {
        display(configurationManager, null);
    }

    public static void display(ConfigurationManager configurationManager,
                               File logFile) {

        LogAnalyzer analyzer = getInstance();
        analyzer.setConfigurationManager(configurationManager);
        analyzer.loadLogFile(logFile);
        analyzer.setVisible(true);
    }

    public static void main(String[] args) {
        String workingDirectoryName = ".";
        if (args.length > 0) {
            workingDirectoryName = args[0];
        }
        final File workingDirectory = new File(workingDirectoryName);

        final ConfigurationManager manager =
                new ConfigurationManager(workingDirectory);

        // force log analyzer L&F to system L&F
        try {
            String lookAndFeel = UIManager.getSystemLookAndFeelClassName();
            UIManager.setLookAndFeel(lookAndFeel);
        } catch( Exception e ) {
            LOG.warn("failed to change UI to system look and feel", e);
        }

        LogAnalyzer analyzer = getInstance();
        analyzer.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                System.exit(0);
            }
        });

        //Schedule a job for the event-dispatching thread:
        //creating and showing this application's GUI.
        javax.swing.SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                display(manager);
            }
        });
    }

    private static final Logger LOG =
            Logger.getLogger(VenkmanLogAnalyzer.class);
}

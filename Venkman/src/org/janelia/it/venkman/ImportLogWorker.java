/*
 * Copyright (c) 2014 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.venkman;

import org.apache.log4j.Logger;
import org.janelia.it.venkman.config.Configuration;
import org.janelia.it.venkman.config.ConfigurationManager;
import org.janelia.it.venkman.config.LarvaBehaviorParameters;
import org.janelia.it.venkman.config.ParameterCollectionCategory;
import org.janelia.it.venkman.config.ParameterCollectionId;
import org.janelia.it.venkman.config.rules.LEDStimulus;
import org.janelia.it.venkman.data.LarvaFrameData;
import org.janelia.it.venkman.gui.Dashboard;
import org.janelia.it.venkman.gui.NarrowOptionPane;
import org.janelia.it.venkman.gui.collection.ImportBehaviorDialog;
import org.janelia.it.venkman.gui.log.VenkmanLogAnalyzer;
import org.janelia.it.venkman.log.LogReader;
import org.janelia.it.venkman.log.LogSession;
import org.janelia.it.venkman.message.ResponseMessage;
import org.janelia.it.venkman.rules.ImportedStimulus;
import org.janelia.it.venkman.rules.LarvaStimulusRules;

import javax.swing.*;
import java.awt.*;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Imports and processes tracker data from a venkman log file.
 *
 * @author Eric Trautman
 */
public class ImportLogWorker
        extends SwingWorker<Void, Integer> {

    private ConfigurationManager configurationManager;
    private String configurationName;
    private File logFile;
    private Point behaviorDialogLocation;
    private boolean manuallyEnterConfigurationData;
    private boolean isBatchMode;
    private String sessionId;
    
    public ImportLogWorker(ConfigurationManager configurationManager,
                           File logFile,
                           Point behaviorDialogLocation) {
        this(configurationManager, 
             "import/manual-entry", 
             logFile, 
             behaviorDialogLocation, 
             true,
             false);
    }

    public ImportLogWorker(ConfigurationManager configurationManager,
                           String configurationName,
                           File logFile) {
        this(configurationManager, 
             configurationName, 
             logFile, 
             false);
    }

    public ImportLogWorker(ConfigurationManager configurationManager,
                           String configurationName,
                           File logFile,
                           boolean isBatchMode) {
        this(configurationManager,
             configurationName,
             logFile,
             null,
             false,
             isBatchMode);
    }

    public ImportLogWorker(ConfigurationManager configurationManager,
                           String configurationName,
                           File logFile,
                           Point behaviorDialogLocation,
                           boolean manuallyEnterConfigurationData,
                           boolean isBatchMode) {
        this.configurationManager = configurationManager;
        this.configurationName = configurationName;
        this.logFile = logFile;
        Matcher m = LOG_NAME.matcher(logFile.getName());
        if (m.matches() && (m.groupCount() == 1)) {
            this.sessionId = "import-" + m.group(1);
        } else {
            this.sessionId = "import-" + logFile.getName();
        }
        this.behaviorDialogLocation = behaviorDialogLocation;
        this.manuallyEnterConfigurationData = manuallyEnterConfigurationData;
        this.isBatchMode = isBatchMode;
    }

    /**
     * Imports skeleton data from the log file and sends it through
     * a rules session instance to be processed again with the selected
     * configuration parameters, logging the results in a new log file.
     *
     * @return nothing.
     *
     * @throws Exception
     *   if a failure occurs during the import process.
     */
    @Override
    protected Void doInBackground()
            throws Exception {

        try {
            RulesSession session =
                    new RulesSession(sessionId,
                                     new ByteArrayInputStream(new byte[0]),
                                     new ByteArrayOutputStream(),
                                     configurationManager);

            LogReader reader = new LogReader(logFile);

            // TODO: handle large files in some reasonable manner
            reader.read();
            final LogSession logSession = reader.getSession();

            ResponseMessage openResponse;
            if (manuallyEnterConfigurationData) {
                
                openResponse =
                        openSessionWithManuallyEnteredData(
                                session,
                                logSession.getLarvaBehaviorParameters(),
                                logSession.getLarvaStimulusRules());

            } else if (isBatchMode) {

                openResponse =
                        openSessionWithImportedStimulus(
                                session,
                                logSession.getFrameDataList(),
                                logSession.getStimulusList());

            } else {
                openResponse = session.openSession(configurationName, UNDEFINED_VERSION);
            }            

            LOG.debug("open session response is: " + openResponse);

            if ((openResponse != null) &&
                (openResponse.getStatusCode() == ResponseMessage.STATUS_OK)) {

                int frameCount = 0;
                final List<LarvaFrameData> list = logSession.getFrameDataList();
                final int totalFrames = list.size();
                for (LarvaFrameData frameData : list) {
                    session.processLarvaSkeleton(frameData.getSkeleton(), UNDEFINED_VERSION);
                    frameCount++;
                    if ((frameCount % 1800) == 0) {
                        LOG.debug("processed " + frameCount + " frames (" +
                                  ((frameCount * 100) / totalFrames) + "%)");
                    }
                }

                LOG.debug("processed " + frameCount + " frames (100%)");

                session.close();

                final String sessionLogFilePathAndName = 
                        session.getLogFilePathAndName();
                final String message = 
                        "The following log file was successfully imported:\n" + 
                        logFile.getAbsolutePath() + 
                        "\n\nA new import processing results log file has " +
                        "been saved here:\n" + sessionLogFilePathAndName;

                if (isBatchMode) {

                    LOG.info(message);

                } else {

                    final String reviewPrompt =
                            "\n\nDo you want to review the new import  " +
                            "processing results in the Log Analyzer?";

                    SwingUtilities.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            final int choice =
                                    NarrowOptionPane.showConfirmDialog(
                                            Dashboard.getDashboardFrame(),
                                            message + reviewPrompt,
                                            "Import Succeeded",
                                            JOptionPane.YES_NO_OPTION);
                            if (choice == JOptionPane.YES_OPTION) {
                                VenkmanLogAnalyzer.display(
                                        configurationManager,
                                        new File(sessionLogFilePathAndName));
                            }

                        }
                    });
                }
            }
        } catch (Exception e) {
            
            LOG.error("import failure", e);

            // TODO: handle non-splittable exception messages
            
            final String message = "Import of " + logFile.getAbsolutePath() +
                                   " failed with error:\n\n" +
                                   e.getMessage() +
                                   "\n\nSee launch window for more details.";
            
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    NarrowOptionPane.showMessageDialog(
                            Dashboard.getDashboardFrame(),
                            message,
                            "Import Failed",
                            JOptionPane.ERROR_MESSAGE);
                }
            });
             
            throw e;
        }

        return null;
    }

    private ResponseMessage openSessionWithManuallyEnteredData(RulesSession session,
                                                               LarvaBehaviorParameters behaviorParameters,
                                                               LarvaStimulusRules rules) {

        ResponseMessage openResponse = null;

        ImportBehaviorDialog dialog = new ImportBehaviorDialog(behaviorParameters);
        dialog.setLocation(behaviorDialogLocation);
        dialog.pack();
        dialog.setModal(true);
        dialog.setVisible(true);

        behaviorParameters = dialog.getBehaviorParameters();

        if (behaviorParameters != null) {
            final ParameterCollectionId configurationId =
                    new ParameterCollectionId(
                            ParameterCollectionCategory.CONFIGURATION,
                            configurationName);
            final ParameterCollectionId behaviorId =
                    new ParameterCollectionId(
                            ParameterCollectionCategory.BEHAVIOR,
                            configurationName);
            final Configuration configuration =
                    new Configuration(configurationId, behaviorId, null);

            openResponse = session.openSession(configuration,
                                               behaviorParameters,
                                               rules,
                                               UNDEFINED_VERSION);
        } else {
            LOG.debug("import cancelled from edit behavior dialog");
        }

        return openResponse;
    }

    private ResponseMessage openSessionWithImportedStimulus(RulesSession session,
                                                            List<LarvaFrameData> importedFrameData,
                                                            List<LEDStimulus> importedStimulusList) {

        final Configuration configuration =
                configurationManager.getConfiguration(configurationName);
        final LarvaBehaviorParameters behaviorParameters =
                configurationManager.getBehaviorParameters(
                        configuration.getBehaviorParametersId());
        final ImportedStimulus stimulusRules =
                new ImportedStimulus(logFile.getAbsolutePath(),
                                     importedFrameData,
                                     importedStimulusList);

        return session.openSession(configuration,
                                   behaviorParameters,
                                   stimulusRules,
                                   UNDEFINED_VERSION);
    }

    private static final Logger LOG = Logger.getLogger(ImportLogWorker.class);

    private static final Pattern LOG_NAME =
            Pattern.compile("venkman-log-(\\d{8}-\\d{9}).*");

    public static void main(String[] args) {

        final String usage =
                "\nUSAGE: java -cp venkman.jar " +
                ImportLogWorker.class.getName() +
                " <configuration name> <log file> [log file ...]\n";

        if (args.length < 2) {
            System.out.println(usage);
            System.exit(1);
        }

        try {
            final String configurationName = args[0];
            final File workingDirectory = new File(".");
            final ConfigurationManager manager =
                    new ConfigurationManager(workingDirectory);

            final Configuration config =
                    manager.getConfiguration(configurationName);
            if (config == null) {
                System.out.println(
                        "\nERROR: configuration '" + configurationName +
                        "' cannot be found.\n");
                System.out.println("Valid configuration names are: ");
                for (String name : manager.getConfigurationNames()) {
                    System.out.println("  " + name);
                }
                System.out.println(usage);
                System.exit(1);
            }

            ImportLogWorker worker;
            File logFile;
            for (int i = 1; i < args.length; i++) {
                logFile = new File(args[i]);
                if (logFile.exists() && logFile.canRead()) {
                    worker = new ImportLogWorker(manager,
                                                 configurationName,
                                                 logFile,
                                                 true);
                    worker.doInBackground();
                } else {
                    LOG.warn("skipping missing or non-readable log file " +
                             logFile.getAbsolutePath());
                }
            }
        } catch (Throwable t) {
            LOG.error("import processing failed", t);
        }
    }

    // The message protocol version is not important for imported data,
    // so this noticeably different value is defined here and used whenever versions are expected.
    private static final String UNDEFINED_VERSION = "0.0";
}

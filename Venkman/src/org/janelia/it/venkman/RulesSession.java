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
import org.janelia.it.venkman.config.ParameterCollectionId;
import org.janelia.it.venkman.config.rules.Stimulus;
import org.janelia.it.venkman.data.LarvaFrameData;
import org.janelia.it.venkman.data.LarvaSkeleton;
import org.janelia.it.venkman.jaxb.MarshallerCache;
import org.janelia.it.venkman.message.ArenaBackgroundResponse;
import org.janelia.it.venkman.message.GetSessionParametersResponse;
import org.janelia.it.venkman.message.ListConfigurationsResponse;
import org.janelia.it.venkman.message.Message;
import org.janelia.it.venkman.message.MessageType;
import org.janelia.it.venkman.message.OpenSessionResponse;
import org.janelia.it.venkman.message.ProcessLarvaSkeletonResponse;
import org.janelia.it.venkman.message.ResponseMessage;
import org.janelia.it.venkman.message.StatusResponse;
import org.janelia.it.venkman.rules.LarvaStimulusRules;
import org.janelia.it.venkman.rules.TrackerArenaProvider;

import javax.xml.bind.JAXBException;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedList;
import java.util.List;

/**
 * Manages communication with tracker for a single rules session.
 *
 * @author Eric Trautman
 */
public class RulesSession implements Runnable {

    private String sessionId;
    private BufferedReader in;
    private PrintWriter out;
    private ConfigurationManager configurationManager;
    private boolean continueProcessing;

    /** The session logger (or null if logging is disabled) */
    private org.janelia.it.venkman.log.Logger logger;

    /**
     * History of data derived for received video frames.
     * Data for the most recent frame is at the beginning of the list.
     */
    private LinkedList<LarvaFrameData> frameHistory;

    /** The configurable behavior parameters for frame data calculations. */
    private LarvaBehaviorParameters behaviorParameters;

    private LarvaStimulusRules stimulusRules;

    /** Cache of JAXB marshaller instances (for writing stimulus parameters). */
    private MarshallerCache marshallerCache;

    private long totalLarvaSkeletonRequestNanoseconds;
    private long numberOfLarvaSkeletonRequests;

    public RulesSession(String sessionId,
                        InputStream in,
                        OutputStream out,
                        ConfigurationManager configurationManager) {
        this.sessionId = sessionId;
        this.in = new BufferedReader(new InputStreamReader(in));
        this.out = new PrintWriter(out, true);
        this.configurationManager = configurationManager;
        this.continueProcessing = true;
        this.logger = new org.janelia.it.venkman.log.Logger(
                configurationManager.getLogDirectory(),
                sessionId);

        this.frameHistory = new LinkedList<LarvaFrameData>();
        this.behaviorParameters = null;
        this.stimulusRules = null;
        this.marshallerCache = new MarshallerCache();
        this.totalLarvaSkeletonRequestNanoseconds = 0;
        this.numberOfLarvaSkeletonRequests = 0;
    }

    public String getSessionId() {
        return sessionId;
    }

    public String getLogFilePathAndName() {
        String pathAndName = null;
        if (logger != null) {
            pathAndName = logger.getLogFilePathAndName();
        }
        return pathAndName;
    }

    /**
     * Process request messages from this session's input stream until
     * a close session message is received or an unrecoverable error occurs.
     */
    @Override
    public void run() {

        boolean isLarvaSkeletonRequest;
        long requestStartTime;

        ResponseMessage response;
        try {

            String request;
            Message message;
            MessageType type = null;
            while (continueProcessing && (request = in.readLine()) != null) {

                isLarvaSkeletonRequest = false;
                requestStartTime = System.nanoTime();

                try {
                    message = Message.getRequestMessage(request);

                    if (type != MessageType.PROCESS_LARVA_SKELETON_REQUEST) {
                        logMessageToConsole("received", message);
                    }

                    type = message.getType();
                    switch (type) {

                        case PROCESS_LARVA_SKELETON_REQUEST:

                            isLarvaSkeletonRequest = true;

                            response = validateSessionId(message);
                            if (response == null) {
                                final LarvaSkeleton skeleton = new LarvaSkeleton(
                                        message.getField(1),   // frame time
                                        message.getField(2),   // head x
                                        message.getField(3),   // head y
                                        message.getField(4),   // midpoint x
                                        message.getField(5),   // midpoint y
                                        message.getField(6),   // tail x
                                        message.getField(7),   // tail y
                                        message.getField(8),   // length
                                        message.getField(9),   // centroid x
                                        message.getField(10),  // centroid y
                                        message.getField(11),  // head to body angle
                                        message.getField(12)); // tail bearing

                                response = processLarvaSkeleton(skeleton, message.getVersion());
                            }
                            break;

                        case LIST_CONFIGURATIONS_REQUEST:
                            response = listConfigurations(message.getVersion());
                            break;

                        case ARENA_BACKGROUND_REQUEST:
                            response = getArenaBackground(message.getField(1),
                                                          message.getField(2),
                                                          message.getField(3));
                            break;

                        case OPEN_SESSION_REQUEST:
                            response = openSession(message.getField(1),
                                                   message.getVersion());
                            break;

                        case GET_SESSION_PARAMETERS_REQUEST:
                            response = validateSessionId(message);
                            if (response == null) {
                                response = getSessionParameters();
                            }
                            break;

                        case CLOSE_SESSION_REQUEST:
                            response = validateSessionId(message);
                            if (response == null) {
                                continueProcessing = false;
                                response = new StatusResponse(
                                        ResponseMessage.STATUS_OK,
                                        getCloseSessionMessage());
                            }
                            break;

                        default:
                            throw new IllegalArgumentException(
                                    "invalid request: type '" + type.getName() +
                                    "' not supported");
                    }

                } catch (IllegalArgumentException e) {

                    LOG.error("run: bad request", e);
                    response = StatusResponse.badRequest(e.getMessage());

                } catch (Exception e) {

                    LOG.error("run: server error", e);
                    response = StatusResponse.serverError(e);

                }

                if (type != MessageType.PROCESS_LARVA_SKELETON_REQUEST) {
                    logMessageToConsole("returning", response);
                }

                out.println(response);

                if (isLarvaSkeletonRequest) {
                    totalLarvaSkeletonRequestNanoseconds += System.nanoTime() - requestStartTime;
                    numberOfLarvaSkeletonRequests++;
                }
            }

        } catch (Throwable t) {

            final String message = getCloseSessionMessage();
            LOG.error("run: server error, " + message, t);
            response = StatusResponse.serverError(message, t);

            try {
                out.println(response);
                logMessageToConsole("returning", response);
            } catch (Exception e1) {
                LOG.error("run: failed to send error response", e1);
            }

        } finally {
            close();
        }

    }

    private void logMessageToConsole(String context,
                                     Message message) {
        if (LOG.isDebugEnabled()) {
            final String text = message.toString();
            String logText;
            if (text.length() > 110) {
                logText = text.substring(0,50) + " ... " +
                          text.substring(text.length() - 50);
            } else {
                logText = text;
            }
            LOG.debug(context + " (" + (message.size() + 2) +
                      " fields): " + logText);
        }
    }

    public ResponseMessage processLarvaSkeleton(LarvaSkeleton skeleton,
                                                String version) {

        LarvaFrameData frameData = new LarvaFrameData(skeleton);

        frameData.calculateDerivedData(frameHistory, behaviorParameters);

        frameHistory.addFirst(frameData);

        final List<? extends Stimulus> currentStimulusList;
        if (stimulusRules == null) {
            currentStimulusList = null;
        } else {
            currentStimulusList =
                    stimulusRules.determineStimulus(frameHistory,
                                                    behaviorParameters);
            frameData.setStimulusList(currentStimulusList);
        }

        // log frame data after determineStimulus call in case
        // call changes any of the data
        logger.log(frameData);

        // TODO: manage frame history size?

        ProcessLarvaSkeletonResponse skeletonResponse =
                new ProcessLarvaSkeletonResponse(
                        version,
                        String.valueOf(skeleton.getCaptureTime()),
                        String.valueOf(frameData.getBehaviorMode()));

        if (currentStimulusList != null) {
            for (Stimulus currentStimulus : currentStimulusList) {
                currentStimulus.addFieldsToMessage(skeletonResponse);
            }
        }

        return skeletonResponse;
    }

    public void close() {
        continueProcessing = false;

        try {
            logLarvaRequestProcessingStats();
            logger.stopLogging();
        } catch (Throwable t) {
            LOG.error("failed to stop rules processor logging", t);
        }

        closeStream(in);
        in = null;

        closeStream(out);
        out = null;
    }

    public boolean isClosed() {
        return (! continueProcessing);
    }

    @Override
    public String toString() {
        return "RulesSession{" +
               "sessionId='" + sessionId + '\'' +
               ", isClosed='" + isClosed() + '\'' +
               '}';
    }

    protected ResponseMessage listConfigurations(String version) {

        ListConfigurationsResponse response = new ListConfigurationsResponse(version);
        for (String name : configurationManager.getConfigurationNames(version)) {
            response.addConfigurationName(name);
        }

        return response;
    }

    protected ResponseMessage getArenaBackground(String configurationName,
                                                 String bgWidth,
                                                 String bgHeight) {

        ResponseMessage response;

        Configuration configuration =
                configurationManager.getConfiguration(configurationName);

        if (configuration == null) {
            response = StatusResponse.notFound(
                    "invalid arena background request: configuration '" +
                    configurationName + "' not found");
        } else {

            LarvaStimulusRules rules = null;
            final ParameterCollectionId stimulusId =
                    configuration.getStimulusParametersId();
            if (stimulusId != null) {
                rules = configurationManager.getStimulusRules(stimulusId);
            }
            if (rules instanceof TrackerArenaProvider) {
                TrackerArenaProvider provider =
                        (TrackerArenaProvider) rules;
                final int width = Integer.parseInt(bgWidth);
                final int height = Integer.parseInt(bgHeight);
                final double[][] arena = provider.getArena(width, height);
                response = new ArenaBackgroundResponse(arena);
            } else {
                response = new ArenaBackgroundResponse();
            }

        }

        return response;
    }

    protected ResponseMessage openSession(String configurationName,
                                          String version) {

        ResponseMessage response;

        Configuration configuration =
                configurationManager.getConfiguration(configurationName);

        if (configuration == null) {
            response = StatusResponse.notFound(
                    "invalid open session request: configuration '" +
                    configurationName + "' not found");
        } else {
            LarvaBehaviorParameters behaviorParameters =
                    configurationManager.getBehaviorParameters(
                            configuration.getBehaviorParametersId());
            if (behaviorParameters == null) {
                throw new IllegalStateException(
                        "failed to load behavior parameters for " +
                        configurationName);
            }

            LarvaStimulusRules stimulusRules = null;
            final ParameterCollectionId stimulusId =
                    configuration.getStimulusParametersId();
            if (stimulusId != null) {
                stimulusRules =
                        configurationManager.getStimulusRules(stimulusId);
            }

            response = openSession(configuration,
                                   behaviorParameters,
                                   stimulusRules,
                                   version);
        }

        return response;
    }

    protected ResponseMessage openSession(Configuration configuration,
                                          LarvaBehaviorParameters behaviorParameters,
                                          LarvaStimulusRules stimulusRules,
                                          String version) {

        this.behaviorParameters = behaviorParameters;
        this.stimulusRules = stimulusRules;
        if (stimulusRules != null) {
            stimulusRules.init(logger);
            this.behaviorParameters = stimulusRules.overrideBehaviorParameters(behaviorParameters);
        }

        ResponseMessage response = new OpenSessionResponse(version, sessionId);

        logger.log(configuration);
        logger.log(behaviorParameters);
        if (stimulusRules != null) {
            logger.log(stimulusRules);
        }

        return response;
    }

    private ResponseMessage getSessionParameters()
            throws JAXBException, IOException {
        final int bufferSize = 2000000; // default to 2MB buffer
        ByteArrayOutputStream out = new ByteArrayOutputStream(bufferSize);
        out.write("<venkmanParameters>\n".getBytes());
        marshallerCache.marshal(behaviorParameters, out);
        if (stimulusRules != null) {
            marshallerCache.marshal(stimulusRules, out);
        }
        out.write("\n</venkmanParameters>".getBytes());
        GetSessionParametersResponse response =
                new GetSessionParametersResponse();
        response.addParameters(out.toByteArray());
        return response;
    }

    private ResponseMessage validateSessionId(Message message) {

        ResponseMessage response = null;

        final String requestSessionId = message.getField(0);
        if (! sessionId.equals(requestSessionId)) {
            response = StatusResponse.badRequest(
                    "invalid request: expected session id '" +
                    sessionId + "' but received '" + requestSessionId + "'");
        }

        return response;
    }

    private String getCloseSessionMessage() {
        return "closed session " + sessionId;
    }

    private void closeStream(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (Exception e) {
                LOG.warn("failed to close stream, ignoring error", e);
            }
        }
    }

    private void logLarvaRequestProcessingStats() {
        if (totalLarvaSkeletonRequestNanoseconds > 0) {

            final int nanosecondsInMillisecond = 1000000;
            final long totalLarvaSkeletonRequestMilliseconds =
                    totalLarvaSkeletonRequestNanoseconds / nanosecondsInMillisecond;

            final double avgResponseNanoseconds =
                    (double) totalLarvaSkeletonRequestNanoseconds / numberOfLarvaSkeletonRequests;
            final double avgResponseMilliseconds = avgResponseNanoseconds / nanosecondsInMillisecond;
            final BigDecimal scaledAvgResponseMilliseconds =
                    new BigDecimal(avgResponseMilliseconds).setScale(3, RoundingMode.HALF_UP);
            logger.logMessage("processed " + numberOfLarvaSkeletonRequests +
                              " larva skeleton requests in " + totalLarvaSkeletonRequestMilliseconds +
                              " ms, average response time: " + scaledAvgResponseMilliseconds + " ms/request");
        }
    }

    private static final Logger LOG = Logger.getLogger(RulesSession.class);
}

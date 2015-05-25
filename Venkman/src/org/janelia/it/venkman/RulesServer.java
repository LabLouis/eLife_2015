/*
 * Copyright (c) 2014 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.venkman;

import org.apache.log4j.Logger;
import org.janelia.it.venkman.config.ConfigurationManager;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

/**
 * Simple server that listens on a port for tracker connection requests.
 * A new rules session thread is created for each connection.
 *
 * @author Eric Trautman
 */
public class RulesServer implements Runnable {

    private int port;
    private List<RulesSession> sessions;
    private boolean continueProcessing;
    private ServerSocket serverSocket;
    private ConfigurationManager configurationManager;

    public RulesServer(int port,
                       File workDirectory) {
        this.port = port;
        this.sessions = new ArrayList<RulesSession>();
        this.continueProcessing = true;
        this.serverSocket = null;
        this.configurationManager = new ConfigurationManager(workDirectory);
    }

    public void run() {

        LOG.info("run: entry, accepting requests on port " + port);

        try {
            serverSocket = new ServerSocket(port);
            Socket clientSocket;
            String sessionId;
            RulesSession session;
            Thread sessionThread;
            while (continueProcessing) {
                clientSocket = serverSocket.accept();
                sessionId = "sid-" + sessions.size();
                session = buildSession(sessionId, clientSocket);
                sessionThread = new Thread(session);
                sessionThread.start();

                cleanUpClosedSessionReferences();
                sessions.add(session);
            }
        } catch (Exception e) {
            LOG.error("run: caught exception while attempting " +
                      "to listen on port " + port + ", sessions=" +
                      sessions, e);
        } finally {
            stop();
        }

        LOG.info("run: exit");
    }

    public void stop() {
        continueProcessing = false;

        if (serverSocket != null) {
            try {
                serverSocket.close(); // should force accept to throw exception
                serverSocket = null;
            } catch (Exception e) {
                LOG.error("closeServerSocket: failed to close server socket " +
                          "on port " + port);
            }
        }

        for (RulesSession session : sessions) {
            if (session != null) {
                session.close();
            }
        }
    }

    protected ConfigurationManager getConfigurationManager() {
        return configurationManager;
    }

    protected RulesSession buildSession(String sessionId,
                                        Socket clientSocket)
            throws IOException {
        return new RulesSession(sessionId,
                                clientSocket.getInputStream(),
                                clientSocket.getOutputStream(),
                                configurationManager);
    }

    private void cleanUpClosedSessionReferences() {
        RulesSession session;
        for (int i = 0; i < sessions.size(); i++) {
            session = sessions.get(i);
            if ((session != null) && session.isClosed()) {
                sessions.set(i, null);
            }
        }
    }

    private static final Logger LOG = Logger.getLogger(RulesServer.class);
}

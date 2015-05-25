/*
 * Copyright (c) 2014 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.venkman.test;

import org.apache.log4j.Logger;
import org.janelia.it.venkman.RulesServer;
import org.janelia.it.venkman.RulesSession;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

/**
 * Mock rules server for testing.
 *
 * @author Eric Trautman
 */
public class MockRulesServer extends RulesServer {

    private List<String> skeletonResponses;

    public MockRulesServer(int port,
                           File file) throws IOException {
        super(port, file.getCanonicalFile().getParentFile());

        this.skeletonResponses = new ArrayList<String>();
        BufferedReader reader = null;

        try {
            reader = new BufferedReader(new FileReader(file));
            String line;
            while ((line = reader.readLine()) != null) {
                this.skeletonResponses.add(line);
            }
            LOG.info("loaded " + skeletonResponses.size() + " responses from " + file.getAbsolutePath());
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    LOG.warn("ignoring file close exception", e);
                }
            }
        }

    }

    protected RulesSession buildSession(String sessionId,
                                        Socket clientSocket)
            throws IOException {
        return new MockRulesSession(sessionId,
                                    clientSocket.getInputStream(),
                                    clientSocket.getOutputStream(),
                                    getConfigurationManager(),
                                    skeletonResponses);
    }

    private static final Logger LOG = Logger.getLogger(MockRulesServer.class);
}

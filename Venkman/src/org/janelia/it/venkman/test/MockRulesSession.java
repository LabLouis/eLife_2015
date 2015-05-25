/*
 * Copyright (c) 2014 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.venkman.test;

import org.janelia.it.venkman.RulesSession;
import org.janelia.it.venkman.config.ConfigurationManager;
import org.janelia.it.venkman.data.LarvaSkeleton;
import org.janelia.it.venkman.message.ListConfigurationsResponse;
import org.janelia.it.venkman.message.OpenSessionResponse;
import org.janelia.it.venkman.message.ProcessLarvaSkeletonResponse;
import org.janelia.it.venkman.message.ResponseMessage;
import org.janelia.it.venkman.message.StatusResponse;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.List;

/**
 * Mock rules session for testing.
 *
 * @author Eric Trautman
 */
public class MockRulesSession extends RulesSession {

    private List<String> skeletonResponses;
    private int index;


    public MockRulesSession(String sessionId,
                            InputStream in,
                            OutputStream out,
                            ConfigurationManager configurationManager,
                            List<String> skeletonResponses) {
        super(sessionId, in, out, configurationManager);
        this.skeletonResponses = skeletonResponses;
        this.index = -1;
    }

    @Override
    protected ResponseMessage listConfigurations(String version) {

        System.out.println("received list request");

        ListConfigurationsResponse response = new ListConfigurationsResponse(version);
        for (String name : CONFIGURATION_NAMES) {
            response.addConfigurationName(name);
        }

        return response;
    }

    @Override
    protected ResponseMessage openSession(String configurationName,
                                          String version) {

        System.out.println("received open request");

        ResponseMessage response;

        if (CONFIGURATION_NAMES.contains(configurationName)) {

            response = new OpenSessionResponse(version, getSessionId());

        } else {

            response = StatusResponse.notFound(
                    "invalid open session request: configuration '" +
                    configurationName + "' not found");
        }

        return response;
    }

    @Override
    public ResponseMessage processLarvaSkeleton(LarvaSkeleton skeleton,
                                                String version) {

        index++;
        if (index >= skeletonResponses.size()) {
            index = 0;
        }

        String fieldString = skeletonResponses.get(index);
        String[] fields = fieldString.split(",");
        ProcessLarvaSkeletonResponse skeletonResponse =
                new ProcessLarvaSkeletonResponse(
                        version,
                        String.valueOf(skeleton.getCaptureTime()),
                        fields[0]);

        for (int i = 1; i < fields.length; i++) {
            skeletonResponse.addField(fields[i]);
        }

        return skeletonResponse;
    }

    private static final List<String> CONFIGURATION_NAMES =
            Arrays.asList("test-configuration-a", "test-configuration-b");

}

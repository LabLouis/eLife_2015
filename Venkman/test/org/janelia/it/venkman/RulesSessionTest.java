/*
 * Copyright (c) 2014 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.venkman;

import junit.framework.Assert;
import org.janelia.it.venkman.config.Configuration;
import org.janelia.it.venkman.config.ConfigurationManager;
import org.janelia.it.venkman.config.LarvaBehaviorParameters;
import org.janelia.it.venkman.config.ParameterCollectionCategory;
import org.janelia.it.venkman.config.ParameterCollectionId;
import org.janelia.it.venkman.config.rules.BehaviorLimitedKinematicVariableFunctionList;
import org.janelia.it.venkman.config.rules.IntensityValue;
import org.janelia.it.venkman.config.rules.LEDFlashPattern;
import org.janelia.it.venkman.config.rules.SingleVariableFunction;
import org.janelia.it.venkman.rules.ScaledRunIntensity;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;

import static org.janelia.it.venkman.rules.ScaledRunIntensity.DEFAULT_NON_RUN_INTENSITY_VALUE;


/**
 * Tests the {@link RulesSession} class.
 *
 * @author Eric Trautman
 */
public class RulesSessionTest {

    private static TestWorkingDirectory testDirectory;
    private static ConfigurationManager manager;

    @BeforeClass
    public static void setUp() throws Exception {
        testDirectory = new TestWorkingDirectory();
        manager = testDirectory.getManager();
        final ParameterCollectionId behaviorId =
                new ParameterCollectionId(ParameterCollectionCategory.BEHAVIOR,
                                          "group-b",
                                          "name-b");
        manager.saveCollection(behaviorId,
                               new LarvaBehaviorParameters());
        final ParameterCollectionId stimulusId =
                new ParameterCollectionId(ParameterCollectionCategory.STIMULUS,
                                          "group-s",
                                          "name-s");
        manager.saveCollection(stimulusId,
                               new ScaledRunIntensity(new LEDFlashPattern(),
                                                      DEFAULT_NON_RUN_INTENSITY_VALUE,
                                                      0.0,
                                                      new IntensityValue(0.0),
                                                      0,
                                                      new SingleVariableFunction(),
                                                      0.0,
                                                      false,
                                                      0,
                                                      new SingleVariableFunction(),
                                                      0.0,
                                                      new BehaviorLimitedKinematicVariableFunctionList()));
        final ParameterCollectionId configId =
                new ParameterCollectionId(ParameterCollectionCategory.CONFIGURATION,
                                          "test",
                                          "configuration-a");
        manager.saveCollection(configId,
                               new Configuration(configId,
                                                 behaviorId,
                                                 stimulusId));
    }

    @AfterClass
    public static void tearDown() {
        testDirectory.delete();
    }

    @Test
    public void testRun() {
        final String sessionId = "test-session-1";
        final String inData =
                "<list-configurations-request,1>\n" +
                "<open-session-request,1,1.0.0,test/configuration-a>\n" +
                "<larva-skeleton-request,1,test-session-1,22,4,5,6,7,8,9,10,11,12,13,14>\n" +
                "<larva-skeleton-request,1,test-session-1,33,4,5,6,7,8,9,10,11,12,13,14>\n" +
//                "<get-session-parameters-request,1,test-session-1>\n" +
                "<close-session-request,1,test-session-1>\n";
        final InputStream in = new ByteArrayInputStream(inData.getBytes());
        ByteArrayOutputStream out = new ByteArrayOutputStream();


        RulesSession session = new RulesSession(sessionId,
                                                in,
                                                out,
                                                manager);
        session.run();

        final String expectedOutData =
                "<list-configurations-response,1,200,test/configuration-a>\n" +
                "<open-session-response,1,200,test-session-1>\n" +
                "<larva-skeleton-response,1,200,22,stop,0.0,60>\n" +
                "<larva-skeleton-response,1,200,33,cast-right,0.0,60>\n" +
//                "<get-session-parameters-response,1,200,long-base64-encoded-value>\n" +
                "<status-response,1,200,closed session test-session-1>\n";

        Assert.assertEquals("invalid response messages",
                            expectedOutData, out.toString());
    }
}
/*
 * Copyright (c) 2014 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.venkman.message;

import org.janelia.it.venkman.config.rules.LEDArrayStimulus;
import org.janelia.it.venkman.config.rules.LEDStimulus;
import org.junit.Assert;
import org.junit.Test;


/**
 * Tests the {@link Message} class.
 *
 * @author Eric Trautman
 */
public class MessageTest {

    @Test
    public void testGetRequestMessage() throws Exception {

        Object[][] validRequests = {
                // request, expected-type, expected-version, expected-list
                {
                        "<list-configurations-request,2>",
                        MessageType.LIST_CONFIGURATIONS_REQUEST,
                        "2" },
                {
                        "<list-configurations-request,>",
                        MessageType.LIST_CONFIGURATIONS_REQUEST,
                        "" },
                {
                        "<list-configurations-request>",
                        MessageType.LIST_CONFIGURATIONS_REQUEST,
                        "" },
                {
                        "<arena-background-request,1,2,test-configuration,200,250>",
                        MessageType.ARENA_BACKGROUND_REQUEST,
                        "1",
                        "2",
                        "test-configuration",
                        "200",
                        "250" },
                {
                        "<open-session-request,1,2,test-configuration>",
                        MessageType.OPEN_SESSION_REQUEST,
                        "1",
                        "2",
                        "test-configuration" },
                {
                        "<get-session-parameters-request,1,session-a>",
                        MessageType.GET_SESSION_PARAMETERS_REQUEST,
                        "1",
                        "session-a" },
                {
                        "<close-session-request,1,session-a>",
                        MessageType.CLOSE_SESSION_REQUEST,
                        "1",
                        "session-a" },
                {
                        "<larva-skeleton-request,1,3,4,5,6,7,8,9,10,11,12,13,14,15>",
                        MessageType.PROCESS_LARVA_SKELETON_REQUEST,
                        "1", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13", "14", "15" },
        };

        String request;
        Message message;
        int fieldNumber;
        for (Object[] row : validRequests) {
            request = (String) row[0];
            message = Message.getRequestMessage(request);

            Assert.assertEquals("invalid type for " + request,
                                row[1], message.getType());

            Assert.assertEquals("invalid version for " + request,
                                row[2], message.getVersion());

            for (int i = 3; i < row.length; i++) {
                fieldNumber = i - 3;
                Assert.assertEquals("invalid field[" + fieldNumber + "] for " +
                                    request,
                                    row[i], message.getField(fieldNumber));
            }

        }

    }

    @Test(expected = IllegalArgumentException.class)
    public void testShortMessage() {
        Message.getRequestMessage("<>");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testMessageWithoutBegin() {
        Message.getRequestMessage("list-configurations-request,1>");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testMessageWithoutEnd() {
        Message.getRequestMessage("<list-configurations-request,1");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNonExistentMessageType() {
        Message.getRequestMessage("<bogus-but-not-too-short>");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNonRequestMessageType() {
        Message.getRequestMessage("<status-response,1,500,oops>");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testTooFewFieldsMessage() {
        Message.getRequestMessage("<open-session-response,1,200>");
    }

    @Test
    public void testListConfigurationsResponseToString() {
        ListConfigurationsResponse response = new ListConfigurationsResponse("2");
        response.addConfigurationName("name-1");
        validateResponseString(response, "<list-configurations-response,2,200,name-1>");
    }

    @Test
    public void testOpenSessionResponseToString() {
        OpenSessionResponse response = new OpenSessionResponse("2", "test-session-id");
        validateResponseString(response, "<open-session-response,2,200,test-session-id>");
    }

    @Test
    public void testStatusResponseToString() {
        StatusResponse response =
                new StatusResponse(ResponseMessage.STATUS_OK, "test-message");
        validateResponseString(response,
                               "<status-response,1,200,test-message>");
    }

    @Test
    public void testProcessLarvaSkeletonResponseVersion1ToString() {
        ProcessLarvaSkeletonResponse response = new ProcessLarvaSkeletonResponse("1", "33", "run");
        final LEDStimulus stimulus = new LEDStimulus(44.0, 55);
        stimulus.addFieldsToMessage(response);
        validateResponseString(response,
                               "<larva-skeleton-response,1,200,33,run,44.0,55>");
    }

    @Test
    public void testProcessLarvaSkeletonResponseVersion2ToString() {
        ProcessLarvaSkeletonResponse response = new ProcessLarvaSkeletonResponse("2", "33", "run");
        final LEDArrayStimulus stimulus1 = new LEDArrayStimulus(11.0, 22.0, 33.0, 44.0, new LEDStimulus(55.0, 66).toList());
        stimulus1.addFieldsToMessage(response);
        final LEDArrayStimulus stimulus2 = new LEDArrayStimulus(1.0, 2.0, 3.0, 4.0, new LEDStimulus(5.0, 6).toList());
        stimulus2.addFieldsToMessage(response);
        validateResponseString(response,
                               "<larva-skeleton-response,2,200,33,run,11.0|22.0|33.0|44.0|55.0|66,1.0|2.0|3.0|4.0|5.0|6>");
    }

    private void validateResponseString(ResponseMessage message,
                                        String expectedStringValue) {
        Assert.assertEquals(expectedStringValue, message.toString());
    }
}
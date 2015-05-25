/*
 * Copyright (c) 2011 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.venkman.message;

/**
 * The message types supported by the rules server.
 *
 * @author Eric Trautman
 */
public enum MessageType {
    LIST_CONFIGURATIONS_REQUEST("list-configurations-request"),
    LIST_CONFIGURATIONS_RESPONSE("list-configurations-response"),
    ARENA_BACKGROUND_REQUEST("arena-background-request"),
    ARENA_BACKGROUND_RESPONSE("arena-background-response"),
    OPEN_SESSION_REQUEST("open-session-request"),
    OPEN_SESSION_RESPONSE("open-session-response"),
    GET_SESSION_PARAMETERS_REQUEST("get-session-parameters-request"),
    GET_SESSION_PARAMETERS_RESPONSE("get-session-parameters-response"),
    CLOSE_SESSION_REQUEST("close-session-request"),
    STATUS_RESPONSE("status-response"),
    PROCESS_LARVA_SKELETON_REQUEST("larva-skeleton-request"),
    PROCESS_LARVA_SKELETON_RESPONSE("larva-skeleton-response");

    private String name;
    private boolean isRequest;

    private MessageType(String name) {
        this.name = name;
        this.isRequest = name.endsWith("request");
    }

    public String getName() {
        return name;
    }

    public boolean isRequest() {
        return isRequest;
    }

    @Override
    public String toString() {
        return name;
    }
}

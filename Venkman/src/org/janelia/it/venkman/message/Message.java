/*
 * Copyright (c) 2014 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.venkman.message;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Base class for messages exchanged with rules server.
 *
 * @author Eric Trautman
 */
public class Message {

    private MessageType type;
    private String version;
    private List<String> fields;

    public Message(MessageType type,
                   String version,
                   List<String> fields) {
        this.type = type;
        this.version = version;
        this.fields = fields;
    }

    public MessageType getType() {
        return type;
    }

    public String getVersion() {
        return version;
    }

    public String getField(int index) {
        return fields.get(index);
    }

    public void addField(String field) {
        this.fields.add(field);
    }

    public int size() {
        return fields.size();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(256);
        sb.append('<');
        sb.append(type.getName());
        sb.append(FIELD_SEPARATOR);
        sb.append(version);
        for (String field : fields) {
            sb.append(FIELD_SEPARATOR);
            sb.append(field);
        }
        sb.append('>');
        return sb.toString();
    }

    public static Message getRequestMessage(String requestString)
            throws IllegalArgumentException {

        int len = requestString.length();
        if (len < 3) {
            throw new IllegalArgumentException(
                    "invalid request: message too short");
        }

        if (requestString.charAt(0) != '<') {
            throw new IllegalArgumentException(
                    "invalid request: message missing begin tag");
        }

        len = len - 1;
        if (requestString.charAt(len) != '>') {
            throw new IllegalArgumentException(
                    "invalid request: message missing end tag");
        }

        int start = 1;
        int stop = requestString.indexOf(FIELD_SEPARATOR, start);
        if (stop == -1) {
            stop = len;
        }
        final String typeName = requestString.substring(start, stop);
        final MessageType type = getRequestType(typeName);
        if (type == null) {
            throw new IllegalArgumentException(
                    "invalid request: unknown message type '" + typeName + "'");
        }

        final String version;
        start = stop + 1;
        if (start < len) {
            stop = requestString.indexOf(FIELD_SEPARATOR, start);
            if (stop == -1) {
                stop = len;
            }
            version = requestString.substring(start, stop);
        } else {
            version = "";
        }

        ArrayList<String> fieldList = new ArrayList<String>(15);
        start = stop + 1;
        for (; start < len; start = stop + 1) {
            stop = requestString.indexOf(FIELD_SEPARATOR, start);
            if (stop == -1) {
                fieldList.add(requestString.substring(start, len));
                break;
            } else {
                fieldList.add(requestString.substring(start, stop));
            }
        }

        int minimumNumberOfFields = 0;
        switch (type) {
            case PROCESS_LARVA_SKELETON_REQUEST:
                minimumNumberOfFields = 13;
                break;
            case ARENA_BACKGROUND_REQUEST:
                minimumNumberOfFields = 4;
                break;
            case OPEN_SESSION_REQUEST:
                minimumNumberOfFields = 2;
                break;
            case GET_SESSION_PARAMETERS_REQUEST:
                minimumNumberOfFields = 1;
                break;
            case CLOSE_SESSION_REQUEST:
                minimumNumberOfFields = 1;
                break;
        }

        if (fieldList.size() < minimumNumberOfFields) {
            throw new IllegalArgumentException(
                    "invalid request: type '" + typeName +
                    "' must have at least " +
                    (minimumNumberOfFields + 2) + " fields");
        }

        return new Message(type, version, fieldList);
    }

    public static MessageType getRequestType(String name) {
        return REQUEST_NAME_TO_VALUE_MAP.get(name);
    }

    private static final char FIELD_SEPARATOR = ',';

    private static final Map<String, MessageType> REQUEST_NAME_TO_VALUE_MAP;
    static {
        HashMap<String, MessageType> map = new HashMap<String, MessageType>();
        for (MessageType type : MessageType.values()) {
            if (type.isRequest()) {
                map.put(type.getName(), type);
            }
        }
        REQUEST_NAME_TO_VALUE_MAP = map;
    }

}

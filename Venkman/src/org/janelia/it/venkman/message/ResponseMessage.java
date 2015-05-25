/*
 * Copyright (c) 2011 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.venkman.message;

import java.util.ArrayList;

/**
 * Base class for all response messages.
 *
 * @author Eric Trautman
 */
public class ResponseMessage
        extends Message {

    public static int STATUS_OK = 200;
    public static int STATUS_BAD_REQUEST = 400;
    public static int STATUS_NOT_FOUND = 404;
    public static int STATUS_INTERNAL_SERVER_ERROR = 500;

    private int statusCode;

    public ResponseMessage(MessageType type,
                           String version,
                           int statusCode) {
        super(type,
              version,
              new ArrayList<String>());
        this.statusCode = statusCode;
        addField(String.valueOf(statusCode));
    }

    public int getStatusCode() {
        return statusCode;
    }
}

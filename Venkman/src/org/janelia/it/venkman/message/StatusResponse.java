/*
 * Copyright (c) 2011 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.venkman.message;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

/**
 * A basic status response message.
 *
 * @author Eric Trautman
 */
public class StatusResponse
        extends ResponseMessage {

    public StatusResponse(int statusCode,
                          String message) {
        super(MessageType.STATUS_RESPONSE,
              "1",
              statusCode);
        addField(message);
    }

    public static StatusResponse badRequest(String message) {
        return new StatusResponse(STATUS_BAD_REQUEST, message);
    }

    public static StatusResponse notFound(String message) {
        return new StatusResponse(STATUS_NOT_FOUND, message);
    }

    public static StatusResponse serverError(Throwable t) {
        return serverError("", t);
    }

    public static StatusResponse serverError(String message,
                                             Throwable t) {
        return new StatusResponse(STATUS_INTERNAL_SERVER_ERROR,
                                  buildMessage(message, t));
    }

    private static String buildMessage(String message,
                                       Throwable t) {
        String errorMessage;
        if (message != null) {
            errorMessage = message;
        } else {
            errorMessage = "";
        }

        if (t != null) {
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            PrintStream ps = new PrintStream(os);
            t.printStackTrace(ps);
            errorMessage = message + os.toString();
        }

        int stop = errorMessage.indexOf('\n');
        if (stop > -1) {
            errorMessage = errorMessage.substring(0, stop);
        }

        errorMessage = errorMessage.replace(',',';');

        return errorMessage;
    }
}

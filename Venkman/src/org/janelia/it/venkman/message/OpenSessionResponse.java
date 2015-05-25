/*
 * Copyright (c) 2014 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.venkman.message;

/**
 * A response message for an open session request.
 *
 * @author Eric Trautman
 */
public class OpenSessionResponse
        extends ResponseMessage {

    public OpenSessionResponse(String version,
                               String sessionId) {
        super(MessageType.OPEN_SESSION_RESPONSE,
              version,
              STATUS_OK);
        addField(sessionId);
    }

}

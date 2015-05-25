/*
 * Copyright (c) 2011 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.venkman.message;


import javax.xml.bind.DatatypeConverter;

/**
 * A response message containing the parameters for the current session.
 *
 * @author Eric Trautman
 */
public class GetSessionParametersResponse
        extends ResponseMessage {

    public GetSessionParametersResponse() {
        super(MessageType.GET_SESSION_PARAMETERS_RESPONSE,
              "1",
              STATUS_OK);
    }

    public void addParameters(byte[] parameters) {
        final String encodedParameters =
                DatatypeConverter.printBase64Binary(parameters);
        addField(encodedParameters);
    }
}

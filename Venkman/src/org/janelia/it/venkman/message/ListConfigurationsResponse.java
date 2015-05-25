/*
 * Copyright (c) 2014 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.venkman.message;

/**
 * A response message containing a list of selectable rules configurations.
 *
 * @author Eric Trautman
 */
public class ListConfigurationsResponse extends ResponseMessage {

    public ListConfigurationsResponse(String version) {
        super(MessageType.LIST_CONFIGURATIONS_RESPONSE,
              version,
              STATUS_OK);
    }

    public void addConfigurationName(String name) {
        addField(name);
    }
}

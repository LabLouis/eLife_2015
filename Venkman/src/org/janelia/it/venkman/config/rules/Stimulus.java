/*
 * Copyright (c) 2014 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.venkman.config.rules;

import org.janelia.it.venkman.message.Message;

/**
 * Common interface for all stimulus data.
 *
 * @author Eric Trautman
 */
public interface Stimulus {

    /**
     * Converts stimulus data to message fields and adds them to the specified message.
     *
     * @param  message  message to which stimulus data should be appended.
     */
    public void addFieldsToMessage(Message message);

}

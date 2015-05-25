/*
 * Copyright (c) 2014 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.venkman.message;

/**
 * A response message for a process larva skeleton request.
 *
 * @author Eric Trautman
 */
public class ProcessLarvaSkeletonResponse
        extends ResponseMessage {

    public ProcessLarvaSkeletonResponse(String version,
                                        String frame,
                                        String motionState) {
        super(MessageType.PROCESS_LARVA_SKELETON_RESPONSE,
              version,
              STATUS_OK);
        addField(frame);
        addField(motionState);
    }

}

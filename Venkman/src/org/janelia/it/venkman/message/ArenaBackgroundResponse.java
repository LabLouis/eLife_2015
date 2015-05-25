/*
 * Copyright (c) 2011 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.venkman.message;

import java.math.BigDecimal;

/**
 * A response message for an arena background request.
 *
 * @author Eric Trautman
 */
public class ArenaBackgroundResponse
        extends ResponseMessage {

    public ArenaBackgroundResponse() {
        super(MessageType.ARENA_BACKGROUND_RESPONSE,
              "1",
              STATUS_OK);
        setEmptyArena();
    }

    public ArenaBackgroundResponse(double[][] arena) {
        super(MessageType.ARENA_BACKGROUND_RESPONSE,
              "1",
              STATUS_OK);
        setArena(arena);
    }

    private void setEmptyArena() {
        addField("0"); // width
        addField("0"); // height
    }

    private void setArena(double[][] arena) {

        final int height = arena.length;
        if (height > 0) {

            final int width = arena[0].length;
            addField(String.valueOf(width));
            addField(String.valueOf(height));
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    addField(getMessageValue(arena[y][x]));
                }
            }

        } else {
            setEmptyArena();
        }
    }

    private String getMessageValue(double value) {
        BigDecimal bd = new BigDecimal(value);
        bd = bd.setScale(1, BigDecimal.ROUND_HALF_UP);
        return String.valueOf(bd);
    }
}

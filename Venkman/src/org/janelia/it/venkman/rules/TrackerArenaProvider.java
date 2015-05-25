/*
 * Copyright (c) 2011 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.venkman.rules;

/**
 * Interface for any rule that supports providing the tracker with
 * an intensity background for the arena.
 *
 * @author Eric Trautman
 */
public interface TrackerArenaProvider {

    /**
     * @param  width   the width of the arena.
     * @param  height  the height of the arena.
     *
     * @return matrix of intensity percentages for the tracker arena.
     */
    public double[][] getArena(int width,
                               int height);
}

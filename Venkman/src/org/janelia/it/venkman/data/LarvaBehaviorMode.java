/*
 * Copyright (c) 2014 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.venkman.data;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Behavioral modes for larva.
 *
 * @author Eric Trautman
 */
public enum LarvaBehaviorMode {

    /** Discrete mode that indicates the larva is running forward. */
    RUN("run"),

    /** Discrete mode that indicates the larva is backing up. */
    BACK_UP("back-up"),

    /** Discrete mode that indicates the larva has stopped moving. */
    STOP("stop"),

    /** Derived mode that is only used for behavior analysis comparisons. */
    IGNORE("ignore"),

    /** Aggregate mode that indicates the larva is turning or casting. */
    SAMPLING("sampling"),

    /** Discrete mode that indicates the larva is turning to the right. */
    TURN_RIGHT("turn-right"),

    /** Discrete mode that indicates the larva is casting to the right. */
    CAST_RIGHT("cast-right"),

    /** Discrete mode that indicates the larva is turning to the left. */
    TURN_LEFT("turn-left"),

    /** Discrete mode that indicates the larva is casting to the left. */
    CAST_LEFT("cast-left");

    private String name;

    private LarvaBehaviorMode(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return name;
    }

    /**
     * Equivalence test that supports sampling as a synonym for casting.
     *
     * @param  that  mode to compare to this mode.
     *
     * @return true if the specified mode is equivalent to
     *         (but not necessarily the same as) this mode;
     *         otherwise false.
     */
    public boolean isEquivalent(LarvaBehaviorMode that) {
        boolean result = false;
        if (this == that) {
            result = true;
        } else if ((this == CAST_RIGHT) || (this == CAST_LEFT)) {
            result = (that == SAMPLING);
        } else if (this == SAMPLING) {
            result = ((that == CAST_RIGHT) || (that == CAST_LEFT));
        }
        return result;
    }

    /**
     * Compares specified modes for strict equality, handling null values.
     *
     * @param  left   left side mode to compare.
     * @param  right  right side mode to compare.
     *
     * @return true if both modes are the same or both modes are null;
     *         otherwise false.
     */
    public static boolean isSameMode(LarvaBehaviorMode left,
                                     LarvaBehaviorMode right) {
        return ((left != null) && left.equals(right)) ||
               ((left == null) && (right == null));
    }

    /**
     * @return the full set of discrete modes (excluding any aggregate and derived modes).
     */
    public static Set<LarvaBehaviorMode> getDiscreteModes() {
        return new LinkedHashSet<LarvaBehaviorMode>(
                Arrays.asList(new LarvaBehaviorMode[] {
                        RUN, BACK_UP, STOP, TURN_RIGHT, CAST_RIGHT, TURN_LEFT, CAST_LEFT
                }));
    }
}

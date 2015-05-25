/*
 * Copyright (c) 2011 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.venkman.config.rules;

import org.janelia.it.venkman.data.LarvaFrameData;
import org.janelia.it.venkman.data.TrackerPoint;

import java.util.Arrays;
import java.util.List;
import java.util.Vector;

/**
 * Enumerated set of possible larva frame data positional variables
 * for use in indexing into a function.
 *
 * @author Eric Trautman
 */
public enum PositionalVariable
        implements LarvaFrameData.PointMapper {
    HEAD(
            new LarvaFrameData.PointMapper() {
                @Override
                public TrackerPoint getValue(LarvaFrameData frameData) {
                    return frameData.getSkeleton().getHead();
                }
            }),
    MIDPOINT(
            new LarvaFrameData.PointMapper() {
                @Override
                public TrackerPoint getValue(LarvaFrameData frameData) {
                    return frameData.getSkeleton().getMidpoint();
                }
            }),
    TAIL(
            new LarvaFrameData.PointMapper() {
                @Override
                public TrackerPoint getValue(LarvaFrameData frameData) {
                    return frameData.getSkeleton().getTail();
                }
            }),
    CENTROID(
            new LarvaFrameData.PointMapper() {
                @Override
                public TrackerPoint getValue(LarvaFrameData frameData) {
                    return frameData.getSkeleton().getCentroid();
                }
            });

    private LarvaFrameData.PointMapper mapper;

    PositionalVariable(LarvaFrameData.PointMapper mapper) {
        this.mapper = mapper;
    }

    public TrackerPoint getValue(LarvaFrameData frameData) {
        return mapper.getValue(frameData);
    }

    public static List<PositionalVariable> getValuesList() {
        return Arrays.asList(PositionalVariable.values());
    }

    public static Vector<PositionalVariable> getValuesVector() {
        return new Vector<PositionalVariable>(getValuesList());
    }
}

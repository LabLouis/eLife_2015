/*
 * Copyright (c) 2014 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.venkman.config.rules;

import org.janelia.it.venkman.data.LarvaFrameData;

import java.util.Arrays;
import java.util.List;
import java.util.Vector;

/**
 * Enumerated set of possible larva frame data kinematic variables
 * for use in indexing into a function.
 *
 * @author Eric Trautman
 */
public enum KinematicVariable implements LarvaFrameData.ValueMapper {
    HEAD_ANGLE(
            -180,
            180,
            "degrees",
            new LarvaFrameData.ValueMapper() {
                @Override
                public double getValue(LarvaFrameData frameData) {
                    return frameData.getHeadAngle();
                }
            }),
    BODY_ANGLE(
            -180,
            180,
            "degrees",
            new LarvaFrameData.ValueMapper() {
                @Override
                public double getValue(LarvaFrameData frameData) {
                    return frameData.getBodyAngle();
                }
            }),
    HEAD_ANGLE_SPEED(
            -6000, // 33 fps * -180
            6000,  // 33 fps * 180
            "degrees/s",
            new LarvaFrameData.ValueMapper() {
                @Override
                public double getValue(LarvaFrameData frameData) {
                    return frameData.getHeadAngleSpeed();
                }
            }),
    SMOOTHED_HEAD_ANGLE_SPEED(
            -6000, // 33 fps * -180
            6000,  // 33 fps * 180
            "degrees/s",
            new LarvaFrameData.ValueMapper() {
                @Override
                public double getValue(LarvaFrameData frameData) {
                    return frameData.getSmoothedHeadAngleSpeed();
                }
            }),
    BODY_ANGLE_SPEED(
            -6000, // 33 fps * -180
            6000,  // 33 fps * 180
            "degrees/s",
            new LarvaFrameData.ValueMapper() {
                @Override
                public double getValue(LarvaFrameData frameData) {
                    return frameData.getBodyAngleSpeed();
                }
            }),
    SMOOTHED_BODY_ANGLE_SPEED(
            -6000, // 33 fps * -180
            6000,  // 33 fps * 180
            "degrees/s",
            new LarvaFrameData.ValueMapper() {
                @Override
                public double getValue(LarvaFrameData frameData) {
                    return frameData.getSmoothedBodyAngleSpeed();
                }
            }),
    // TODO: confirm default maximums for speeds
    HEAD_SPEED(
            0,  // distance always positive
            20,
            "mm/s",
            new LarvaFrameData.ValueMapper() {
                @Override
                public double getValue(LarvaFrameData frameData) {
                    return frameData.getHeadSpeed();
                }
            }),
    MIDPOINT_SPEED(
            0,  // distance always positive
            20,
            "mm/s",
            new LarvaFrameData.ValueMapper() {
                @Override
                public double getValue(LarvaFrameData frameData) {
                    return frameData.getMidpointSpeed();
                }
            }),
    TAIL_SPEED(
            0,  // distance always positive
            20,
            "mm/s",
            new LarvaFrameData.ValueMapper() {
                @Override
                public double getValue(LarvaFrameData frameData) {
                    return frameData.getTailSpeed();
                }
            }),
    CENTROID_SPEED(
            0,  // distance always positive
            20,
            "mm/s",
            new LarvaFrameData.ValueMapper() {
                @Override
                public double getValue(LarvaFrameData frameData) {
                    return frameData.getCentroidSpeed();
                }
            }),
    LENGTH(
            0,
            10,
            "mm",
            new LarvaFrameData.ValueMapper() {
                @Override
                public double getValue(LarvaFrameData frameData) {
                    return frameData.getSkeleton().getLength();
                }
            }),
    PERCENTAGE_OF_MAX_LENGTH(
            0,
            100,
            "%",
            new LarvaFrameData.ValueMapper() {
                @Override
                public double getValue(LarvaFrameData frameData) {
                    double value;
                    if (frameData.isMaxLengthDerivationComplete()) {
                        value = frameData.getPercentageOfMaxLength();
                    } else {
                        value = 0.0;
                    }
                    return value;
                }
            });

    private double defaultMinimum;
    private double defaultMaximum;
    private String units;
    private LarvaFrameData.ValueMapper mapper;

    KinematicVariable(double defaultMinimum,
                      double defaultMaximum,
                      String units,
                      LarvaFrameData.ValueMapper mapper) {
        this.mapper = mapper;
        this.units = units;
        this.defaultMinimum = defaultMinimum;
        this.defaultMaximum = defaultMaximum;
    }

    public double getDefaultMinimum() {
        return defaultMinimum;
    }

    public double getDefaultMaximum() {
        return defaultMaximum;
    }

    public String getUnits() {
        return units;
    }

    public double getValue(LarvaFrameData frameData) {
        return mapper.getValue(frameData);
    }

    public static List<KinematicVariable> getValuesList() {
        return Arrays.asList(KinematicVariable.values());
    }

    public static Vector<KinematicVariable> getValuesVector() {
        return new Vector<KinematicVariable>(getValuesList());
    }

}

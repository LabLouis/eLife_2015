/*
 * Copyright (c) 2014 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.venkman.config.rules;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;

/**
 * A value (potentially) with additive Gaussian noise.
 *
 * @author Eric Trautman
 */
@XmlAccessorType(XmlAccessType.FIELD)
public class IntensityValue {

    @XmlElement
    private double value;

    @XmlElement
    private NoiseGenerator noiseGenerator;

    public IntensityValue() {
        this(0.0);
    }

    /**
     * Constructs a value without noise.
     *
     * @param  value  the static intensity value.
     */
    public IntensityValue(double value) {
        this(value, null);
    }

    /**
     * Constructs a value that makes use of the specified noise generator.
     *
     * @param  value           the core intensity value.
     * @param  noiseGenerator  generates additive noise for the value.
     */
    public IntensityValue(double value,
                          NoiseGenerator noiseGenerator) {
        this.value = value;
        this.noiseGenerator = noiseGenerator;
    }

    /**
     * @return this intensity value.
     *         If a noise generator has been configured, noise is added to
     *         the value.
     */
    public double getValue() {

        final double adjustedValue;
        if (noiseGenerator == null) {
            adjustedValue = value;
        } else {
            adjustedValue = value + noiseGenerator.getNoise();
        }

        return adjustedValue;
    }

    public double getValueWithoutNoise() {
        return value;
    }

    public boolean isNoiseEnabled() {
        return (noiseGenerator != null);
    }

    public NoiseGenerator getNoiseGenerator() {
        return noiseGenerator;
    }
}

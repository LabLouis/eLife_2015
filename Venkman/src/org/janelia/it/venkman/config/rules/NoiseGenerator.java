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
import javax.xml.bind.annotation.XmlTransient;
import java.util.List;
import java.util.Random;

/**
 * Creates Gaussian noise.
 *
 * @author Eric Trautman
 */
@XmlAccessorType(XmlAccessType.FIELD)
public class NoiseGenerator {

    @XmlElement
    private double standardDeviation;

    @XmlElement
    private double mean;

    @XmlTransient
    private boolean isNoiseScaled;

    @XmlTransient
    private Random generator;

    /**
     * Constructs a default unscaled generator (mean 0, sd 1).
     */
    public NoiseGenerator() {
        this(0, 1);
    }

    /**
     * Generates Gaussian noise with the specified mean and standard deviation.
     *
     * @param  mean               the average value for the normal distribution.
     * @param  standardDeviation  the amount of variation from the mean.
     */
    public NoiseGenerator(double mean,
                          double standardDeviation) {
        this.standardDeviation = standardDeviation;
        this.mean = mean;
        this.isNoiseScaled = (mean != 0) || (standardDeviation != 1);
        this.generator = new Random();
    }

    public double getMean() {
        return mean;
    }

    public double getStandardDeviation() {
        return standardDeviation;
    }

    /**
     * @return a newly generated noise value.
     */
    public double getNoise() {

        // normal distribution with mean 0 and variance 1
        double noise = generator.nextGaussian();
        if (isNoiseScaled) {
            noise = (noise * standardDeviation) + mean;
        }

        return noise;
    }

    /**
     * Adds noise to each stimulus in the specified list using this generator along with the specified SNR.
     * Derived values less than 0 are set to 0.
     * Derived values greater than 100 are set to 100.
     *
     * Core algorithm (from Daeyeon) is:
     * <pre>
     *   v = i + ( ( i / SNR ) * N(0,1) )
     * </pre>
     *
     * @param  signalToNoiseRatio  configured signal to noise ratio.
     *                             If zero, values will be left unmodified.
     *
     * @param  stimulusList        list of intensity values to modify.
     */
    public void addNoiseUsingRatio(double signalToNoiseRatio,
                                   List<LEDStimulus> stimulusList) {
        if ((signalToNoiseRatio > 0) || (signalToNoiseRatio < 0)) {
            for (LEDStimulus stimulus : stimulusList) {
                final double noiseFactor = stimulus.getIntensityPercentage() / signalToNoiseRatio;
                final double scaledNoise = noiseFactor * getNoise();
                stimulus.addIntensityPercentage(scaledNoise);
            }
        }
    }
}

/*
 * Copyright (c) 2012 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.venkman.gui.parameter;

import org.janelia.it.venkman.config.rules.IntensityValue;
import org.janelia.it.venkman.config.rules.NoiseGenerator;

import javax.swing.*;
import java.awt.*;
import java.lang.reflect.Field;
import java.math.BigDecimal;

/**
 * User interface for intensity value parameters.
 *
 * @author Eric Trautman
 */
public class IntensityValueParameter
        extends AbstractExperimentParameter {

    private IntensityValue originalValue;
    private DecimalParameter percentage;
    private DecimalParameter noiseMean;
    private DecimalParameter noiseStandardDeviation;

    private IntensityValueComponent component;

    public IntensityValueParameter(String displayName,
                                   boolean required,
                                   Field dataField,
                                   IntensityValue originalValue) {

        super(displayName, required, dataField);

        this.originalValue = originalValue;

        this.percentage = new DecimalParameter("Intensity Percentage",
                                               true,
                                               null, // don't call apply!
                                               ZERO,
                                               ONE_HUNDRED);
        this.percentage.setDoubleValue(originalValue.getValueWithoutNoise());

        this.noiseMean = new DecimalParameter("Noise Mean",
                                              false,
                                              null, // don't call apply!
                                              NEGATIVE_ONE_HUNDRED,
                                              ONE_HUNDRED);

        this.noiseStandardDeviation =
                new DecimalParameter("Noise Standard Deviation",
                                     false,
                                     null, // don't call apply!
                                     ZERO,
                                     FIFTY);

        NoiseGenerator noiseGenerator = originalValue.getNoiseGenerator();
        if (noiseGenerator == null) {
            noiseGenerator = new NoiseGenerator();
        }
        this.noiseMean.setDoubleValue(
                noiseGenerator.getMean());
        this.noiseStandardDeviation.setDoubleValue(
                noiseGenerator.getStandardDeviation());

        this.component = new IntensityValueComponent(
                this.percentage,
                originalValue.isNoiseEnabled(),
                this.noiseMean,
                this.noiseStandardDeviation);
    }

    @Override
    public Component getReadOnlyComponent() {
        String displayValue =
                String.valueOf(originalValue.getValueWithoutNoise());
        if (originalValue.isNoiseEnabled()) {
            displayValue = displayValue +
                           ", noise mean: " + noiseMean.getValue() +
                           ", noise standard deviation: " +
                           noiseStandardDeviation.getValue();
        }
        return new JLabel(displayValue);
    }

    @Override
    public Component getComponent() {
        return component.getContentPanel();
    }

    @Override
    public void validate() throws IllegalArgumentException {
        this.percentage.validate();
        if (component.isNoiseEnabled()) {
            this.noiseMean.validate();
            this.noiseStandardDeviation.validate();
        }
    }

    @Override
    public void applyValue(Object object)
            throws IllegalArgumentException, IllegalAccessException {
        validate();
        Field dataField = getDataField();

        NoiseGenerator noiseGenerator = null;
        if (component.isNoiseEnabled()) {
            noiseGenerator = new NoiseGenerator(
                    noiseMean.getDoubleValue(),
                    noiseStandardDeviation.getDoubleValue());
        }
        IntensityValue updatedValue =
                new IntensityValue(percentage.getDoubleValue(),
                                   noiseGenerator);
        dataField.set(object, updatedValue);
    }

    private static final BigDecimal ZERO = new BigDecimal(0.0);
    private static final BigDecimal FIFTY = new BigDecimal(50.0);
    private static final BigDecimal ONE_HUNDRED = new BigDecimal(100.0);
    private static final BigDecimal NEGATIVE_ONE_HUNDRED =
            new BigDecimal(-100.0);
}
/*
 * Copyright (c) 2011 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.venkman.config.rules;

import org.janelia.it.venkman.TestUtilities;
import org.janelia.it.venkman.data.LarvaFrameData;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests the {@link KinematicVariableFunction} class.
 *
 * @author Eric Trautman
 */
public class KinematicVariableFunctionTest {

    private KinematicVariableFunction kvFunction;

    @Before
    public void setup() {
        final double[] values = {0.0, 1.0, 2.0, 3.0, 4.0};
        kvFunction = new KinematicVariableFunction(KinematicVariable.BODY_ANGLE,
                                                   values);
    }

    @Test
    public void testGetValue() throws Exception {

        final double[] values = kvFunction.getValues();
        double tailBearing;
        LarvaFrameData frameData;
        for (int i = 0; i < (values.length - 1); i++) {
            tailBearing = i + 0.1;
            frameData = TestUtilities.getFrameDataWithTailBearing(tailBearing);
            TestUtilities.assertEquals("invalid value returned for " + tailBearing,
                                       tailBearing,
                                       kvFunction.getValue(frameData));
        }

    }

    @Test(expected = IllegalArgumentException.class)
    public void testValidateWithNullData() {
        new KinematicVariableFunction(null, kvFunction.getValues());
    }

}

/*
 * Copyright (c) 2014 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.venkman.gui.parameter;

import org.janelia.it.venkman.config.LarvaBehaviorParameters;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests the {@link ParameterGroup} class.
 *
 * @author Eric Trautman
 */
public class ParameterGroupTest {

    @Before
    public void setUp() throws Exception {

    }

    @Test
    public void testIt() throws Exception {
        LarvaBehaviorParameters data = new LarvaBehaviorParameters();
        data.setMinHeadAngleForCasting(0.3);
        data.setMinBodyAngleSpeedDuration(3);

        ParameterGroup<LarvaBehaviorParameters> group =
                new ParameterGroup<LarvaBehaviorParameters>(data);
        for (ExperimentParameter p : group.getParameterList()) {
            if (p instanceof DecimalParameter) {
              ((DecimalParameter)p).setValue("0.9");
            } else if (p instanceof NumericParameter) {
              ((NumericParameter)p).setValue("9");
            }
        }

        group.applyParameters();

        double d = data.getMinHeadAngleForCasting();
        Assert.assertTrue("d=" + d, d > 0.89 && d < 0.91);

        long l = data.getMinBodyAngleSpeedDuration();
        Assert.assertEquals("l=" + l, 9, l);

    }

}

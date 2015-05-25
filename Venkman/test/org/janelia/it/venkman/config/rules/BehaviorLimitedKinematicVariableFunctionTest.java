/*
 * Copyright (c) 2014 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.venkman.config.rules;

import org.janelia.it.venkman.TestUtilities;
import org.janelia.it.venkman.data.LarvaBehaviorMode;
import org.janelia.it.venkman.data.LarvaFrameData;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


/**
 * Tests the {@link BehaviorLimitedKinematicVariableFunction} class.
 *
 * @author Eric Trautman
 */
public class BehaviorLimitedKinematicVariableFunctionTest {

    private String expectedXml;
    private BehaviorLimitedKinematicVariableFunction expectedValue;
    private JAXBContext context;

    @Before
    public void setUp() throws Exception {

        expectedXml =
                "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
                "<behaviorLimitedKinematicVariableFunction>\n" +
                "    <minimumInputValue>0.0</minimumInputValue>\n" +
                "    <maximumInputValue>200.0</maximumInputValue>\n" +
                "    <inputRangeErrorHandlingMethod>REPEAT_MINIMUM_AND_MAXIMUM</inputRangeErrorHandlingMethod>\n" +
                "    <minimumOutputValue>1.0</minimumOutputValue>\n" +
                "    <maximumOutputValue>1.0</maximumOutputValue>\n" +
                "    <factor>0.0</factor>\n" +
                "    <values>1.0</values>\n" +
                "    <variable>PERCENTAGE_OF_MAX_LENGTH</variable>\n" +
                "    <behaviorModes>\n" +
                "        <larvaBehaviorMode>RUN</larvaBehaviorMode>\n" +
                "        <larvaBehaviorMode>BACK_UP</larvaBehaviorMode>\n" +
                "        <larvaBehaviorMode>STOP</larvaBehaviorMode>\n" +
                "        <larvaBehaviorMode>TURN_RIGHT</larvaBehaviorMode>\n" +
                "        <larvaBehaviorMode>CAST_RIGHT</larvaBehaviorMode>\n" +
                "        <larvaBehaviorMode>TURN_LEFT</larvaBehaviorMode>\n" +
                "        <larvaBehaviorMode>CAST_LEFT</larvaBehaviorMode>\n" +
                "    </behaviorModes>\n" +
                "    <isAdditive>false</isAdditive>\n" +
                "</behaviorLimitedKinematicVariableFunction>\n";

        expectedValue =
                new BehaviorLimitedKinematicVariableFunction(LarvaBehaviorMode.getDiscreteModes(),
                                                             KinematicVariable.PERCENTAGE_OF_MAX_LENGTH,
                                                             0,
                                                             200,
                                                             OutOfRangeErrorHandlingMethod.REPEAT_MINIMUM_AND_MAXIMUM,
                                                             new double[] {1});

        context = JAXBContext.newInstance(expectedValue.getClass());
    }

    @Test
    public void testJaxbMarshall() throws Exception {
        Marshaller m = context.createMarshaller();
        m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
        StringWriter writer = new StringWriter();
        m.marshal(expectedValue, writer);
        StringBuffer sb = writer.getBuffer();
        String marshaledXml = sb.toString();
        Assert.assertEquals("input and output xml do not match",
                            expectedXml, marshaledXml);
    }

    @Test
    public void testJaxbUnmarshaller() throws Exception {

        Unmarshaller unm = context.createUnmarshaller();
        Object o = unm.unmarshal(new StringReader(expectedXml));
        if (o instanceof BehaviorLimitedKinematicVariableFunction) {
            BehaviorLimitedKinematicVariableFunction value = (BehaviorLimitedKinematicVariableFunction) o;
            Assert.assertEquals("invalid kinematic variable parsed", expectedValue.getVariable(), value.getVariable());
        } else {
            Assert.fail("returned an object of type " +
                        o.getClass().getName() + " instead of type " +
                        BehaviorLimitedKinematicVariableFunction.class.getName());
        }

    }

    @Test
    public void testApplyFactorValue() throws Exception {

        final Set<LarvaBehaviorMode> runSet = new HashSet<LarvaBehaviorMode>(Arrays.asList(LarvaBehaviorMode.RUN));
        final double constantOutput = 0.5;

        // ---------------------------------------------
        // test 1: factor with mode

        final LarvaFrameData frameData = TestUtilities.getFrameDataWithTailBearing(1.2);
        frameData.setValuesForTesting(LarvaBehaviorMode.RUN, 1.0);

        List<LEDStimulus> stimulusList = new LEDStimulus(1, 1000).toList();

        final BehaviorLimitedKinematicVariableFunction factorFunction =
                new BehaviorLimitedKinematicVariableFunction(runSet,
                                                             false,
                                                             KinematicVariable.BODY_ANGLE,
                                                             -180,
                                                             180,
                                                             OutOfRangeErrorHandlingMethod.REPEAT_MINIMUM_AND_MAXIMUM,
                                                             new double[] { constantOutput });

        factorFunction.applyValue(frameData, stimulusList, 0);

        Assert.assertEquals("factor should be applied for run",
                            constantOutput,
                            stimulusList.get(0).getIntensityPercentage(),
                            EPSILON);

        // ---------------------------------------------
        // test 2: factor without mode

        frameData.setValuesForTesting(LarvaBehaviorMode.STOP, 1.0);
        stimulusList = new LEDStimulus(1, 1000).toList();

        factorFunction.applyValue(frameData, stimulusList, 0);

        Assert.assertEquals("factor should NOT be applied for stop",
                            1.0,
                            stimulusList.get(0).getIntensityPercentage(),
                            EPSILON);
    }

    @Test
    public void testApplyAddendValue() throws Exception {

        final Set<LarvaBehaviorMode> stopSet = new HashSet<LarvaBehaviorMode>(Arrays.asList(LarvaBehaviorMode.STOP));
        final double constantOutput = 0.5;

        // ---------------------------------------------
        // test 1: addend without mode

        final LarvaFrameData frameData = TestUtilities.getFrameDataWithTailBearing(1.2);
        frameData.setValuesForTesting(LarvaBehaviorMode.RUN, 1.0);

        List<LEDStimulus> stimulusList = new LEDStimulus(1, 1000).toList();

        final BehaviorLimitedKinematicVariableFunction addendFunction =
                new BehaviorLimitedKinematicVariableFunction(stopSet,
                                                             true,
                                                             KinematicVariable.BODY_ANGLE,
                                                             -180,
                                                             180,
                                                             OutOfRangeErrorHandlingMethod.REPEAT_MINIMUM_AND_MAXIMUM,
                                                             new double[] { constantOutput });

        addendFunction.applyValue(frameData, stimulusList, 0);

        Assert.assertEquals("addend should NOT be applied for run",
                            1.0,
                            stimulusList.get(0).getIntensityPercentage(),
                            EPSILON);

        // ---------------------------------------------
        // test 2: addend with mode

        frameData.setValuesForTesting(LarvaBehaviorMode.STOP, 1.0);
        stimulusList = new LEDStimulus(1, 1000).toList();

        addendFunction.applyValue(frameData, stimulusList, 0);

        Assert.assertEquals("addend should be applied for stop",
                            1.0 + constantOutput,
                            stimulusList.get(0).getIntensityPercentage(),
                            EPSILON);
    }

    private static final double EPSILON = 0.1;
}
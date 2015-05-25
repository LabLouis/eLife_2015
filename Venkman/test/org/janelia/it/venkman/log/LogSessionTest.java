/*
 * Copyright (c) 2012 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.venkman.log;

import junit.framework.Assert;
import org.janelia.it.venkman.rules.StimulusRuleImplementationsTest;
import org.junit.Test;

import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.XmlElementRefs;
import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


/**
 * Tests the {@link LogSession} class.
 *
 * @author Eric Trautman
 */
public class LogSessionTest {

    /**
     * Ensures that XmlElementRef annotations have been defined for
     * all rules implementation classes.
     *
     * @throws Exception
     */
    @Test
    public void testLarvaStimulusRulesAnnotations() throws Exception {

        final String fieldMessage = "larvaStimulusRules field in " +
                                    LogSession.class.getName();

        final List<Class> rulesImplementationClassList =
                StimulusRuleImplementationsTest.getRulesImplementationClasses();

        Field rulesField =
                LogSession.class.getDeclaredField("larvaStimulusRules");

        Set<Class> annotatedClassSet = new HashSet<Class>();

        final XmlElementRefs refsAnnotation =
                rulesField.getAnnotation(XmlElementRefs.class);

        Assert.assertNotNull(
                fieldMessage + " is missing an XmlElementRefs annotation",
                refsAnnotation);

        for (XmlElementRef ref : refsAnnotation.value()) {
            annotatedClassSet.add(ref.type());
        }

        StringBuilder missingClasses = new StringBuilder();
        for (Class c : rulesImplementationClassList) {
            if (! annotatedClassSet.contains(c)) {
                missingClasses.append("    ");
                missingClasses.append(c.getName());
                missingClasses.append('\n');
            }
        }

        Assert.assertTrue(
                "The following classes do not have an XmlRefElement " +
                "annotation on the " + fieldMessage + ":\n" +
                missingClasses,
                missingClasses.length() == 0);
    }

}
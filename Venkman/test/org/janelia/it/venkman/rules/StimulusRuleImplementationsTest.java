/*
 * Copyright (c) 2012 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.venkman.rules;

import junit.framework.Assert;
import org.junit.Test;

import java.io.File;
import java.io.FilenameFilter;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;


/**
 * Ensures that all rule implementation classes have been identified in the
 * {@link StimulusRuleImplementations} class.
 *
 * @author Eric Trautman
 */
public class StimulusRuleImplementationsTest {

    public static List<Class> getRulesImplementationClasses() throws Exception {

        final Package rulesPackage = LarvaStimulusRules.class.getPackage();
        final String rulesPackagePath =
                rulesPackage.getName().replace('.', '/');
        final File rulesPackageDirectory = new File("src/" + rulesPackagePath);

        Assert.assertTrue(rulesPackageDirectory.getAbsolutePath() +
                          " does not exist",
                          rulesPackageDirectory.exists());

        final File[] rulesSourceFiles = rulesPackageDirectory.listFiles(
                new FilenameFilter() {
                    @Override
                    public boolean accept(File dir,
                                          String name) {
                        return (name.endsWith(".java"));
                    }
                });

        Assert.assertTrue("no rules implementation .java files found in " +
                          rulesPackageDirectory.getAbsolutePath(),
                          rulesSourceFiles.length > 0);

        List<Class> rulesImplementationClassList = new ArrayList<Class>();
        String className;
        Class clazz;
        for (File sourceFile : rulesSourceFiles) {
            className = sourceFile.getName();
            className = className.substring(0, (className.length() - 5));
            className = rulesPackage.getName() + '.' + className;
            clazz = Class.forName(className);

            if (! clazz.isInterface() &&
                (! Modifier.isAbstract(clazz.getModifiers())) &&
                LarvaStimulusRules.class.isAssignableFrom(clazz)) {
                rulesImplementationClassList.add(clazz);
            }
        }

        Assert.assertTrue("no rules implementation classes found in " +
                          rulesPackageDirectory.getAbsolutePath(),
                          rulesImplementationClassList.size() > 0);

        return rulesImplementationClassList;
    }

    @Test
    public void testRuleIdentification() throws Exception {

        final List<Class> rulesImplementationClassList =
                getRulesImplementationClasses();
        final List<Class> registeredClasses =
                StimulusRuleImplementations.getClasses();

        StringBuilder missingClasses = new StringBuilder();
        for (Class c : rulesImplementationClassList) {
            if (! registeredClasses.contains(c)) {
                missingClasses.append("    ");
                missingClasses.append(c.getName());
                missingClasses.append('\n');
            }
        }

        Assert.assertTrue("The following classes are missing from the " +
                          "StimulusRuleImplementations list:\n" +
                          missingClasses,
                          missingClasses.length() == 0);
    }

}
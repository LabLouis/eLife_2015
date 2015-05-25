/*
 * Copyright (c) 2014 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.venkman.rules;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * List of rule implementation classes.
 * If the need arises, this could be dynamically specified and/or generated.
 *
 * NOTE: Any classes specified here must also be specified in
 *       {@link org.janelia.it.venkman.log.LogSession}.
 *
 * @author Eric Trautman
 */
public class StimulusRuleImplementations {

    private static final List<Class> classes;
    static {
        List<Class> l = new ArrayList<Class>();

        l.add(DefinedEnvironment.class);
        l.add(DefinedEnvironmentBasedUponOrientation.class);
        l.add(DefinedEnvironmentForMaximumLengthWithAdditiveFunction.class);
        l.add(ImportedStimulus.class);
        l.add(ScaledRunIntensity.class);
        l.add(ScaledRunIntensityWithRandomDelay.class);

        classes = Collections.unmodifiableList(l);
    }

    public static List<Class> getClasses() {
        return classes;
    }

}

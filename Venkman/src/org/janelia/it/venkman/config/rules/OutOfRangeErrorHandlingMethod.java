/*
 * Copyright (c) 2014 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.venkman.config.rules;

import java.util.Arrays;
import java.util.List;
import java.util.Vector;

/**
 * Enumerated set of options for handling out of range input values.
 *
 * @author Eric Trautman
 */
public enum OutOfRangeErrorHandlingMethod {
    END_SESSION_FOR_MINIMUM_AND_MAXIMUM("end session for minimum and maximum",
                                        "end session for values less than minimum or greater than maximum",
                                        false, false),
    END_SESSION_FOR_MINIMUM_REPEAT_MAXIMUM("end session for minimum, repeat maximum",
                                           "end session for values less than minimum, use maximum for values greater than maximum",
                                           false, true),
    REPEAT_MINIMUM_END_SESSION_FOR_MAXIMUM("repeat minimum, end session for maximum",
                                           "use minimum for values less than minimum, end session for values greater than maximum",
                                           true, false),
    REPEAT_MINIMUM_AND_MAXIMUM("repeat minimum and maximum",
                               "use minimum for values less than minimum, use maximum for values greater than maximum",
                               true, true);

    private String displayName;
    private String description;
    private boolean repeatMinimum;
    private boolean repeatMaximum;

    private OutOfRangeErrorHandlingMethod(String displayName,
                                          String description,
                                          boolean repeatMinimum,
                                          boolean repeatMaximum) {
        this.displayName = displayName;
        this.description = description;
        this.repeatMinimum = repeatMinimum;
        this.repeatMaximum = repeatMaximum;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public boolean repeatMinimum() {
        return repeatMinimum;
    }

    public boolean repeatMaximum() {
        return repeatMaximum;
    }

    @Override
    public String toString() {
        return displayName;
    }

    public static List<OutOfRangeErrorHandlingMethod> getValuesList() {
        return Arrays.asList(OutOfRangeErrorHandlingMethod.values());
    }

    public static Vector<OutOfRangeErrorHandlingMethod> getValuesVector() {
        return new Vector<OutOfRangeErrorHandlingMethod>(getValuesList());
    }

}

/*
 * Copyright (c) 2011 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.venkman.config;

/**
 * The parameter collection categories supported by the rules server.
 *
 * @author Eric Trautman
 */
public enum ParameterCollectionCategory {

    CONFIGURATION("configuration", "Configuration"),
    BEHAVIOR("behavior", "Behavior"),
    STIMULUS("stimulus", "Stimulus");

    private String name;
    private String displayName;

    /**
     * Constructs a category.
     *
     * @param  name       the external name for the category.
     */
    private ParameterCollectionCategory(String name,
                                        String displayName) {
        this.name = name;
        this.displayName = displayName;
    }

    public String getName() {
        return name;
    }

    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String toString() {
        return name;
    }
}

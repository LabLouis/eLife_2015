/*
 * Copyright (c) 2012 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.venkman.config;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Identifies a specific parameter collection.
 *
 * @author Eric Trautman
 */
@XmlType(propOrder={"category", "groupName", "name"})
public class ParameterCollectionId {

    private ParameterCollectionCategory category;
    private String groupName;
    private String name;

    @SuppressWarnings({"UnusedDeclaration"})
    private ParameterCollectionId() {
    }

    /**
     * Constructs an id for the specified category and full name.
     *
     * @param  category  collection category.
     * @param  fullName  full name composed from the group and standard names.
     *
     * @throws IllegalArgumentException
     *   if the category or names have not been properly specified.
     */
    public ParameterCollectionId(ParameterCollectionCategory category,
                                 String fullName)
            throws IllegalArgumentException {


        setCategory(category);
        Matcher m = FULL_NAME_PATTERN.matcher(fullName);
        if (m.matches() && (m.groupCount() == 2)) {
            setGroupName(m.group(1));
            setName(m.group(2));
        }
    }

    /**
     * Constructs an id for the specified category and names.
     *
     * @param  category  collection category.
     * @param  groupName collection group name.
     * @param  name      collection standard name.
     *
     * @throws IllegalArgumentException
     *   if the category or names have not been properly specified.
     */
    public ParameterCollectionId(ParameterCollectionCategory category,
                                 String groupName,
                                 String name)
            throws IllegalArgumentException {

        setCategory(category);
        setGroupName(groupName);
        setName(name);
    }

    @XmlElement
    public ParameterCollectionCategory getCategory() {
        return category;
    }

    public void setCategory(ParameterCollectionCategory category)
            throws IllegalArgumentException {
        if (category == null) {
            throw new IllegalArgumentException(
                    "Please specify a category for the parameters.");
        }
        this.category = category;
    }

    @XmlElement
    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName)
            throws IllegalArgumentException {
        final String n = groupName.trim();
        if (n.length() == 0) {
            throw new IllegalArgumentException(
                    "Please specify a group for the parameters.");
        }
        this.groupName = normalizeName(n);
    }

    @XmlElement
    public String getName() {
        return name;
    }

    public void setName(String name)
            throws IllegalArgumentException {
        final String n = name.trim();
        if (n.length() == 0) {
            throw new IllegalArgumentException(
                    "Please specify a name for the parameters.");
        }
        this.name = normalizeName(n);
    }

    /**
     * @return a full name composed from the group and standard names.
     */
    public String getFullName() {
        return getFullCollectionName(groupName, name);
    }

    /**
     * @return true if this id is properly defined; otherwise false.
     */
    public boolean isDefined() {
        return ((groupName != null) && (name != null));
    }

    @Override
    public String toString() {
        return category + "__" + groupName + "__" + name;
    }

    @Override
    public boolean equals(Object o) {
        boolean isEqual = false;
        if (this == o) {
            isEqual = true;
        } else if (o instanceof ParameterCollectionId) {
            ParameterCollectionId that = (ParameterCollectionId) o;
            isEqual = ((category == that.category) &&
                       (groupName.equals(that.groupName)) &&
                       (name.equals(that.name)));
        }
        return isEqual;
    }

    @Override
    public int hashCode() {
        int result = category.hashCode();
        result = 31 * result + groupName.hashCode();
        result = 31 * result + name.hashCode();
        return result;
    }

    /**
     * @param  fullName  full name in the form [group]/[name].
     *
     * @return id for the specified configuration.
     */
    public static ParameterCollectionId getConfigurationId(String fullName) {
        return new ParameterCollectionId(
                ParameterCollectionCategory.CONFIGURATION, fullName);
    }

    /**
     * @param  fullName  full name in the form [group]/[name].
     *
     * @return id for the specified behavior parameters.
     */
    public static ParameterCollectionId getBehaviorId(String fullName) {
        return new ParameterCollectionId(
                ParameterCollectionCategory.BEHAVIOR, fullName);
    }

    /**
     * @param  fullName  full name in the form [group]/[name].
     *
     * @return id for the specified stimulus parameters.
     */
    public static ParameterCollectionId getStimulusId(String fullName) {
        return new ParameterCollectionId(
                ParameterCollectionCategory.STIMULUS, fullName);
    }

    /**
     * @param  groupName collection group name.
     * @param  name      collection name.
     *
     * @return a full name composed from the collection's group
     *         and standard names.
     */
    public static String getFullCollectionName(String groupName,
                                               String name) {
        return groupName + "/" + name;
    }

    /**
     * Replaces any spaces or slashes in the specified name with a hyphen.
     *
     * @param  name  name to normalize.
     *
     * @return normalized group name.
     */
    public static String normalizeName(String name) {
        name = name.replace(' ', '-');
        name = name.replace('/', '-');
        return name;
    }

    private static final Pattern FULL_NAME_PATTERN =
            Pattern.compile("^(.+)/(.+)$");
}

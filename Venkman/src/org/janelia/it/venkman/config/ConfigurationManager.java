/*
 * Copyright (c) 2014 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.venkman.config;

import org.apache.log4j.Logger;
import org.janelia.it.venkman.rules.LarvaStimulusRules;
import org.janelia.it.venkman.rules.StimulusRuleImplementations;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.ValidationEvent;
import javax.xml.bind.ValidationEventHandler;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Manages repository of configuration information for the rules server.
 *
 * @author Eric Trautman
 */
public class ConfigurationManager {

    private File workDirectory;
    private File logDirectory;
    private Map<ParameterCollectionCategory, File> categoryToBaseDirectoryMap;
    private JAXBContext jaxbContext;

    private PropertyChangeSupport propertyChangeSupport;

    /**
     * Locates and creates (if necessary) the base set of configuration
     * and log directories for the rules server.
     *
     * @param  workDirectory  the parent directory for all rules server
     *                        configuration and log information.
     *
     * @throws IllegalArgumentException
     *   if any errors occur reading or writing to the work directory.
     *
     * @throws IllegalStateException
     *   if the JAXB context cannot be created.
     */
    public ConfigurationManager(File workDirectory)
            throws IllegalArgumentException, IllegalStateException {

        this.workDirectory = workDirectory;

        String logDirectoryName = System.getProperty("venkman-log-directory");
        if (logDirectoryName == null) {
            this.logDirectory = new File(workDirectory, "logs");
        } else {
            this.logDirectory = new File(logDirectoryName);
        }

        if (! this.logDirectory.exists()) {
            //noinspection ResultOfMethodCallIgnored
            this.logDirectory.mkdirs();
        } else if (! this.logDirectory.canWrite()) {
            throw new IllegalArgumentException(
                    "not permitted to write to log directory " +
                    this.logDirectory.getAbsolutePath());
        }

        LOG.info("init: log directory is " + logDirectory.getAbsolutePath());

        this.categoryToBaseDirectoryMap =
                new HashMap<ParameterCollectionCategory, File>();
        for (ParameterCollectionCategory category :
                ParameterCollectionCategory.values()) {
            this.categoryToBaseDirectoryMap.put(category,
                                                getCategoryDirectory(category));
        }

        List<Class> list = new ArrayList<Class>();
        list.addAll(StimulusRuleImplementations.getClasses());
        list.add(Configuration.class);
        list.add(LarvaBehaviorParameters.class);
        Class[] classesToBeBound = list.toArray(new Class[list.size()]);

        try {
            this.jaxbContext = JAXBContext.newInstance(classesToBeBound);
        } catch (JAXBException e) {
            final String message = "failed to initialize JAXB context with " +
                                   list;
            LOG.error(message, e);
            throw new IllegalStateException(message, e);
        }

        this.propertyChangeSupport = new PropertyChangeSupport(this);

        LOG.info("init: work directory is " + workDirectory.getAbsolutePath());
    }

    /**
     * @return the rules server log directory.
     */
    public File getLogDirectory() {
        return logDirectory;
    }

    /**
     * @return the list of full names for each persisted configuration.
     */
    public List<String> getConfigurationNames() {
        return getCollectionNames(ParameterCollectionCategory.CONFIGURATION);
    }

    /**
     * @param  version  rules version to use for filtering.
     *
     * @return the list of full names for each persisted configuration with
     *         stimulus rules that support the specified version.
     */
    public List<String> getConfigurationNames(String version) {

        List<String> allNames = getConfigurationNames();
        List<String> supportsVersionNames = new ArrayList<String>(allNames.size());

        Configuration configuration;
        ParameterCollectionId stimulusId;
        LarvaStimulusRules rules;
        for (String name : allNames) {
            configuration = getConfiguration(name);
            stimulusId = configuration.getStimulusParametersId();
            if (stimulusId != null) {
                rules = getStimulusRules(stimulusId);
                if (rules.supportsVersion(version)) {
                    supportsVersionNames.add(name);
                }
            }
        }

        return supportsVersionNames;
    }

    /**
     * @return the list of full names for each persisted
     *         behavior configuration.
     */
    public List<String> getBehaviorConfigurationNames() {
        return getCollectionNames(ParameterCollectionCategory.BEHAVIOR);
    }

    /**
     * @return the list of full names for each persisted
     *         stimulus rule configuration.
     */
    public List<String> getStimulusConfigurationNames() {
        return getCollectionNames(ParameterCollectionCategory.STIMULUS);
    }

    /**
     * @param  category  the category to check.
     *
     * @return the list of full names for the specified category.
     */
    public List<String> getCollectionNames(ParameterCollectionCategory category) {
        File baseDirectory = categoryToBaseDirectoryMap.get(category);
        return getParameterCollectionNames(baseDirectory);
    }

    /**
     * @param  category  the group category.
     *
     * @return the list of group names for the specified category.
     */
    public List<String> getGroupNamesInCategory(ParameterCollectionCategory category) {
        final File categoryDirectory = new File(workDirectory,
                                                category.getName());
        List<String> list = new ArrayList<String>();
        final File[] categoryFiles = categoryDirectory.listFiles();
        if (categoryFiles != null) {
            for (File group : categoryFiles) {
                if (group.isDirectory()) {
                    list.add(group.getName());
                }
            }
            Collections.sort(list);
        }
        return list;
    }

    /**
     * @param  category   category for the group.
     * @param  groupName  name of the group.
     *
     * @return list of parameter collection instances for the specified group.
     */
    public List<String> getCollectionNamesInGroup(ParameterCollectionCategory category,
                                                  String groupName) {
        List<String> list = new ArrayList<String>();
        final File categoryDirectory = new File(workDirectory,
                                                category.getName());
        final File groupDirectory = new File(categoryDirectory, groupName);
        list = addGroupCollectionNamesToList(groupDirectory, list);
        Collections.sort(list);
        return list;
    }

    /**
     * @param  fullName  full name of the desired configuration
     *                   in the form [group]/[name].
     *
     * @return the persisted configuration information for the specified name
     *         or null if none exists.
     */
    public Configuration getConfiguration(String fullName) {
        final ParameterCollectionId id =
                ParameterCollectionId.getConfigurationId(fullName);
        Configuration configuration = null;
        if (id.isDefined()) {
            try {
                configuration = (Configuration) loadFile(id);
            } catch (Exception e) {
                LOG.error("getConfiguration: failed to load " + fullName, e);
            }
        } else {
            LOG.error("getConfiguration: id could not be derived from name '" +
                      fullName + "'");
        }

        return configuration;
    }

    /**
     * @param  id  behavior parameter identifier.
     *
     * @return the persisted behavior parameters for the specified id
     *         or null if none exists.
     */
    public LarvaBehaviorParameters getBehaviorParameters(ParameterCollectionId id) {
        LarvaBehaviorParameters parameters = null;
        if (id != null) {
            try {
                parameters = (LarvaBehaviorParameters) loadFile(id);
            } catch (Exception e) {
                LOG.error("getBehaviorParameters: failed to load " + id, e);
            }
        }
        return parameters;
    }

    /**
     * @param  id  stimulus rules identifier.
     *
     * @return the persisted stimulus rules parameters for the specified id
     *         or null if none exists.
     */
    public LarvaStimulusRules getStimulusRules(ParameterCollectionId id) {
        LarvaStimulusRules rules = null;
        if (id != null) {
            try {
                rules = (LarvaStimulusRules) loadFile(id);
            } catch (Exception e) {
                LOG.error("getStimulusRules: failed to load " + id, e);
            }
        }
        return rules;
    }

    /**
     * @param  id  stimulus rules identifier.
     *
     * @return the rules code for the specified id or null if none exists.
     */
    public String getStimulusRulesCode(ParameterCollectionId id) {
        // TODO: optimize code loading for dashboard
        String code = null;
        if (id != null) {
            LarvaStimulusRules rules = getStimulusRules(id);
            if (rules != null) {
                code = rules.getCode();
            }
        }
        return code;
    }

    /**
     * @param  id  parameter collection identifier.
     *
     * @return true if the specified collection exists; otherwise false.
     */
    public boolean isExistingCollection(ParameterCollectionId id) {
        final File file = getParametersFile(id);
        return file.exists();
    }

    /**
     * Persists the specified parameter collection.
     *
     * @param  id          parameter collection identifier.
     * @param  collection  collection to persist.
     *
     * @throws IllegalArgumentException
     *   if the id and collection information is inconsistent or
     *   if the collection cannot be persisted for any other reason.
     */
    public void saveCollection(ParameterCollectionId id,
                               Object collection)
            throws IllegalArgumentException {
        saveCollectionWithoutNotification(id, collection);
        notifyChangeListeners();
    }

    /**
     * Persists the specified parameter collection, renaming it if necessary.
     *
     * @param  originalId  original id for the collection
     *                     or null if this is a new collection.
     * @param  category    collection category.
     * @param  groupName   collection group name.
     * @param  name        collection name.
     * @param  collection  collection parameter data.
     *
     * @throws IllegalArgumentException
     *   if any of the collection identifier components are missing or
     *   if the collection is supposed to be new (originalId is null)
     *   but another collection with the same group/name already exists.
     */
    public void saveCollection(ParameterCollectionId originalId,
                               ParameterCollectionCategory category,
                               String groupName,
                               String name,
                               Object collection)
            throws IllegalArgumentException {

        final ParameterCollectionId id =
                new ParameterCollectionId(category, groupName, name);

        if (originalId == null) {
            if (isExistingCollection(id)) {
                throw new IllegalArgumentException(
                        "The " + category.getName() + " parameter set " +
                        id.getFullName() + " already exists.  " +
                        "Please choose another name if you wish add " +
                        "a new set of parameters.");
            }
        } else if (! id.equals(originalId)) {
            renameCollection(originalId, id);
        }

        saveCollection(id, collection);
    }

    /**
     * Renames the specified collection and updates any references
     * with the new name.
     *
     * @param  fromId  former collection ID.
     * @param  toId    new collection ID.
     *
     * @throws IllegalArgumentException
     *   if the to collection already exists or
     *   the from and to categories do not match.
     */
    public void renameCollection(ParameterCollectionId fromId,
                                 ParameterCollectionId toId)
            throws IllegalArgumentException {

        final ParameterCollectionCategory fromCategory = fromId.getCategory();
        final ParameterCollectionCategory toCategory = toId.getCategory();

        if (fromCategory != toCategory) {
            throw new IllegalArgumentException(
                    toCategory.getDisplayName() + " category specified for rename of " +
                    fromCategory.getName() + " collection.");
        }

        final File fromFile = getParametersFile(fromId);
        final File toFile = getParametersFile(toId);
        Configuration config;

        if (toFile.exists()) {
            throw new IllegalArgumentException(
                    "The " + toCategory.getName() + " parameters " + toId.getFullName() +
                    " already exists.  Please delete these parameters " + "first or choose another group/name.");
        } else {
            //noinspection ResultOfMethodCallIgnored
            fromFile.renameTo(toFile);
        }

        if (ParameterCollectionCategory.CONFIGURATION.equals(fromCategory)) {
            config = getConfiguration(toId.getFullName());
            config.setId(toId);
            saveCollectionWithoutNotification(toId, config);
        } else {
            final List<String> references = getConfigurationReferences(fromCategory, null, fromId);
            final boolean isBehaviorGroupNameChange = ParameterCollectionCategory.BEHAVIOR.equals(fromCategory);
            for (String groupReference : references) {
                config = getConfiguration(groupReference);
                if (isBehaviorGroupNameChange) {
                    config.setBehaviorParametersId(toId);
                } else {
                    config.setStimulusParametersId(toId);
                }
                saveCollectionWithoutNotification(config.getId(), config);
            }
        }
    }

    /**
     * Deletes the specified collection.
     *
     * @param  category  category for the collection.
     * @param  fullName  collection's full name in the form [group]/[name].
     *
     * @throws IllegalArgumentException
     *   if the collection is a behavior or stimulus collection that is
     *   referenced by one or more configurations.
     */
    public void deleteCollection(ParameterCollectionCategory category,
                                 String fullName)
            throws IllegalArgumentException {

        final ParameterCollectionId collectionId =
                new ParameterCollectionId(category, fullName);
        final List<String> references =
                getConfigurationReferences(category, null, collectionId);

        if (references.size() > 0) {
            StringBuilder sb = new StringBuilder(128);
            sb.append("The ").append(category.getName());
            sb.append(" parameters ").append(fullName);
            sb.append(" may not be deleted because they are referenced by ");
            sb.append("the following configurations:\n");
            appendNames(references, sb);
            sb.append("Please delete these configurations first.");
            throw new IllegalArgumentException(sb.toString());
        }

        final File file = getParametersFile(collectionId);
        //noinspection ResultOfMethodCallIgnored
        file.delete();

        notifyChangeListeners();
    }

    /**
     * Adds the specified group if it does not already exist.
     *
     * @param  category   category for the group.
     * @param  groupName  name of the group.
     */
    public void addGroup(ParameterCollectionCategory category,
                         String groupName) {
        findOrCreateGroupDirectory(
                category,
                ParameterCollectionId.normalizeName(groupName));
    }

    /**
     * Renames the specified group and updates any references
     * with the new name.
     *
     * @param  category       category for the group.
     * @param  fromGroupName  former name of the group.
     * @param  toGroupName    new name of the group.
     */
    public void renameGroup(ParameterCollectionCategory category,
                            String fromGroupName,
                            String toGroupName) {

        toGroupName = ParameterCollectionId.normalizeName(toGroupName);

        final File fromGroupDirectory =
                findOrCreateGroupDirectory(category, fromGroupName);
        final File toGroupDirectory =
                findOrCreateGroupDirectory(category, toGroupName);

        final File[] toGroupFiles = toGroupDirectory.listFiles();
        if ((toGroupFiles == null) || (toGroupFiles.length == 0)) {
            //noinspection ResultOfMethodCallIgnored
            fromGroupDirectory.renameTo(toGroupDirectory);
        } else {
            final File[] fromGroupFiles = fromGroupDirectory.listFiles();
            if (fromGroupFiles != null) {
                for (File file : fromGroupFiles) {
                    //noinspection ResultOfMethodCallIgnored
                    file.renameTo(new File(toGroupDirectory, file.getName()));
                }
            }
            //noinspection ResultOfMethodCallIgnored
            fromGroupDirectory.delete();
        }

        Configuration config;
        ParameterCollectionId obsoleteId;
        if (ParameterCollectionCategory.CONFIGURATION.equals(category)) {
            for (String fullName : getCollectionNamesInGroup(category,
                                                             toGroupName)) {
                config = getConfiguration(fullName);
                obsoleteId = config.getId();
                obsoleteId.setGroupName(toGroupName);
                saveCollectionWithoutNotification(config.getId(), config);
            }
        } else {
            final List<String> references =
                    getConfigurationReferences(category, fromGroupName, null);
            final boolean isBehaviorGroupNameChange =
                    ParameterCollectionCategory.BEHAVIOR.equals(category);
            for (String groupReference : references) {
                config = getConfiguration(groupReference);
                if (isBehaviorGroupNameChange) {
                    obsoleteId = config.getBehaviorParametersId();
                } else {
                    obsoleteId = config.getStimulusParametersId();
                }
                obsoleteId.setGroupName(toGroupName);
                saveCollectionWithoutNotification(config.getId(), config);
            }
        }

        notifyChangeListeners();
    }

    /**
     * Deletes the specified group.
     *
     * @param  category   category for the group.
     * @param  groupName  name of the group.
     *
     * @throws IllegalArgumentException
     *   if the group is a behavior or stimulus group that is referenced
     *   by one or more configurations.
     */
    public void deleteGroup(ParameterCollectionCategory category,
                            String groupName)
            throws IllegalArgumentException {

        final List<String> references =
                getConfigurationReferences(category, groupName, null);

        if (references.size() > 0) {
            StringBuilder sb = new StringBuilder(128);
            sb.append("The ");
            sb.append(category.getName());
            sb.append(" group ");
            sb.append(groupName);
            sb.append(" may not be deleted because it is referenced by the ");
            sb.append("following configurations:\n");
            appendNames(references, sb);
            sb.append("Please delete these configurations first.");
            throw new IllegalArgumentException(sb.toString());
        }

        final File directory = findOrCreateGroupDirectory(category, groupName);
        recursiveDelete(directory);
        notifyChangeListeners();
    }

    /**
     * Register the specified listener for change notifications.
     *
     * @param  listener  listener to be added.
     */
    public void addChangeListener(PropertyChangeListener listener) {
        propertyChangeSupport.addPropertyChangeListener(listener);
    }

    /**
     * Remove the specified listener from receiving change notifications.
     *
     * @param  listener  listener to be removed.
     */
//    public void removeChangeListener(PropertyChangeListener listener) {
//        propertyChangeSupport.removePropertyChangeListener(listener);
//    }

    /**
     * Recursively deletes the specified directory and
     * any files/directories it contains.
     *
     * @param  directory  directory to delete.
     */
    public static void recursiveDelete(File directory) {
        final File[] directoryFiles = directory.listFiles();
        if (directoryFiles != null) {
            for (File file : directoryFiles) {
                if (file.isDirectory()) {
                    recursiveDelete(file);
                } else {
                    //noinspection ResultOfMethodCallIgnored
                    file.delete();
                }
            }
        }
        //noinspection ResultOfMethodCallIgnored
        directory.delete();
    }

    /**
     * Appends up to 10 of the specified names to the specified string builder.
     *
     * @param  names  names to append.
     * @param  sb     buffer to which names should be appended.
     */
    public static void appendNames(List<String> names,
                                   StringBuilder sb) {
        int count = 0;
        for (String name : names) {
            count++;
            sb.append("  ");
            sb.append(name);
            sb.append("\n");
            if (count > 9) {
                sb.append("  (").append(names.size() - count);
                sb.append(" more) ...\n");
                break;
            }
        }
    }

    private File getCategoryDirectory(ParameterCollectionCategory category)
            throws IllegalArgumentException {

        final File directory = new File(workDirectory,
                                        category.getName());
        if (! directory.exists()) {
            if (! workDirectory.canWrite()) {
                throw new IllegalArgumentException(
                        "not permitted to create " +
                        directory.getAbsolutePath());
            }
            if (! directory.mkdir()) {
                throw new IllegalArgumentException(
                        "failed to create " +
                        directory.getAbsolutePath());
            }
        }

        if (! (directory.canRead() && directory.canWrite())) {
            throw new IllegalArgumentException(
                    "cannot read from and/or write to " +
                    directory.getAbsolutePath());
        }

        return directory;
    }

    private void saveCollectionWithoutNotification(ParameterCollectionId id,
                                                   Object collection)
            throws IllegalArgumentException {
        try {
            final File file = getParametersFile(id);
            Marshaller m = jaxbContext.createMarshaller();
            m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            m.setEventHandler(JAXB_HANDLER);
            m.marshal(collection, file);
        } catch (Exception e) {
            String message = "failed to save collection " + id;
            LOG.error(message, e);
            throw new IllegalArgumentException(message, e);
        }
    }

    private List<String> getParameterCollectionNames(File collectionDirectory) {
        List<String> list = new ArrayList<String>();
        final File[] directoryFiles = collectionDirectory.listFiles();
        if (directoryFiles != null) {
            for (File group : directoryFiles) {
                list = addGroupCollectionNamesToList(group, list);
            }
            Collections.sort(list);
        }
        return list;
    }

    private List<String> addGroupCollectionNamesToList(File groupDirectory,
                                                       List<String> list) {
        if (groupDirectory.exists() && groupDirectory.isDirectory()) {
            final String groupName = groupDirectory.getName();
            final File[] directoryFiles = groupDirectory.listFiles();
            if (directoryFiles != null) {
                String name;
                for (File config : directoryFiles) {
                    name = config.getName();
                    if (name.endsWith(".xml")) {
                        name = name.substring(0, name.length() - 4);
                        list.add(ParameterCollectionId.getFullCollectionName(
                                groupName, name));
                    }
                }
            }
        }
        return list;
    }

    /**
     * @param  category      category for the group or collection.
     * @param  groupName     name of the group or null if checking collections.
     * @param  collectionId  id of the collection or null if checking groups.
     *
     * @return list of configurations that reference the specified group or
     *         collection; if the specified category is CONFIGURATION
     *         an empty list is always returned.
     */
    private List<String> getConfigurationReferences(ParameterCollectionCategory category,
                                                    String groupName,
                                                    ParameterCollectionId collectionId) {

        List<String> groupReferences = new ArrayList<String>();

        final boolean checkGroup = (collectionId == null);

        if (! ParameterCollectionCategory.CONFIGURATION.equals(category)) {
            Configuration config;
            ParameterCollectionId configurationId;
            ParameterCollectionId nonConfigurationId;
            for (String name : getConfigurationNames()) {
                config = getConfiguration(name);
                if (ParameterCollectionCategory.BEHAVIOR.equals(category)) {
                    nonConfigurationId = config.getBehaviorParametersId();
                } else {
                    nonConfigurationId = config.getStimulusParametersId();
                }
                if (checkGroup) {
                    if ((nonConfigurationId != null) &&
                        (groupName.equals(nonConfigurationId.getGroupName()))) {
                        configurationId = config.getId();
                        groupReferences.add(configurationId.getFullName());
                    }
                } else {
                    if (collectionId.equals(nonConfigurationId)) {
                        configurationId = config.getId();
                        groupReferences.add(configurationId.getFullName());
                    }
                }
            }
        }

        return groupReferences;
    }

    private Object loadFile(ParameterCollectionId id)
            throws JAXBException,
                   FileNotFoundException,
                   ClassNotFoundException {

        Object object = null;
        FileInputStream in = null;
        final File file = getParametersFile(id);
        try {
            in = new FileInputStream(file);
            Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
            unmarshaller.setEventHandler(JAXB_HANDLER);
            object = unmarshaller.unmarshal(in);
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    LOG.warn("loadFile: failed to close " + file.getAbsolutePath(), e);
                }
            }
        }

        return object;
    }

    private File getParametersFile(ParameterCollectionId id) {
        final ParameterCollectionCategory category = id.getCategory();
        final File groupDirectory =
                findOrCreateGroupDirectory(category, id.getGroupName());
        return new File(groupDirectory, id.getName() + ".xml");
    }

    private File findOrCreateGroupDirectory(ParameterCollectionCategory category,
                                            String groupName) {
        final File categoryDirectory = new File(workDirectory,
                                                category.getName());
        final File groupDirectory = new File(categoryDirectory,
                                             groupName);
        if (! groupDirectory.exists()) {
            //noinspection ResultOfMethodCallIgnored
            groupDirectory.mkdirs();
        }
        return groupDirectory;
    }

    /**
     * Notifies any listeners that they should reload information from
     * this manager.
     */
    private void notifyChangeListeners() {
        propertyChangeSupport.firePropertyChange(
                new PropertyChangeEvent(this,
                                        "notification",
                                        null,
                                        "something changed"));
    }

    private static final Logger LOG =
            Logger.getLogger(ConfigurationManager.class);

    /** Ensures that JAXB marshaling errors are not hidden. */
    private static final ValidationEventHandler JAXB_HANDLER =
            new ValidationEventHandler() {
                public boolean handleEvent(ValidationEvent event ) {

                    boolean shouldProcessingContinue = false;

                    final String message = event.getMessage();

                    if (message.contains("unexpected element")) {
                        LOG.info("ignoring JAXB validation error:\n\n  " +
                                 message + "\n\n");
                        shouldProcessingContinue = true;
                    } else {
                        LOG.error("JAXB validation failed, message is:\n\n  " +
                                  message + "\n\nlocator is:\n\n  " +
                                  event.getLocator() + "\n\n");
                        //noinspection ThrowableResultOfMethodCallIgnored
                        LOG.error("JAXB linked exception (for debug)",
                                  event.getLinkedException());
                    }

                    return shouldProcessingContinue;
                }
            };
}

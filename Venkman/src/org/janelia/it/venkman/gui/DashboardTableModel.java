/*
 * Copyright (c) 2011 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.venkman.gui;

import org.janelia.it.venkman.config.Configuration;
import org.janelia.it.venkman.config.ConfigurationManager;
import org.janelia.it.venkman.config.ParameterCollectionId;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.List;

/**
 * Table model for the dashboard of rule configurations.
 *
 * @author Eric Trautman
 */
public class DashboardTableModel extends AbstractTableModel {

    private static final String[] COLUMN_NAMES = {
            "Configuration", "Behavior", "Rule", "Stimulus"
    };

    private ConfigurationManager configurationManager;
    private List<Configuration> configurations;

    public DashboardTableModel(ConfigurationManager configurationManager) {
        this.configurationManager = configurationManager;
        refreshConfigurations();
    }

    /**
     * Returns the number of rows in the model. A <code>JTable</code> uses this
     * method to determine how many rows it should display.  This method should be
     * quick, as it is called frequently during rendering.
     *
     * @return the number of rows in the model
     *
     * @see #getColumnCount
     */
    @Override
    public int getRowCount() {
        return configurations.size();
    }

    /**
     * Returns the number of columns in the model. A <code>JTable</code> uses this
     * method to determine how many columns it should create and display by
     * default.
     *
     * @return the number of columns in the model
     *
     * @see #getRowCount
     */
    @Override
    public int getColumnCount() {
        return 4;
    }

    @Override
    public String getColumnName(int col) {
        return COLUMN_NAMES[col];
    }

    /**
     * Returns the value for the cell at <code>columnIndex</code> and
     * <code>rowIndex</code>.
     *
     * @param    rowIndex    the row whose value is to be queried
     * @param    columnIndex the column whose value is to be queried
     * @return the value Object at the specified cell
     */
    @Override
    public Object getValueAt(int rowIndex,
                             int columnIndex) {
        String value = null;
        if ((rowIndex >= 0) && (rowIndex < configurations.size())) {
            final Configuration configuration = configurations.get(rowIndex);
            if (columnIndex == 0) {
                value = getCollectionName(configuration.getId());
            } else if (columnIndex == 1) {
                value = getCollectionName(
                        configuration.getBehaviorParametersId());
            } else if (columnIndex == 2) {
                value = configurationManager.getStimulusRulesCode(
                                configuration.getStimulusParametersId());
            } else if (columnIndex == 3) {
                value = getCollectionName(
                        configuration.getStimulusParametersId());
            }
        }
        return value;
    }

    public void refreshConfigurations() {
        // TODO: move off of event dispatch thread
        List<String> fullNames = configurationManager.getConfigurationNames();
        List<Configuration> updatedConfigurations =
                new ArrayList<Configuration>(fullNames.size());
        for (String fullName : fullNames) {
            updatedConfigurations.add(
                    configurationManager.getConfiguration(fullName));
        }
        configurations = updatedConfigurations;
    }

    public ParameterCollectionId getIdAt(int rowIndex,
                                         int columnIndex) {
        ParameterCollectionId id = null;
        if ((rowIndex >= 0) && (rowIndex < configurations.size())) {
            final Configuration configuration = configurations.get(rowIndex);
            if (columnIndex == 0) {
                id = configuration.getId();
            } else if (columnIndex == 1) {
                id = configuration.getBehaviorParametersId();
            } else if (columnIndex == 3) {
                id = configuration.getStimulusParametersId();
            }
        }
        return id;
    }

    private String getCollectionName(ParameterCollectionId id) {
        String fullName = null;
        if (id != null) {
            fullName = id.getFullName();
        }
        return fullName;
    }
}

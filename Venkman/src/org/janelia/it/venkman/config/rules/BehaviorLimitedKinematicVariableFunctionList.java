/*
 * Copyright (c) 2014 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.venkman.config.rules;

import org.janelia.it.venkman.data.LarvaFrameData;

import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.List;

/**
 * Container for ordered list of functions with convenience methods for applying
 * common operations to each function in the list.
 *
 * @author Eric Trautman
 */
@XmlRootElement
public class BehaviorLimitedKinematicVariableFunctionList {

    @XmlElementRef
    private List<BehaviorLimitedKinematicVariableFunction> functionList;

    public BehaviorLimitedKinematicVariableFunctionList() {
        this(null);
    }

    public BehaviorLimitedKinematicVariableFunctionList(List<BehaviorLimitedKinematicVariableFunction> functionList) {
        this.functionList = new ArrayList<BehaviorLimitedKinematicVariableFunction>();
        if (functionList != null) {
            this.functionList.addAll(functionList);
        }
    }

    /**
     * @return the number of functions in this list.
     */
    public int size() {
        return functionList.size();
    }

    /**
     * @return true if this list has at least one function;
     *         otherwise false.
     */
    public boolean hasFunctions() {
        return functionList.size() > 0;
    }

    /**
     * @param  index  index of the desired function.
     *
     * @return the function at the specified index in this list.
     */
    public BehaviorLimitedKinematicVariableFunction get(int index) {
        return functionList.get(index);
    }

    /**
     * Appends the specified function to the end of this list.
     *
     * @param  function  function to append.
     */
    public void append(BehaviorLimitedKinematicVariableFunction function) {
        functionList.add(function);
    }

    /**
     * Loops through this ordered list of functions, and applies each function result value to
     * the intensities in the specified stimulusList.
     *
     * @param  currentFrameData  data for the current frame (used for behavior mode).
     * @param  stimulusList      base list of stimulus values to scale.
     * @param  minimum           floor for scaled intensity values.
     */
    public void applyValues(LarvaFrameData currentFrameData,
                            List<LEDStimulus> stimulusList,
                            double minimum) {

        for (BehaviorLimitedKinematicVariableFunction function : functionList) {
            function.applyValue(currentFrameData, stimulusList, minimum);
        }

    }
}

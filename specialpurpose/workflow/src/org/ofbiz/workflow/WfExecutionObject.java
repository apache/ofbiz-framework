/*******************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *******************************************************************************/
package org.ofbiz.workflow;

import java.sql.Timestamp;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericValue;

/**
 * WfExecutionObject - Workflow Execution Object Interface
 */
public interface WfExecutionObject {

    /**
     * @throws WfException General workflow exception.
     * @return Current state of this object.
     */
    public List workflowStateType() throws WfException;

    /**
     * @throws WfException General workflow exception.
     * @return
     */
    public List whileOpenType() throws WfException;

    /**
     * @throws WfException General workflow exception.
     * @return Reason for not running.
     */
    public List whyNotRunningType() throws WfException;

    /**
     * @throws WfException General workflow exception.
     * @return Termination art of this process ot activity.
     */
    public List howClosedType() throws WfException;

    /**
     * Retrieve the list of all valid states.
     * @throws WfException General workflow exception.
     * @return List of valid states.
     */
    public List validStates() throws WfException;

    /**
     * Retrieve the current state of this process or activity.
     * @throws WfException General workflow exception
     * @return Current state.
     */
    public String state() throws WfException;

    /**
     * Set new state for this process or activity.
     * @param newState New state to be set.
     * @throws WfException General workflow exception.
     * @throws InvalidState The state is invalid.
     * @throws TransitionNotAllowed The transition is not allowed.
     */
    public void changeState(String newState) throws WfException, InvalidState, TransitionNotAllowed;

    /**
     * Getter for attribute 'name'.
     * @throws WfException General workflow exception.
     * @return Name of the object.
     */
    public String name() throws WfException;

    /**
     * Setter for attribute 'name'
     * @param newValue Set the name of the object.
     * @throws WfException General workflow exception.
     */
    public void setName(String newValue) throws WfException;

    /** Getter for the runtime key
     * @throws WfException
     * @return Key of the runtime object
     */
    public String runtimeKey() throws WfException;

    /**
     * Getter for definition key
     * @throws WfException General workflow exception.
     * @return Key of the definition object.
     */
    public String key() throws WfException;

    /**
     * Getter for attribute 'description'.
     * @throws WfException General workflow exception.
     * @return Description of this object.
     */
    public String description() throws WfException;

    /**
     * Setter for attribute 'description'.
     * @param newValue New value for attribute 'description'.
     * @throws WfException General workflow exception.
     */
    public void setDescription(String newValue) throws WfException;

    /**
     * Getter for attribute 'context'.
     * @throws WfException General workflow exception.
     * @return Process context.
     */
    public Map processContext() throws WfException;

    /**
     * Set the process context
     * @param newValue Set new process data.
     * @throws WfException General workflow exception.
     * @throws InvalidData The data is invalid.
     * @throws UpdateNotAllowed Update the context is not allowed.
     */
    public void setProcessContext(Map newValue) throws WfException, InvalidData, UpdateNotAllowed;

    /**
     * Set the process context (with previously stored data)
     * @param newValue RuntimeData entity key.
     * @throws WfException General workflow exception.
     * @throws InvalidData The data is invalid.
     * @throws UpdateNotAllowed Update the context is not allowed.
     */
    public void setProcessContext(String newValue) throws WfException, InvalidData, UpdateNotAllowed;

    /**
     * Get the Runtime Data key (context)
     * @return String primary key for the runtime (context) data
     * @throws WfException
     */
    public String contextKey() throws WfException;

    /**
     * Getter for attribute 'priority'.
     * @throws WfException General workflow exception.
     * @return Getter Priority of
     */
    public long priority() throws WfException;

    /**
     * Setter for attribute 'priority'.
     * @param newValue
     * @throws WfException General workflow exception
     */
    public void setPriority(long newValue) throws WfException;

    /**
     * Resume this process or activity.
     * @throws WfException General workflow exception.
     * @throws CannotResume
     * @throws NotRunning
     * @throws NotSuspended
     */
    public void resume() throws WfException, CannotResume, NotRunning, NotSuspended;

    /**
     * Suspend this process or activity.
     * @throws WfException General workflow exception.
     * @throws CannotSuspend
     * @throws NotRunning
     * @throws AlreadySuspended
     */
    public void suspend() throws WfException, CannotSuspend, NotRunning, AlreadySuspended;

    /**
     * Terminate this process or activity.
     * @throws WfException General workflow exception
     * @throws CannotStop
     * @throws NotRunning
     */
    public void terminate() throws WfException, CannotStop, NotRunning;

    /**
     * Abort the execution of this process or activity.
     * @throws WfException General workflow exception.
     * @throws CannotStop The execution cannot be sopped.
     * @throws NotRunning The process or activity is not yet running.
     */
    public void abort() throws WfException, CannotStop, NotRunning;

    /**
     * Getter for history count.
     * @throws WfException Generall workflow exception
     * @throws HistoryNotAvailable History can not be retrieved
     * @return Count of history Elements
     */
    public int howManyHistory() throws WfException, HistoryNotAvailable;

    /**
     * Search in the history for specific elements.
     * @param query Search criteria.
     * @param namesInQuery elements to search.
     * @throws WfException General workflow exception
     * @throws HistoryNotAvailable
     * @return Found history elements that meet the search criteria.
     */
    public Iterator getIteratorHistory(String query, java.util.Map namesInQuery) throws WfException, HistoryNotAvailable;

    /**
     * Getter for history sequence.
     * @param maxNumber Maximum number of element in result list.
     * @throws WfException General workflow exception.
     * @throws HistoryNotAvailable
     * @return List of History objects.
     */
    public List getSequenceHistory(int maxNumber) throws WfException, HistoryNotAvailable;

    /**
     * Predicate to check if a 'member' is an element of the history.
     * @param member An element of the history.
     * @throws WfException General workflow exception.
     * @return true if the element of the history, false otherwise.
     */
    public boolean isMemberOfHistory(WfExecutionObject member) throws WfException;

    /**
     * Getter for timestamp of last state change.
     * @throws WfException General workflow exception.
     * @return Timestamp of last state change.
     */
    public Timestamp lastStateTime() throws WfException;

    /**
     * Gets the GenericValue object of the definition.
     * @returns GenericValue object of the definition.
     * @throws WfException
     */
    public GenericValue getDefinitionObject() throws WfException;

    /**
     * Gets the GenericValue object of the runtime workeffort.
     * @returns GenericValue object of the runtime workeffort.
     * @throws WfException
     */
    public GenericValue getRuntimeObject() throws WfException;

    /**
     * Returns the delegator being used by this workflow
     * @return GenericDelegator used for this workflow
     * @throws WfException
     */
    public GenericDelegator getDelegator() throws WfException;

} // interface WfExecutionObjectOperations


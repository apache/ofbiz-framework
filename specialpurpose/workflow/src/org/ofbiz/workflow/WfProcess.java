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

import java.util.Iterator;
import java.util.Map;
import java.util.List;

/**
 * WfProcess - Workflow Process Interface
 */
public interface WfProcess extends WfExecutionObject {

    /**
     * Gets the requester of this process
     * @throws WfException
     * @return requester of this process
     */
    public WfRequester requester() throws WfException;

    /**
     * Sets the requester for this process
     * @param newValue
     * @throws WfException
     * @throws CannotChangeRequester
     */
    public void setRequester(WfRequester newValue) throws WfException, CannotChangeRequester;

    /**
     * @throws WfException
     * @return returns how many steps
     */
    public int howManyStep() throws WfException;

    /**
     * @throws WfException
     * @return returns an iterator of WfActivity
     */
    public Iterator<WfActivity> getIteratorStep() throws WfException;

    /**
     * @param maxNumber
     * @throws WfException
     * @return List of WfActivity objects.
     */
    public List<WfActivity> getSequenceStep(int maxNumber) throws WfException;

    /**
     * @param member
     * @throws WfException
     * @return returns if is a member of step
     */
    public boolean isMemberOfStep(WfActivity member) throws WfException;

    /**
     * Gets the manager associated with this process
     * @throws WfException
     * @return returns the manager
     */
    public WfProcessMgr manager() throws WfException;

    /**
     * Gets the results of this process
     * @throws WfException
     * @throws ResultNotAvailable
     * @return result Map of results from this process
     */
    public Map<String, Object> result() throws WfException, ResultNotAvailable;

    /**
     * Receives activity results.
     * @param activity WfActivity sending the results.
     * @param results Map of the results.
     * @throws WfException
     */
    public void receiveResults(WfActivity activity, Map<String, Object> results) throws WfException;

    /**
     * Receives notification when an activity has completed.
     * @param activity WfActivity which has completed.
     * @throws WfException
     */
    public void activityComplete(WfActivity activity) throws WfException;

    /**
     * Starts the Workflow process
     * @throws WfException
     * @throws CannotStart
     * @throws AlreadyRunning
     */
    public void start() throws WfException, CannotStart, AlreadyRunning;

    /**
     * Starts the Workflow process on the defined activity
     * @param activityId The activity to start
     * @throws WfException
     * @throws CannotStart
     * @throws AlreadyRunning
     */
    public void start(String activityId) throws WfException, CannotStart, AlreadyRunning;

    /**
     * @param state
     * @throws WfException
     * @throws InvalidState
     * @return returns an iterator of WfActivity
     
     */
    public Iterator<WfActivity> getActivitiesInState(String state) throws WfException, InvalidState;

} // interface WfProcessOperations

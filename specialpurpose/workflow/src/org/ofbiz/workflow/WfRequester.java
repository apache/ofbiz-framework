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
import java.util.List;
import java.util.Map;

import org.ofbiz.service.GenericRequester;

/**
 * WfRequester - Workflow Requester Interface
 */
public interface WfRequester {

    /**
     * Gets the number of processes.
     * @throws WfException
     * @return Count of the number of workflow processes
     */
    public int howManyPerformer() throws WfException;

    /** Gets an iterator of processes.
     * @throws WfException
     * @return Iterator of workflow processes.
     */
    public Iterator getIteratorPerformer() throws WfException;

    /**
     * A list of processes
     * @param maxNumber
     * @throws WfException
     * @return List of WfProcess objects.
     */
    public List getSequencePerformer(int maxNumber) throws WfException;

    /**
     * Checks if a WfProcess is associated with this requester object
     * @param member
     * @throws WfException
     * @return true if the process is found.
     */
    public boolean isMemberOfPerformer(WfProcess member) throws WfException;

    /**
     * Registers a process with this requester; starts the process.
     *@param process to register
     *@param context to initialize the process with
     *@param requester associated with the service
     *@throws WfException
     */
    public void registerProcess(WfProcess process, Map context, GenericRequester requester) throws WfException;

    /**
     * Receives notice of event status changes
     * @param event
     * @throws WfException
     * @throws InvalidPerformer
     */
    public void receiveEvent(WfEventAudit event) throws WfException, InvalidPerformer;

} // interface WfRequesterOperations

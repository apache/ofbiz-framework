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

/**
 * WfResource - Workflow Resource Interface
 */
public interface WfResource {

    /** 
     * Gets the number of work items
     * @throws WfException
     * @return Count of work items
     */
    public int howManyWorkItem() throws WfException;

    /** 
     * Gets an iterator of work items
     * @throws WfException
     * @return Iterator of work items
     */
    public Iterator getIteratorWorkItem() throws WfException;

    /** 
     * Gets the work items
     * @param maxNumber
     * @throws WfException
     * @return List of WfAssignment objects.
     */
    public List getSequenceWorkItem(int maxNumber) throws WfException;

    /** 
     * Checks if an assignment object is associated with this resource
     * @param member The assignment object to check
     * @throws WfException
     * @return true if assignment is part of the work list
     */
    public boolean isMemberOfWorkItems(WfAssignment member) throws WfException;

    /** 
     * Gets the resource key.
     * @throws WfException
     * @return String of the resouce key.
     */
    public String resourceKey() throws WfException;

    /** 
     * Gets the resource name
     * @throws WfException
     * @return String of the resource name
     */
    public String resourceName() throws WfException;

    /** 
     * Gets the role id of this resource
     * @throws WfException
     * @return String role id of this participant or null if none
     */
    public String resourceRoleId() throws WfException;

    /** 
     * Gets the party id of this resource
     * @throws WfException
     * @return String party id of this participant or null if none
     */
    public String resourcePartyId() throws WfException;

    /** 
     * Release the resouce from the assignement
     * @param fromAssigment
     * @param releaseInfo
     * @throws WfException
     * @throws NotAssigned
     */
    public void release(WfAssignment fromAssignment, String releaseInfo) throws WfException, NotAssigned;

} // interface WfResourceOperations

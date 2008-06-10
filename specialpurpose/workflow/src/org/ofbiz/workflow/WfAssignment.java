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

/**
 * WfAssignment - Workflow Assignment Interface
 */
public interface WfAssignment {

    /**
     * Gets the activity object of this assignment.
     * @return WfActivity The activity object of this assignment.
     * @throws WfException
     */
    public WfActivity activity() throws WfException;

    /**
     * Gets the assignee (resource) of this assignment.
     * @return WfResource The assigned resource.
     * @throws WfException
     */
    public WfResource assignee() throws WfException;

    /**
     * Sets the assignee of this assignment.
     * @param newValue The new assigned resource.
     * @throws WfException
     * @throws InvalidResource
     */
    public void setAssignee(WfResource newValue) throws WfException, InvalidResource;

    /**
     * Gets the from date of this assignment.
     * @return Timestamp when this assignment first began.
     * @throws WfException
     */
    public java.sql.Timestamp fromDate() throws WfException;

    /**
     * Mark this assignment as accepted.
     * @throws WfException
     */
    public void accept() throws WfException;

    /**
     * Set the results of this assignment.
     * @param Map The results of the assignement.
     * @throws WfException
     */
    public void setResult(java.util.Map results) throws WfException;

    /**
     * Mark this assignment as complete.
     * @throws WfException
     */
    public void complete() throws WfException;

    /**
     * Mark this assignment as delegated.
     * @throws WfException
     */
    public void delegate() throws WfException;

    /**
     * Change the status of this assignment.
     * @param status The new status.
     * @throws WfException
     */
    public void changeStatus(String status) throws WfException;

    /**
     * Gets the status of this assignment.
     * @return String status code for this assignment.
     * @throws WfException
     */
    public String status() throws WfException;

    /**
     * Removes the stored data for this object.
     * @throws WfException
     */
    public void remove() throws WfException;

} // interface WfAssignmentOperations

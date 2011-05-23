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

/**
 * WfEventAudit - Workflow Event Audit Interface
 */
public interface WfEventAudit {

    /**
     * @throws WfException
     * @throws SourceNotAvailable
     * @return return the source
     */
    public WfExecutionObject source() throws WfException, SourceNotAvailable;

    /**
     * @throws WfException
     * @return return the timestamp
     */
    public Timestamp timeStamp() throws WfException;

    /**
     * @throws WfException
     * @return return the event type
     */
    public String eventType() throws WfException;

    /**
     * @throws WfException
     * @return return the activity key
     */
    public String activityKey() throws WfException;

    /**
     * @throws WfException
     * @return return the activity name
     */
    public String activityName() throws WfException;

    /**
     * @throws WfException
     * @return return the process key
     */
    public String processKey() throws WfException;

    /**
     * @throws WfException
     * @return return the process name
     */
    public String processName() throws WfException;

    /**
     * @throws WfException
     * @return return the process mgr name
     */
    public String processMgrName() throws WfException;

    /**
     * @throws WfException
     * @return return the process mgr version
     */
    public String processMgrVersion() throws WfException;

} // interface WfEventAuditOperations

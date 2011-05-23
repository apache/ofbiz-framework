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
 * WfCreateProcessEventAudit - Workflow Create Process Event Audit Interface
 */
public interface WfCreateProcessEventAudit extends WfEventAudit {

    /**
     * @throws WfException
     * @return return the activity key
     */
    public String pActivityKey() throws WfException;

    /**
     * @throws WfException
     * @return return the process key
     */
    public String pProcessKey() throws WfException;

    /**
     * @throws WfException
     * @return return the process name
     */
    public String pProcessName() throws WfException;

    /**
     * @throws WfException
     * @return return the process mgr name
     */
    public String pProcessMgrName() throws WfException;

    /**
     * @throws WfException
     * @return return the process mgr version
     */
    public String pProcessMgrVersion() throws WfException;

} // interface WfCreateProcessEventAudit

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

import java.util.Map;
import java.util.Iterator;
import java.util.List;

/**
 * WfProcessMgr - Workflow Process Manager Interface
 */
public interface WfProcessMgr {

    /**
     * @throws WfException
     * @return
     */
    public int howManyProcess() throws WfException;

    /**
     * @throws WfException
     * @return
     */
    public Iterator getIteratorProcess() throws WfException;

    /**
     * @param maxNumber
     * @throws WfException
     * @return List of WfProcess objects.
     */
    public List getSequenceProcess(int maxNumber) throws WfException;

    /**
     * @param member
     * @throws WfException
     * @return
     */
    public boolean isMemberOfProcess(WfProcess member) throws WfException;

    /**
     * @throws WfException
     * @return
     */
    public List processMgrStateType() throws WfException;

    /**
     * @param newState
     * @throws WfException
     * @throws TransitionNotAllowed
     */
    public void setProcessMgrState(String newState) throws WfException, TransitionNotAllowed;

    /**
     * @throws WfException
     * @return
     */
    public String name() throws WfException;

    /**
     * @throws WfException
     * @return
     */
    public String description() throws WfException;

    /**
     * @throws WfException
     * @return
     */
    public String category() throws WfException;

    /**
     * @throws WfException
     * @return
     */
    public String version() throws WfException;

    /**
     * @throws WfException
     * @return
     */
    public Map contextSignature() throws WfException;

    /**
     * @throws WfException
     * @return
     */
    public Map resultSignature() throws WfException;

    /**
    * @throws WfException
    * @return initial context based on DataFields
    */
    public Map getInitialContext() throws WfException;

    /**
     * Create a WfProcess object
     * @param requester
     * @throws WfException
     * @throws NotEnabled
     * @throws InvalidRequester
     * @throws RequesterRequired
     * @return WfProcess created
     */
    public WfProcess createProcess(WfRequester requester)
        throws WfException, NotEnabled, InvalidRequester, RequesterRequired;

} // interface WfProcessMgr

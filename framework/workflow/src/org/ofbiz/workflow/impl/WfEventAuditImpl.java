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
package org.ofbiz.workflow.impl;

import java.sql.Timestamp;
import java.util.Date;

import org.ofbiz.base.util.ObjectType;
import org.ofbiz.workflow.SourceNotAvailable;
import org.ofbiz.workflow.WfActivity;
import org.ofbiz.workflow.WfEventAudit;
import org.ofbiz.workflow.WfException;
import org.ofbiz.workflow.WfExecutionObject;
import org.ofbiz.workflow.WfProcess;

/**
 * WfEventAuditImpl - Workflow Event Audit implementation
 */
public class WfEventAuditImpl implements WfEventAudit {

    private WfExecutionObject object = null;
    private String eventType = null;
    private Timestamp timeStamp = null;

    public WfEventAuditImpl(WfExecutionObject object, String eventType) {
        this.object = object;
        this.eventType = eventType;
        this.timeStamp = new Timestamp(new Date().getTime());
    }
    
    /**
     * @see org.ofbiz.workflow.WfEventAudit#source()
     */
    public WfExecutionObject source() throws WfException, SourceNotAvailable {
        return object;
    }
 
    /**
     * @see org.ofbiz.workflow.WfEventAudit#timeStamp()
     */
    public Timestamp timeStamp() throws WfException {
        return timeStamp;
    }

    /**
     * @see org.ofbiz.workflow.WfEventAudit#eventType()
     */
    public String eventType() throws WfException {
        return eventType;
    }

    /**
     * @see org.ofbiz.workflow.WfEventAudit#activityKey()
     */
    public String activityKey() throws WfException {
        try {
            if (ObjectType.instanceOf(object, "org.ofbiz.workflow.WfActivity"))
                return object.key();
        } catch (Exception e) {
            throw new WfException("Source is not a WfActivity object");
        }
        throw new WfException("Source is not a WfActivity object");
    }

    /**
     * @see org.ofbiz.workflow.WfEventAudit#activityName()
     */
    public String activityName() throws WfException {
        try {
            if (ObjectType.instanceOf(object, "org.ofbiz.workflow.WfActivity"))
                return object.name();
        } catch (Exception e) {}
        throw new WfException("Source is not a WfActivity object");

    }

    /**
     * @see org.ofbiz.workflow.WfEventAudit#processKey()
     */
    public String processKey() throws WfException {
        try {
            if (ObjectType.instanceOf(object, "org.ofbiz.workflow.WfProcess"))
                return object.key();
        } catch (Exception e) {}
        throw new WfException("Source is not a WfProcess object");

    }

    /**
     * @see org.ofbiz.workflow.WfEventAudit#processName()
     */
    public String processName() throws WfException {
        try {
            if (ObjectType.instanceOf(object, "org.ofbiz.workflow.WfProcess"))
                return object.name();
        } catch (Exception e) {}
        throw new WfException("Source is not a WfProcess object");

    }

    /**
     * @see org.ofbiz.workflow.WfEventAudit#processMgrName()
     */
    public String processMgrName() throws WfException {
        try {
            if (ObjectType.instanceOf(object, "org.ofbiz.workflow.WfProcess"))
                return ((WfProcess) object).manager().name();
            else if (ObjectType.instanceOf(object, "org.ofbiz.workflow.WfActivity"))
                return ((WfActivity) object).container().manager().name();
        } catch (Exception e) {}
        throw new WfException("Illegal source object");
    }

    /**
     * @see org.ofbiz.workflow.WfEventAudit#processMgrVersion()
     */
    public String processMgrVersion() throws WfException {
        try {
            if (ObjectType.instanceOf(object, "org.ofbiz.workflow.WfProcess"))
                return ((WfProcess) object).manager().version();
            else if (ObjectType.instanceOf(object, "org.ofbiz.workflow.WfActivity"))
                return ((WfActivity) object).container().manager().version();
        } catch (Exception e) {}
        throw new WfException("Illegal source object");
    }
}


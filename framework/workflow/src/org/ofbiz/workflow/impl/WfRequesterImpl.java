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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Locale;

import org.ofbiz.base.util.Debug;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.service.GenericRequester;
import org.ofbiz.service.GenericServiceException;
import org.ofbiz.service.ModelService;
import org.ofbiz.workflow.InvalidPerformer;
import org.ofbiz.workflow.SourceNotAvailable;
import org.ofbiz.workflow.WfEventAudit;
import org.ofbiz.workflow.WfException;
import org.ofbiz.workflow.WfProcess;
import org.ofbiz.workflow.WfProcessMgr;
import org.ofbiz.workflow.WfRequester;

/**
 * WfRequesterImpl - Workflow Requester implementation
 */
public class WfRequesterImpl implements WfRequester {
    
    public static final String module = WfRequesterImpl.class.getName();

    protected Map performers = null;
      
    /**
     * Method WfRequesterImpl.
     */
    public WfRequesterImpl() {
        this.performers = new HashMap();
    }

    /**
     * @see org.ofbiz.workflow.WfRequester#registerProcess(org.ofbiz.workflow.WfProcess, java.util.Map, org.ofbiz.service.GenericRequester)
     */  
    public void registerProcess(WfProcess process, Map context, GenericRequester requester) throws WfException {
        if (process == null)
            throw new WfException("Process cannot be null");
        if (context == null)
            throw new WfException("Context should not be null");

        performers.put(process, requester);
        WfProcessMgr mgr = process.manager();

        // Validate the process context w/ what was passed.
        try {
            if (Debug.verboseOn()) Debug.logVerbose("Validating w/ signature: " + mgr.contextSignature(), module);
            ModelService.validate(mgr.contextSignature(), context, true, null, ModelService.IN_PARAM, Locale.getDefault());
        } catch (GenericServiceException e) {
            throw new WfException("Context passed does not validate against defined signature: ", e);
        }

        // Set the context w/ the process        
        Map localContext = new HashMap(context);
        localContext.putAll(mgr.getInitialContext());
        process.setProcessContext(localContext);   
        
        // Set the source reference id if one was passed
        GenericValue processDefinition = process.getDefinitionObject();
        String sourceReferenceField = processDefinition.getString("sourceReferenceField");
        if (context.containsKey(sourceReferenceField)) {  
            GenericValue processObj = process.getRuntimeObject();
            if (processObj != null) {
                try {
                    processObj.set("sourceReferenceId", localContext.get(sourceReferenceField));
                    processObj.store();
                } catch (GenericEntityException e) {
                    throw new WfException("Cannot set sourceReferenceId on the process runtime object", e);
                }
            }
        }
                  
    }

    /**
     * @see org.ofbiz.workflow.WfRequester#howManyPerformer()
     */    
    public int howManyPerformer() throws WfException {
        return performers.size();
    }
  
    /**
     * @see org.ofbiz.workflow.WfRequester#getIteratorPerformer()
     */
    public Iterator getIteratorPerformer() throws WfException {
        return performers.keySet().iterator();
    }
   
    /**
     * @see org.ofbiz.workflow.WfRequester#getSequencePerformer(int)
     */
    public List getSequencePerformer(int maxNumber) throws WfException {
        if (maxNumber > 0)
            return new ArrayList(performers.keySet()).subList(0, (maxNumber - 1));
        return new ArrayList(performers.keySet());
    }
  
    /**
     * @see org.ofbiz.workflow.WfRequester#isMemberOfPerformer(org.ofbiz.workflow.WfProcess)
     */
    public boolean isMemberOfPerformer(WfProcess member) throws WfException {
        return performers.containsKey(member);
    }
   
    /**
     * @see org.ofbiz.workflow.WfRequester#receiveEvent(org.ofbiz.workflow.WfEventAudit)
     */
    public synchronized void receiveEvent(WfEventAudit event) throws WfException, InvalidPerformer {
        // Should the source of the audit come from the process? if so use this.
        WfProcess process = null;

        try {
            process = (WfProcess) event.source();
        } catch (SourceNotAvailable sna) {
            throw new InvalidPerformer("Could not get the performer", sna);
        } catch (ClassCastException cce) {
            throw new InvalidPerformer("Not a valid process object", cce);
        }
        if (process == null)
            throw new InvalidPerformer("No performer specified");
        if (!performers.containsKey(process))
            throw new InvalidPerformer("Performer not assigned to this requester");

        GenericRequester req = null;

        if (performers.containsKey(process))
            req = (GenericRequester) performers.get(process);
        if (req != null)
            req.receiveResult(process.result());
    }
}


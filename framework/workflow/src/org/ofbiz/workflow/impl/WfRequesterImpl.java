/*
 * $Id: WfRequesterImpl.java 5462 2005-08-05 18:35:48Z jonesde $
 *
 * Copyright (c) 2001, 2002 The Open For Business Project - www.ofbiz.org
 *
 * Permission is hereby granted, free of charge, to any person obtaining a
 * copy of this software and associated documentation files (the "Software"),
 * to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
 * OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY
 * CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT
 * OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR
 * THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 */
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
 *
 * @author     <a href="mailto:jaz@ofbiz.org">Andy Zeneski</a>
 * @version    $Rev$
 * @since      2.0
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


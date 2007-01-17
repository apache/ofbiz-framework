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
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.StringUtil;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.service.GenericResultWaiter;
import org.ofbiz.service.ModelService;
import org.ofbiz.workflow.WfException;

/**
 * WfActivityToolImplementation.java
 */
public class WfActivityToolImplementation extends WfActivityAbstractImplementation {

    public static final String module = WfActivityToolImplementation.class.getName();

    public WfActivityToolImplementation(WfActivityImpl wfActivity) {
        super(wfActivity);
    }

    /**
     * @see org.ofbiz.workflow.impl.WfActivityAbstractImplementation#run()
     */
    public void run() throws WfException {
        Collection tools = null;
        //Linea agregada por Oswin Ondarza
        String allParams = "";
        try {
            tools = getActivity().getDefinitionObject().getRelated("WorkflowActivityTool");
        } catch (GenericEntityException e) {
            throw new WfException(e.getMessage(), e);
        }
        if (tools == null) {
            setComplete(true);
            return; // Null tools mean nothing to do (same as route?)
        }

        if (Debug.verboseOn())
            Debug.logVerbose("[WfActivity.runTool] : Running tools (" + tools.size() + ").", module);
        List waiters = new ArrayList();
        Iterator i = tools.iterator();
        while (i.hasNext()) {
            GenericValue thisTool = (GenericValue) i.next();
            String serviceName = null;
            String toolId = thisTool.getString("toolId");
            String params = thisTool.getString("actualParameters");
            String toolTypeEnumId = thisTool.getString("toolTypeEnumId");
            
            //Linea agregada por Oswin Ondarza
            allParams = allParams + "," + params;
            String extend = thisTool.getString("extendedAttributes");
            
            Map extendedAttr = StringUtil.strToMap(extend);            
            if (extendedAttr != null && extendedAttr.containsKey("serviceName"))
                serviceName = (String) extendedAttr.get("serviceName");
                
            serviceName = serviceName != null ? serviceName : (toolTypeEnumId.equals("WTT_APPLICATION") ? 
                    "wfActivateApplication" : toolId);                                                       
            waiters.add(runService(serviceName, params, extend));
        }

        while (waiters.size() > 0) {
            Iterator wi = waiters.iterator();
            Collection remove = new ArrayList();
            while (wi.hasNext()) {
                GenericResultWaiter thw = (GenericResultWaiter) wi.next();
                
                if (thw.isCompleted()) {
                    Map thwResult = null;
                    if (thw.status() == GenericResultWaiter.SERVICE_FINISHED) {
                        thwResult = thw.getResult();
                        Debug.logVerbose("Service finished.", module);
                    } else if (thw.status() == GenericResultWaiter.SERVICE_FAILED) {
                        Debug.logError(thw.getThrowable(), "Service failed", module);
                    }
                    if (thwResult != null && thwResult.containsKey(ModelService.RESPONSE_MESSAGE)) {
                        if (thwResult.get(ModelService.RESPONSE_MESSAGE).equals(ModelService.RESPOND_ERROR)) {
                            String errorMsg = (String) thwResult.remove(ModelService.ERROR_MESSAGE);
                            Debug.logError("Service Error: " + errorMsg, module);
                        }
                        thwResult.remove(ModelService.RESPONSE_MESSAGE);
                    }                    
                    
                    try {
                        if (thwResult != null)
                            this.setResult(thwResult, allParams);
                    } catch (IllegalStateException e) {
                        throw new WfException("Unknown error", e);
                    }
                    remove.add(thw);
                }
            }
            waiters.removeAll(remove);
        }
        
        setComplete(true);
    }

    protected void setResult(Map result, String allParams) {
        Map newResult = new HashMap(result);
        List params = StringUtil.split(allParams, ",");
        Iterator i = params.iterator();
        while (i.hasNext()) {
            Object keyExpr = i.next();
            String keyExprStr = (String) keyExpr;

            if (keyExprStr != null && keyExprStr.trim().toLowerCase().startsWith("name:")) {
                List couple = StringUtil.split(keyExprStr.trim().substring(5).trim(), "=");
                Object keyParam = ((String) couple.get(0)).trim();
                Object keyNewParam = ((String) couple.get(1)).trim();

                if (result.containsKey(keyParam)) {
                    newResult.put(keyNewParam, result.get(keyParam));
                    newResult.remove(keyParam);

                }
            }
        }
        if (Debug.verboseOn()) Debug.logVerbose("Setting result in context: " + newResult, module);
        this.setResult(newResult);
    }
}

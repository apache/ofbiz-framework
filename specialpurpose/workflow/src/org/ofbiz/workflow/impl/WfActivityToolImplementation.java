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
    @Override
    public void run() throws WfException {
        List<GenericValue> tools = null;
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
        List<GenericResultWaiter> waiters = new ArrayList<GenericResultWaiter>();
        for (GenericValue thisTool : tools) {
            String serviceName = null;
            String toolId = thisTool.getString("toolId");
            String params = thisTool.getString("actualParameters");
            String toolTypeEnumId = thisTool.getString("toolTypeEnumId");

            allParams = allParams + "," + params;
            String extend = thisTool.getString("extendedAttributes");

            Map<String, String> extendedAttr = StringUtil.strToMap(extend);
            if (extendedAttr != null && extendedAttr.containsKey("serviceName")) {
                serviceName = (String) extendedAttr.get("serviceName");
            }

            serviceName = serviceName != null ? serviceName : (toolTypeEnumId.equals("WTT_APPLICATION") ? "wfActivateApplication" : toolId);
            waiters.add(runService(serviceName, params, extend));
        }

        while (waiters.size() > 0) {
            List<GenericResultWaiter> remove = new ArrayList<GenericResultWaiter>();
            for (GenericResultWaiter thw : waiters) {
                if (thw.isCompleted()) {
                    Map<String, Object> thwResult = null;
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

    protected void setResult(Map<String, Object> result, String allParams) {
        Map<String, Object> newResult = new HashMap<String, Object>(result);
        List<String> params = StringUtil.split(allParams, ",");
        
        for (String keyExprStr : params) {
            if (keyExprStr != null && keyExprStr.trim().toLowerCase().startsWith("name:")) {
                List<String> couple = StringUtil.split(keyExprStr.trim().substring(5).trim(), "=");
                String keyParam = (couple.get(0)).trim();
                String keyNewParam = (couple.get(1)).trim();

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

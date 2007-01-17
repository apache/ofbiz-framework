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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.ofbiz.base.util.Debug;
import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.GenericResultWaiter;
import org.ofbiz.service.GenericServiceException;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.service.ModelService;
import org.ofbiz.workflow.WfException;

/**
 * WfActivityAbstractImplementation.java
 */
public abstract class WfActivityAbstractImplementation {

    public static final String module = WfActivityAbstractImplementation.class.getName();

    private WfActivityImpl wfActivity = null;
    private Map resultContext = new HashMap();
    private boolean complete = false;

    public WfActivityAbstractImplementation(WfActivityImpl wfActivity) {
        this.wfActivity = wfActivity;
    }

    /**
     * Run the implementation.
     * @throws WfException
     */
    public abstract void run() throws WfException;

    protected GenericResultWaiter runService(String serviceName, String params, String extend) throws WfException {
        LocalDispatcher dispatcher = getActivity().getDispatcher();
        DispatchContext dctx = dispatcher.getDispatchContext();
        ModelService service = null;
        Debug.logVerbose("[WfActivityAbstractImplementation.runService] : Getting the service model.", module);
        try {
            service = dctx.getModelService(serviceName);
        } catch (GenericServiceException e) {
            throw new WfException(e.getMessage(), e);
        }
        if (service == null)
            throw new WfException("Cannot determine model service for service name");

        return runService(service, params, extend);
    }

    protected GenericResultWaiter runService(ModelService service, String params, String extend) throws WfException { 
        LocalDispatcher dispatcher = getActivity().getDispatcher();
        List paramNames = service.getParameterNames(ModelService.IN_PARAM, true);
        if (paramNames != null && paramNames.size() == 0)
            paramNames =  null;
                 
        Map ctx = getActivity().actualContext(params, extend, paramNames, false);
        
        GenericResultWaiter waiter = new GenericResultWaiter();
        Debug.logVerbose("[WfActivityAbstractImplementation.runService] : Invoking the service.", module);
        try {
            dispatcher.runAsync(service.name, ctx, waiter, false);
        } catch (GenericServiceException e) {
            throw new WfException(e.getMessage(), e);
        }

        return waiter;
    }

    protected void setResult(Map result) {
        this.resultContext.putAll(result);
    }

    protected WfActivityImpl getActivity() throws WfException {
        if (this.wfActivity == null)
            throw new WfException("Activity object is null");
        return wfActivity;
    }

    /**
     * Returns the result context.
     * @return Map
     */
    public Map getResult() {
        return resultContext;
    }

    /** 
     * Getter for property complete.
     * @return Value of property complete.
     */
    public boolean isComplete() {
        return this.complete;
    }

    /** 
     * Setter for property complete.
     * @param complete New value of property complete.
     */
    protected void setComplete(boolean complete) {
        this.complete = complete;
    }
}

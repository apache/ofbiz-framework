/*
 * $Id: WfActivityAbstractImplementation.java 5462 2005-08-05 18:35:48Z jonesde $
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
 *
 * @author     <a href="mailto:jaz@ofbiz.org">Andy Zeneski</a> 
 * @author     Oswin Ondarza and Manuel Soto 
 * @version    $Rev$
 * @since      2.0
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

/*
 * $Id: ServiceCondition.java 5462 2005-08-05 18:35:48Z jonesde $
 *
 * Copyright (c) 2003 The Open For Business Project - www.ofbiz.org
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
import java.util.Map;

import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.GenericServiceException;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.service.ModelService;
import org.ofbiz.workflow.EvaluationException;
import org.ofbiz.workflow.TransitionCondition;


/**
 * ServiceCondition - Invokes a special service for condition evaluation
 * 
 * To call a service set a Transition ExtendedAttribute named 'serviceName', services are required
 * to return a Boolean OUT parameter named 'evaluationResult'
 *
 * @author     <a href="mailto:jaz@ofbiz.org">Andy Zeneski</a> 
 * @version    $Rev$
 * @since      2.1
 */
public class ServiceCondition implements TransitionCondition {

    /**
     * @see org.ofbiz.workflow.TransitionCondition#evaluateCondition(java.util.Map, java.util.Map, java.lang.String, org.ofbiz.service.DispatchContext)
     */
    public Boolean evaluateCondition(Map context, Map attrs, String expression, DispatchContext dctx) throws EvaluationException {
        // get the service to call
        String serviceName = (String) attrs.get("serviceName");
        if (serviceName == null || serviceName.length() == 0)
            throw new EvaluationException("Invalid serviceName; be sure to set the serviceName ExtendedAttribute");
          
        // get the dispatcher   
        LocalDispatcher dispatcher = dctx.getDispatcher();
        if (dispatcher == null)
            throw new EvaluationException("Bad LocalDispatcher found in the DispatchContext");
        
        // create a map of all context and extended attributes, attributes will overwrite context values
        Map newContext = new HashMap(context);
        newContext.putAll(attrs);
        
        // build the context for the service
        Map serviceContext = null;
        ModelService model = null;
        try {
            model = dctx.getModelService(serviceName);
            serviceContext = model.makeValid(newContext, ModelService.IN_PARAM);
        } catch (GenericServiceException e) {
            throw new EvaluationException("Cannot get ModelService object for service named: " + serviceName, e);            
        }
        
        // invoke the service
        Map serviceResult = null;
        try {
            serviceResult = dispatcher.runSync(serviceName, serviceContext);
        } catch (GenericServiceException e) {
            throw new EvaluationException("Cannot invoke the service named: " + serviceName, e);
        }
        
        // get the evaluationResult object from the result
        Boolean evaluationResult = null;
        try {
            evaluationResult = (Boolean) serviceResult.get("evaluationResult");
        } catch (ClassCastException e) {
            throw new EvaluationException("Service did not return a valid evaluationResult object");
        }
        
        return evaluationResult;
    }

}

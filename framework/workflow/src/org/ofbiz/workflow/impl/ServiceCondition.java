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

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
package org.ofbiz.minilang.method.callops;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.GeneralException;
import org.ofbiz.base.util.ObjectType;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.minilang.SimpleMethod;
import org.ofbiz.minilang.method.ContextAccessor;
import org.ofbiz.minilang.method.MethodContext;
import org.ofbiz.minilang.method.MethodOperation;
import org.ofbiz.service.GenericServiceException;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.service.ModelParam;
import org.ofbiz.service.ModelService;
import org.w3c.dom.Element;

/**
 * Sets all Service parameters/attributes in the to-map using the map as a source
 */
public class SetServiceFields extends MethodOperation {
    
    public static final String module = CallService.class.getName();
    
    String serviceName;
    ContextAccessor mapAcsr;
    ContextAccessor toMapAcsr;

    public SetServiceFields(Element element, SimpleMethod simpleMethod) {
        super(element, simpleMethod);
        serviceName = element.getAttribute("service-name");
        mapAcsr = new ContextAccessor(element.getAttribute("map-name"));
        toMapAcsr = new ContextAccessor(element.getAttribute("to-map-name"));
    }

    public boolean exec(MethodContext methodContext) {
        String serviceName = methodContext.expandString(this.serviceName);

        Map fromMap = (Map) mapAcsr.get(methodContext);
        if (fromMap == null) {
            Debug.logWarning("The from map in set-service-field was not found with name: " + mapAcsr, module);
            return true;
        }

        Map toMap = (Map) toMapAcsr.get(methodContext);
        if (toMap == null) {
            toMap = new HashMap();
            toMapAcsr.put(methodContext, toMap);
        }
        
        LocalDispatcher dispatcher = methodContext.getDispatcher();
        ModelService modelService = null;
        try {
            modelService = dispatcher.getDispatchContext().getModelService(serviceName);
        } catch (GenericServiceException e) {
            String errMsg = "In set-service-fields could not get service definition for service name [" + serviceName + "]: " + e.toString();
            Debug.logError(e, errMsg, module);
            methodContext.setErrorReturn(errMsg, simpleMethod);
            return false;
        }
        Iterator inModelParamIter = modelService.getInModelParamList().iterator();
        while (inModelParamIter.hasNext()) {
            ModelParam modelParam = (ModelParam) inModelParamIter.next();
            
            if (fromMap.containsKey(modelParam.name)) {
                Object value = fromMap.get(modelParam.name);

                if (UtilValidate.isNotEmpty(modelParam.type)) {
                    try {
                        value = ObjectType.simpleTypeConvert(value, modelParam.type, null, null, false);
                    } catch (GeneralException e) {
                        String errMsg = "Could not convert field value for the parameter/attribute: [" + modelParam.name + "] on the [" + serviceName + "] service to the [" + modelParam.type + "] type for the value [" + value + "]: " + e.toString();
                        Debug.logError(e, errMsg, module);
                        throw new IllegalArgumentException(errMsg);
                    }
                }
                
                toMap.put(modelParam.name, value);
            }
        }
        
        return true;
    }

    public String rawString() {
        // TODO: something more than the empty tag
        return "<set-service-fields/>";
    }
    public String expandedString(MethodContext methodContext) {
        // TODO: something more than a stub/dummy
        return this.rawString();
    }
}

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

import java.util.List;
import java.util.Map;

import javolution.util.FastList;
import javolution.util.FastMap;

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
    public static final class SetServiceFieldsFactory implements Factory<SetServiceFields> {
        public SetServiceFields createMethodOperation(Element element, SimpleMethod simpleMethod) {
            return new SetServiceFields(element, simpleMethod);
        }

        public String getName() {
            return "set-service-fields";
        }
    }

    public static final String module = CallService.class.getName();

    String serviceName;
    ContextAccessor<Map<String, ? extends Object>> mapAcsr;
    ContextAccessor<Map<String, Object>> toMapAcsr;
    ContextAccessor<List<Object>> errorListAcsr;

    public SetServiceFields(Element element, SimpleMethod simpleMethod) {
        super(element, simpleMethod);
        serviceName = element.getAttribute("service-name");
        mapAcsr = new ContextAccessor<Map<String, ? extends Object>>(element.getAttribute("map"), element.getAttribute("map-name"));
        toMapAcsr = new ContextAccessor<Map<String, Object>>(element.getAttribute("to-map"), element.getAttribute("to-map-name"));
        errorListAcsr = new ContextAccessor<List<Object>>(element.getAttribute("error-list-name"), "error_list");
    }

    @Override
    public boolean exec(MethodContext methodContext) {
        List<Object> messages = errorListAcsr.get(methodContext);
        if (messages == null) {
            messages = FastList.newInstance();
            errorListAcsr.put(methodContext, messages);
        }

        String serviceName = methodContext.expandString(this.serviceName);

        Map<String, ? extends Object> fromMap = mapAcsr.get(methodContext);
        if (fromMap == null) {
            Debug.logWarning("The from map in set-service-field was not found with name: " + mapAcsr, module);
            return true;
        }

        Map<String, Object> toMap = toMapAcsr.get(methodContext);
        if (toMap == null) {
            toMap = FastMap.newInstance();
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
        for (ModelParam modelParam: modelService.getInModelParamList()) {
            if (fromMap.containsKey(modelParam.name)) {
                Object value = fromMap.get(modelParam.name);

                if (UtilValidate.isNotEmpty(modelParam.type)) {
                    try {
                        value = ObjectType.simpleTypeConvert(value, modelParam.type, null, methodContext.getTimeZone(), methodContext.getLocale(), true);
                    } catch (GeneralException e) {
                        String errMsg = "Could not convert field value for the parameter/attribute: [" + modelParam.name + "] on the [" + serviceName + "] service to the [" + modelParam.type + "] type for the value [" + value + "]: " + e.toString();
                        Debug.logError(e, errMsg, module);
                        // add the message to the list and set the value to null - tried and failed, just leave it out
                        messages.add(errMsg);
                        value = null;
                    }
                }

                toMap.put(modelParam.name, value);
            }
        }

        return true;
    }

    public String getServiceName() {
        return this.serviceName;
    }

    @Override
    public String rawString() {
        // TODO: something more than the empty tag
        return "<set-service-fields/>";
    }
    @Override
    public String expandedString(MethodContext methodContext) {
        // TODO: something more than a stub/dummy
        return this.rawString();
    }
}

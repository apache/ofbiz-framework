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
package org.apache.ofbiz.service.group;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.ModelService;
import org.apache.ofbiz.service.ServiceDispatcher;
import org.w3c.dom.Element;

/**
 * GroupServiceModel.java
 */
public class GroupServiceModel {

    public static final String module = GroupServiceModel.class.getName();

    private String serviceName, serviceMode;
    private boolean resultToContext = false;
    private boolean optionalParams = false;

    /**
     * Constructor using DOM element
     * @param service DOM element for the service
     */
    public GroupServiceModel(Element service) {
        this.serviceName = service.getAttribute("name");
        this.serviceMode = service.getAttribute("mode");
        this.resultToContext = "true".equalsIgnoreCase(service.getAttribute("result-to-context"));
        this.optionalParams = "optional".equalsIgnoreCase(service.getAttribute("parameters"));
    }

    /**
     * Basic constructor
     * @param serviceName name of the service
     * @param serviceMode service invocation mode (sync|async)
     */
    public GroupServiceModel(String serviceName, String serviceMode) {
        this.serviceName = serviceName;
        this.serviceMode = serviceMode;
    }

    /**
     * Getter for the service mode
     * @return String
     */
    public String getMode() {
        return this.serviceMode;
    }

    /**
     * Getter for the service name
     * @return String
     */
    public String getName() {
        return this.serviceName;
    }

    /**
     * Returns true if the results of this service are to go back into the context
     * @return boolean
     */
    public boolean resultToContext() {
        return this.resultToContext;
    }

    /**
     * Returns true of the parameters for this service are to be included as optional
     * @return boolean
     */
    public boolean isOptional() {
        return this.optionalParams;
    }

    /**
     * Invoker method to invoke this service
     * @param dispatcher ServiceDispatcher used for this invocation
     * @param localName Name of the LocalDispatcher used
     * @param context Context for this service (will use only valid parameters)
     * @return Map result Map
     * @throws GenericServiceException
     */
    public Map<String, Object> invoke(ServiceDispatcher dispatcher, String localName, Map<String, Object> context) throws GenericServiceException {
        DispatchContext dctx = dispatcher.getLocalContext(localName);
        ModelService model = dctx.getModelService(getName());

        Map<String, Object> thisContext = model.makeValid(context, ModelService.IN_PARAM);
        Debug.logInfo("Running grouped service [" + serviceName + "]", module);
        if ("async".equals(getMode())) {
            List<String> requiredOut = model.getParameterNames(ModelService.OUT_PARAM, false);
            if (requiredOut.size() > 0) {
                Debug.logWarning("Grouped service (" + getName() + ") requested 'async' invocation; running sync because of required OUT parameters.", module);
                return dispatcher.runSync(localName, model, thisContext);
            }
            dispatcher.runAsync(localName, model, thisContext, false);
            return new HashMap<>();
        }
        return dispatcher.runSync(localName, model, thisContext);
    }

    /**
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        StringBuilder str = new StringBuilder();
        str.append(getName());
        str.append("::");
        str.append(getMode());
        str.append("::");
        return str.toString();
    }
}

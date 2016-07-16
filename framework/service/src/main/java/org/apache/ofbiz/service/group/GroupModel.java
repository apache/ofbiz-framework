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

import java.util.LinkedList;
import java.util.List;
import java.util.*;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilXml;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.ServiceDispatcher;
import org.apache.ofbiz.service.ServiceUtil;
import org.w3c.dom.Element;

/**
 * GroupModel.java
 */
public class GroupModel {

    public static final String module = GroupModel.class.getName();

    private String groupName, sendMode;
    private List<GroupServiceModel> services;
    private boolean optional = false;
    private int lastServiceRan;

    /**
     * Constructor using DOM Element
     * @param group DOM element for the group
     */
    public GroupModel(Element group) {
        this.sendMode = group.getAttribute("send-mode");
        this.groupName = group.getAttribute("name");
        this.services = new LinkedList<GroupServiceModel>();
        this.lastServiceRan = -1;

        if (groupName == null) {
            throw new IllegalArgumentException("Group Definition found with no name attribute! : " + group);
        }

        for (Element service : UtilXml.childElementList(group, "invoke")) {
            services.add(new GroupServiceModel(service));
        }

        List<? extends Element> oldServiceTags = UtilXml.childElementList(group, "service");
        if (oldServiceTags.size() > 0) {
            for (Element service : oldServiceTags) {
                services.add(new GroupServiceModel(service));
            }
            Debug.logWarning("Service Group Definition : [" + group.getAttribute("name")
                    + "] found with OLD 'service' attribute, change to use 'invoke'", module);
        }

        if (Debug.verboseOn())
            Debug.logVerbose("Created Service Group Model --> " + this, module);
    }

    /**
     * Basic Constructor
     * @param groupName Name of the group
     * @param sendMode Mode used (see DTD)
     * @param services List of GroupServiceModel objects
     */
    public GroupModel(String groupName, String sendMode, List<GroupServiceModel> services) {
        this.lastServiceRan = -1;
        this.groupName = groupName;
        this.sendMode = sendMode;
        this.services = services;
    }

    /**
     * Getter for group name
     * @return String
     */
    public String getGroupName() {
        return this.groupName;
    }

    /**
     * Getter for send mode
     * @return String
     */
    public String getSendMode() {
        return this.sendMode;
    }

    /**
     * Returns a list of services in this group
     * @return List
     */
    public List<GroupServiceModel> getServices() {
        return this.services;
    }

    public boolean isOptional() {
        return optional;
    }

    /**
     * Invokes the group of services in order defined
     * @param dispatcher ServiceDispatcher used for invocation
     * @param localName Name of the LocalDispatcher (namespace)
     * @param context Full parameter context (combined for all services)
     * @return Map Result Map
     * @throws GenericServiceException
     */
    public Map<String, Object> run(ServiceDispatcher dispatcher, String localName, Map<String, Object> context)
            throws GenericServiceException {
        if (this.getSendMode().equals("all")) {
            return runAll(dispatcher, localName, context);
        } else if (this.getSendMode().equals("round-robin")) {
            return runIndex(dispatcher, localName, context, (++lastServiceRan % services.size()));
        } else if (this.getSendMode().equals("random")) {
            int randomIndex = (int) (Math.random() * (services.size()));
            return runIndex(dispatcher, localName, context, randomIndex);
        } else if (this.getSendMode().equals("first-available")) {
            return runOne(dispatcher, localName, context);
        } else if (this.getSendMode().equals("none")) {
            return new HashMap<String, Object>();
        } else {
            throw new GenericServiceException("This mode is not currently supported");
        }
    }

    /**
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        StringBuilder str = new StringBuilder();
        str.append(getGroupName());
        str.append("::");
        str.append(getSendMode());
        str.append("::");
        str.append(getServices());
        return str.toString();
    }

    private Map<String, Object> runAll(ServiceDispatcher dispatcher, String localName, Map<String, Object> context)
            throws GenericServiceException {
        Map<String, Object> runContext = UtilMisc.makeMapWritable(context);
        Map<String, Object> result = new HashMap<String, Object>();
        for (GroupServiceModel model : services) {
            if (Debug.verboseOn())
                Debug.logVerbose("Using Context: " + runContext, module);
            Map<String, Object> thisResult = model.invoke(dispatcher, localName, runContext);
            if (Debug.verboseOn())
                Debug.logVerbose("Result: " + thisResult, module);

            // make sure we didn't fail
            if (ServiceUtil.isError(thisResult)) {
                Debug.logError("Grouped service [" + model.getName() + "] failed.", module);
                return thisResult;
            }

            result.putAll(thisResult);
            if (model.resultToContext()) {
                runContext.putAll(thisResult);
                Debug.logVerbose("Added result(s) to context.", module);
            }
        }
        return result;
    }

    private Map<String, Object> runIndex(ServiceDispatcher dispatcher, String localName, Map<String, Object> context, int index)
            throws GenericServiceException {
        GroupServiceModel model = services.get(index);
        return model.invoke(dispatcher, localName, context);
    }

    private Map<String, Object> runOne(ServiceDispatcher dispatcher, String localName, Map<String, Object> context)
            throws GenericServiceException {
        Map<String, Object> result = null;
        for (GroupServiceModel model : services) {
            try {
                result = model.invoke(dispatcher, localName, context);
            } catch (GenericServiceException e) {
                Debug.logError("Service: " + model + " failed.", module);
            }
        }
        if (result == null) {
            throw new GenericServiceException("All services failed to run; none available.");
        }
        return result;
    }
}

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
package org.ofbiz.service.group;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilXml;
import org.ofbiz.service.GenericServiceException;
import org.ofbiz.service.ServiceDispatcher;
import org.ofbiz.service.ServiceUtil;

import org.w3c.dom.Element;

/**
 * GroupModel.java
 */
public class GroupModel {
    
    public static final String module = GroupModel.class.getName();
    
    private String groupName, sendMode;    
    private List services;
    private boolean optional = false;
    private int lastServiceRan;
    
    /**
     * Constructor using DOM Element
     * @param group DOM element for the group
     */
    public GroupModel(Element group) {
        this.sendMode = group.getAttribute("send-mode");
        this.groupName = group.getAttribute("name");
        this.services = new LinkedList();
        this.lastServiceRan = -1;

        if (groupName == null) {
            throw new IllegalArgumentException("Group Definition found with no name attribute! : " + group);
        }

        List serviceList = UtilXml.childElementList(group, "invoke");
        if (serviceList != null && serviceList.size() > 0) {
            Iterator i = serviceList.iterator();
            while (i.hasNext()) {
                Element service = (Element) i.next();
                services.add(new GroupServiceModel(service));
            }
        }

        List oldServiceTags = UtilXml.childElementList(group, "service");
        if (oldServiceTags != null && oldServiceTags.size() > 0) {
            Iterator i = oldServiceTags.iterator();
            while (i.hasNext()) {
                Element service = (Element) i.next();
                services.add(new GroupServiceModel(service));
            }
            Debug.logWarning("Service Group Definition : [" + group.getAttribute("name") + "] found with OLD 'service' attribute, change to use 'invoke'", module);
        }

        if (Debug.verboseOn()) Debug.logVerbose("Created Service Group Model --> " + this, module);
    }
    
    /**
     * Basic Constructor
     * @param groupName Name of the group
     * @param sendMode Mode used (see DTD)
     * @param services List of GroupServiceModel objects
     */
    public GroupModel(String groupName, String sendMode, List services) {
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
    public List getServices() {
        return this.services;
    }
    
    /**
     * Invokes the group of services in order defined
     * @param dispatcher ServiceDispatcher used for invocation
     * @param localName Name of the LocalDispatcher (namespace)
     * @param context Full parameter context (combined for all services)
     * @return Map Result Map
     * @throws GenericServiceException
     */
    public Map run(ServiceDispatcher dispatcher, String localName, Map context) throws GenericServiceException {
        if (this.getSendMode().equals("all")) {
            return runAll(dispatcher, localName, context);
        } else if (this.getSendMode().equals("round-robin")) {
            return runIndex(dispatcher, localName, context, (++lastServiceRan % services.size()));   
        } else if (this.getSendMode().equals("random")) {
            int randomIndex = (int) (Math.random() * (double) (services.size())); 
            return runIndex(dispatcher, localName, context, randomIndex);
        } else if (this.getSendMode().equals("first-available")) {
            return runOne(dispatcher, localName, context);  
        } else if (this.getSendMode().equals("none")) {
            return new HashMap();                                 
        } else { 
            throw new GenericServiceException("This mode is not currently supported");
        }
    }
    
    /**     
     * @see java.lang.Object#toString()
     */
    public String toString() {
        StringBuffer str = new StringBuffer();
        str.append(getGroupName());
        str.append("::");
        str.append(getSendMode());
        str.append("::");        
        str.append(getServices());
        return str.toString();
    }
    
    private Map runAll(ServiceDispatcher dispatcher, String localName, Map context) throws GenericServiceException {
        Map runContext = new HashMap(context);
        Map result = new HashMap();
        Iterator i = services.iterator();
        while (i.hasNext()) {
            GroupServiceModel model = (GroupServiceModel) i.next();
            if (Debug.verboseOn()) Debug.logVerbose("Using Context: " + runContext, module);
            Map thisResult = model.invoke(dispatcher, localName, runContext);
            if (Debug.verboseOn()) Debug.logVerbose("Result: " + thisResult, module);
            
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
    
    private Map runIndex(ServiceDispatcher dispatcher, String localName, Map context, int index) throws GenericServiceException {
        GroupServiceModel model = (GroupServiceModel) services.get(index);
        return model.invoke(dispatcher, localName, context);
    } 
    
    private Map runOne(ServiceDispatcher dispatcher, String localName, Map context) throws GenericServiceException {      
        Map result = null;        
        Iterator i = services.iterator();
        while (i.hasNext() && result != null) {
            GroupServiceModel model = (GroupServiceModel) i.next();
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

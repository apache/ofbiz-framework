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
package org.ofbiz.order.task;

import java.util.Map;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;

/**
 * Order Processing Task Worker
 */
public class TaskWorker {
    
    public static final String module = TaskWorker.class.getName();
    
    public static String getCustomerName(GenericValue orderTaskList) {
        String lastName = orderTaskList.getString("customerLastName");
        String firstName = orderTaskList.getString("customerFirstName");
        //String groupName = orderTaskList.getString("customerGroupName");
        String groupName = null; // this is only until the entity gets fixed
        if (groupName != null) {
            return groupName;
        } else if (lastName != null) {
            String name = lastName;
            if (firstName != null)
                name = name + ", " + firstName;
            return name;
        } else {
            return "";
        }
    } 
    
    static Map statusMapping = UtilMisc.toMap("WF_NOT_STARTED", "Waiting", "WF_RUNNING", "Active", "WF_COMPLETE", "Complete", "WF_SUSPENDED", "Hold");
    
    public static String getPrettyStatus(GenericValue orderTaskList) {
        String statusId = orderTaskList.getString("currentStatusId");        
        String prettyStatus = (String) statusMapping.get(statusId);
        if (prettyStatus == null)
            prettyStatus = "?";
        return prettyStatus;
    }
        
     
    public static String getRoleDescription(GenericValue orderTaskList) {        
        GenericValue role = null;
        try {
            Map pkFields = UtilMisc.toMap("roleTypeId", orderTaskList.getString("roleTypeId"));
            role = orderTaskList.getDelegator().findByPrimaryKey("RoleType", pkFields);
        } catch (GenericEntityException e) {
            Debug.logError(e, "Cannot get RoleType entity value", module);
            return orderTaskList.getString("roleTypeId");
        }
        return role.getString("description");
    }   
    
}

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
package org.apache.ofbiz.order.task;

import java.util.Map;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;

/**
 * Order Processing Task Worker
 */
public final class TaskWorker {

    private static final String MODULE = TaskWorker.class.getName();
    private static final Map<String, String> STATUS_MAPPING = UtilMisc.toMap("WF_NOT_STARTED", "Waiting", "WF_RUNNING", "Active", "WF_COMPLETE",
            "Complete", "WF_SUSPENDED", "Hold");

    private TaskWorker() { }

    public static String getCustomerName(GenericValue orderTaskList) {
        String lastName = orderTaskList.getString("customerLastName");
        String firstName = orderTaskList.getString("customerFirstName");
        if (lastName != null) {
            String name = lastName;
            if (firstName != null) {
                name = name + ", " + firstName;
            }
            return name;
        } else {
            return "";
        }
    }


    public static String getPrettyStatus(GenericValue orderTaskList) {
        String statusId = orderTaskList.getString("currentStatusId");
        String prettyStatus = STATUS_MAPPING.get(statusId);
        if (prettyStatus == null) {
            prettyStatus = "?";
        }
        return prettyStatus;
    }


    public static String getRoleDescription(GenericValue orderTaskList) {
        GenericValue role = null;
        try {
            Map<String, ? extends Object> pkFields = UtilMisc.toMap("roleTypeId", orderTaskList.getString("roleTypeId"));
            role = orderTaskList.getDelegator().findOne("RoleType", pkFields, false);
        } catch (GenericEntityException e) {
            Debug.logError(e, "Cannot get RoleType entity value", MODULE);
            return orderTaskList.getString("roleTypeId");
        }
        return role.getString("description");
    }

}

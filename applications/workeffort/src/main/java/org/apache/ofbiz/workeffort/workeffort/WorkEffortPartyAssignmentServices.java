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

package org.apache.ofbiz.workeffort.workeffort;

import java.util.Map;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.service.ModelService;

/**
 * WorkEffortPartyAssignmentServices - Services to handle form input and other data changes.
 */
public class WorkEffortPartyAssignmentServices {

    public static final String module = WorkEffortPartyAssignmentServices.class.getName();

    public static void updateWorkflowEngine(GenericValue wepa, GenericValue userLogin, LocalDispatcher dispatcher) {
        // if the WorkEffort is an ACTIVITY, check for accept or complete new status...
        Delegator delegator = wepa.getDelegator();
        GenericValue workEffort = null;

        try {
            workEffort = EntityQuery.use(delegator).from("WorkEffort").where("workEffortId", wepa.get("workEffortId")).queryOne();
        } catch (GenericEntityException e) {
            Debug.logWarning(e, module);
        }
        if (workEffort != null && "ACTIVITY".equals(workEffort.getString("workEffortTypeId"))) {
            // TODO: restrict status transitions

            String statusId = (String) wepa.get("statusId");
            Map<String, Object> context = UtilMisc.toMap("workEffortId", wepa.get("workEffortId"), "partyId", wepa.get("partyId"),
                    "roleTypeId", wepa.get("roleTypeId"), "fromDate", wepa.get("fromDate"),
                    "userLogin", userLogin);

            if ("CAL_ACCEPTED".equals(statusId)) {
                // accept the activity assignment
                try {
                    Map<String, Object> results = dispatcher.runSync("wfAcceptAssignment", context);

                    if (results != null && results.get(ModelService.ERROR_MESSAGE) != null)
                        Debug.logWarning((String) results.get(ModelService.ERROR_MESSAGE), module);
                } catch (GenericServiceException e) {
                    Debug.logWarning(e, module);
                }
            } else if ("CAL_COMPLETED".equals(statusId)) {
                // complete the activity assignment
                try {
                    Map<String, Object> results = dispatcher.runSync("wfCompleteAssignment", context);

                    if (results != null && results.get(ModelService.ERROR_MESSAGE) != null)
                        Debug.logWarning((String) results.get(ModelService.ERROR_MESSAGE), module);
                } catch (GenericServiceException e) {
                    Debug.logWarning(e, module);
                }
            } else if ("CAL_DECLINED".equals(statusId)) {
                // decline the activity assignment
                try {
                    Map<String, Object> results = dispatcher.runSync("wfDeclineAssignment", context);

                    if (results != null && results.get(ModelService.ERROR_MESSAGE) != null)
                        Debug.logWarning((String) results.get(ModelService.ERROR_MESSAGE), module);
                } catch (GenericServiceException e) {
                    Debug.logWarning(e, module);
                }
            } else {// do nothing...
            }
        }
    }
}

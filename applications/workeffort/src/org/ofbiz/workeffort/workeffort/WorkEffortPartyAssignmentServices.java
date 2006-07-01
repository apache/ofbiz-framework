/*
 * $Id: WorkEffortPartyAssignmentServices.java 5462 2005-08-05 18:35:48Z jonesde $
 *
 * Copyright (c) 2002 The Open For Business Project - www.ofbiz.org
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
package org.ofbiz.workeffort.workeffort;

import java.util.Map;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.service.GenericServiceException;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.service.ModelService;

/**
 * WorkEffortPartyAssignmentServices - Services to handle form input and other data changes.
 *
 * @author     <a href="mailto:jonesde@ofbiz.org">David E. Jones</a>
 * @author     <a href="mailto:jaz@ofbiz.org">Andy Zeneski</a>
 * @version    $Rev$
 * @since      2.0
 */
public class WorkEffortPartyAssignmentServices {
    
    public static final String module = WorkEffortPartyAssignmentServices.class.getName();

    public static void updateWorkflowEngine(GenericValue wepa, GenericValue userLogin, LocalDispatcher dispatcher) {
        // if the WorkEffort is an ACTIVITY, check for accept or complete new status...
        GenericDelegator delegator = wepa.getDelegator();
        GenericValue workEffort = null;

        try {
            workEffort = delegator.findByPrimaryKey("WorkEffort", UtilMisc.toMap("workEffortId", wepa.get("workEffortId")));
        } catch (GenericEntityException e) {
            Debug.logWarning(e, module);
        }
        if (workEffort != null && "ACTIVITY".equals(workEffort.getString("workEffortTypeId"))) {
            // TODO: restrict status transitions

            String statusId = (String) wepa.get("statusId");
            Map context = UtilMisc.toMap("workEffortId", wepa.get("workEffortId"), "partyId", wepa.get("partyId"),
                    "roleTypeId", wepa.get("roleTypeId"), "fromDate", wepa.get("fromDate"),
                    "userLogin", userLogin);

            if ("CAL_ACCEPTED".equals(statusId)) {
                // accept the activity assignment
                try {
                    Map results = dispatcher.runSync("wfAcceptAssignment", context);

                    if (results != null && results.get(ModelService.ERROR_MESSAGE) != null)
                        Debug.logWarning((String) results.get(ModelService.ERROR_MESSAGE), module);
                } catch (GenericServiceException e) {
                    Debug.logWarning(e, module);
                }
            } else if ("CAL_COMPLETED".equals(statusId)) {
                // complete the activity assignment
                try {
                    Map results = dispatcher.runSync("wfCompleteAssignment", context);

                    if (results != null && results.get(ModelService.ERROR_MESSAGE) != null)
                        Debug.logWarning((String) results.get(ModelService.ERROR_MESSAGE), module);
                } catch (GenericServiceException e) {
                    Debug.logWarning(e, module);
                }
            } else if ("CAL_DECLINED".equals(statusId)) {
                // decline the activity assignment
                try {
                    Map results = dispatcher.runSync("wfDeclineAssignment", context);

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

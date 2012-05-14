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
package org.ofbiz.common.status;

import java.util.List;

import javax.servlet.jsp.PageContext;

import javolution.util.FastList;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;

/**
 * StatusWorker
 */
public class StatusWorker {

    public static final String module = StatusWorker.class.getName();

    public static void getStatusItems(PageContext pageContext, String attributeName, String statusTypeId) {
        Delegator delegator = (Delegator) pageContext.getRequest().getAttribute("delegator");

        try {
            List<GenericValue> statusItems = delegator.findByAnd("StatusItem", UtilMisc.toMap("statusTypeId", statusTypeId), UtilMisc.toList("sequenceId"), true);

            if (statusItems != null)
                pageContext.setAttribute(attributeName, statusItems);
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
        }
    }

    public static void getStatusItems(PageContext pageContext, String attributeName, String statusTypeIdOne, String statusTypeIdTwo) {
        Delegator delegator = (Delegator) pageContext.getRequest().getAttribute("delegator");
        List<GenericValue> statusItems = FastList.newInstance();

        try {
            List<GenericValue> calItems = delegator.findByAnd("StatusItem", UtilMisc.toMap("statusTypeId", statusTypeIdOne), UtilMisc.toList("sequenceId"), true);

            if (calItems != null)
                statusItems.addAll(calItems);
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
        }
        try {
            List<GenericValue> taskItems = delegator.findByAnd("StatusItem", UtilMisc.toMap("statusTypeId", statusTypeIdTwo), UtilMisc.toList("sequenceId"), true);

            if (taskItems != null)
                statusItems.addAll(taskItems);
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
        }

        if (statusItems.size() > 0)
            pageContext.setAttribute(attributeName, statusItems);
    }

    public static void getStatusValidChangeToDetails(PageContext pageContext, String attributeName, String statusId) {
        Delegator delegator = (Delegator) pageContext.getRequest().getAttribute("delegator");
        List<GenericValue> statusValidChangeToDetails = null;

        try {
            statusValidChangeToDetails = delegator.findByAnd("StatusValidChangeToDetail", UtilMisc.toMap("statusId", statusId), UtilMisc.toList("sequenceId"), true);
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
        }

        if (statusValidChangeToDetails != null)
            pageContext.setAttribute(attributeName, statusValidChangeToDetails);
    }
}

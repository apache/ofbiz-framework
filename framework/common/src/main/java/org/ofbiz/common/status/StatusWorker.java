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

import java.util.LinkedList;
import java.util.List;

import javax.servlet.jsp.PageContext;

import org.ofbiz.base.util.Debug;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.util.EntityQuery;

/**
 * StatusWorker
 */
public final class StatusWorker {

    public static final String module = StatusWorker.class.getName();

    private StatusWorker() {}

    public static void getStatusItems(PageContext pageContext, String attributeName, String statusTypeId) {
        Delegator delegator = (Delegator) pageContext.getRequest().getAttribute("delegator");

        try {
            List<GenericValue> statusItems = EntityQuery.use(delegator)
                                                        .from("StatusItem")
                                                        .where("statusTypeId", statusTypeId)
                                                        .orderBy("sequenceId")
                                                        .cache(true)
                                                        .queryList();
            if (statusItems != null)
                pageContext.setAttribute(attributeName, statusItems);
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
        }
    }

    public static void getStatusItems(PageContext pageContext, String attributeName, String statusTypeIdOne, String statusTypeIdTwo) {
        Delegator delegator = (Delegator) pageContext.getRequest().getAttribute("delegator");
        List<GenericValue> statusItems = new LinkedList<GenericValue>();

        try {
             List<GenericValue> calItems = EntityQuery.use(delegator)
                                                      .from("StatusItem")
                                                      .where("statusTypeId", statusTypeIdOne)
                                                      .orderBy("sequenceId")
                                                      .cache(true)
                                                      .queryList();
            if (calItems != null)
                statusItems.addAll(calItems);
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
        }
        try {
            List<GenericValue> taskItems = EntityQuery.use(delegator)
                                                      .from("StatusItem")
                                                      .where("statusTypeId", statusTypeIdTwo)
                                                      .orderBy("sequenceId")
                                                      .cache(true)
                                                      .queryList();
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
            statusValidChangeToDetails = EntityQuery.use(delegator)
                                                    .from("StatusValidChangeToDetail")
                                                    .where("statusId", statusId)
                                                    .orderBy("sequenceId")
                                                    .cache(true)
                                                    .queryList();
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
        }

        if (statusValidChangeToDetails != null)
            pageContext.setAttribute(attributeName, statusValidChangeToDetails);
    }
}

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

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.ServiceUtil;

/**
 * StatusServices
 */
public class StatusServices {
    
    public static final String module = StatusServices.class.getName();
    
    public static Map getStatusItems(DispatchContext ctx, Map context) {
        GenericDelegator delegator = ctx.getDelegator();
        List statusTypes = (List) context.get("statusTypeIds");
        if (statusTypes == null || statusTypes.size() == 0) {
            return ServiceUtil.returnError("Parameter statusTypeIds can not be null and must contain at least one element");
        }
        
        Iterator i = statusTypes.iterator();
        List statusItems = new LinkedList();
        while (i.hasNext()) {
            String statusTypeId = (String) i.next();
            try {
                Collection myStatusItems = delegator.findByAndCache("StatusItem", UtilMisc.toMap("statusTypeId", statusTypeId), UtilMisc.toList("sequenceId"));
                statusItems.addAll(myStatusItems);
            } catch (GenericEntityException e) {
                Debug.logError(e, module);
            }
        }        
        Map ret = new HashMap();
        ret.put("statusItems",statusItems);
        return ret;
    }

    public static Map getStatusValidChangeToDetails(DispatchContext ctx, Map context) {
        GenericDelegator delegator = ctx.getDelegator();
        List statusValidChangeToDetails = null;
        String statusId = (String) context.get("statusId");
        try {
            statusValidChangeToDetails = delegator.findByAndCache("StatusValidChangeToDetail", UtilMisc.toMap("statusId", statusId), UtilMisc.toList("sequenceId"));
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
        }
        Map ret = ServiceUtil.returnSuccess();
        if (statusValidChangeToDetails != null) {
            ret.put("statusValidChangeToDetails", statusValidChangeToDetails);
        }
        return ret;        
    }
}

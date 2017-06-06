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
package org.apache.ofbiz.content.content;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilHttp;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilProperties;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.transaction.GenericTransactionException;
import org.apache.ofbiz.entity.transaction.TransactionUtil;
import org.apache.ofbiz.entity.util.EntityListIterator;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.security.Security;


/**
 * ContentEvents Class
 */
public class ContentEvents {

    public static final String module = ContentEvents.class.getName();
    public static final String resource = "ContentErrorUiLabels";

    /**
     * Updates/adds keywords for all contents
     *
     * @param request HTTPRequest object for the current request
     * @param response HTTPResponse object for the current request
     * @return String specifying the exit status of this event
     */
    public static String updateAllContentKeywords(HttpServletRequest request, HttpServletResponse response) {
        Delegator delegator = (Delegator) request.getAttribute("delegator");
        Security security = (Security) request.getAttribute("security");

        String updateMode = "CREATE";
        String errMsg=null;

        String doAll = request.getParameter("doAll");

        // check permissions before moving on...
        if (!security.hasEntityPermission("CONTENTMGR", "_" + updateMode, request.getSession())) {
            Map<String, String> messageMap = UtilMisc.toMap("updateMode", updateMode);
            errMsg = UtilProperties.getMessage(resource,"contentevents.not_sufficient_permissions", messageMap, UtilHttp.getLocale(request));
            request.setAttribute("_ERROR_MESSAGE_", errMsg);
            return "error";
        }

        int numConts = 0;
        int errConts = 0;

        boolean beganTx = false;
        EntityQuery contentQuery = EntityQuery.use(delegator).from("Content");
        
        try {
            // begin the transaction
            beganTx = TransactionUtil.begin(7200);
            if (Debug.infoOn()) {
                long count = contentQuery.queryCount();
                Debug.logInfo("========== Found " + count + " contents to index ==========", module);
            }
            GenericValue content;
            try (EntityListIterator entityListIterator = contentQuery.queryIterator()) {
                while ((content = entityListIterator.next()) != null) {
                    ContentKeywordIndex.indexKeywords(content, "Y".equals(doAll));
                    numConts++;
                    if (numConts % 500 == 0) {
                        Debug.logInfo("Keywords indexed for " + numConts + " so far", module);
                    }
                }
            } catch (GenericEntityException e) {
                errMsg = "[ContentEvents.updateAllContentKeywords] Could not create content-keyword (write error); message: " + e.getMessage();
                Debug.logWarning(errMsg, module);
                errConts++;
                request.setAttribute("_ERROR_MESSAGE_", errMsg);
            }
        } catch (GenericEntityException gee) {
            Debug.logWarning(gee, gee.getMessage(), module);
            Map<String, String> messageMap = UtilMisc.toMap("gee", gee.toString());
            errMsg = UtilProperties.getMessage(resource,"contentevents.error_getting_content_list", messageMap, UtilHttp.getLocale(request));
            request.setAttribute("_ERROR_MESSAGE_", errMsg);
            try {
                TransactionUtil.rollback(beganTx, gee.getMessage(), gee);
            } catch (GenericTransactionException e1) {
                Debug.logError(e1, module);
            }
            return "error";

        }
        // commit the transaction
        try {
            TransactionUtil.commit(beganTx);
        } catch (GenericTransactionException e) {
            Debug.logError(e, module);
        }

        if (errConts == 0) {
            Map<String, String> messageMap = UtilMisc.toMap("numConts", Integer.toString(numConts));
            errMsg = UtilProperties.getMessage(resource,"contentevents.keyword_creation_complete_for_contents", messageMap, UtilHttp.getLocale(request));
            request.setAttribute("_EVENT_MESSAGE_", errMsg);
            return "success";
        } else {
            Map<String, String> messageMap = UtilMisc.toMap("numConts", Integer.toString(numConts));
            messageMap.put("errConts", Integer.toString(errConts));
            errMsg = UtilProperties.getMessage(resource,"contentevents.keyword_creation_complete_for_contents_with_errors", messageMap, UtilHttp.getLocale(request));
            request.setAttribute("_ERROR_MESSAGE_", errMsg);
            return "error";
        }
    }
}

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
package org.ofbiz.content.content;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilHttp;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilProperties;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.transaction.TransactionUtil;
import org.ofbiz.entity.util.EntityListIterator;
import org.ofbiz.content.content.ContentKeywordIndex;
import org.ofbiz.security.Security;


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
        //String errMsg = "";
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

        EntityListIterator entityListIterator = null;
        int numConts = 0;
        int errConts = 0;

        boolean beganTx = false;
        try {
            // begin the transaction
            beganTx = TransactionUtil.begin(7200);
            try {
                if (Debug.infoOn()) {
                    long count = delegator.findCountByCondition("Content", null, null, null);
                    Debug.logInfo("========== Found " + count + " contents to index ==========", module);
                }
                entityListIterator = delegator.find("Content", null, null, null, null, null);
            } catch (GenericEntityException gee) {
                Debug.logWarning(gee, gee.getMessage(), module);
                Map<String, String> messageMap = UtilMisc.toMap("gee", gee.toString());
                errMsg = UtilProperties.getMessage(resource,"contentevents.error_getting_content_list", messageMap, UtilHttp.getLocale(request));
                request.setAttribute("_ERROR_MESSAGE_", errMsg);
                throw gee;
            }

            GenericValue content;
            while ((content = entityListIterator.next()) != null) {
                try {
                    ContentKeywordIndex.indexKeywords(content, "Y".equals(doAll));
                } catch (GenericEntityException e) {
                    //request.setAttribute("_ERROR_MESSAGE_", errMsg);
                    Debug.logWarning("[ContentEvents.updateAllContentKeywords] Could not create content-keyword (write error); message: " + e.getMessage(), module);
                    errConts++;
                }
                numConts++;
                if (numConts % 500 == 0) {
                    Debug.logInfo("Keywords indexed for " + numConts + " so far", module);
                }
            }
        } catch (GenericEntityException e) {
            try {
                TransactionUtil.rollback(beganTx, e.getMessage(), e);
            } catch (Exception e1) {
                Debug.logError(e1, module);
            }
            return "error";
        } catch (Throwable t) {
            Debug.logError(t, module);
            request.setAttribute("_ERROR_MESSAGE_", t.getMessage());
            try {
                TransactionUtil.rollback(beganTx, t.getMessage(), t);
            } catch (Exception e2) {
                Debug.logError(e2, module);
            }
            return "error";
        } finally {
            if (entityListIterator != null) {
                try {
                    entityListIterator.close();
                } catch (GenericEntityException gee) {
                    Debug.logError(gee, "Error closing EntityListIterator when indexing content keywords.", module);
                }
            }

            // commit the transaction
            try {
                TransactionUtil.commit(beganTx);
            } catch (Exception e) {
                Debug.logError(e, module);
            }
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

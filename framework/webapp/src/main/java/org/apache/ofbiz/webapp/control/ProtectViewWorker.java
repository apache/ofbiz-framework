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
package org.apache.ofbiz.webapp.control;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilHttp;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilProperties;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.util.EntityQuery;

/**
 * Common Workers
 */
public final class ProtectViewWorker {

    private static final String MODULE = ProtectViewWorker.class.getName();
    private static final String RESOURCE_WEBAPP = "WebappUiLabels";
    private static final Map<String, Long> HITS_BY_VIEW_ACCESSED = new ConcurrentHashMap<>();
    private static final Map<String, Long> DURATION_BY_VIEW_ACCESSED = new ConcurrentHashMap<>();
    private static final Long ONE = 1L;

    private ProtectViewWorker() { }

    /**
     * An HTTP WebEvent handler that checks to see if an userLogin should be tarpitted
     * The decision is made in regard of number of hits in last period of time
     * @param request The HTTP request object for the current JSP or Servlet request.
     * @param response The HTTP response object for the current JSP or Servlet request.
     * @return String
     */
    public static String checkProtectedView(HttpServletRequest request, HttpServletResponse response) {
        HttpSession session = request.getSession();
        String viewNameId = RequestHandler.getRequestUri(request.getPathInfo());
        GenericValue userLogin = (GenericValue) session.getAttribute("userLogin");
        Delegator delegator = (Delegator) request.getAttribute("delegator");
        String returnValue = "success";

        if (userLogin != null) {
            String userLoginId = userLogin.getString("userLoginId");
            try {
                List<GenericValue> protectedViews = EntityQuery.use(delegator)
                                                               .from("UserLoginAndProtectedView")
                                                               .where("userLoginId", userLoginId, "viewNameId", viewNameId)
                                                               .cache(true)
                                                               .queryList();
                // Any views to deal with ?
                if (UtilValidate.isNotEmpty(protectedViews)) {
                    Long now = System.currentTimeMillis(); // we are not in a margin of some milliseconds

                    // Is this login/view couple already tarpitted ? (ie denied access to view for login for a period of time)
                    List<GenericValue> tarpittedLoginViews = EntityQuery.use(delegator)
                                                                        .from("TarpittedLoginView")
                                                                        .where("userLoginId", userLoginId, "viewNameId", viewNameId)
                                                                        .cache(true)
                                                                        .queryList();
                    String viewNameUserLoginId = viewNameId + userLoginId;
                    if (UtilValidate.isNotEmpty(tarpittedLoginViews)) {
                        GenericValue tarpittedLoginView = tarpittedLoginViews.get(0);
                        Long tarpitReleaseDateTime = (Long) tarpittedLoginView.get("tarpitReleaseDateTime");
                        if (now < tarpitReleaseDateTime) {
                            String tarpittedMessage = UtilProperties.getMessage(RESOURCE_WEBAPP, "protectedviewevents.tarpitted_message",
                                    UtilHttp.getLocale(request));
                            // reset since now protected by the tarpit duration
                            HITS_BY_VIEW_ACCESSED.put(viewNameUserLoginId, 0L);
                            return ":_protect_:" + tarpittedMessage;
                        }
                    }
                    GenericValue protectedView = protectedViews.get(0);
                    // 1st hit ?
                    Long curMaxHits = HITS_BY_VIEW_ACCESSED.get(viewNameUserLoginId);
                    if (UtilValidate.isEmpty(curMaxHits)) {
                        HITS_BY_VIEW_ACCESSED.put(viewNameUserLoginId, ONE);
                        Long maxHitsDuration = (Long) protectedView.get("maxHitsDuration") * 1000;
                        DURATION_BY_VIEW_ACCESSED.put(viewNameUserLoginId, now + maxHitsDuration);
                    } else {
                        Long maxDuration = DURATION_BY_VIEW_ACCESSED.get(viewNameUserLoginId);
                        Long newMaxHits = curMaxHits + ONE;
                        HITS_BY_VIEW_ACCESSED.put(viewNameUserLoginId, newMaxHits);
                        // Are we in a period of time where we need to check if there was too much hits ?
                        if (now < maxDuration) {
                            // Check if over the max hit count...
                            if (newMaxHits > protectedView.getLong("maxHits")) { // yes : block and set tarpitReleaseDateTime
                                String blockedMessage = UtilProperties.getMessage(RESOURCE_WEBAPP, "protectedviewevents.blocked_message",
                                        UtilHttp.getLocale(request));
                                returnValue = ":_protect_:" + blockedMessage;

                                Long tarpitDuration = (Long) protectedView.get("tarpitDuration") * 1000;

                                GenericValue tarpittedLoginView = delegator.makeValue("TarpittedLoginView");
                                tarpittedLoginView.set("userLoginId", userLoginId);
                                tarpittedLoginView.set("viewNameId", viewNameId);
                                tarpittedLoginView.set("tarpitReleaseDateTime", now + tarpitDuration);

                                try {
                                    delegator.createOrStore(tarpittedLoginView);
                                } catch (GenericEntityException e) {
                                    Debug.logError(e, "Could not save TarpittedLoginView:", MODULE);
                                }
                            }
                        } else {
                            // The tarpit period is over, begin a new one.
                            // Actually it's not a discrete process but we do as it was...
                            // We don't need precision here, a theft will be caught anyway !
                            // We could also take an average of hits in the last x periods of time as initial value,
                            // but it does not make any more sense.
                            // Of course for this to work well the tarpitting period must be long enough...
                            HITS_BY_VIEW_ACCESSED.put(viewNameUserLoginId, ONE);
                            Long maxHitsDuration = (Long) protectedView.get("maxHitsDuration") * 1000;
                            DURATION_BY_VIEW_ACCESSED.put(viewNameUserLoginId, now + maxHitsDuration);
                        }
                    }
                }
            } catch (GenericEntityException e) {
                Map<String, String> messageMap = UtilMisc.toMap("errMessage", e.getMessage());
                String errMsg = UtilProperties.getMessage("CommonUiLabels", "CommonDatabaseProblem", messageMap, UtilHttp.getLocale(request));
                Debug.logError(e, errMsg, MODULE);
            }
        }

        return returnValue;
    }
}

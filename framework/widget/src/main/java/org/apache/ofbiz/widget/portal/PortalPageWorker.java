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
package org.apache.ofbiz.widget.portal;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.GeneralException;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.condition.EntityCondition;
import org.apache.ofbiz.entity.condition.EntityOperator;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.entity.util.EntityUtil;
import org.apache.ofbiz.security.Security;
import org.apache.ofbiz.widget.WidgetWorker;

/**
 * PortalPageWorker Class
 */
public class PortalPageWorker {

    private static final String MODULE = PortalPageWorker.class.getName();

    public PortalPageWorker() { }

    /**
     * Render portal page as text ext string.
     * @param delegator the delegator
     * @param portalPageId the portal page id
     * @param templateContext the template context
     * @param cache the cache
     * @return the string
     * @throws GeneralException the general exception
     * @throws IOException the io exception
     */
    public String renderPortalPageAsTextExt(Delegator delegator, String portalPageId, Map<String, Object> templateContext,
            boolean cache) throws GeneralException, IOException {
        return "success";
    }

    /**
    * Returns a list of PortalPages that have the specified parentPortalPageId as parent.
    * If a specific PortalPage exists for the current userLogin it is returned instead of the original one.
    */
    public static List<GenericValue> getPortalPages(String parentPortalPageId, Map<String, Object> context) {
        List<GenericValue> portalPages = null;
        if (UtilValidate.isNotEmpty(parentPortalPageId)) {
            Delegator delegator = WidgetWorker.getDelegator(context);
            try {
                // first get public pages
                EntityCondition cond =
                        EntityCondition.makeCondition(UtilMisc.toList(
                        EntityCondition.makeCondition("ownerUserLoginId", EntityOperator.EQUALS, "_NA_"),
                        EntityCondition.makeCondition(UtilMisc.toList(
                                EntityCondition.makeCondition("portalPageId", EntityOperator.EQUALS, parentPortalPageId),
                                EntityCondition.makeCondition("parentPortalPageId", EntityOperator.EQUALS, parentPortalPageId)),
                                EntityOperator.OR)),
                        EntityOperator.AND);
                portalPages = EntityQuery.use(delegator).from("PortalPage").where(cond).queryList();
                List<GenericValue> userPortalPages = new ArrayList<>();
                if (UtilValidate.isNotEmpty(context.get("userLogin"))) { // check if a user is logged in
                    String userLoginId = ((GenericValue) context.get("userLogin")).getString("userLoginId");
                    // replace with private pages
                    for (GenericValue portalPage : portalPages) {
                        List<GenericValue> privatePortalPages = EntityQuery.use(delegator).from("PortalPage")
                                .where("ownerUserLoginId", userLoginId, "originalPortalPageId", portalPage.getString("portalPageId"))
                                .queryList();
                        if (UtilValidate.isNotEmpty(privatePortalPages)) {
                            userPortalPages.add(privatePortalPages.get(0));
                        } else {
                            userPortalPages.add(portalPage);
                        }
                    }
                    // add any other created private pages
                    userPortalPages.addAll(EntityQuery.use(delegator).from("PortalPage")
                            .where("ownerUserLoginId", userLoginId, "originalPortalPageId", null, "parentPortalPageId", parentPortalPageId)
                            .queryList());
                }
                portalPages = EntityUtil.orderBy(userPortalPages, UtilMisc.toList("sequenceNum"));
            } catch (GenericEntityException e) {
                Debug.logError("Could not retrieve portalpages:" + e.getMessage(), MODULE);
            }
        }
        return portalPages;
    }

    /**
    * Returns the PortalPage with the specified portalPageId.
    * If a specific PortalPage exists for the current userLogin it is returned instead of the original one.
    */
    public static GenericValue getPortalPage(String portalPageId, Map<String, Object> context) {
        GenericValue portalPage = null;
        if (UtilValidate.isNotEmpty(portalPageId)) {
            Delegator delegator = WidgetWorker.getDelegator(context);
            try {
                // Get the current userLoginId
                String userLoginId = "_NA_";
                if (UtilValidate.isNotEmpty(context.get("userLogin"))) { // check if a user is logged in
                    userLoginId = ((GenericValue) context.get("userLogin")).getString("userLoginId");
                }

                // Get the PortalPage ensuring that it is either owned by the user or a system page
                EntityCondition cond = EntityCondition.makeCondition(UtilMisc.toList(
                        EntityCondition.makeCondition("portalPageId", EntityOperator.EQUALS, portalPageId),
                        EntityCondition.makeCondition(UtilMisc.toList(
                                EntityCondition.makeCondition("ownerUserLoginId", EntityOperator.EQUALS, "_NA_"),
                                EntityCondition.makeCondition("ownerUserLoginId", EntityOperator.EQUALS, userLoginId)),
                                EntityOperator.OR)),
                        EntityOperator.AND);
                List<GenericValue> portalPages = EntityQuery.use(delegator).from("PortalPage").where(cond).queryList();
                if (UtilValidate.isNotEmpty(portalPages)) {
                    portalPage = EntityUtil.getFirst(portalPages);
                }
                // If a derived PortalPage private to the user exists, returns this instead of the system one
                cond = EntityCondition.makeCondition(UtilMisc.toList(
                        EntityCondition.makeCondition("originalPortalPageId", EntityOperator.EQUALS, portalPageId),
                        EntityCondition.makeCondition("ownerUserLoginId", EntityOperator.EQUALS, userLoginId)),
                        EntityOperator.AND);
                List<GenericValue> privateDerivedPortalPages = EntityQuery.use(delegator).from("PortalPage").where(cond).queryList();
                if (UtilValidate.isNotEmpty(privateDerivedPortalPages)) {
                    portalPage = EntityUtil.getFirst(privateDerivedPortalPages);
                }
            } catch (GenericEntityException e) {
                Debug.logError("Could not retrieve portalpage:" + e.getMessage(), MODULE);
            }
        }
        return portalPage;
    }

    /**
    * Checks if the user is allowed to configure the PortalPage.
    * PortalPage configuration is allowed if he is the PortalPage owner or he has got the PORTALPAGE_ADMIN permission
    */
    public static Boolean userIsAllowedToConfigure(String portalPageId, Map<String, Object> context) {
        Boolean userIsAllowed = false;

        if (UtilValidate.isNotEmpty(portalPageId)) {
            GenericValue userLogin = (GenericValue) context.get("userLogin");
            if (userLogin != null) {
                String userLoginId = (String) userLogin.get("userLoginId");
                Security security = (Security) context.get("security");

                Boolean hasPortalAdminPermission = security.hasPermission("PORTALPAGE_ADMIN", userLogin);
                try {
                    Delegator delegator = WidgetWorker.getDelegator(context);
                    GenericValue portalPage = EntityQuery.use(delegator).from("PortalPage").where("portalPageId", portalPageId).queryOne();
                    if (portalPage != null) {
                        String ownerUserLoginId = (String) portalPage.get("ownerUserLoginId");
                        // Users with PORTALPAGE_ADMIN permission can configure every Portal Page
                        userIsAllowed = (ownerUserLoginId.equals(userLoginId) || hasPortalAdminPermission);
                    }
                } catch (GenericEntityException e) {
                    return false;
                }
            }
        }

        return userIsAllowed;
    }
}

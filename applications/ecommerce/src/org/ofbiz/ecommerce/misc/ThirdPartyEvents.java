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
package org.ofbiz.ecommerce.misc;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilDateTime;
import org.ofbiz.base.util.UtilHttp;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilProperties;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.util.EntityUtil;


public class ThirdPartyEvents {
    
    public static final String module = ThirdPartyEvents.class.getName();

    public final static String DISTRIBUTOR_ID = "_DISTRIBUTOR_ID_";
    public final static String AFFILIATE_ID = "_AFFILIATE_ID_";

    /** Save the association id(s) specified in the request object into the session.
     *@param request The HTTPRequest object for the current request
     *@param response The HTTPResponse object for the current request
     *@return String specifying the exit status of this event
     */
    public static String setAssociationId(HttpServletRequest request, HttpServletResponse response) {
        Map requestParams = UtilHttp.getParameterMap(request);

        // check distributor
        String distriParam[] = { "distributor_id", "distributorid", "distributor" };
        String distributorId = null;

        for (int i = 0; i < distriParam.length; i++) {
            String param = distriParam[i];

            if (requestParams.containsKey(param)) {
                distributorId = (String) requestParams.get(param);
                break;
            } else if (requestParams.containsKey(param.toUpperCase())) {
                distributorId = (String) requestParams.get(param.toUpperCase());
                break;
            }
        }

        // check affiliate
        String affiliParam[] = { "affiliate_id", "affiliateid", "affiliate", "affil" };
        String affiliateId = null;

        for (int i = 0; i < affiliParam.length; i++) {
            String param = affiliParam[i];

            if (requestParams.containsKey(param)) {
                affiliateId = (String) requestParams.get(param);
                break;
            } else if (requestParams.containsKey(param.toUpperCase())) {
                affiliateId = (String) requestParams.get(param.toUpperCase());
                break;
            }
        }

        if (UtilValidate.isNotEmpty(distributorId)) {
            request.getSession().setAttribute(DISTRIBUTOR_ID, distributorId);
            updateAssociatedDistributor(request, response);
        }
        if (UtilValidate.isNotEmpty(affiliateId)) {
            request.getSession().setAttribute(AFFILIATE_ID, affiliateId);
            updateAssociatedAffiliate(request, response);
        }

        return "success";
    }

    /** Update the distributor association for the logged in user, if possible.
     *@param request The HTTPRequest object for the current request
     *@param response The HTTPResponse object for the current request
     *@return String specifying the exit status of this event
     */
    public static String updateAssociatedDistributor(HttpServletRequest request, HttpServletResponse response) {
        GenericDelegator delegator = (GenericDelegator) request.getAttribute("delegator");
        GenericValue userLogin = (GenericValue) request.getSession().getAttribute("userLogin");
        GenericValue party = null;

        java.net.URL ecommercePropertiesUrl = null;

        try {
            ecommercePropertiesUrl = ((ServletContext) request.getAttribute("servletContext")).getResource("/WEB-INF/ecommerce.properties");
        } catch (java.net.MalformedURLException e) {
            Debug.logWarning(e, module);
        }

        String store = UtilProperties.getPropertyValue(ecommercePropertiesUrl, "distributor.store.customer");

        if (store == null || store.toUpperCase().startsWith("N")) {
            return "success";
        }

        String storeOnClick = UtilProperties.getPropertyValue(ecommercePropertiesUrl, "distributor.store.onclick");

        if (storeOnClick == null || storeOnClick.toUpperCase().startsWith("N")) {
            return "success";
        }

        try {
            party = userLogin == null ? null : userLogin.getRelatedOne("Party");
        } catch (GenericEntityException gee) {
            Debug.logWarning(gee, module);
        }

        if (party != null) {
            // if a distributorId is already associated, it will be used instead
            String currentDistributorId = getId(party, "DISTRIBUTOR");

            if (UtilValidate.isEmpty(currentDistributorId)) {
                String distributorId = (String) request.getSession().getAttribute(DISTRIBUTOR_ID);

                if (UtilValidate.isNotEmpty(distributorId)) {
                    List toBeStored = new LinkedList();

                    // create distributor Party ?? why?
                    // create distributor PartyRole ?? why?
                    // create PartyRelationship
                    GenericValue partyRelationship = delegator.makeValue("PartyRelationship", UtilMisc.toMap("partyIdFrom", party.getString("partyId"), "partyIdTo", distributorId, "roleTypeIdFrom", "CUSTOMER", "roleTypeIdTo", "DISTRIBUTOR"));

                    partyRelationship.set("fromDate", UtilDateTime.nowTimestamp());
                    partyRelationship.set("partyRelationshipTypeId", "DISTRIBUTION_CHANNEL");
                    toBeStored.add(partyRelationship);

                    toBeStored.add(delegator.makeValue("Party", UtilMisc.toMap("partyId", distributorId)));
                    toBeStored.add(delegator.makeValue("PartyRole", UtilMisc.toMap("partyId", distributorId, "roleTypeId", "DISTRIBUTOR")));
                    try {
                        delegator.storeAll(toBeStored);
                        if (Debug.infoOn()) Debug.logInfo("Distributor for user " + party.getString("partyId") + " set to " + distributorId, module);
                    } catch (GenericEntityException gee) {
                        Debug.logWarning(gee, module);
                    }
                } else {
                    // no distributorId is available
                    Debug.logInfo("No distributor in session or already associated with user " + userLogin.getString("partyId"), module);
                    return "success";
                }
            } else {
                request.getSession().setAttribute(DISTRIBUTOR_ID, currentDistributorId);
            }

            return "success";
        } else {
            // not logged in
            Debug.logWarning("Cannot associate distributor since not logged in yet", module);
            return "success";
        }
    }

    /** Update the affiliate association for the logged in user, if possible.
     *@param request The HTTPRequest object for the current request
     *@param response The HTTPResponse object for the current request
     *@return String specifying the exit status of this event
     */
    public static String updateAssociatedAffiliate(HttpServletRequest request, HttpServletResponse response) {
        GenericDelegator delegator = (GenericDelegator) request.getAttribute("delegator");
        GenericValue userLogin = (GenericValue) request.getSession().getAttribute("userLogin");
        GenericValue party = null;

        java.net.URL ecommercePropertiesUrl = null;

        try {
            ecommercePropertiesUrl = ((ServletContext) request.getAttribute("servletContext")).getResource("/WEB-INF/ecommerce.properties");
        } catch (java.net.MalformedURLException e) {
            Debug.logWarning(e, module);
        }

        String store = UtilProperties.getPropertyValue(ecommercePropertiesUrl, "affiliate.store.customer");

        if (store == null || store.toUpperCase().startsWith("N"))
            return "success";
        String storeOnClick = UtilProperties.getPropertyValue(ecommercePropertiesUrl, "affiliate.store.onclick");

        if (storeOnClick == null || storeOnClick.toUpperCase().startsWith("N"))
            return "success";

        try {
            party = userLogin == null ? null : userLogin.getRelatedOne("Party");
        } catch (GenericEntityException gee) {
            Debug.logWarning(gee, module);
        }

        if (party != null) {
            // if a distributorId is already associated, it will be used instead
            String currentAffiliateId = getId(party, "AFFILIATE");

            if (UtilValidate.isEmpty(currentAffiliateId)) {
                String affiliateId = (String) request.getSession().getAttribute(AFFILIATE_ID);

                if (UtilValidate.isNotEmpty(affiliateId)) {
                    // create PartyRelationship
                    GenericValue partyRelationship = delegator.makeValue("PartyRelationship", UtilMisc.toMap("partyIdFrom", party.getString("partyId"), "partyIdTo", affiliateId, "roleTypeIdFrom", "CUSTOMER", "roleTypeIdTo", "AFFILIATE"));

                    partyRelationship.set("fromDate", UtilDateTime.nowTimestamp());
                    partyRelationship.set("partyRelationshipTypeId", "SALES_AFFILIATE");
                    try {
                        delegator.create(partyRelationship);
                        if (Debug.infoOn()) Debug.logInfo("Affiliate for user " + party.getString("partyId") + " set to " + affiliateId, module);
                    } catch (GenericEntityException gee) {
                        Debug.logWarning(gee, module);
                    }
                } else {
                    // no distributorId is available
                    Debug.logInfo("No affiliate in session or already associated with user " + userLogin.getString("partyId"), module);
                    return "success";
                }
            } else {
                request.getSession().setAttribute(AFFILIATE_ID, currentAffiliateId);
            }

            return "success";
        } else {
            // not logged in
            Debug.logWarning("Cannot associate affiliate since not logged in yet", module);
            return "success";
        }
    }

    private static GenericValue getPartyRelationship(GenericValue party, String roleTypeTo) {
        try {
            return EntityUtil.getFirst(EntityUtil.filterByDate(party.getRelatedByAnd("FromPartyRelationship", UtilMisc.toMap("roleTypeIdTo", roleTypeTo)), true));
        } catch (GenericEntityException gee) {
            Debug.logWarning(gee, module);
        }
        return null;
    }

    private static String getId(GenericValue party, String roleTypeTo) {
        GenericValue partyRelationship = getPartyRelationship(party, roleTypeTo);

        return partyRelationship == null ? null : partyRelationship.getString("partyIdTo");
    }

}

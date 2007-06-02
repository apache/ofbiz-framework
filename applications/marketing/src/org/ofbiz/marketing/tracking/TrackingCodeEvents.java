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
package org.ofbiz.marketing.tracking;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilDateTime;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilProperties;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.webapp.stats.VisitHandler;
import org.ofbiz.webapp.website.WebSiteWorker;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.product.category.CategoryWorker;

/**
 * Events used for maintaining TrackingCode related information
 */
public class TrackingCodeEvents {

    public static final String module = TrackingCodeEvents.class.getName();

    /** If TrackingCode monitoring is desired this event should be added to the list 
     * of events that run on every request. This event looks for the parameter 
     * <code>autoTrackingCode</code> or a shortened version: <code>atc</code>.
     */
    public static String checkTrackingCodeUrlParam(HttpServletRequest request, HttpServletResponse response) {
        String trackingCodeId = request.getParameter("autoTrackingCode");
        if (UtilValidate.isEmpty(trackingCodeId)) trackingCodeId = request.getParameter("atc");
        
        if (UtilValidate.isNotEmpty(trackingCodeId)) {
            //tracking code is specified on the request, get the TrackingCode value and handle accordingly
            GenericDelegator delegator = (GenericDelegator) request.getAttribute("delegator");
            GenericValue trackingCode;
            try {
                trackingCode = delegator.findByPrimaryKeyCache("TrackingCode", UtilMisc.toMap("trackingCodeId", trackingCodeId));
            } catch (GenericEntityException e) {
                Debug.logError(e, "Error looking up TrackingCode with trackingCodeId [" + trackingCodeId + "], ignoring this trackingCodeId", module);
                return "error";
            }
            
            if (trackingCode == null) {
                Debug.logError("TrackingCode not found for trackingCodeId [" + trackingCodeId + "], ignoring this trackingCodeId.", module);
                //this return value will be ignored, but we'll designate this as an error anyway
                return "error";
            }

            return processTrackingCode(trackingCode, request, response);
        } else {
            return "success";
        }
    }

    /** If TrackingCode monitoring is desired this event should be added to the list 
     * of events that run on every request. This event looks for the parameter 
     * <code>ptc</code> and handles the value as a Partner Managed Tracking Code.
     * 
     * If the specified trackingCodeId exists then it is used as is, otherwise a new one
     * is created with the ptc value as the trackingCodeId. The values for the fields of 
     * the new TrackingCode can come from one of two places: if a <code>dtc</code> parameter
     * is included the value will be used to lookup a TrackingCode with default values,
     * otherwise the default trackingCodeId will be looked up in the <code>partner.trackingCodeId.default</code>
     * in the <code>general.properties</code> file. If that is still not found just use an empty TrackingCode.
     */
    public static String checkPartnerTrackingCodeUrlParam(HttpServletRequest request, HttpServletResponse response) {
        String trackingCodeId = request.getParameter("ptc");
        
        if (UtilValidate.isNotEmpty(trackingCodeId)) {
            //partner managed tracking code is specified on the request
            GenericDelegator delegator = (GenericDelegator) request.getAttribute("delegator");
            GenericValue trackingCode;
            try {
                trackingCode = delegator.findByPrimaryKeyCache("TrackingCode", UtilMisc.toMap("trackingCodeId", trackingCodeId));
            } catch (GenericEntityException e) {
                Debug.logError(e, "Error looking up TrackingCode with trackingCodeId [" + trackingCodeId + "], ignoring this trackingCodeId", module);
                return "error";
            }
            
            if (trackingCode == null) {
                //create new TrackingCode with default values from a "dtc" parameter or from a properties file
                
                String dtc = request.getParameter("dtc");
                if (UtilValidate.isEmpty(dtc)) {
                    dtc = UtilProperties.getPropertyValue("general", "partner.trackingCodeId.default");
                }
                if (UtilValidate.isNotEmpty(dtc)) {
                    GenericValue defaultTrackingCode = null;
                    try {
                        defaultTrackingCode = delegator.findByPrimaryKeyCache("TrackingCode", UtilMisc.toMap("trackingCodeId", dtc));
                    } catch (GenericEntityException e) {
                        Debug.logError(e, "Error looking up Default values TrackingCode with trackingCodeId [" + dtc + "], not using the dtc value for new TrackingCode defaults", module);
                    }
                    
                    if (defaultTrackingCode != null) {
                        defaultTrackingCode.set("trackingCodeId", trackingCodeId);
                        defaultTrackingCode.set("trackingCodeTypeId", "PARTNER_MGD"); 
                        //null out userLogin fields, no use tracking to customer, or is there?; set dates to current
                        defaultTrackingCode.set("createdDate", UtilDateTime.nowTimestamp());
                        defaultTrackingCode.set("createdByUserLogin", null);
                        defaultTrackingCode.set("lastModifiedDate", UtilDateTime.nowTimestamp());
                        defaultTrackingCode.set("lastModifiedByUserLogin", null);
                        
                        trackingCode = defaultTrackingCode;
                        try {
                            trackingCode.create();
                        } catch (GenericEntityException e) {
                            Debug.logError(e, "Error creating new Partner TrackingCode with trackingCodeId [" + trackingCodeId + "], ignoring this trackingCodeId", module);
                            return "error";
                        }
                    }
                }
                
                //if trackingCode is still null then the defaultTrackingCode thing didn't work out, use empty TrackingCode
                if (trackingCode == null) {
                    trackingCode = delegator.makeValue("TrackingCode", null);
                    trackingCode.set("trackingCodeId", trackingCodeId);
                    trackingCode.set("trackingCodeTypeId", "PARTNER_MGD");
                    //leave userLogin fields empty, no use tracking to customer, or is there?; set dates to current
                    trackingCode.set("createdDate", UtilDateTime.nowTimestamp());
                    trackingCode.set("lastModifiedDate", UtilDateTime.nowTimestamp());

                    //use nearly unlimited trackable lifetime: 10 billion seconds, 310 years
                    trackingCode.set("trackableLifetime", new Long(10000000000L));
                    //use 2592000 seconds as billable lifetime: equals 1 month
                    trackingCode.set("billableLifetime", new Long(2592000));

                    trackingCode.set("comments", "This TrackingCode has default values because no default TrackingCode could be found.");
                    
                    Debug.logWarning("No default TrackingCode record was found, using a TrackingCode with hard coded default values: " + trackingCode, module);
                    
                    try {
                        trackingCode.create();
                    } catch (GenericEntityException e) {
                        Debug.logError(e, "Error creating new Partner TrackingCode with trackingCodeId [" + trackingCodeId + "], ignoring this trackingCodeId", module);
                        return "error";
                    }
                }
            }

            return processTrackingCode(trackingCode, request, response);
        } else {
            return "success";
        }
    }

    private static String processTrackingCode(GenericValue trackingCode, HttpServletRequest request, HttpServletResponse response) {
        GenericDelegator delegator = (GenericDelegator) request.getAttribute("delegator");
        String trackingCodeId = trackingCode.getString("trackingCodeId");
        
        //check effective dates
        java.sql.Timestamp nowStamp = UtilDateTime.nowTimestamp();
        if (trackingCode.get("fromDate") != null && nowStamp.before(trackingCode.getTimestamp("fromDate"))) {
            if (Debug.infoOn()) Debug.logInfo("The TrackingCode with ID [" + trackingCodeId + "] has not yet gone into effect, ignoring this trackingCodeId", module);
            return "success";
        }
        if (trackingCode.get("thruDate") != null && nowStamp.after(trackingCode.getTimestamp("thruDate"))) {
            if (Debug.infoOn()) Debug.logInfo("The TrackingCode with ID [" + trackingCodeId + "] has expired, ignoring this trackingCodeId", module);
            return "success";
        }
        
        //persist that info by associating with the current visit
        GenericValue visit = VisitHandler.getVisit(request.getSession());
        if (visit == null) {
            Debug.logWarning("Could not get visit, not associating trackingCode [" + trackingCodeId + "] with visit", module);
        } else {
            GenericValue trackingCodeVisit = delegator.makeValue("TrackingCodeVisit", 
                    UtilMisc.toMap("trackingCodeId", trackingCodeId, "visitId", visit.get("visitId"), 
                    "fromDate", UtilDateTime.nowTimestamp(), "sourceEnumId", "TKCDSRC_URL_PARAM"));
            try {
                trackingCodeVisit.create();
            } catch (GenericEntityException e) {
                Debug.logError(e, "Error while saving TrackingCodeVisit", module);
            }
        }

        
        // write trackingCode cookies with the value set to the trackingCodeId
        // NOTE: just write these cookies and if others exist from other tracking codes they will be overwritten, ie only keep the newest
        
        // load the properties from the website entity        
        String cookieDomain = null;
        
        String webSiteId = WebSiteWorker.getWebSiteId(request);
        if (webSiteId != null) {
            try {
                GenericValue webSite = delegator.findByPrimaryKeyCache("WebSite", UtilMisc.toMap("webSiteId", webSiteId));
                if (webSite != null) {
                    cookieDomain = webSite.getString("cookieDomain");
                }
            } catch (GenericEntityException e) {
                Debug.logWarning(e, "Problems with WebSite entity; using global default cookie domain", module);
            }
        }

        if (cookieDomain == null) {
            cookieDomain = UtilProperties.getPropertyValue("url", "cookie.domain", "");
        }

        // if trackingCode.trackableLifetime not null and is > 0 write a trackable cookie with name in the form: TKCDT_{trackingCode.trackingCodeTypeId} and timeout will be trackingCode.trackableLifetime
        Long trackableLifetime = trackingCode.getLong("trackableLifetime");
        if (trackableLifetime != null && trackableLifetime.longValue() > 0) {
            Cookie trackableCookie = new Cookie("TKCDT_" + trackingCode.getString("trackingCodeTypeId"), trackingCode.getString("trackingCodeId"));
            trackableCookie.setMaxAge(trackableLifetime.intValue());
            trackableCookie.setPath("/");
            trackableCookie.setVersion(1);
            if (cookieDomain.length() > 0) trackableCookie.setDomain(cookieDomain);
            response.addCookie(trackableCookie);
        }
        
        // if trackingCode.billableLifetime not null and is > 0 write a billable cookie with name in the form: TKCDB_{trackingCode.trackingCodeTypeId} and timeout will be trackingCode.billableLifetime
        Long billableLifetime = trackingCode.getLong("billableLifetime");
        if (billableLifetime != null && billableLifetime.longValue() > 0) {
            Cookie billableCookie = new Cookie("TKCDB_" + trackingCode.getString("trackingCodeTypeId"), trackingCode.getString("trackingCodeId"));
            billableCookie.setMaxAge(billableLifetime.intValue());
            billableCookie.setPath("/");
            billableCookie.setVersion(1);
            if (cookieDomain.length() > 0) billableCookie.setDomain(cookieDomain);
            response.addCookie(billableCookie);
        }
        
        // if site id exist in cookies then not req to create it if exist with diffrent site then create it
        int siteIdCookieAge = (60 * 60 * 24 * 365); // should this be configurable?
        String siteId = request.getParameter("siteId");
        if (siteId != null && siteId.length() > 0) {
            String visitorSiteIdCookieName = "Ofbiz.TKCD.SiteId";
            String visitorSiteId = null;
            // first try to get the current ID from the visitor cookie
            javax.servlet.http.Cookie[] cookies = request.getCookies();
            if (cookies != null) {
                for (int i = 0; i < cookies.length; i++) {
                    if (cookies[i].getName().equals(visitorSiteIdCookieName)) {
                        visitorSiteId = cookies[i].getValue();
                        break;
                    }
                }
            }

            if ( visitorSiteId == null || (visitorSiteId != null && !visitorSiteId.equals(siteId)) ) {
                // if trackingCode.siteId is  not null  write a trackable cookie with name in the form: Ofbiz.TKCSiteId and timeout will be 60 * 60 * 24 * 365
                Cookie siteIdCookie = new Cookie("Ofbiz.TKCD.SiteId" ,siteId);
                siteIdCookie.setMaxAge(siteIdCookieAge);
                siteIdCookie.setPath("/");
                siteIdCookie.setVersion(1);
                if (cookieDomain.length() > 0) siteIdCookie.setDomain(cookieDomain);
                    response.addCookie(siteIdCookie);
                // if trackingCode.siteId is  not null  write a trackable cookie with name in the form: Ofbiz.TKCSiteId and timeout will be 60 * 60 * 24 * 365
                Cookie updatedTimeStampCookie = new Cookie("Ofbiz.TKCD.UpdatedTimeStamp" ,UtilDateTime.nowTimestamp().toString());
                updatedTimeStampCookie.setMaxAge(siteIdCookieAge);
                updatedTimeStampCookie.setPath("/");
                updatedTimeStampCookie.setVersion(1);
                if (cookieDomain.length() > 0) updatedTimeStampCookie.setDomain(cookieDomain);
                    response.addCookie(updatedTimeStampCookie);
            }
        }

        // if we have overridden logo, css and/or catalogId set some session attributes
        HttpSession session = request.getSession();
        String overrideLogo = trackingCode.getString("overrideLogo");
        if (overrideLogo != null)
            session.setAttribute("overrideLogo", overrideLogo);
        String overrideCss = trackingCode.getString("overrideCss");
        if (overrideCss != null)
            session.setAttribute("overrideCss", overrideCss);   
        String prodCatalogId = trackingCode.getString("prodCatalogId");
        if (prodCatalogId != null && prodCatalogId.length() > 0) {  
            session.setAttribute("CURRENT_CATALOG_ID", prodCatalogId);   
            CategoryWorker.setTrail(request, new ArrayList());       
        }
        
        // if forward/redirect is needed, do a response.sendRedirect and return null to tell the control servlet to not do any other requests/views
        String redirectUrl = trackingCode.getString("redirectUrl");
        if (UtilValidate.isNotEmpty(redirectUrl)) {
            try {
                response.sendRedirect(redirectUrl);
            } catch (java.io.IOException e) {
                Debug.logError(e, "Could not redirect as requested in the trackingCode to: " + redirectUrl, module);
            }
            return null;
        }
                
        return "success";
    }
    
    /** If attaching TrackingCode Cookies to the visit is desired this event should be added to the list 
     * of events that run on the first hit in a visit.
     */
    public static String checkTrackingCodeCookies(HttpServletRequest request, HttpServletResponse response) {
        GenericDelegator delegator = (GenericDelegator) request.getAttribute("delegator");
        java.sql.Timestamp nowStamp = UtilDateTime.nowTimestamp();
        GenericValue visit = VisitHandler.getVisit(request.getSession());
        if (visit == null) {
            Debug.logWarning("Could not get visit, not checking trackingCode cookies to associate with visit", module);
        } else {
            // loop through cookies and look for ones with a name that starts with TKCDT_ for trackable cookies
            Cookie[] cookies = request.getCookies();

            if (cookies != null && cookies.length > 0) {
                for (int i = 0; i < cookies.length; i++) {
                    if (cookies[i].getName().startsWith("TKCDT_")) {
                        String trackingCodeId = cookies[i].getValue();
                        GenericValue trackingCode;
                        try {
                            trackingCode = delegator.findByPrimaryKeyCache("TrackingCode", UtilMisc.toMap("trackingCodeId", trackingCodeId));
                        } catch (GenericEntityException e) {
                            Debug.logError(e, "Error looking up TrackingCode with trackingCodeId [" + trackingCodeId + "], ignoring this trackingCodeId", module);
                            continue;
                        }

                        if (trackingCode == null) {
                            Debug.logError("TrackingCode not found for trackingCodeId [" + trackingCodeId + "], ignoring this trackingCodeId.", module);
                            //this return value will be ignored, but we'll designate this as an error anyway
                            continue;
                        }

                        //check effective dates
                        if (trackingCode.get("fromDate") != null && nowStamp.before(trackingCode.getTimestamp("fromDate"))) {
                            if (Debug.infoOn()) Debug.logInfo("The TrackingCode with ID [" + trackingCodeId + "] has not yet gone into effect, ignoring this trackingCodeId", module);
                            continue;
                        }
                        if (trackingCode.get("thruDate") != null && nowStamp.after(trackingCode.getTimestamp("thruDate"))) {
                            if (Debug.infoOn()) Debug.logInfo("The TrackingCode with ID [" + trackingCodeId + "] has expired, ignoring this trackingCodeId", module);
                            continue;
                        }
                        
                        // for each trackingCodeId found in this way attach to the visit with the TKCDSRC_COOKIE sourceEnumId
                        GenericValue trackingCodeVisit = delegator.makeValue("TrackingCodeVisit", 
                                UtilMisc.toMap("trackingCodeId", trackingCodeId, "visitId", visit.get("visitId"), 
                                "fromDate", nowStamp, "sourceEnumId", "TKCDSRC_COOKIE"));
                        try {
                            //not doing this inside a transaction, want each one possible to go in
                            trackingCodeVisit.create();
                        } catch (GenericEntityException e) {
                            Debug.logError(e, "Error while saving TrackingCodeVisit", module);
                            //don't return error, want to get as many as possible: return "error";
                        }
                    }
                }
            }
        }
        
        return "success";
    }
    
    /** Makes a list of TrackingCodeOrder entities to be attached to the current order; called by the createOrder event; the values in the returned List will not have the orderId set */
    public static List makeTrackingCodeOrders(HttpServletRequest request) {
        GenericDelegator delegator = (GenericDelegator) request.getAttribute("delegator");
        java.sql.Timestamp nowStamp = UtilDateTime.nowTimestamp();
        List trackingCodeOrders = new LinkedList();
        
        Cookie[] cookies = request.getCookies();
        String affiliateReferredTimeStamp = null;
        String siteId = null;
        String isBillable = null;
        String trackingCodeId = null; 
        if (cookies != null && cookies.length > 0) {
            for (int i = 0; i < cookies.length; i++) {
                String cookieName = cookies[i].getName();

                Debug.logInfo(" cookieName is " + cookieName, module);
                Debug.logInfo(" cookieValue is " + cookies[i].getValue(), module);
                // find the siteId cookie if it exists
                if ("Ofbiz.TKCD.SiteId".equals(cookieName)) {
                    siteId = cookies[i].getValue();
                }

                // find the referred timestamp cookie if it exists
                if ("Ofbiz.TKCD.UpdatedTimeStamp".equals(cookieName)) {
                    affiliateReferredTimeStamp = cookies[i].getValue();
                }

                // find any that start with TKCDB_ for billable tracking code cookies with isBillable=Y
                // also and for each TKCDT_ cookie that doesn't have a corresponding billable code add it to the list with isBillable=N
                // This cookie value keeps trackingCodeId
                if (cookieName.startsWith("TKCDB_")) {
                    isBillable = "Y";
                    trackingCodeId = cookies[i].getValue();
                } else if (cookieName.startsWith("TKCDT_")) {
                    isBillable = "N";
                    trackingCodeId = cookies[i].getValue();
                }
                
            }
        }
        GenericValue trackingCode = null;
        try {
            trackingCode = delegator.findByPrimaryKeyCache("TrackingCode", UtilMisc.toMap("trackingCodeId", trackingCodeId));
        } catch (GenericEntityException e) {
            Debug.logError(e, "Error looking up TrackingCode with trackingCodeId [" + trackingCodeId + "], ignoring this trackingCodeId", module);
        }

        if (trackingCode != null) {
            //check effective dates
            if (trackingCode.get("fromDate") != null && nowStamp.before(trackingCode.getTimestamp("fromDate"))) {
                if (Debug.infoOn()) Debug.logInfo("The TrackingCode with ID [" + trackingCodeId + "] has not yet gone into effect, ignoring this trackingCodeId", module);
            }
            if (trackingCode.get("thruDate") != null && nowStamp.after(trackingCode.getTimestamp("thruDate"))) {
                if (Debug.infoOn()) Debug.logInfo("The TrackingCode with ID [" + trackingCodeId + "] has expired, ignoring this trackingCodeId", module);
            }
            GenericValue trackingCodeOrder = delegator.makeValue("TrackingCodeOrder", 
                    UtilMisc.toMap("trackingCodeTypeId", trackingCode.get("trackingCodeTypeId"), 
                    "trackingCodeId", trackingCodeId, "isBillable", isBillable, "siteId", siteId,
                    "hasExported", "N", "affiliateReferredTimeStamp", affiliateReferredTimeStamp));
            
            Debug.logInfo(" trackingCodeOrder is " + trackingCodeOrder, module);
            trackingCodeOrders.add(trackingCodeOrder);
        } else {
            Debug.logError("TrackingCode not found for trackingCodeId [" + trackingCodeId + "], ignoring this trackingCodeId.", module);
        }

        return trackingCodeOrders;
    }
}

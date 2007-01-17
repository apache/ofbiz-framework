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
package org.ofbiz.common;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.StringUtil;
import org.ofbiz.base.util.UtilHttp;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.base.util.cache.UtilCache;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.security.Security;

/**
 * Common Services
 */
public class CommonEvents {
    
    public static final String module = CommonEvents.class.getName();
           
    public static UtilCache appletSessions = new UtilCache("AppletSessions", 0, 600000, true);
    
    public static String checkAppletRequest(HttpServletRequest request, HttpServletResponse response) { 
        GenericDelegator delegator = (GenericDelegator) request.getAttribute("delegator");                
        String sessionId = request.getParameter("sessionId");
        String visitId = request.getParameter("visitId");
        sessionId = sessionId.trim();
        visitId = visitId.trim();
        
        String responseString = "";
        
        GenericValue visit = null;
        try {
            visit = delegator.findByPrimaryKey("Visit", UtilMisc.toMap("visitId", visitId));                             
        } catch (GenericEntityException e) {
            Debug.logError(e, "Cannot Visit Object", module);
        }
       
        if (visit != null && visit.getString("sessionId").equals(sessionId) && appletSessions.containsKey(sessionId)) {            
            Map sessionMap = (Map) appletSessions.get(sessionId);
            if (sessionMap != null && sessionMap.containsKey("followPage"))
                responseString = (String) sessionMap.remove("followPage");                                                             
        } 
        
        try {
            PrintWriter out = response.getWriter();
            response.setContentType("text/plain");
            out.println(responseString);
            out.close();
        } catch (IOException e) {
            Debug.logError(e, "Problems writing servlet output!", module);
        }
                                                
        return "success";
    }   
    
    public static String receiveAppletRequest(HttpServletRequest request, HttpServletResponse response) {
        GenericDelegator delegator = (GenericDelegator) request.getAttribute("delegator");                
        String sessionId = request.getParameter("sessionId");
        String visitId = request.getParameter("visitId");
        sessionId = sessionId.trim();
        visitId = visitId.trim();
                
        String responseString = "ERROR";
        
        GenericValue visit = null;
        try {
            visit = delegator.findByPrimaryKey("Visit", UtilMisc.toMap("visitId", visitId));                             
        } catch (GenericEntityException e) {
            Debug.logError(e, "Cannot Visit Object", module);
        }        
        
        if (visit.getString("sessionId").equals(sessionId)) {                                                                             
            String currentPage = request.getParameter("currentPage");
            if (appletSessions.containsKey(sessionId)) {              
                Map sessionMap = (Map) appletSessions.get(sessionId);
                String followers = (String) sessionMap.get("followers");
                List folList = StringUtil.split(followers, ",");
                Iterator i = folList.iterator();
                while (i.hasNext()) {
                    String follower = (String) i.next();
                    Map folSesMap = UtilMisc.toMap("followPage", currentPage);
                    appletSessions.put(follower, folSesMap);
                }
            }           
            responseString = "OK";
        }

        try {
            PrintWriter out = response.getWriter();
            response.setContentType("text/plain");
            out.println(responseString);
            out.close();
        } catch (IOException e) {
            Debug.logError(e, "Problems writing servlet output!", module);
        }
        
        return "success";
    }
    
    public static String setAppletFollower(HttpServletRequest request, HttpServletResponse response) {
        Security security = (Security) request.getAttribute("security");
        GenericValue userLogin = (GenericValue) request.getSession().getAttribute("userLogin");
        String visitId = request.getParameter("visitId");
        if (visitId != null) request.setAttribute("visitId", visitId);
        if (security.hasPermission("SEND_CONTROL_APPLET", userLogin)) { 
            String followerSessionId = request.getParameter("followerSid");
            String followSessionId = request.getParameter("followSid");
            Map follow = (Map) appletSessions.get(followSessionId);
            if (follow == null) follow = new HashMap();
            String followerListStr = (String) follow.get("followers");
            if (followerListStr == null) {
                followerListStr = followerSessionId;
            } else {
                followerListStr = followerListStr + "," + followerSessionId;
            }
            appletSessions.put(followSessionId, follow);
            appletSessions.put(followerSessionId, null);            
        }
        return "success";                
    }

    public static String setFollowerPage(HttpServletRequest request, HttpServletResponse response) {
        Security security = (Security) request.getAttribute("security");
        GenericValue userLogin = (GenericValue) request.getSession().getAttribute("userLogin");
        String visitId = request.getParameter("visitId");
        if (visitId != null) request.setAttribute("visitId", visitId);
        if (security.hasPermission("SEND_CONTROL_APPLET", userLogin)) { 
            String followerSessionId = request.getParameter("followerSid");
            String pageUrl = request.getParameter("pageUrl");
            Map follow = (Map) appletSessions.get(followerSessionId);
            if (follow == null) follow = new HashMap();
            follow.put("followPage", pageUrl);
            appletSessions.put(followerSessionId, follow);
        }
        return "success";                
    }

    /** Simple event to set the users per-session locale setting */
    public static String setSessionLocale(HttpServletRequest request, HttpServletResponse response) {
        String localeString = request.getParameter("locale");
        if (UtilValidate.isNotEmpty(localeString)) {
            UtilHttp.setLocale(request, localeString);

            // update the UserLogin object
            GenericValue userLogin = (GenericValue) request.getSession().getAttribute("userLogin");
            if (userLogin == null) {
                userLogin = (GenericValue) request.getSession().getAttribute("autoUserLogin");
            }

            if (userLogin != null) {
                GenericValue ulUpdate = GenericValue.create(userLogin);
                ulUpdate.set("lastLocale", localeString);
                try {
                    ulUpdate.store();
                    userLogin.refreshFromCache();
                } catch (GenericEntityException e) {
                    Debug.logWarning(e, module);
                }
            }
        }
        return "success";
    }

    /** Simple event to set the users per-session currency uom value */
    public static String setSessionCurrencyUom(HttpServletRequest request, HttpServletResponse response) {
        String currencyUom = request.getParameter("currencyUom");
        if (UtilValidate.isNotEmpty(currencyUom)) {
            // update the session
            UtilHttp.setCurrencyUom(request.getSession(), currencyUom);

            // update the UserLogin object
            GenericValue userLogin = (GenericValue) request.getSession().getAttribute("userLogin");
            if (userLogin == null) {
                userLogin = (GenericValue) request.getSession().getAttribute("autoUserLogin");
            }

            if (userLogin != null) {
                GenericValue ulUpdate = GenericValue.create(userLogin);
                ulUpdate.set("lastCurrencyUom", currencyUom);
                try {
                    ulUpdate.store();
                    userLogin.refreshFromCache();
                } catch (GenericEntityException e) {
                    Debug.logWarning(e, module);
                }
            }
        }
        return "success";
    }
}


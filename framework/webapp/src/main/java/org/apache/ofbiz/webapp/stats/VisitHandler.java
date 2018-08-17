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
package org.apache.ofbiz.webapp.stats;

import java.net.InetAddress;
import java.sql.Timestamp;
import java.util.Locale;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilProperties;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.DelegatorFactory;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.model.ModelEntity;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.entity.util.EntityUtilProperties;

/**
 * Handles saving and maintaining visit information
 */
public class VisitHandler {
    // Debug module name
    public static final String module = VisitHandler.class.getName();

    public static final String visitorCookieName = "OFBiz.Visitor";

    protected static final InetAddress address;
    static {
        InetAddress tmpAddress = null;
        try {
            tmpAddress = InetAddress.getLocalHost();
        } catch (java.net.UnknownHostException e) {
            Debug.logError("Unable to get server's internet address: " + e.toString(), module);
        }
        address = tmpAddress;
    }

    public static void setUserLogin(HttpSession session, GenericValue userLogin, boolean userCreated) {
        if (userLogin == null) return;
        ModelEntity modelUserLogin = userLogin.getModelEntity();

        GenericValue visitor = (GenericValue) session.getAttribute("visitor");
        if (visitor != null) {
            visitor.set("userLoginId", userLogin.get("userLoginId"));
            if (modelUserLogin.isField("partyId")) {
                visitor.set("partyId", userLogin.get("partyId"));
            }
            try {
                visitor.store();
            } catch (GenericEntityException e) {
                Debug.logError(e, "Could not update visitor: ", module);
            }
        }

        GenericValue visit = getVisit(session);
        if (visit != null) {
            visit.set("userLoginId", userLogin.get("userLoginId"));
            if (modelUserLogin.isField("partyId")) {
                visit.set("partyId", userLogin.get("partyId"));
            }
            visit.set("userCreated", userCreated);

            // make sure the visitorId is still in place
            if (visitor != null) {
                visit.set("visitorId", visitor.get("visitorId"));
            }

            try {
                visit.store();
            } catch (GenericEntityException e) {
                Debug.logError(e, "Could not update visit: ", module);
            }
        }
    }

    public static String getVisitId(HttpSession session) {
        GenericValue visit = getVisit(session);
        if (visit != null) {
            return visit.getString("visitId");
        } else {
            return null;
        }
    }

    /** Get the visit from the session, or create if missing */
    public static GenericValue getVisit(HttpSession session) {
        // this defaults to true: ie if anything but "false" it will be true
        if (!UtilProperties.propertyValueEqualsIgnoreCase("serverstats", "stats.persist.visit", "false")) {
            GenericValue visit = (GenericValue) session.getAttribute("visit");
            if (visit == null) {
                synchronized (session) {
                    visit = (GenericValue) session.getAttribute("visit");
                    if (visit == null) {
                        Delegator delegator = null;

                        // first try the session attribute delegatorName
                        String delegatorName = (String) session.getAttribute("delegatorName");
                        if (UtilValidate.isNotEmpty(delegatorName)) {
                            delegator = DelegatorFactory.getDelegator(delegatorName);
                        }

                        // then try the ServletContext attribute delegator, should always be there...
                        if (delegator == null) {
                            delegator = (Delegator) session.getServletContext().getAttribute("delegator");
                        }

                        if (delegator == null) {
                            Debug.logError("Could not find delegator with delegatorName [" + delegatorName + "] in session, or a delegator attribute in the ServletContext, not creating Visit entity", module);
                        } else {
                            String webappName = (String) session.getAttribute("_WEBAPP_NAME_");
                            Locale initialLocaleObj = (Locale) session.getAttribute("_CLIENT_LOCALE_");
                            String initialRequest = (String) session.getAttribute("_CLIENT_REQUEST_");
                            String initialReferrer = (String) session.getAttribute("_CLIENT_REFERER_");
                            String initialUserAgent = (String) session.getAttribute("_CLIENT_USER_AGENT_");

                            String initialLocale = initialLocaleObj != null ? initialLocaleObj.toString() : "";

                            if (UtilValidate.isEmpty(webappName)) {
                                Debug.logInfo(new Exception(), "The webappName was empty, somehow the initial request settings were missing.", module);
                            }

                            visit = delegator.makeValue("Visit");
                            visit.set("sessionId", session.getId());
                            visit.set("fromDate", new Timestamp(session.getCreationTime()));

                            visit.set("initialLocale", initialLocale);
                            visit.set("initialRequest", initialRequest);
                            visit.set("initialReferrer", initialReferrer);
                            if (initialUserAgent != null) visit.set("initialUserAgent", initialUserAgent.length() > 250 ? initialUserAgent.substring(0, 250) : initialUserAgent);
                            visit.set("webappName", webappName);
                            if (UtilProperties.propertyValueEquals("serverstats", "stats.proxy.enabled", "true")) {
                                visit.set("clientIpAddress", session.getAttribute("_CLIENT_FORWARDED_FOR_"));
                            } else {
                                visit.set("clientIpAddress", session.getAttribute("_CLIENT_REMOTE_ADDR_"));
                            }
                            visit.set("clientHostName", session.getAttribute("_CLIENT_REMOTE_HOST_"));
                            visit.set("clientUser", session.getAttribute("_CLIENT_REMOTE_USER_"));

                            // get the visitorId
                            GenericValue visitor = (GenericValue) session.getAttribute("visitor");
                            if (visitor != null) {
                                String visitorId = visitor.getString("visitorId");
                                
                                // sometimes these values get stale, so check it before we use it
                                try {
                                    GenericValue checkVisitor = EntityQuery.use(delegator).from("Visitor").where("visitorId", visitorId).queryOne();
                                    if (checkVisitor == null) {
                                        GenericValue newVisitor = delegator.create("Visitor", "visitorId", visitorId);
                                        session.setAttribute("visitor", newVisitor);
                                    }
                                    visit.set("visitorId", visitorId);
                                } catch (GenericEntityException e) {
                                    Debug.logWarning("Problem checking the visitorId: " + e.toString(), module);
                                }
                            }

                            // get localhost ip address and hostname to store
                            if (address != null) {
                                visit.set("serverIpAddress", address.getHostAddress());
                                visit.set("serverHostName", address.getHostName());
                            }

                            try {
                                visit = delegator.createSetNextSeqId(visit);
                                session.setAttribute("visit", visit);
                            } catch (GenericEntityException e) {
                                Debug.logError(e, "Could not create new visit:", module);
                                visit = null;
                            }
                        }
                    }
                }
            }

            if (visit == null) {
                Debug.logWarning("Could not find or create the visit...", module);
            }
            return visit;
        }
        return null;
    }

    public static GenericValue getVisitor(HttpServletRequest request, HttpServletResponse response) {
        // this defaults to true: ie if anything but "false" it will be true
    	Delegator delegator = (Delegator) request.getAttribute("delegator");
        if (!EntityUtilProperties.propertyValueEqualsIgnoreCase("serverstats", "stats.persist.visitor", "false", delegator)) {
            HttpSession session = request.getSession();

            GenericValue visitor = (GenericValue) session.getAttribute("visitor");
            if (visitor == null) {
                synchronized (session) {
                    visitor = (GenericValue) session.getAttribute("visitor");
                    if (visitor == null) {

                        String delegatorName = (String) session.getAttribute("delegatorName");
                        if (delegator == null && UtilValidate.isNotEmpty(delegatorName)) {
                            delegator = DelegatorFactory.getDelegator(delegatorName);
                        }

                        if (delegator == null) {
                            Debug.logError("Could not find delegator in request or with delegatorName [" + delegatorName + "] in session, not creating/getting Visitor entity", module);
                        } else {
                            // first try to get the current ID from the visitor cookie
                            String cookieVisitorId = null;
                            Cookie[] cookies = request.getCookies();
                            if (Debug.verboseOn()) Debug.logVerbose("Cookies:" + cookies, module);
                            if (cookies != null) {
                                for (int i = 0; i < cookies.length; i++) {
                                    if (cookies[i].getName().equals(visitorCookieName)) {
                                        cookieVisitorId = cookies[i].getValue();
                                        break;
                                    }
                                }
                            }

                            if (Debug.infoOn()) Debug.logInfo("Found visitorId [" + cookieVisitorId + "] in cookie", module);

                            if (UtilValidate.isEmpty(cookieVisitorId)) {
                                // no visitor cookie? create visitor and send back cookie too
                                visitor = delegator.makeValue("Visitor");
                                try {
                                    delegator.createSetNextSeqId(visitor);
                                } catch (GenericEntityException e) {
                                    Debug.logError(e, "Could not create new visitor:", module);
                                    visitor = null;
                                }
                            } else {
                                try {
                                    visitor = EntityQuery.use(delegator).from("Visitor").where("visitorId", cookieVisitorId).queryOne();
                                    if (visitor == null) {
                                        // looks like we have an ID that doesn't exist in our database, so we'll create a new one
                                        visitor = delegator.makeValue("Visitor");
                                        visitor = delegator.createSetNextSeqId(visitor);
                                        if (Debug.infoOn()) {
                                            String visitorId = visitor != null ? visitor.getString("visitorId") : "empty visitor";
                                            Debug.logInfo("The visitorId [" + cookieVisitorId + "] found in cookie was invalid, creating new Visitor with ID [" + visitorId + "]", module);
                                        }
                                    }
                                } catch (GenericEntityException e) {
                                    Debug.logError(e, "Error finding visitor with ID from cookie: " + cookieVisitorId, module);
                                    visitor = null;
                                }
                            }
                        }

                        if (visitor != null) {
                            // we got one, and it's a new one since it was null before
                            session.setAttribute("visitor", visitor);

                            // create the cookie and send it back, this may be done over and over, in effect frequently refreshing the cookie
                            Cookie visitorCookie = new Cookie(visitorCookieName, visitor.getString("visitorId"));
                            visitorCookie.setMaxAge(60 * 60 * 24 * 365);
                            visitorCookie.setPath("/");
                            visitorCookie.setSecure(true);
                            visitorCookie.setHttpOnly(true);
                            response.addCookie(visitorCookie);
                        }
                    }
                }
            }
            if (visitor == null) {
                Debug.logWarning(new Exception(), "Could not find or create the visitor...", module);
            }
            return visitor;
        }
        return null;
    }
}

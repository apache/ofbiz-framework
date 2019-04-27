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

import java.sql.Timestamp;
import java.util.Enumeration;
import java.util.Map;

import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilDateTime;
import org.apache.ofbiz.base.util.UtilGenerics;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.serialize.XmlSerializer;
import org.apache.ofbiz.entity.transaction.TransactionUtil;
import org.apache.ofbiz.entity.util.EntityQuery;

/**
 * HttpSessionListener that gathers and tracks various information and statistics
 */
public class ControlEventListener implements HttpSessionListener {
    // Debug module name
    public static final String module = ControlEventListener.class.getName();

    protected static long totalActiveSessions = 0;
    protected static long totalPassiveSessions = 0;

    public ControlEventListener() {}

    @Override
    public void sessionCreated(HttpSessionEvent event) {
        HttpSession session = event.getSession();

        // get/create the visit
        // NOTE: don't create the visit here, just let the control servlet do it; GenericValue visit = VisitHandler.getVisit(session);

        countCreateSession();

        // property setting flag for logging stats
        if (System.getProperty("org.apache.ofbiz.log.session.stats") != null) {
            session.setAttribute("org.apache.ofbiz.log.session.stats", "Y");
        }

        Debug.logInfo("Creating session: " + ControlActivationEventListener.showSessionId(session), module);
    }

    @Override
    public void sessionDestroyed(HttpSessionEvent event) {
        HttpSession session = event.getSession();

        // Finalize the Visit
        boolean beganTransaction = false;
        try {
            beganTransaction = TransactionUtil.begin();

            // instead of using this message, get directly from session attribute so it won't create a new one: GenericValue visit = VisitHandler.getVisit(session);
            GenericValue visit = (GenericValue) session.getAttribute("visit");
            if (visit != null) {
                Delegator delegator = visit.getDelegator();
                visit = EntityQuery.use(delegator).from("Visit").where("visitId", visit.get("visitId")).queryOne();
                if (visit != null) {
                    visit.set("thruDate", new Timestamp(session.getLastAccessedTime()));
                    visit.store();
                }
            } else {
                Debug.logInfo("Could not find visit value object in session [" + ControlActivationEventListener.showSessionId(session) + "] that is being destroyed", module);
            }

            // Store the UserLoginSession
            String userLoginSessionString = getUserLoginSession(session);
            GenericValue userLogin = (GenericValue) session.getAttribute("userLogin");
            if (userLogin != null && userLoginSessionString != null) {
                GenericValue userLoginSession = null;
                userLoginSession = userLogin.getRelatedOne("UserLoginSession", false);

                if (userLoginSession == null) {
                    userLoginSession = userLogin.getDelegator().makeValue("UserLoginSession",
                            UtilMisc.toMap("userLoginId", userLogin.getString("userLoginId")));
                    userLogin.getDelegator().create(userLoginSession);
                }
                userLoginSession.set("savedDate", UtilDateTime.nowTimestamp());
                userLoginSession.set("sessionData", userLoginSessionString);
                userLoginSession.store();
            }

            countDestroySession();
            Debug.logInfo("Destroying session: " + ControlActivationEventListener.showSessionId(session), module);
            this.logStats(session, visit);
        } catch (GenericEntityException e) {
            try {
                // only rollback the transaction if we started one...
                TransactionUtil.rollback(beganTransaction, "Error saving information about closed HttpSession", e);
            } catch (GenericEntityException e2) {
                Debug.logError(e2, "Could not rollback transaction: " + e2.toString(), module);
            }

            Debug.logError(e, "Error in session destuction information persistence", module);
        } finally {
            // only commit the transaction if we started one... this will throw an exception if it fails
            try {
                TransactionUtil.commit(beganTransaction);
            } catch (GenericEntityException e) {
                Debug.logError(e, "Could not commit transaction for update visit for session destuction", module);
            }
        }
    }

    public void logStats(HttpSession session, GenericValue visit) {
        if (Debug.verboseOn() || session.getAttribute("org.apache.ofbiz.log.session.stats") != null) {
            Debug.logInfo("<===================================================================>", module);
            Debug.logInfo("Session ID     : " + ControlActivationEventListener.showSessionId(session), module);
            Debug.logInfo("Created Time   : " + session.getCreationTime(), module);
            Debug.logInfo("Last Access    : " + session.getLastAccessedTime(), module);
            Debug.logInfo("Max Inactive   : " + session.getMaxInactiveInterval(), module);
            Debug.logInfo("--------------------------------------------------------------------", module);
            Debug.logInfo("Total Sessions : " + ControlEventListener.getTotalActiveSessions(), module);
            Debug.logInfo("Total Active   : " + ControlEventListener.getTotalActiveSessions(), module);
            Debug.logInfo("Total Passive  : " + ControlEventListener.getTotalPassiveSessions(),  module);
            Debug.logInfo("** note : this session has been counted as destroyed.", module);
            Debug.logInfo("--------------------------------------------------------------------", module);
            if (visit != null) {
                Debug.logInfo("Visit ID       : " + visit.getString("visitId"), module);
                Debug.logInfo("Party ID       : " + visit.getString("partyId"), module);
                Debug.logInfo("Client IP      : " + visit.getString("clientIpAddress"), module);
                Debug.logInfo("Client Host    : " + visit.getString("clientHostName"), module);
                Debug.logInfo("Client User    : " + visit.getString("clientUser"), module);
                Debug.logInfo("WebApp         : " + visit.getString("webappName"), module);
                Debug.logInfo("Locale         : " + visit.getString("initialLocale"), module);
                Debug.logInfo("UserAgent      : " + visit.getString("initialUserAgent"), module);
                Debug.logInfo("Referrer       : " + visit.getString("initialReferrer"), module);
                Debug.logInfo("Initial Req    : " + visit.getString("initialRequest"), module);
                Debug.logInfo("Visit From     : " + visit.getString("fromDate"), module);
                Debug.logInfo("Visit Thru     : " + visit.getString("thruDate"), module);
            }
            Debug.logInfo("--------------------------------------------------------------------", module);
            Debug.logInfo("--- Start Session Attributes: ---", module);
            Enumeration<String> sesNames = null;
            try {
                sesNames = UtilGenerics.cast(session.getAttributeNames());
            } catch (IllegalStateException e) {
                Debug.logInfo("Cannot get session attributes : " + e.getMessage(), module);
            }
            while (sesNames != null && sesNames.hasMoreElements()) {
                String attName = sesNames.nextElement();
                Debug.logInfo(attName + ":" + session.getAttribute(attName), module);
            }
            Debug.logInfo("--- End Session Attributes ---", module);
            Debug.logInfo("<===================================================================>", module);
        }
    }

    public static long getTotalActiveSessions() {
        return totalActiveSessions;
    }

    public static long getTotalPassiveSessions() {
        return totalPassiveSessions;
    }

    public static long getTotalSessions() {
        return totalActiveSessions + totalPassiveSessions;
    }

    public static void countCreateSession() {
        totalActiveSessions++;
    }

    public static void countDestroySession() {
        totalActiveSessions--;
    }

    public static void countPassivateSession() {
        totalActiveSessions--;
        totalPassiveSessions++;
    }

    public static void countActivateSession() {
        totalActiveSessions++;
        totalPassiveSessions--;
    }

    private String getUserLoginSession(HttpSession session) {
        Map<String, ?> userLoginSession = UtilGenerics.cast(session.getAttribute("userLoginSession"));

        String sessionData = null;
        if (UtilValidate.isNotEmpty(userLoginSession)) {
            try {
                sessionData = XmlSerializer.serialize(userLoginSession);
            } catch (Exception e) {
                Debug.logWarning(e, "Problems serializing UserLoginSession", module);
            }
        }
        return sessionData;
    }
}

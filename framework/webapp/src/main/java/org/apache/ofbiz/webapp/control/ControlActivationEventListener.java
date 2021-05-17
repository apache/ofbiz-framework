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

import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionActivationListener;
import javax.servlet.http.HttpSessionEvent;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilProperties;

/**
 * HttpSessionListener that gathers and tracks various information and statistics
 */
public class ControlActivationEventListener implements HttpSessionActivationListener {
    // Debug MODULE name
    private static final String MODULE = ControlActivationEventListener.class.getName();

    public ControlActivationEventListener() { }

    @Override
    public void sessionWillPassivate(HttpSessionEvent event) {
        ControlEventListener.countPassivateSession();
        Debug.logInfo("Passivating session: " + showSessionId(event.getSession()), MODULE);
    }

    @Override
    public void sessionDidActivate(HttpSessionEvent event) {
        ControlEventListener.countActivateSession();
        Debug.logInfo("Activating session: " + showSessionId(event.getSession()), MODULE);
    }
    public static String showSessionId(HttpSession session) {
        boolean showSessionIdInLog = UtilProperties.propertyValueEqualsIgnoreCase("requestHandler", "show-sessionId-in-log", "Y");
        if (showSessionIdInLog) {
            return " sessionId=" + session.getId();
        }
        return " hidden sessionId by default.";
    }

}

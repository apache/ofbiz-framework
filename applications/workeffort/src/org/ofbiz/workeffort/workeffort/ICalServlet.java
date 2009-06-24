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

package org.ofbiz.workeffort.workeffort;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;

import javax.servlet.http.HttpServlet;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import net.fortuna.ical4j.model.Calendar;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilJ2eeCompat;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.GenericDelegator;

@SuppressWarnings("serial")
public class ICalServlet extends HttpServlet {
    public static final String module = ICalServlet.class.getName();

    /** Initialize this servlet. */
    public void init() throws ServletException {
        super.init();
        if (Debug.infoOn()) {
            Debug.logInfo("[ICalServlet.init] Loading iCalendar Servlet mounted on path " + this.getServletContext().getRealPath("/"), module);
        }
    }

    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String path = req.getPathInfo();
        if (UtilValidate.isEmpty(path)) {
            path = "/";
        }
        String workEffortId = path.substring(1);
        if (workEffortId.contains("/")) {
            workEffortId = workEffortId.substring(0, workEffortId.indexOf("/"));
        }
        if (workEffortId.length() < 1) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }
        if (Debug.infoOn()) {
            Debug.logInfo("[ICalServlet.doGet] workEffortId = " + workEffortId, module);
        }
        GenericDelegator delegator = null;
        HttpSession session = req.getSession();
        String delegatorName = (String) session.getAttribute("delegatorName");
        if (UtilValidate.isNotEmpty(delegatorName)) {
            delegator = GenericDelegator.getGenericDelegator(delegatorName);
        }
        if (delegator == null) {
            delegator = (GenericDelegator) this.getServletContext().getAttribute("delegator");
        }
        if (delegator == null) {
            Debug.logError("[ICalServlet.doGet] ERROR: delegator not found in ServletContext", module);
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return;
        }
        Calendar calendar = null;
        try {
            calendar = ICalWorker.getICalendar(delegator, workEffortId);
        } catch (Exception e) {
            Debug.logError("[ICalServlet.doGet] Error while getting iCalendar: " + e, module);
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return;
        }
        if (calendar == null) {
            resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        resp.setContentType("text/calendar");
        resp.setStatus(HttpServletResponse.SC_OK);
        Writer writer = null;
        if (UtilJ2eeCompat.useOutputStreamNotWriter(this.getServletContext())) {
            ServletOutputStream ros = resp.getOutputStream();
            writer = new OutputStreamWriter(ros, "UTF-8");
        } else {
            writer = resp.getWriter();
        }
        writer.write(calendar.toString());
        writer.close();
        if (Debug.infoOn()) {
            Debug.logInfo("[ICalServlet.doGet] finished request", module);
        }
    }
};

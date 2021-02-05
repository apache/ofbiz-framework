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
package org.apache.ofbiz.webapp;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilHttp;
import org.apache.ofbiz.common.JsLanguageFilesMappingUtil;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.util.EntityListIterator;
import org.apache.ofbiz.entity.util.EntityQuery;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Locale;

public class AfterLoginEvents {

    private static final String MODULE = AfterLoginEvents.class.getName();
    private static final String SCRIPT_SHOW_LAST_VISIT_DATE;

    static {
        SCRIPT_SHOW_LAST_VISIT_DATE = "<span id='showLastLogin'></span><script>"
                + "importLibrary(%s, function () {\n"
                + "var dateFormat = Date.CultureInfo.formatPatterns.shortDate + ' ' + Date.CultureInfo.formatPatterns.longTime;\n"
                + "var message = 'Your last visit was on '+new Date('%s').toString(dateFormat);\n"
                + "$('#showLastLogin').replaceWith(message);\n"
                + "});\n</script>";
    }

    public static String showLastVisit(HttpServletRequest request, HttpServletResponse response) {

        // guard against re-popup while moving to other web application when tomcat SSO is enabled
        if (!"login".equals(request.getAttribute("thisRequestUri"))) {
            return "success";
        }

        HttpSession session = request.getSession();
        Delegator delegator = (Delegator) request.getAttribute("delegator");
        GenericValue userLogin = (GenericValue) session.getAttribute("userLogin");

        String userLoginId = (String) userLogin.get("userLoginId");

        try (EntityListIterator eli = EntityQuery.use(delegator)
                .from("Visit")
                .where("userLoginId", userLoginId)
                .orderBy("-fromDate")
                .cursorScrollInsensitive()
                .maxRows(2)
                .queryIterator()) {
            if (eli != null) {
                GenericValue visit = null;
                int count = 0;
                while ((visit = eli.next()) != null) {
                    if (count == 1) {
                        Timestamp fromDate = visit.getTimestamp("fromDate");
                        Locale locale = UtilHttp.getLocale(request);
                        String libJs = "['" + JsLanguageFilesMappingUtil.getFile("datejs", locale.toString()) + "']";
                        SimpleDateFormat formatter = new SimpleDateFormat("EE MMM d y H:m:s ZZZ");
                        String dateString = formatter.format(fromDate);
                        request.setAttribute("_UNSAFE_EVENT_MESSAGE_", String.format(SCRIPT_SHOW_LAST_VISIT_DATE, libJs, dateString));
                    }
                    count++;
                }
            }
        } catch (GenericEntityException e) {
            Debug.logError(e, MODULE);
            request.setAttribute("_ERROR_MESSAGE_", e.getMessage());
            return "error";
        }
        return "success";
    }
}

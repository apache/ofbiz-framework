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
package org.ofbiz.widget;

import java.util.Map;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.ofbiz.base.util.UtilHttp;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.webapp.control.RequestHandler;
import org.ofbiz.webapp.taglib.ContentUrlTag;

public class WidgetWorker {

    public static final String module = WidgetWorker.class.getName();

    public WidgetWorker () {}

    public static void buildHyperlinkUrl(StringBuffer buffer, String requestName, String targetType, HttpServletRequest request, HttpServletResponse response, Map context) {
        String localRequestName = UtilHttp.encodeAmpersands(requestName);
        
        if ("intra-app".equals(targetType)) {
            appendOfbizUrl(buffer, "/" + localRequestName, request, response);
        } else if ("inter-app".equals(targetType)) {
            String fullTarget = localRequestName;
            buffer.append(fullTarget);
            String externalLoginKey = (String) request.getAttribute("externalLoginKey");
            if (UtilValidate.isNotEmpty(externalLoginKey)) {
                if (fullTarget.indexOf('?') == -1) {
                    buffer.append('?');
                } else {
                    buffer.append("&amp;");
                }
                buffer.append("externalLoginKey=");
                buffer.append(externalLoginKey);
            }
        } else if ("content".equals(targetType)) {
            appendContentUrl(buffer, localRequestName, request);
        } else if ("plain".equals(targetType)) {
            buffer.append(localRequestName);
        } else {
            buffer.append(localRequestName);
        }

    }

    public static void appendOfbizUrl(StringBuffer buffer, String location, HttpServletRequest request, HttpServletResponse response) {
        ServletContext ctx = (ServletContext) request.getAttribute("servletContext");
        RequestHandler rh = (RequestHandler) ctx.getAttribute("_REQUEST_HANDLER_");
        // make and append the link
        buffer.append(rh.makeLink(request, response, location));
    }

    public static void appendContentUrl(StringBuffer buffer, String location, HttpServletRequest request) {
        ContentUrlTag.appendContentPrefix(request, buffer);
        buffer.append(location);
    }

    public static void makeHyperlinkString(StringBuffer buffer, String linkStyle, String targetType, String target, String description, HttpServletRequest request, HttpServletResponse response, Map context, String targetWindow) {
        if (UtilValidate.isNotEmpty(description)) {
            buffer.append("<a");

            if (UtilValidate.isNotEmpty(linkStyle)) {
                buffer.append(" class=\"");
                buffer.append(linkStyle);
                buffer.append("\"");
            }

            buffer.append(" href=\"");

            WidgetWorker.buildHyperlinkUrl(buffer, target, targetType, request, response, context);

            buffer.append("\"");
            
            if (UtilValidate.isNotEmpty(targetWindow)) {
                buffer.append(" target=\"");
                buffer.append(targetWindow);
                buffer.append("\"");
            }


            buffer.append('>');

            buffer.append(description);
            buffer.append("</a>");
        }
    }
}

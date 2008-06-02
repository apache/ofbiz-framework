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

import java.io.IOException;
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

    public static void buildHyperlinkUrl(Appendable writer, String requestName, String targetType, HttpServletRequest request, HttpServletResponse response, Map<String, Object> context) throws IOException {
        String localRequestName = UtilHttp.encodeAmpersands(requestName);
        
        if ("intra-app".equals(targetType)) {
            appendOfbizUrl(writer, "/" + localRequestName, request, response);
        } else if ("inter-app".equals(targetType)) {
            String fullTarget = localRequestName;
            writer.append(fullTarget);
            String externalLoginKey = (String) request.getAttribute("externalLoginKey");
            if (UtilValidate.isNotEmpty(externalLoginKey)) {
                if (fullTarget.indexOf('?') == -1) {
                    writer.append('?');
                } else {
                    writer.append("&amp;");
                }
                writer.append("externalLoginKey=");
                writer.append(externalLoginKey);
            }
        } else if ("content".equals(targetType)) {
            appendContentUrl(writer, localRequestName, request);
        } else if ("plain".equals(targetType)) {
            writer.append(localRequestName);
        } else {
            writer.append(localRequestName);
        }

    }

    public static void appendOfbizUrl(Appendable writer, String location, HttpServletRequest request, HttpServletResponse response) throws IOException {
        ServletContext ctx = (ServletContext) request.getAttribute("servletContext");
        RequestHandler rh = (RequestHandler) ctx.getAttribute("_REQUEST_HANDLER_");
        // make and append the link
        writer.append(rh.makeLink(request, response, location));
    }

    public static void appendContentUrl(Appendable writer, String location, HttpServletRequest request) throws IOException {
        StringBuffer buffer = new StringBuffer();
        ContentUrlTag.appendContentPrefix(request, buffer);
        writer.append(buffer.toString());
        writer.append(location);
    }

    public static void makeHyperlinkString(Appendable writer, String linkStyle, String targetType, String target, String description, HttpServletRequest request, HttpServletResponse response, Map<String, Object> context, String targetWindow, String event, String action) throws IOException {
        if (UtilValidate.isNotEmpty(description)) {
            writer.append("<a");

            if (UtilValidate.isNotEmpty(linkStyle)) {
                writer.append(" class=\"");
                writer.append(linkStyle);
                writer.append("\"");
            }

            writer.append(" href=\"");

            buildHyperlinkUrl(writer, target, targetType, request, response, context);

            writer.append("\"");
            
            if (UtilValidate.isNotEmpty(targetWindow)) {
                writer.append(" target=\"");
                writer.append(targetWindow);
                writer.append("\"");
            }

            if (UtilValidate.isNotEmpty(event) && UtilValidate.isNotEmpty(action)) {
                writer.append(" ");
                writer.append(event);
                writer.append("=\"");
                writer.append(action);
                writer.append('"');
            }

            writer.append('>');

            writer.append(description);
            writer.append("</a>");
        }
    }
}

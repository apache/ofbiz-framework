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
import java.io.Writer;
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

    public static void buildHyperlinkUrl(Writer writer, String requestName, String targetType, HttpServletRequest request, HttpServletResponse response, Map<String, Object> context) throws IOException {
        String localRequestName = UtilHttp.encodeAmpersands(requestName);
        
        if ("intra-app".equals(targetType)) {
            appendOfbizUrl(writer, "/" + localRequestName, request, response);
        } else if ("inter-app".equals(targetType)) {
            String fullTarget = localRequestName;
            writer.write(fullTarget);
            String externalLoginKey = (String) request.getAttribute("externalLoginKey");
            if (UtilValidate.isNotEmpty(externalLoginKey)) {
                if (fullTarget.indexOf('?') == -1) {
                    writer.write('?');
                } else {
                    writer.write("&amp;");
                }
                writer.write("externalLoginKey=");
                writer.write(externalLoginKey);
            }
        } else if ("content".equals(targetType)) {
            appendContentUrl(writer, localRequestName, request);
        } else if ("plain".equals(targetType)) {
            writer.write(localRequestName);
        } else {
            writer.write(localRequestName);
        }

    }

    public static void appendOfbizUrl(Writer writer, String location, HttpServletRequest request, HttpServletResponse response) throws IOException {
        ServletContext ctx = (ServletContext) request.getAttribute("servletContext");
        RequestHandler rh = (RequestHandler) ctx.getAttribute("_REQUEST_HANDLER_");
        // make and append the link
        writer.write(rh.makeLink(request, response, location));
    }

    public static void appendContentUrl(Writer writer, String location, HttpServletRequest request) throws IOException {
        StringBuffer buffer = new StringBuffer();
        ContentUrlTag.appendContentPrefix(request, buffer);
        writer.write(buffer.toString());
        writer.write(location);
    }

    public static void makeHyperlinkString(Writer writer, String linkStyle, String targetType, String target, String description, HttpServletRequest request, HttpServletResponse response, Map<String, Object> context, String targetWindow, String event, String action) throws IOException {
        if (UtilValidate.isNotEmpty(description)) {
            writer.write("<a");

            if (UtilValidate.isNotEmpty(linkStyle)) {
                writer.write(" class=\"");
                writer.write(linkStyle);
                writer.write("\"");
            }

            writer.write(" href=\"");

            buildHyperlinkUrl(writer, target, targetType, request, response, context);

            writer.write("\"");
            
            if (UtilValidate.isNotEmpty(targetWindow)) {
                writer.write(" target=\"");
                writer.write(targetWindow);
                writer.write("\"");
            }

            if (UtilValidate.isNotEmpty(event) && UtilValidate.isNotEmpty(action)) {
                writer.write(" ");
                writer.write(event);
                writer.write("=\"");
                writer.write(action);
                writer.write('"');
            }

            writer.write('>');

            writer.write(description);
            writer.write("</a>");
        }
    }
}

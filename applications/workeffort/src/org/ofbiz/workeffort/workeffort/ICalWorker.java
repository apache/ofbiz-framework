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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import javolution.util.FastList;
import javolution.util.FastMap;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilHttp;
import org.ofbiz.base.util.UtilJ2eeCompat;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilProperties;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.base.util.UtilXml;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.service.GenericServiceException;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.service.ModelService;
import org.ofbiz.webapp.stats.VisitHandler;
import org.ofbiz.webapp.webdav.PropFindHelper;
import org.ofbiz.webapp.webdav.ResponseHelper;
import org.ofbiz.webapp.webdav.WebDavUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/** iCalendar worker class. This class handles the WebDAV requests and
 * delegates the calendar conversion tasks to <code>ICalConverter</code>.
 */
public class ICalWorker {
    public static final String module = ICalWorker.class.getName();
    
    @SuppressWarnings("unchecked")
    protected static Map<String, Object> createConversionContext(HttpServletRequest request) {
        Map<String, Object> context = FastMap.newInstance();
        Enumeration<String> attributeEnum = request.getAttributeNames();
        while (attributeEnum.hasMoreElements()) {
            String attributeName = attributeEnum.nextElement();
            context.put(attributeName, request.getAttribute(attributeName));
        }
        context.put("parameters", request.getParameterMap());
        context.put("locale", UtilHttp.getLocale(request));
        return context;
    }

    protected static Writer getWriter(HttpServletResponse response, ServletContext context) throws IOException {
        Writer writer = null;
        if (UtilJ2eeCompat.useOutputStreamNotWriter(context)) {
            ServletOutputStream ros = response.getOutputStream();
            writer = new OutputStreamWriter(ros, "UTF-8");
        } else {
            writer = response.getWriter();
        }
        return writer;
    }

    public static void handleGetRequest(HttpServletRequest request, HttpServletResponse response, ServletContext context) throws ServletException, IOException {
        setupRequest(request, response);
        String workEffortId = (String) request.getAttribute("workEffortId");
        if (workEffortId == null) {
            Debug.logInfo("[handleGetRequest] workEffortId missing", module);
            response.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }
        Debug.logInfo("[handleGetRequest] workEffortId = " + workEffortId, module);
        try {
            String calendar = ICalConverter.getICalendar(workEffortId, createConversionContext(request));
            if (calendar == null) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            response.setContentType("text/calendar");
            response.setStatus(HttpServletResponse.SC_OK);
            Writer writer = getWriter(response, context);
            writer.write(calendar);
            writer.close();
        } catch (Exception e) {
            Debug.logError(e, "[handleGetRequest] Error while sending calendar: ", module);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return;
        }
    }

    public static void handlePropFindRequest(HttpServletRequest request, HttpServletResponse response, ServletContext context) throws ServletException, IOException {
        setupRequest(request, response);
        String workEffortId = (String) request.getAttribute("workEffortId");
        if (workEffortId == null) {
            Debug.logInfo("[handlePropFindRequest] workEffortId missing", module);
            response.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }
        Debug.logInfo("[handlePropFindRequest] workEffortId = " + workEffortId, module);
        try {
            Document requestDocument = WebDavUtil.getDocumentFromRequest(request);
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            UtilXml.writeXmlDocument(os, requestDocument, "UTF-8", true, true);
            if (Debug.verboseOn()) {
                Debug.logVerbose("[handlePropFindRequest] PROPFIND body:\r\n" + os.toString(), module);
            }
            PropFindHelper helper = new PropFindHelper(requestDocument);
            if (!helper.isAllProp() && !helper.isPropName()) {
                Document responseDocument = helper.getResponseDocument();
                List<Element> supportedProps = FastList.newInstance();
                List<Element> unSupportedProps = FastList.newInstance();
                List<Element> propElements = helper.getFindPropsList(ResponseHelper.DAV_NAMESPACE_URI);
                for (Element propElement : propElements) {
                    if ("getetag".equals(propElement.getLocalName())) {
                        Element etagElement = helper.createElementSetValue("D:getetag", String.valueOf(System.currentTimeMillis()));
                        supportedProps.add(etagElement);
                        continue;
                    }
                    if ("getlastmodified".equals(propElement.getLocalName())) {
                        Element lmElement = helper.createElementSetValue("D:getlastmodified", WebDavUtil.formatDate(WebDavUtil.RFC1123_DATE_FORMAT, new Date()));
                        supportedProps.add(lmElement);
                        continue;
                    }
                    unSupportedProps.add(responseDocument.createElementNS(propElement.getNamespaceURI(), propElement.getTagName()));
                }
                Element responseElement = helper.createResponseElement();
                responseElement.appendChild(helper.createHrefElement("/" + workEffortId + "/"));
                if (supportedProps.size() > 0) {
                    Element propElement = helper.createPropElement(supportedProps);
                    responseElement.appendChild(helper.createPropStatElement(propElement, ResponseHelper.STATUS_200));
                }
                if (unSupportedProps.size() > 0) {
                    Element propElement = helper.createPropElement(unSupportedProps);
                    responseElement.appendChild(helper.createPropStatElement(propElement, ResponseHelper.STATUS_404));
                }
                Element rootElement = helper.createMultiStatusElement();
                rootElement.appendChild(responseElement);
                responseDocument.appendChild(rootElement);
                os = new ByteArrayOutputStream();
                UtilXml.writeXmlDocument(os, responseDocument, "UTF-8", true, true);
                if (Debug.verboseOn()) {
                    Debug.logVerbose("[handlePropFindRequest] PROPFIND response:\r\n" + os.toString(), module);
                }
                ResponseHelper.prepareResponse(response, 207, "Multi-Status");
                Writer writer = getWriter(response, context);
                helper.writeResponse(writer);
                writer.close();
                return;
            }
        } catch (Exception e) {
            Debug.logError(e, "PROPFIND error: ", module);
        }
        response.setStatus(HttpServletResponse.SC_OK);
        response.flushBuffer();
    }

    public static void handlePutRequest(HttpServletRequest request, HttpServletResponse response, ServletContext context) throws ServletException, IOException {
        String contentType = request.getContentType();
        Debug.logInfo("[handlePutRequest] content type = " + contentType, module);
        if (contentType != null && !"text/calendar".equals(contentType)) {
            Debug.logInfo("[handlePutRequest] invalid content type", module);
            response.sendError(HttpServletResponse.SC_CONFLICT);
            return;
        }
        setupRequest(request, response);
        String workEffortId = (String) request.getAttribute("workEffortId");
        if (workEffortId == null) {
            Debug.logInfo("[handlePutRequest] workEffortId missing", module);
            response.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }
        Debug.logInfo("[handlePutRequest] workEffortId = " + workEffortId, module);
        try {
            ICalConverter.storeCalendar(request.getInputStream(), createConversionContext(request));
        } catch (Exception e) {
            Debug.logError(e, "[handlePutRequest] Error while updating calendar: ", module);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return;
        }
        response.setStatus(HttpServletResponse.SC_OK);
    }

    protected static void logInUser(HttpServletRequest request, HttpServletResponse response) throws GenericServiceException, GenericEntityException {
        GenericValue userLogin = null;
        String username = request.getParameter("USERNAME");
        String password = request.getParameter("PASSWORD");
        if (UtilValidate.isEmpty(username) || UtilValidate.isEmpty(password)) {
            return;
        }
        if ("true".equalsIgnoreCase(UtilProperties.getPropertyValue("security.properties", "username.lowercase"))) {
            username = username.toLowerCase();
        }
        if ("true".equalsIgnoreCase(UtilProperties.getPropertyValue("security.properties", "password.lowercase"))) {
            password = password.toLowerCase();
        }
        HttpSession session = request.getSession();
        LocalDispatcher dispatcher = (LocalDispatcher) request.getAttribute("dispatcher");
        Map<String, Object> result = dispatcher.runSync("userLogin", UtilMisc.toMap("login.username", username, "login.password", password, "locale", UtilHttp.getLocale(request)));
        if (ModelService.RESPOND_SUCCESS.equals(result.get(ModelService.RESPONSE_MESSAGE))) {
            userLogin = (GenericValue) result.get("userLogin");
            request.setAttribute("userLogin", userLogin);
            session.setAttribute("userLogin", userLogin);
            VisitHandler.getVisitor(request, response);
        } else {
            return;
        }
        GenericValue person = userLogin.getRelatedOne("Person");
        GenericValue partyGroup = userLogin.getRelatedOne("PartyGroup");
        if (person != null) request.setAttribute("person", person);
        if (partyGroup != null) request.setAttribute("partyGroup", partyGroup);
    }

    protected static void setupRequest(HttpServletRequest request, HttpServletResponse response) {
        String path = request.getPathInfo();
        if (UtilValidate.isEmpty(path)) {
            path = "/";
        }
        String workEffortId = path.substring(1);
        if (workEffortId.contains("/")) {
            workEffortId = workEffortId.substring(0, workEffortId.indexOf("/"));
        }
        if (workEffortId.length() < 1) {
            return;
        }
        request.setAttribute("workEffortId", workEffortId);
        try {
            logInUser(request, response);
        } catch (Exception e) {
            Debug.logError(e, "Error while logging in user: ", module);
        }
    }
}

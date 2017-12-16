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

package org.apache.ofbiz.workeffort.workeffort;

import java.io.IOException;
import java.io.Writer;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilGenerics;
import org.apache.ofbiz.base.util.UtilHttp;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.base.util.UtilXml;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.service.ServiceUtil;
import org.apache.ofbiz.webapp.stats.VisitHandler;
import org.apache.ofbiz.webapp.webdav.PropFindHelper;
import org.apache.ofbiz.webapp.webdav.ResponseHelper;
import org.apache.ofbiz.webapp.webdav.WebDavUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

/** iCalendar worker class. This class handles the WebDAV requests and
 * delegates the calendar conversion tasks to <code>ICalConverter</code>.
 */
public final class ICalWorker {

    public static final String module = ICalWorker.class.getName();

    private ICalWorker() {};

    public static final class ResponseProperties {
        public final int statusCode;
        public final String statusMessage;
        public ResponseProperties(int statusCode, String statusMessage) {
            this.statusCode = statusCode;
            this.statusMessage = statusMessage;
        }
    }

    private static Map<String, Object> createConversionContext(HttpServletRequest request) {
        Map<String, Object> context = new HashMap<>();
        Enumeration<String> attributeEnum = UtilGenerics.cast(request.getAttributeNames());
        while (attributeEnum.hasMoreElements()) {
            String attributeName = attributeEnum.nextElement();
            context.put(attributeName, request.getAttribute(attributeName));
        }
        context.put("parameters", request.getParameterMap());
        context.put("locale", UtilHttp.getLocale(request));
        return context;
    }

    /** Create an HTTP Forbidden response. The calendar converter will use this
     * response when a user is logged in, but they don't have the basic CRUD
     * permissions to perform an action. Returning a Forbidden status will
     * prevent the client from trying the operation again.
     *
     * @param statusMessage Optional status message - usually <code>null</code>
     * for security reasons
     * @return Create an HTTP Forbidden response
     */
    public static ResponseProperties createForbiddenResponse(String statusMessage) {
        return new ResponseProperties(HttpServletResponse.SC_FORBIDDEN, statusMessage);
    }

    /** Create an HTTP Unauthorized response. The calendar converter will use this
     * response when a user is not logged in, and basic CRUD permissions are
     * needed to perform an action. Returning an Unauthorized status will
     * force the client to authenticate the user, then try the operation again.
     *
     * @param statusMessage Optional status message - usually <code>null</code>
     * for security reasons
     * @return Create an HTTP Unauthorized response
     */
    public static ResponseProperties createNotAuthorizedResponse(String statusMessage) {
        return new ResponseProperties(HttpServletResponse.SC_UNAUTHORIZED, statusMessage);
    }

    public static ResponseProperties createNotFoundResponse(String statusMessage) {
        return new ResponseProperties(HttpServletResponse.SC_NOT_FOUND, statusMessage);
    }

    public static ResponseProperties createOkResponse(String statusMessage) {
        return new ResponseProperties(HttpServletResponse.SC_OK, statusMessage);
    }

    /** Create an HTTP Partial Content response. The calendar converter will use this
     * response when a calendar is only partially updated.
     *
     * @param statusMessage A message describing which calendar components were
     * not updated
     * @return Create an HTTP Partial Content response.
     */
    public static ResponseProperties createPartialContentResponse(String statusMessage) {
        return new ResponseProperties(HttpServletResponse.SC_PARTIAL_CONTENT, statusMessage);
    }

    private static Date getLastModifiedDate(HttpServletRequest request) throws GenericEntityException {
        String workEffortId = (String) request.getAttribute("workEffortId");
        Delegator delegator = (Delegator) request.getAttribute("delegator");
        GenericValue publishProperties = EntityQuery.use(delegator).from("WorkEffort").where("workEffortId", workEffortId).queryOne();
        GenericValue iCalData = publishProperties.getRelatedOne("WorkEffortIcalData", false);
        if (iCalData != null) {
            return iCalData.getTimestamp("lastUpdatedStamp");
        }
        return publishProperties.getTimestamp("lastUpdatedStamp");
    }

    public static void handleGetRequest(HttpServletRequest request, HttpServletResponse response, ServletContext context) throws IOException {
        if (!isValidRequest(request, response)) {
            return;
        }
        String workEffortId = (String) request.getAttribute("workEffortId");
        Debug.logInfo("[handleGetRequest] workEffortId = " + workEffortId, module);
        ResponseProperties responseProps = null;
        try {
            responseProps = ICalConverter.getICalendar(workEffortId, createConversionContext(request));
        } catch (Exception e) {
            Debug.logError(e, "[handleGetRequest] Error while sending calendar: ", module);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return;
        }
        if (responseProps.statusCode == HttpServletResponse.SC_OK) {
            response.setContentType("text/calendar");
        }
        writeResponse(responseProps, request, response, context);
    }

    public static void handlePropFindRequest(HttpServletRequest request, HttpServletResponse response, ServletContext context) throws IOException {
        if (!isValidRequest(request, response)) {
            return;
        }
        String workEffortId = (String) request.getAttribute("workEffortId");
        Debug.logInfo("[handlePropFindRequest] workEffortId = " + workEffortId, module);
        try {
            Document requestDocument = WebDavUtil.getDocumentFromRequest(request);
            if (Debug.verboseOn()) {
                Debug.logVerbose("[handlePropFindRequest] PROPFIND body:\r\n" + UtilXml.writeXmlDocument(requestDocument), module);
            }
            PropFindHelper helper = new PropFindHelper(requestDocument);
            if (!helper.isAllProp() && !helper.isPropName()) {
                Document responseDocument = helper.getResponseDocument();
                List<Element> supportedProps = new LinkedList<>();
                List<Element> unSupportedProps = new LinkedList<>();
                List<Element> propElements = helper.getFindPropsList(ResponseHelper.DAV_NAMESPACE_URI);
                for (Element propElement : propElements) {
                    if ("getetag".equals(propElement.getNodeName())) {
                        Element etagElement = helper.createElementSetValue("D:getetag", String.valueOf(System.currentTimeMillis()));
                        supportedProps.add(etagElement);
                        continue;
                    }
                    if ("getlastmodified".equals(propElement.getNodeName())) {
                        Date lastModified = getLastModifiedDate(request);
                        Element lmElement = helper.createElementSetValue("D:getlastmodified", WebDavUtil.formatDate(WebDavUtil.getRFC1123DateFormat(), lastModified));
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
                if (Debug.verboseOn()) {
                    Debug.logVerbose("[handlePropFindRequest] PROPFIND response:\r\n" + UtilXml.writeXmlDocument(responseDocument), module);
                }
                ResponseHelper.prepareResponse(response, 207, "Multi-Status");
                try (Writer writer = response.getWriter()) {
                    helper.writeResponse(response, writer);
                }
                return;
            }
        } catch (RuntimeException | GenericEntityException | SAXException | ParserConfigurationException e) {
            Debug.logError(e, "PROPFIND error: ", module);
        }
        response.setStatus(HttpServletResponse.SC_OK);
        response.flushBuffer();
    }

    public static void handlePutRequest(HttpServletRequest request, HttpServletResponse response, ServletContext context) throws IOException {
        if (!isValidRequest(request, response)) {
            return;
        }
        String contentType = request.getContentType();
        if (contentType != null && !"text/calendar".equals(contentType)) {
            Debug.logInfo("[handlePutRequest] invalid content type", module);
            response.sendError(HttpServletResponse.SC_CONFLICT);
            return;
        }
        String workEffortId = (String) request.getAttribute("workEffortId");
        Debug.logInfo("[handlePutRequest] workEffortId = " + workEffortId, module);
        ResponseProperties responseProps = null;
        try {
            responseProps = ICalConverter.storeCalendar(request.getInputStream(), createConversionContext(request));
        } catch (Exception e) {
            Debug.logError(e, "[handlePutRequest] Error while updating calendar: ", module);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return;
        }
        writeResponse(responseProps, request, response, context);
    }

    private static boolean isValidRequest(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (!request.isSecure()) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return false;
        }
        setupRequest(request, response);
        String workEffortId = (String) request.getAttribute("workEffortId");
        if (workEffortId == null) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return false;
        }
        return true;
    }

    private static void logInUser(HttpServletRequest request, HttpServletResponse response) throws GenericServiceException, GenericEntityException {
        Map<String, Object> serviceMap = WebDavUtil.getCredentialsFromRequest(request);
        if (serviceMap == null) {
            return;
        }
        serviceMap.put("locale", UtilHttp.getLocale(request));
        GenericValue userLogin = null;
        HttpSession session = request.getSession();
        LocalDispatcher dispatcher = (LocalDispatcher) request.getAttribute("dispatcher");
        Map<String, Object> result = dispatcher.runSync("userLogin", serviceMap);
        if (ServiceUtil.isError(result) || ServiceUtil.isFailure(result)) {
            return;
        }
        userLogin = (GenericValue) result.get("userLogin");
        request.setAttribute("userLogin", userLogin);
        session.setAttribute("userLogin", userLogin);
        VisitHandler.getVisitor(request, response);
        GenericValue person = userLogin.getRelatedOne("Person", false);
        if (person != null) {
            request.setAttribute("person", person);
        } else {
            GenericValue partyGroup = userLogin.getRelatedOne("PartyGroup", false);
            if (partyGroup != null) {
                request.setAttribute("partyGroup", partyGroup);
            }
        }
    }

    private static void setupRequest(HttpServletRequest request, HttpServletResponse response) {
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

    private static void writeResponse(ResponseProperties responseProps, HttpServletRequest request, HttpServletResponse response, ServletContext context) throws IOException {
        if (Debug.verboseOn()) {
            Debug.logVerbose("Returning response: code = " + responseProps.statusCode +
                    ", message = " + responseProps.statusMessage, module);
        }
        response.setStatus(responseProps.statusCode);
        if (responseProps.statusCode == HttpServletResponse.SC_UNAUTHORIZED) {
            response.setHeader("WWW-Authenticate", "Basic realm=\"OFBiz iCalendar " + request.getAttribute("workEffortId") + "\"");
        }
        if (responseProps.statusMessage != null) {
            response.setContentLength(responseProps.statusMessage.length());
            try (Writer writer = response.getWriter()) {
                writer.write(responseProps.statusMessage);
            }
        }
    }
}

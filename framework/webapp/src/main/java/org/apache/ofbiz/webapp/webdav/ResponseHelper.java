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
package org.apache.ofbiz.webapp.webdav;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Writer;

import javax.servlet.http.HttpServletResponse;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilXml;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/** WebDAV response helper class. This class provides helper methods for
 * working with WebDAV requests and responses.*/
public class ResponseHelper {

    private static final String MODULE = ResponseHelper.class.getName();

    public static final String DAV_NAMESPACE_URI = "DAV:";
    public static final String STATUS_200 = "HTTP/1.1 200 OK";
    public static final String STATUS_400 = "HTTP/1.1 400 Bad Request";
    public static final String STATUS_401 = "HTTP/1.1 401 Unauthorized";
    public static final String STATUS_403 = "HTTP/1.1 403 Forbidden";
    public static final String STATUS_404 = "HTTP/1.1 404 Not Found";

    public static void prepareResponse(HttpServletResponse response, int statusCode, String statusString) {
        response.setContentType("application/xml");
        response.setCharacterEncoding("UTF-8");
        if (statusString == null) {
            response.setStatus(statusCode);
        } else {
            try {
                response.sendError(statusCode, statusString);
            } catch (IOException e) {
                Debug.logError(e, MODULE);
            }
        }
    }

    private final Document responseDocument;

    public ResponseHelper() {
        this.responseDocument = UtilXml.makeEmptyXmlDocument();
    }

    /**
     * Create element set value element.
     * @param elementName the element name
     * @param value the value
     * @return the element
     */
    public Element createElementSetValue(String elementName, String value) {
        Element element = this.responseDocument.createElementNS(DAV_NAMESPACE_URI, elementName);
        element.appendChild(element.getOwnerDocument().createTextNode(value));
        element.setNodeValue(value);
        return element;
    }

    /**
     * Create href element element.
     * @param hrefUrl the href url
     * @return the element
     */
    public Element createHrefElement(String hrefUrl) {
        return createElementSetValue("D:href", hrefUrl);
    }

    /**
     * Create multi status element element.
     * @return the element
     */
    public Element createMultiStatusElement() {
        return this.responseDocument.createElementNS(DAV_NAMESPACE_URI, "D:multistatus");
    }

    /**
     * Create response description element element.
     * @param description the description
     * @param lang the lang
     * @return the element
     */
    public Element createResponseDescriptionElement(String description, String lang) {
        Element element = createElementSetValue("D:responsedescription", description);
        if (lang != null) {
            element.setAttribute("xml:lang", lang);
        }
        return element;
    }

    /**
     * Create response element element.
     * @return the element
     */
    public Element createResponseElement() {
        return this.responseDocument.createElementNS(DAV_NAMESPACE_URI, "D:response");
    }

    /**
     * Create status element element.
     * @param statusText the status text
     * @return the element
     */
    public Element createStatusElement(String statusText) {
        return createElementSetValue("D:status", statusText);
    }

    /**
     * Gets response document.
     * @return the response document
     */
    public Document getResponseDocument() {
        return this.responseDocument;
    }

    /**
     * Write response.
     * @param response the response
     * @param writer the writer
     * @throws IOException the io exception
     */
    public void writeResponse(HttpServletResponse response, Writer writer) throws IOException {
        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            UtilXml.writeXmlDocument(os, this.responseDocument, "UTF-8", true, true);
            response.setContentLength(os.size());
            writer.write(os.toString("UTF-8"));
        } catch (Exception e) {
            throw new IOException(e.getMessage());
        }
    }
}

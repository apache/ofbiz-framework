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

    public static final String module = ResponseHelper.class.getName();
    
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
                Debug.logError(e, module);
            }
        }
    }

    protected final Document responseDocument;

    public ResponseHelper() {
        this.responseDocument = UtilXml.makeEmptyXmlDocument();
    }

    public Element createElementSetValue(String elementName, String value) {
        Element element = this.responseDocument.createElementNS(DAV_NAMESPACE_URI, elementName);
        element.appendChild(element.getOwnerDocument().createTextNode(value));
        element.setNodeValue(value);
        return element;
    }

    public Element createHrefElement(String hrefUrl) {
        return createElementSetValue("D:href", hrefUrl);
    }

    public Element createMultiStatusElement() {
        return this.responseDocument.createElementNS(DAV_NAMESPACE_URI, "D:multistatus");
    }

    public Element createResponseDescriptionElement(String description, String lang) {
        Element element = createElementSetValue("D:responsedescription", description);
        if (lang != null) {
            element.setAttribute("xml:lang", lang);
        }
        return element;
    }

    public Element createResponseElement() {
        return this.responseDocument.createElementNS(DAV_NAMESPACE_URI, "D:response");
    }

    public Element createStatusElement(String statusText) {
        return createElementSetValue("D:status", statusText);
    }

    public Document getResponseDocument() {
        return this.responseDocument;
    }

    public void writeResponse(HttpServletResponse response, Writer writer) throws IOException {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        try {
            UtilXml.writeXmlDocument(os, this.responseDocument, "UTF-8", true, true);
        } catch (Exception e) {
            throw new IOException(e.getMessage());
        }
        response.setContentLength(os.size());
        writer.write(os.toString("UTF-8"));
    }
}

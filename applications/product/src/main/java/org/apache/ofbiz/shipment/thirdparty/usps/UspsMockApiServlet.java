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

package org.apache.ofbiz.shipment.thirdparty.usps;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.transform.TransformerException;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.base.util.UtilXml;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * USPS Webtools API Mock API Servlet
 */
@SuppressWarnings("serial")
public class UspsMockApiServlet extends HttpServlet {

    public static final String module = UspsMockApiServlet.class.getName();


    public UspsMockApiServlet() {
        super();
    }

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doGet(request, response);
    }

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        // we're only testing the Rate API right now
        if (!"Rate".equals(request.getParameter("API"))) {
            Debug.logError("Unsupported API [" + request.getParameter("API") + "]", module);
            return;
        }

        String xmlValue = request.getParameter("XML");
        Document requestDocument = null;
        try {
            requestDocument = UtilXml.readXmlDocument(xmlValue, false);
        } catch (Exception e) {
            Debug.logError(e, module);
            return;
        }

        if (requestDocument == null) {
            Debug.logError("In UspsMockApiSerlvet No XML document found in request, quiting now; XML parameter is: " + xmlValue, module);
            return;
        }

        List<? extends Element> packageElementList = UtilXml.childElementList(requestDocument.getDocumentElement(), "Package");
        if (UtilValidate.isNotEmpty(packageElementList)) {

            Document responseDocument = UtilXml.makeEmptyXmlDocument("RateResponse");
            for (Element packageElement: packageElementList) {
                Element responsePackageElement =
                        UtilXml.addChildElement(responseDocument.getDocumentElement(), "Package", responseDocument);
                responsePackageElement.setAttribute("ID", packageElement.getAttribute("ID"));

                UtilXml.addChildElementValue(responsePackageElement, "ZipOrigination",
                        UtilXml.childElementValue(packageElement, "ZipOrigination"), responseDocument);

                UtilXml.addChildElementValue(responsePackageElement, "ZipDestination",
                        UtilXml.childElementValue(packageElement, "ZipDestination"), responseDocument);

                UtilXml.addChildElementValue(responsePackageElement, "Pounds",
                        UtilXml.childElementValue(packageElement, "Pounds"), responseDocument);

                UtilXml.addChildElementValue(responsePackageElement, "Ounces",
                        UtilXml.childElementValue(packageElement, "Ounces"), responseDocument);

                UtilXml.addChildElementValue(responsePackageElement, "Container",
                        UtilXml.childElementValue(packageElement, "Container"), responseDocument);

                UtilXml.addChildElementValue(responsePackageElement, "Size",
                        UtilXml.childElementValue(packageElement, "Size"), responseDocument);

                UtilXml.addChildElementValue(responsePackageElement, "Zone", "1", responseDocument);
                UtilXml.addChildElementValue(responsePackageElement, "Postage", "3.00", responseDocument);
            }

            ByteArrayOutputStream os = new ByteArrayOutputStream();

            try {
                UtilXml.writeXmlDocument(responseDocument, os, "UTF-8", true, false, 0);
            } catch (TransformerException e) {
                Debug.logInfo(e, module);
                return;
            }

            response.setContentType("text/xml");
            ServletOutputStream sos = response.getOutputStream();
            sos.print(os.toString("UTF-8"));
            sos.flush();
        }
    }
}

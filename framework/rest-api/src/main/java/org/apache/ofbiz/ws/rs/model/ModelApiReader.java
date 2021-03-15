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
package org.apache.ofbiz.ws.rs.model;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilXml;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

public final class ModelApiReader {
    private static final String MODULE = ModelApiReader.class.getName();

    private ModelApiReader() {

    }
    public static ModelApi getModelApi(final File apiDef) {
        Element docElement;
        try {
            docElement = UtilXml.readXmlDocument(new FileInputStream(apiDef), true, "REST API file", true)
                    .getDocumentElement();
        } catch (SAXException | ParserConfigurationException | IOException e) {
            e.printStackTrace();
            return null;
        }
        docElement.normalize();
        ModelApi api = new ModelApi();
        api.setDisplayName(UtilXml.checkEmpty(docElement.getAttribute("displayName")).intern());
        api.setName(UtilXml.checkEmpty(docElement.getAttribute("name")).intern());
        api.setDescription(UtilXml.checkEmpty(docElement.getAttribute("description")).intern());
        api.setPublish(Boolean.parseBoolean(UtilXml.checkEmpty(docElement.getAttribute("publish")).intern()));
        for (Element resourceEle : UtilXml.childElementList(docElement, "resource")) {
            createModelResource(resourceEle, api);
        }
        return api;
    }

    private static void createModelResource(Element resourceEle, ModelApi modelApi) {
        ModelResource resource = new ModelResource().name(UtilXml.checkEmpty(resourceEle.getAttribute("name")).intern())
                .description(UtilXml.checkEmpty(resourceEle.getAttribute("description")).intern())
                .displayName(UtilXml.checkEmpty(resourceEle.getAttribute("displayName")).intern())
                .path(UtilXml.checkEmpty(resourceEle.getAttribute("path")).intern())
                .publish(Boolean.parseBoolean(UtilXml.checkEmpty(resourceEle.getAttribute("publish")).intern()))
                .auth(Boolean.parseBoolean(UtilXml.checkEmpty(resourceEle.getAttribute("auth")).intern()));
        createOperations(resourceEle, resource);
        Debug.logInfo(resource.toString(), MODULE);
        modelApi.addResource(resource);
    }

    private static void createOperations(Element resourceEle, ModelResource resource) {
        for (Element operationEle : UtilXml.childElementList(resourceEle, "operation")) {
            Element serviceEle = UtilXml.firstChildElement(operationEle, "service");
            String serviceName = UtilXml.checkEmpty(serviceEle.getAttribute("name")).intern();
            ModelOperation op = new ModelOperation()
                    .path(UtilXml.checkEmpty(operationEle.getAttribute("path")).intern())
                    .verb(UtilXml.checkEmpty(operationEle.getAttribute("verb")).intern()).service(serviceName)
                    .produces(UtilXml.checkEmpty(operationEle.getAttribute("produces")).intern())
                    .consumes(UtilXml.checkEmpty(operationEle.getAttribute("consumes")).intern())
                    .description(UtilXml.checkEmpty(operationEle.getAttribute("description")).intern())
                    .auth(Boolean.parseBoolean(UtilXml.checkEmpty(operationEle.getAttribute("auth")).intern()));
            resource.addOperation(op);
        }
    }

}

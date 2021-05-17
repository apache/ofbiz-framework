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
package org.apache.ofbiz.webtools;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.ofbiz.base.util.UtilHttp;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilProperties;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.base.util.UtilXml;
import org.apache.ofbiz.entity.datasource.GenericDAO;
import org.apache.ofbiz.entity.datasource.GenericHelperInfo;
import org.apache.ofbiz.entity.model.ModelEntity;
import org.w3c.dom.Comment;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class WebToolsDbEvents {
    private static final String MODULE = WebToolsDbEvents.class.getName();
    private static final String RESOURCE = "WebtoolsUiLabels";
    private static Document document;
    private static final String TITLE = "Entities of an Apache Open For Business Project (Apache OFBiz) Component";
    private static final String DESCRIPTION = "None";
    private static final String COPYRIGHT = String.format("Copyright 2001-%d The Apache Software Foundation",
            LocalDate.now().getYear());
    private static final String AUTHOR = "None";
    private static final String VERSION = "1.0";
    private static final String SEPARATOR = "=========================================================";
    private static final String INTRO = "The modules in this file are as follows:";
    private static final String DEFAULTS = "======================== Defaults =======================";
    private static final String HEADER = "======================== Data Model =====================";
    private static final String XMLN_NAME = "xmlns:xsi";
    private static final String XMLN_VALUE = "http://www.w3.org/2001/XMLSchema-instance";
    private static final String XSI_NAME = "xsi:noNamespaceSchemaLocation";
    private static final String XSI_VALUE = "http://ofbiz.apache.org/dtds/entitymodel.xsd";

    /**
     * Indexes the Datasource defined in the entityengine.xml
     *
     * @param request
     * @param response
     * @return
     */
    public static String modelInduceFromDb(HttpServletRequest request, HttpServletResponse response) {
        List<String> errorMessageList = new ArrayList<>();
        Map<String, Object> params = UtilHttp.getParameterMap(request);
        Locale locale = UtilHttp.getLocale(request);

        String induceType = (String) params.get("induceType");
        String datasourceName = (String) params.get("datasourceName");
        String packageName = (String) params.get("packageName");

        if (UtilValidate.isEmpty(induceType)) {
            errorMessageList.add(UtilProperties.getMessage(RESOURCE, "ModelInduceInduceTypeError", locale));
        }

        if (UtilValidate.isEmpty(datasourceName)) {
            errorMessageList.add(UtilProperties.getMessage(RESOURCE, "ModelInduceDatasourceNameError", locale));
        }

        if ("entitygroup".equals(induceType) && UtilValidate.isEmpty(packageName)) {
            errorMessageList.add(UtilProperties.getMessage(RESOURCE, "ModelInduceEntityGroupError", locale));
        }

        if (UtilValidate.isNotEmpty(errorMessageList)) {
            request.setAttribute("errorMessageList", errorMessageList);
            return "error";
        }

        document = UtilXml.makeEmptyXmlDocument();

        Element entitymodel = document.createElement("entitymodel");
        entitymodel.setAttribute(XMLN_NAME, XMLN_VALUE);
        entitymodel.setAttribute(XSI_NAME, XSI_VALUE);
        Comment licenceDisclaimer = UtilXml.createApacheLicenceComment(document);
        document.appendChild(licenceDisclaimer);
        document.appendChild(entitymodel);

        entitymodel.appendChild(document.createComment(SEPARATOR));
        entitymodel.appendChild(document.createComment(DEFAULTS));
        entitymodel.appendChild(document.createComment(SEPARATOR));

        if ("entitymodel".equals(induceType)) {
            UtilXml.addChildElementValue(entitymodel, "title", TITLE, document);
            UtilXml.addChildElementValue(entitymodel, "description", DESCRIPTION, document);
            UtilXml.addChildElementValue(entitymodel, "copyright", COPYRIGHT, document);
            UtilXml.addChildElementValue(entitymodel, "author", AUTHOR, document);
            UtilXml.addChildElementValue(entitymodel, "version", VERSION, document);

            entitymodel
                    .appendChild(document.createComment(SEPARATOR));
            entitymodel
                    .appendChild(document.createComment(HEADER));
            entitymodel.appendChild(document.createComment(INTRO));
            entitymodel
                    .appendChild(document.createComment(SEPARATOR));
        }
        GenericDAO dao = GenericDAO.getGenericDAO(new GenericHelperInfo(null, datasourceName));
        List<ModelEntity> newEntList = dao.induceModelFromDb(new ArrayList<String>());
        if (UtilValidate.isEmpty(newEntList)) {
            request.setAttribute("errorMessageList",
                    UtilMisc.toList(UtilProperties.getMessage(RESOURCE, "ModelInduceDataStructureError", locale)));
            return "error";
        }

        for (ModelEntity entity : newEntList) {
            if ("entitymodel".equals(induceType)) {
                entitymodel.appendChild(entity.toXmlElement(document, packageName));
            } else {
                entitymodel.appendChild(entity.toGroupXmlElement(document, packageName));
            }
        }

        request.setAttribute("inducedText", UtilXml.convertDocumentToXmlString(document));
        return "success";
    }
}

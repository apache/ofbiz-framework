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
package org.apache.ofbiz.widget.model;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.ofbiz.base.location.FlexibleLocation;
import org.apache.ofbiz.base.util.UtilHttp;
import org.apache.ofbiz.base.util.UtilXml;
import org.apache.ofbiz.base.util.cache.UtilCache;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.model.ModelReader;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.widget.renderer.VisualTheme;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

/**
 * Widget Library - Form factory class
 */
public class FormFactory {

    private static final String MODULE = FormFactory.class.getName();
    private static final UtilCache<String, ModelForm> FORM_LOCATION_CACHE =
            UtilCache.createUtilCache("widget.form.locationResource", 0, 0, false);
    private static final UtilCache<String, ModelForm> FORM_WEBAPP_CACHE =
            UtilCache.createUtilCache("widget.form.webappResource", 0, 0, false);

    public static Map<String, ModelForm> getFormsFromLocation(String resourceName, ModelReader entityModelReader,
                                                VisualTheme visualTheme, DispatchContext dispatchContext)
            throws IOException, SAXException, ParserConfigurationException {
        URL formFileUrl = FlexibleLocation.resolveLocation(resourceName);
        Document formFileDoc = UtilXml.readXmlDocument(formFileUrl, true, true);
        return readFormDocument(formFileDoc, entityModelReader, visualTheme, dispatchContext, resourceName);
    }

    public static ModelForm getFormFromLocation(String resourceName, String formName, ModelReader entityModelReader,
                                                VisualTheme visualTheme, DispatchContext dispatchContext)
            throws IOException, SAXException, ParserConfigurationException {
        StringBuilder sb = new StringBuilder(dispatchContext.getDelegator().getDelegatorName());
        sb.append(":").append(resourceName).append("#").append(formName).append(visualTheme.getVisualThemeId());
        String cacheKey = sb.toString();
        ModelForm modelForm = FORM_LOCATION_CACHE.get(cacheKey);
        if (modelForm == null) {
            URL formFileUrl = FlexibleLocation.resolveLocation(resourceName);
            Document formFileDoc = UtilXml.readXmlDocument(formFileUrl, true, true);
            if (formFileDoc == null) {
                throw new IllegalArgumentException("Could not find resource [" + resourceName + "]");
            }
            modelForm = createModelForm(formFileDoc, entityModelReader, visualTheme, dispatchContext, resourceName, formName);
            modelForm = FORM_LOCATION_CACHE.putIfAbsentAndGet(cacheKey, modelForm);
        }
        if (modelForm == null) {
            throw new IllegalArgumentException("Could not find form with name [" + formName + "] in class resource [" + resourceName + "]");
        }
        return modelForm;
    }

    public static ModelForm getFormFromWebappContext(String resourceName, String formName, HttpServletRequest request)
            throws IOException, SAXException, ParserConfigurationException {
        String webappName = UtilHttp.getApplicationName(request);
        VisualTheme visualTheme = UtilHttp.getVisualTheme(request);
        String cacheKey = new StringBuilder().append(webappName)
                .append("::")
                .append(resourceName)
                .append("::")
                .append(formName)
                .append("::")
                .append(visualTheme.getVisualThemeId())
                .toString();
        ModelForm modelForm = FORM_WEBAPP_CACHE.get(cacheKey);
        if (modelForm == null) {
            Delegator delegator = (Delegator) request.getAttribute("delegator");
            LocalDispatcher dispatcher = (LocalDispatcher) request.getAttribute("dispatcher");
            URL formFileUrl = request.getServletContext().getResource(resourceName);
            Document formFileDoc = UtilXml.readXmlDocument(formFileUrl, true, true);
            Element formElement = UtilXml.firstChildElement(formFileDoc.getDocumentElement(), "form", "name", formName);
            modelForm = createModelForm(formElement, delegator.getModelReader(), visualTheme, dispatcher.getDispatchContext(),
                    resourceName, formName);
            modelForm = FORM_WEBAPP_CACHE.putIfAbsentAndGet(cacheKey, modelForm);
        }
        if (modelForm == null) {
            throw new IllegalArgumentException("Could not find form with name [" + formName + "] in webapp resource [" + resourceName
                    + "] in the webapp [" + webappName + "]");
        }
        return modelForm;
    }

    public static Map<String, ModelForm> readFormDocument(Document formFileDoc, ModelReader entityModelReader,
                                                VisualTheme visualTheme, DispatchContext dispatchContext, String formLocation) {
        Map<String, ModelForm> modelFormMap = new HashMap<>();
        if (formFileDoc != null) {
            // read document and construct ModelForm for each form element
            Element rootElement = formFileDoc.getDocumentElement();
            if (!"forms".equalsIgnoreCase(rootElement.getTagName())) {
                rootElement = UtilXml.firstChildElement(rootElement, "forms");
            }
            List<? extends Element> formElements = UtilXml.childElementList(rootElement, "form");
            for (Element formElement : formElements) {
                String formName = formElement.getAttribute("name");
                String cacheKey = new StringBuilder().append(formLocation)
                        .append("#")
                        .append(formName)
                        .append(visualTheme.getVisualThemeId())
                        .toString();
                ModelForm modelForm = FORM_LOCATION_CACHE.get(cacheKey);
                if (modelForm == null) {
                    modelForm = createModelForm(formElement, entityModelReader, visualTheme, dispatchContext, formLocation, formName);
                    modelForm = FORM_LOCATION_CACHE.putIfAbsentAndGet(cacheKey, modelForm);
                }
                modelFormMap.put(formName, modelForm);
            }
        }
        return modelFormMap;
    }

    public static ModelForm createModelForm(Document formFileDoc, ModelReader entityModelReader, VisualTheme visualTheme,
                                            DispatchContext dispatchContext, String formLocation, String formName) {
        Element rootElement = formFileDoc.getDocumentElement();
        if (!"forms".equalsIgnoreCase(rootElement.getTagName())) {
            rootElement = UtilXml.firstChildElement(rootElement, "forms");
        }
        Element formElement = UtilXml.firstChildElement(rootElement, "form", "name", formName);
        return createModelForm(formElement, entityModelReader, visualTheme, dispatchContext, formLocation, formName);
    }

    public static ModelForm createModelForm(Element formElement, ModelReader entityModelReader, VisualTheme visualTheme,
                                            DispatchContext dispatchContext, String formLocation, String formName) {
        String formType = formElement.getAttribute("type");
        if (formType.isEmpty() || "single".equals(formType) || "upload".equals(formType)) {
            return new ModelSingleForm(formElement, formLocation, entityModelReader, visualTheme, dispatchContext);
        }
        return new ModelGrid(formElement, formLocation, entityModelReader, visualTheme, dispatchContext);
    }
}

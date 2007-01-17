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
package org.ofbiz.widget.form;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.xml.parsers.ParserConfigurationException;

import org.ofbiz.base.location.FlexibleLocation;
import org.ofbiz.base.util.UtilHttp;
import org.ofbiz.base.util.UtilXml;
import org.ofbiz.base.util.cache.UtilCache;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.service.LocalDispatcher;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;


/**
 * Widget Library - Form factory class
 */
public class FormFactory {
    
    public static final String module = FormFactory.class.getName();

    public static final UtilCache formLocationCache = new UtilCache("widget.form.locationResource", 0, 0, false);
    public static final UtilCache formWebappCache = new UtilCache("widget.form.webappResource", 0, 0, false);
    
    public static ModelForm getFormFromLocation(String resourceName, String formName, GenericDelegator delegator, LocalDispatcher dispatcher) 
            throws IOException, SAXException, ParserConfigurationException {
        Map modelFormMap = (Map) formLocationCache.get(resourceName);
        if (modelFormMap == null) {
            synchronized (FormFactory.class) {
                modelFormMap = (Map) formLocationCache.get(resourceName);
                if (modelFormMap == null) {
                    ClassLoader loader = Thread.currentThread().getContextClassLoader();
                    if (loader == null) {
                        loader = FormFactory.class.getClassLoader();
                    }
                    
                    URL formFileUrl = null;
                    formFileUrl = FlexibleLocation.resolveLocation(resourceName); //, loader);
                    Document formFileDoc = UtilXml.readXmlDocument(formFileUrl, true);
                    modelFormMap = readFormDocument(formFileDoc, delegator, dispatcher);
                    formLocationCache.put(resourceName, modelFormMap);
                }
            }
        }
        
        ModelForm modelForm = (ModelForm) modelFormMap.get(formName);
        if (modelForm == null) {
            throw new IllegalArgumentException("Could not find form with name [" + formName + "] in class resource [" + resourceName + "]");
        }
        return modelForm;
    }
    
    public static ModelForm getFormFromWebappContext(String resourceName, String formName, HttpServletRequest request) 
            throws IOException, SAXException, ParserConfigurationException {
        String webappName = UtilHttp.getApplicationName(request);
        String cacheKey = webappName + "::" + resourceName;
        
        
        Map modelFormMap = (Map) formWebappCache.get(cacheKey);
        if (modelFormMap == null) {
            synchronized (FormFactory.class) {
                modelFormMap = (Map) formWebappCache.get(cacheKey);
                if (modelFormMap == null) {
                    ServletContext servletContext = (ServletContext) request.getAttribute("servletContext");
                    GenericDelegator delegator = (GenericDelegator) request.getAttribute("delegator");
                    LocalDispatcher dispatcher = (LocalDispatcher) request.getAttribute("dispatcher");
                    
                    URL formFileUrl = servletContext.getResource(resourceName);
                    Document formFileDoc = UtilXml.readXmlDocument(formFileUrl, true);
                    modelFormMap = readFormDocument(formFileDoc, delegator, dispatcher);
                    formWebappCache.put(cacheKey, modelFormMap);
                }
            }
        }
        
        ModelForm modelForm = (ModelForm) modelFormMap.get(formName);
        if (modelForm == null) {
            throw new IllegalArgumentException("Could not find form with name [" + formName + "] in webapp resource [" + resourceName + "] in the webapp [" + webappName + "]");
        }
        return modelForm;
    }
    
    public static Map readFormDocument(Document formFileDoc, GenericDelegator delegator, LocalDispatcher dispatcher) {
        Map modelFormMap = new HashMap();
        if (formFileDoc != null) {
            // read document and construct ModelForm for each form element
            Element rootElement = formFileDoc.getDocumentElement();
            List formElements = UtilXml.childElementList(rootElement, "form");
            Iterator formElementIter = formElements.iterator();
            while (formElementIter.hasNext()) {
                Element formElement = (Element) formElementIter.next();
                ModelForm modelForm = new ModelForm(formElement, delegator, dispatcher);
                modelFormMap.put(modelForm.getName(), modelForm);
            }
        }
        return modelFormMap;
    }
}

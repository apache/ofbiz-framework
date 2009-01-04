/*
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
 */
package org.ofbiz.webtools.labelmanager;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.ofbiz.base.component.ComponentConfig;
import org.ofbiz.base.util.FileUtil;
import org.ofbiz.base.util.GeneralException;
import org.ofbiz.base.util.UtilGenerics;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilProperties;
import org.ofbiz.base.util.UtilXml;

import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.base.util.cache.UtilCache;
import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.ServiceUtil;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class LabelManagerFactory {

    public static final String module = LabelManagerFactory.class.getName();
    public static final String resource = "WebtoolsUiLabels";
    
    protected static UtilCache<String, LabelManagerFactory> labelManagerFactoryCache = new UtilCache<String, LabelManagerFactory>("LabelManagerFactory");
    
    protected static Map<String, LabelInfo> labels = null;
    protected static Map<String, String> fileNamesFound = null;
    protected static Map<String, String> fileComponent = null;
    protected static Set<String> localesFound = null;
    protected static Set<String> componentNamesFound = null;
    protected static int duplicatedLocalesLabels = 0;
    
    protected static String delegatorName;
    
    public static LabelManagerFactory getLabelManagerFactory(String delegatorName) throws GeneralException {
        if (UtilValidate.isEmpty(delegatorName)) {
            delegatorName = "default";
        }
        
        LabelManagerFactory lmf = labelManagerFactoryCache.get(delegatorName);
        
        if (lmf == null) {
            lmf = new LabelManagerFactory(delegatorName);
            labelManagerFactoryCache.put(delegatorName, lmf);
        }
        return lmf;
    }
    
    protected LabelManagerFactory(String delegatorName) throws GeneralException {
        LabelManagerFactory.delegatorName = delegatorName;
        
        prepareAll();
    }
    
    private static void prepareAll() throws GeneralException {
    	labels = new TreeMap<String, LabelInfo>();
        fileNamesFound = new TreeMap<String, String>();
        fileComponent = new TreeMap<String, String>();
        localesFound = new TreeSet<String>();
        componentNamesFound = new TreeSet<String>();
        int duplicatedLocales = 0;
        
        try {
        	Collection<ComponentConfig> componentConfigs = ComponentConfig.getAllComponents();
        	
        	for (ComponentConfig componentConfig: componentConfigs) {
                String componentName = componentConfig.getComponentName();
                List<File> resourceFiles = FileUtil.findXmlFiles(componentConfig.getRootLocation(), null, "resource", null);
                
                for (File resourceFile: resourceFiles) {
                    String fileName = resourceFile.getName();
                    Document resourceDocument = UtilXml.readXmlDocument(resourceFile.toURI().toURL());
                    Element resourceElem = resourceDocument.getDocumentElement();
                    
                    for (Element propertyElem: UtilXml.childElementList(resourceElem, "property")) {
                        String labelKey = propertyElem.getAttribute("key");                            
                        
                        for (Element valueElem: UtilXml.childElementList(propertyElem, "value")) {
                            String localeName = valueElem.getAttribute("xml:lang");
                            String labelValue = UtilXml.elementValue(valueElem);
                            LabelInfo label = (LabelInfo)labels.get(labelKey + "_" + fileName);
                            
                            if (UtilValidate.isEmpty(label)) {
                                label = new LabelInfo(labelKey, fileName, componentName, localeName, labelValue);
                                labels.put(labelKey + "_" + fileName, label);
                            } else {
                                if (label.setLabelValue(localeName, labelValue, false)) {
                                    duplicatedLocales++;
                                }
                            }
                            localesFound.add(localeName);
                            componentNamesFound.add(componentName);
                            fileNamesFound.put(fileName, resourceFile.toURI().toString());
                            fileComponent.put(fileName, componentName);
                        }
                    }
                }
            }
        } catch(IOException ioe) {
            throw new GeneralException(ioe.getMessage());
        } catch(Exception e) {
            throw new GeneralException(e.getMessage());
        }
        duplicatedLocalesLabels = duplicatedLocales;
    }
    
    public static Map<String, LabelInfo> getLabels() {
        return labels;
    }
    
    public static Set<String> getLocalesFound() {
        return localesFound;
    }
    
    public static Map<String, String> getFileNamesFound() {
        return fileNamesFound;
    }
    
    public static String getFileComponent(String fileName) {
        String componentName = null;
        if (UtilValidate.isNotEmpty(fileName)) {
            componentName = fileComponent.get(fileName);
        }
        return componentName;
    }
    
    public static Set<String> getComponentNamesFound() {
        return componentNamesFound;
    }
    
    public static Set<String> getLabelsList() {
        return labels.keySet();
    }
    
    public static int getDuplicatedLocalesLabels() {
        return duplicatedLocalesLabels;
    }
    
    public static Map<String, Object> updateLabelKey(DispatchContext dctx, Map<String, ? extends Object> context) {
        String key = (String)context.get("key");
        String update_label = (String)context.get("update_label");
        String fileName = (String)context.get("fileName");
        String confirm = (String)context.get("confirm");
        List<String> localeNames = UtilGenerics.cast(context.get("localeNames"));
        List<String> localeValues = UtilGenerics.cast(context.get("localeValues"));
        Locale locale = (Locale) context.get("locale");
        
        if (UtilValidate.isNotEmpty(confirm)) {
            LabelInfo label = labels.get(key + "_" + fileName);
        
            // Update a Label
            if (update_label.equalsIgnoreCase("Y")) {
                if (UtilValidate.isNotEmpty(label)) {
                    updateLabelValue(localeNames, localeValues, label, key, fileName);
                }
            // Insert a new Label
            } else {
                if (UtilValidate.isNotEmpty(label)) {
                    return ServiceUtil.returnError(UtilProperties.getMessage(resource, "WebtoolsLabelManagerNewLabelExisting", UtilMisc.toMap("key", key, "fileName", fileName), locale));
                } else {
                    if (UtilValidate.isEmpty(key)) {
                        return ServiceUtil.returnError(UtilProperties.getMessage(resource, "WebtoolsLabelManagerNewLabelEmptyKey", locale));
                    } else {
                        int notEmptyLabels = updateLabelValue(localeNames, localeValues, null, key, fileName);
                        if (notEmptyLabels == 0) {
                            return ServiceUtil.returnError(UtilProperties.getMessage(resource, "WebtoolsLabelManagerNewLabelEmpty", locale));
                        }
                    }
                }
            }    
        }
            
        return ServiceUtil.returnSuccess();
    }
    
    private static int updateLabelValue(List<String> localeNames, List<String> localeValues, LabelInfo label, String key, String fileName) {
        int notEmptyLabels = 0;
        int i = 0;
        while (i < localeNames.size()) {
            String localeName = (String)localeNames.get(i);
            String localeValue = (String)localeValues.get(i);
            
            if (UtilValidate.isNotEmpty(localeValue)) {
                if (label == null) {
                    try {
                        String componentName = getFileComponent(fileName);
                        label = new LabelInfo(key, fileName, componentName, localeName, localeValue);
                        labels.put(key + "_" + fileName, label);
                    } catch(Exception e) {
                        e.printStackTrace();
                    }
                }
                label.setLabelValue(localeName, localeValue, true);
                notEmptyLabels++;
            }
            i++;
        }
        
        if (UtilValidate.isNotEmpty(label) && label.getLabelValueSize() == 0) {
            labels.remove(key + "_" + fileName);
        }
        
        return notEmptyLabels;
    }
}

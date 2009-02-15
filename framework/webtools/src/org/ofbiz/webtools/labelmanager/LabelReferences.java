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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.FileUtil;
import org.ofbiz.base.util.GeneralException;
import org.ofbiz.base.util.UtilFormatOut;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.base.util.UtilXml;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.model.ModelEntity;
import org.ofbiz.entity.model.ModelField;
import org.ofbiz.service.ModelParam;
import org.ofbiz.service.ModelService;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class LabelReferences {

    public static final String module = LabelReferences.class.getName();
    private static final String uiLabelMap = "${uiLabelMap.";
    private static final String uiLabelMapInLayoutSettings = "uiLabelMap.";
    private static final String formFieldTitle = "FormFieldTitle_";
    private static final String getMessage = "UtilProperties.getMessage";
    private static Map<String, Map<String, Integer>> references = null;
    
    public static Map<String, Map<String, Integer>> getLabelReferences() throws GeneralException {
        references = new TreeMap<String, Map<String, Integer>>();
        
        // get labels from FTL files
        getLabelsFromFtlFiles();
        
        // get labels from java files
        getLabelsFromJavaFiles();
        
        // get labels from simple method files
        getLabelsFromSimpleMethodFiles();
        
        // get labels from form widgets files
        getLabelsFromFormWidgets();
        
        // get labels from screen widgets files
        getLabelsFromScreenWidgets();
        
        // get labels from menu widgets files
        getLabelsFromMenuWidgets();
        
        return  references;
    }
    
    private static void getLabelsFromFtlFiles() throws GeneralException {
        try {
            List<File> ftlFiles = FileUtil.findFiles("ftl", null, null, uiLabelMap);
            
            for (File ftlFile: ftlFiles) {
                String fileNameURI = ftlFile.toURI().toString();
                String inFile = FileUtil.readString("UTF-8", ftlFile);
                int pos = 0;
                while (pos >= 0){
                    pos = inFile.indexOf(uiLabelMap, pos);
                    
                    if (pos >= 0) {
                        int endLabel = inFile.indexOf("}", pos);
                        
                        if (endLabel >= 0) {
                            String labelKey = inFile.substring(pos + uiLabelMap.length(), endLabel);
                            setLabelReference(labelKey, fileNameURI);
                            pos = endLabel;
                        } else {
                            pos = pos + uiLabelMap.length();
                        }
                    }
                }
            }
        } catch(IOException ioe) {
            throw new GeneralException(ioe.getMessage());
        }
    }
    
    private static void getLabelsFromJavaFiles() throws GeneralException {
        try {
            List<File> javaFiles = FileUtil.findFiles("java", null, null, getMessage);
            
            for (File javaFile: javaFiles) {
                getJavaLabels(javaFile, getMessage);
            }
        } catch(IOException ioe) {
            throw new GeneralException(ioe.getMessage());
        }
    }
    
    private static void getJavaLabels(File javaFile, String message) throws GeneralException {
        try {
            String fileNameURI = javaFile.toURI().toString();
            String inFile = FileUtil.readString("UTF-8", javaFile);
            int pos = 0;
            while (pos >= 0){
                pos = inFile.indexOf(message, pos);
                
                if (pos >= 0) {
                    int offSet = (pos + 200 > inFile.length()) ? inFile.length() : pos + 200;
                    String searchComma = inFile.substring(pos, offSet);
                    int firstComma = searchComma.indexOf(",\"", 0);
                    
                    if (firstComma < 0) {
                        firstComma = searchComma.indexOf(", \"", 0);
                        pos = pos + firstComma + 3;
                    } else {
                        pos = pos + firstComma + 2;
                    }
                    
                    if (firstComma >= 0) {
                        offSet = (pos + 100 > inFile.length()) ? inFile.length() : pos + 100;
                        searchComma = inFile.substring(pos, offSet);
                        int secondComma = searchComma.indexOf("\",", 0);
                        int endString = pos;
                        
                        if (secondComma < 0) {
                            secondComma = searchComma.indexOf("\" ,", 0);
                            endString = endString + secondComma + 1;
                        } else {
                            endString = endString + secondComma;
                        }
                        
                        if (secondComma >= 0) {
                            setLabelReference(inFile.substring(pos, endString), fileNameURI);
                            pos = endString;
                        }
                    }
                    pos += 1;
                }
            }
        } catch(IOException ioe) {
            throw new GeneralException(ioe.getMessage());
        }
    }
    
    private static void getLabelsFromSimpleMethodFiles() throws GeneralException {
        try {
            List<File> simpleMethodsFiles = FileUtil.findXmlFiles(null, null, "simple-methods", "http://ofbiz.apache.org/dtds/simple-methods.xsd");
            
            for (File simpleMethodFile: simpleMethodsFiles) {
                String fileNameURI = simpleMethodFile.toURI().toString();
                Document simpleMethodDocument = UtilXml.readXmlDocument(simpleMethodFile.toURI().toURL());
                Element rootElem = simpleMethodDocument.getDocumentElement();
                
                for (Element elem1: UtilXml.childElementList(rootElem)) {
                    checkSimpleMethodTag(elem1, fileNameURI);
                    for (Element elem2: UtilXml.childElementList(elem1)) {
                        checkSimpleMethodTag(elem2, fileNameURI);
                        for (Element elem3: UtilXml.childElementList(elem2)) {
                            checkSimpleMethodTag(elem3, fileNameURI);
                            for (Element elem4: UtilXml.childElementList(elem3)) {
                                checkSimpleMethodTag(elem4, fileNameURI);
                                for (Element elem5: UtilXml.childElementList(elem4)) {
                                    checkSimpleMethodTag(elem5, fileNameURI);
                                    for (Element elem6: UtilXml.childElementList(elem5)) {
                                        checkSimpleMethodTag(elem6, fileNameURI);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch(IOException ioe) {
            throw new GeneralException(ioe.getMessage());
        } catch(Exception e) {
            throw new GeneralException(e.getMessage());
        }
    }
    
    private static void checkSimpleMethodTag(Element elem, String fileNameURI) throws GeneralException {
        // fail-property labels
        if ("fail-property".equals(elem.getTagName())) {
            getFailPropertyTag(elem, fileNameURI);
        // property-to-field labels
        } else if ("property-to-field".equals(elem.getTagName())) {
            getPropertyToFieldTag(elem, fileNameURI);
        }
    }
    
    private static void getLabelsFromFormWidgets() throws GeneralException {
        try {
            List<File> formsFiles = FileUtil.findXmlFiles(null, null, "forms", "http://ofbiz.apache.org/dtds/widget-form.xsd");
            
            for (File formsFile: formsFiles) {
                String fileNameURI = formsFile.toURI().toString();
                Document formDocument = UtilXml.readXmlDocument(formsFile.toURI().toURL());
                Element rootElem = formDocument.getDocumentElement();
                
                for (Element elem1: UtilXml.childElementList(rootElem)) {
                    checkFormsTag(elem1, fileNameURI);
                    for (Element elem2: UtilXml.childElementList(elem1)) {
                        checkFormsTag(elem2, fileNameURI);
                        for (Element elem3: UtilXml.childElementList(elem2)) {
                            checkFormsTag(elem3, fileNameURI);
                            for (Element elem4: UtilXml.childElementList(elem3)) {
                                checkFormsTag(elem4, fileNameURI);
                                for (Element elem5: UtilXml.childElementList(elem4)) {
                                    checkFormsTag(elem5, fileNameURI);
                                }
                            }
                        }
                    }
                }
            }
        } catch(IOException ioe) {
            throw new GeneralException(ioe.getMessage());
        } catch(Exception e) {
            throw new GeneralException(e.getMessage());
        }
    }
    
    private static void checkFormsTag(Element elem, String fileNameURI) throws GeneralException {
        // auto fields entity labels
        if ("auto-fields-entity".equals(elem.getTagName())) {
            getAutoFieldsEntityTag(elem, fileNameURI);
        // auto fields service labels
        } else if ("auto-fields-service".equals(elem.getTagName())) {
            getAutoFieldsServiceTag(elem, fileNameURI);
        // field labels
        } else if ("field".equals(elem.getTagName())) {
            getFieldTag(elem, fileNameURI);
        // option description labels
        } else if ("option".equals(elem.getTagName())) {
            getOptionTag(elem, fileNameURI);
        // hyperlink/sub-hyperlink description labels
        } else if ("hyperlink".equals(elem.getTagName()) ||
                   "sub-hyperlink".equals(elem.getTagName())) {
            getHyperlinkTag(elem, fileNameURI);
        }
    }
    
    private static void getLabelsFromScreenWidgets() throws GeneralException {
        try {
            List<File> screensFiles = FileUtil.findXmlFiles(null, null, "screens", "http://ofbiz.apache.org/dtds/widget-screen.xsd");
            
            for (File screensFile: screensFiles) {
                String fileNameURI = screensFile.toURI().toString();
                Document screenDocument = UtilXml.readXmlDocument(screensFile.toURI().toURL());
                Element rootElem = screenDocument.getDocumentElement();
                
                for (Element elem1: UtilXml.childElementList(rootElem)) {
                    checkScreensTag(elem1, fileNameURI);
                    for (Element elem2: UtilXml.childElementList(elem1)) {
                        checkScreensTag(elem2, fileNameURI);
                        for (Element elem3: UtilXml.childElementList(elem2)) {
                            checkScreensTag(elem3, fileNameURI);
                            for (Element elem4: UtilXml.childElementList(elem3)) {
                                checkScreensTag(elem4, fileNameURI);
                                for (Element elem5: UtilXml.childElementList(elem4)) {
                                    checkScreensTag(elem5, fileNameURI);
                                    for (Element elem6: UtilXml.childElementList(elem5)) {
                                        checkScreensTag(elem6, fileNameURI);
                                        for (Element elem7: UtilXml.childElementList(elem6)) {
                                            checkScreensTag(elem7, fileNameURI);
                                            for (Element elem8: UtilXml.childElementList(elem7)) {
                                                checkScreensTag(elem8, fileNameURI);
                                                for (Element elem9: UtilXml.childElementList(elem8)) {
                                                    checkScreensTag(elem9, fileNameURI);
                                                    for (Element elem10: UtilXml.childElementList(elem9)) {
                                                        checkScreensTag(elem10, fileNameURI);
                                                        for (Element elem11: UtilXml.childElementList(elem10)) {
                                                            checkScreensTag(elem11, fileNameURI);
                                                            for (Element elem12: UtilXml.childElementList(elem11)) {
                                                                checkScreensTag(elem12, fileNameURI);
                                                                for (Element elem13: UtilXml.childElementList(elem12)) {
                                                                    checkScreensTag(elem13, fileNameURI);
                                                                    for (Element elem14: UtilXml.childElementList(elem13)) {
                                                                        checkScreensTag(elem14, fileNameURI);
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch(IOException ioe) {
            throw new GeneralException(ioe.getMessage());
        } catch(Exception e) {
            throw new GeneralException(e.getMessage());
        }
    }
    
    private static void checkScreensTag(Element elem, String fileNameURI) {
        // set labels
        if ("set".equals(elem.getTagName())) {
            getSetTag(elem, fileNameURI);
        // screenlet labels
        } else if ("screenlet".equals(elem.getTagName())) {
            getScreenletTag(elem, fileNameURI);
        // label labels
        } else if ("label".equals(elem.getTagName())) {
            getLabelTag(elem, fileNameURI);
        // link labels
        } else if ("link".equals(elem.getTagName())) {
            getLinkTag(elem, fileNameURI);
        }
    }
    
    private static void getLabelsFromMenuWidgets() throws GeneralException {
        try {
            List<File> menusFiles = FileUtil.findXmlFiles(null, null, "menus", "http://ofbiz.apache.org/dtds/widget-menu.xsd");
            
            for (File menuFiles: menusFiles) {
                String fileNameURI = menuFiles.toURI().toString();
                Document menuDocument = UtilXml.readXmlDocument(menuFiles.toURI().toURL());
                Element rootElem = menuDocument.getDocumentElement();
                
                for (Element elem1: UtilXml.childElementList(rootElem)) {
                    checkMenuTag(elem1, fileNameURI);
                    for (Element elem2: UtilXml.childElementList(elem1)) {
                        checkMenuTag(elem2, fileNameURI);
                        for (Element elem3: UtilXml.childElementList(elem2)) {
                            checkMenuTag(elem3, fileNameURI);
                            for (Element elem4: UtilXml.childElementList(elem3)) {
                                checkMenuTag(elem4, fileNameURI);
                                for (Element elem5: UtilXml.childElementList(elem4)) {
                                    checkMenuTag(elem5, fileNameURI);
                                }
                            }
                        }
                    }
                }
            }
        } catch(IOException ioe) {
            throw new GeneralException(ioe.getMessage());
        } catch(Exception e) {
            throw new GeneralException(e.getMessage());
        }
    }
    
    private static void checkMenuTag(Element elem, String fileNameURI) {
        // menu-item labels
        if ("menu-item".equals(elem.getTagName())) {
            getMenuItemTag(elem, fileNameURI);
        }
    }
    
    private static void setLabelReference(String labelKey, String fileNameURI) {
        Map<String, Integer> reference = references.get(labelKey);
        if (UtilValidate.isEmpty(reference)) {
            reference = new TreeMap<String, Integer>();
            reference.put(fileNameURI, new Integer(1));
            references.put(labelKey, reference); 
        } else {
            Integer labelsInFile = reference.get(fileNameURI);
            
            if (UtilValidate.isEmpty(labelsInFile)) {
                labelsInFile = new Integer(1);
            }
            else {
                labelsInFile = new Integer(labelsInFile.intValue() + 1);
            }
            reference.put(fileNameURI, labelsInFile);
        }
    }
    
    private static boolean getLabelFromTag(Element element, String fileNameURI, String attributeValue, String stringToSearch) {
        boolean stringFound = false;
        
        if (UtilValidate.isNotEmpty(attributeValue)) {
            int pos = 0;
            
            while (pos >= 0){
                pos = attributeValue.indexOf(stringToSearch, pos);
                
                if (pos >= 0) {
                    int graph = attributeValue.indexOf("}", pos);
                    
                    if (graph >= 0) {
                        String labelKey = attributeValue.substring(pos + stringToSearch.length(), graph);
                        setLabelReference(labelKey, fileNameURI);
                        stringFound = true;
                        pos = graph;
                    }
                    pos += 1;
                }
            }
        }
        return stringFound;
    }
    
    private static void getSetTag(Element element, String fileNameURI) {
        String setField = UtilFormatOut.checkNull(element.getAttribute("field"));
        String setValue = UtilFormatOut.checkNull(element.getAttribute("value"));
        String fromField = UtilFormatOut.checkNull(element.getAttribute("from-field"));
        
        if (UtilValidate.isNotEmpty(setField)) {
            if (UtilValidate.isNotEmpty(setValue) &&
                ("applicationTitle".equals(setField) ||
                 "titleProperty".equals(setField) ||
                 "title".equals(setField))) {
                // set field with hardcoded labels
                if (!getLabelFromTag(element, fileNameURI, setValue, uiLabelMap)) {
                    setLabelReference(setValue, fileNameURI);;
                }
            } else if (UtilValidate.isNotEmpty(fromField) &&
                       ("layoutSettings.companyName".equals(setField) ||
                        "layoutSettings.companySubtitle".equals(setField))) {
                // set field labels
                if (fromField.startsWith(uiLabelMapInLayoutSettings)) {
                    setLabelReference(fromField.substring(uiLabelMapInLayoutSettings.length(), fromField.length()), fileNameURI);
                // set field with hardcoded labels
                } else {
                    setLabelReference(fromField, fileNameURI);
                }
            }
        }
    }
    
    private static void getScreenletTag(Element element, String fileNameURI) {
        String screenTitle = UtilFormatOut.checkNull(element.getAttribute("title"));
        
        if (UtilValidate.isNotEmpty(screenTitle)) {
            // screenlet title with hardcoded labels
            if (!getLabelFromTag(element, fileNameURI, screenTitle, uiLabelMap)) {
                setLabelReference(screenTitle, fileNameURI);;
            }
        }
    }
    
    private static void getAutoFieldsEntityTag(Element element, String fileNameURI) throws GeneralException {
        try {
            String entityName = UtilFormatOut.checkNull(element.getAttribute("entity-name"));
            String defaultFieldType = UtilFormatOut.checkNull(element.getAttribute("default-field-type"));
            
            if (UtilValidate.isNotEmpty(entityName) && UtilValidate.isNotEmpty(defaultFieldType) && (!("hidden".equals(defaultFieldType)))) {
                ModelEntity entity = LabelManagerFactory.entityModelReader.getModelEntity(entityName);
                
                for (Iterator<ModelField> f = entity.getFieldsIterator(); f.hasNext();) {
                    ModelField field = f.next();
                    setLabelReference(formFieldTitle + field.getName(), fileNameURI);
                }
            }
        } catch(Exception e) {
            throw new GeneralException(e.getMessage());
        }
    }
    
    private static void getAutoFieldsServiceTag(Element element, String fileNameURI) throws GeneralException {
        try {
            String serviceName = UtilFormatOut.checkNull(element.getAttribute("service-name"));
            String defaultFieldType = UtilFormatOut.checkNull(element.getAttribute("default-field-type"));
            
            if (UtilValidate.isNotEmpty(serviceName) && (!("hidden".equals(defaultFieldType)))) {
                ModelService modelService = LabelManagerFactory.dispatchContext.getModelService(serviceName);
                List<ModelParam> modelParams = modelService.getInModelParamList();
                Iterator<ModelParam> modelParamIter = modelParams.iterator();
                
                while (modelParamIter.hasNext()) {
                    ModelParam modelParam = (ModelParam) modelParamIter.next();
                    // skip auto params that the service engine populates...
                    if ("userLogin".equals(modelParam.name) || "locale".equals(modelParam.name) || "timeZone".equals(modelParam.name)) {
                        continue;
                    }
                    
                    if (modelParam.formDisplay) {
                        if (UtilValidate.isNotEmpty(modelParam.entityName) && UtilValidate.isNotEmpty(modelParam.fieldName)) {
                            ModelEntity modelEntity;
                            try {
                                modelEntity = LabelManagerFactory.entityModelReader.getModelEntity(modelParam.entityName);
                                
                                if (modelEntity != null) {
                                    ModelField modelField = modelEntity.getField(modelParam.fieldName);
                                    
                                    if (modelField != null) {
                                        setLabelReference(formFieldTitle + modelField.getName(), fileNameURI);
                                    }
                                }
                            } catch (GenericEntityException e) {
                                Debug.logError(e, module);
                            }
                        }
                        
                        setLabelReference(formFieldTitle + modelParam.name, fileNameURI);
                    }
                }
            }
        } catch(Exception e) {
            throw new GeneralException(e.getMessage());
        }
    }
    
    private static void getHyperlinkTag(Element element, String fileNameURI) {
        String hyperlinkDescription = UtilFormatOut.checkNull(element.getAttribute("description"));
        
        if (UtilValidate.isNotEmpty(hyperlinkDescription)) {
            // hyperlink description with hardcoded labels
            if (!getLabelFromTag(element, fileNameURI, hyperlinkDescription, uiLabelMap)) {
                setLabelReference(hyperlinkDescription, fileNameURI);;
            }
        }
    }
    
    private static void getFieldTag(Element element, String fileNameURI) {
        String labelKey = UtilFormatOut.checkNull(element.getAttribute("name"));
        labelKey = formFieldTitle + labelKey;
        String fieldTitle =  UtilFormatOut.checkNull(element.getAttribute("title"));
        String tooltip =  UtilFormatOut.checkNull(element.getAttribute("tooltip"));
        boolean escludeField= false;
        
        for (Element fieldTypeElem: UtilXml.childElementList(element)) {
            if ("hidden".equals(fieldTypeElem.getTagName())) {
                escludeField = true;
            } else if ("ignore".equals(fieldTypeElem.getTagName())) {
                escludeField = true;
            }
        }
        
        if (!escludeField) {
            // field name labels
            if (UtilValidate.isEmpty(fieldTitle)) {
                setLabelReference(labelKey, fileNameURI);
            } else {
                // field title with hardcoded labels
                if (!getLabelFromTag(element, fileNameURI, fieldTitle, uiLabelMap)) {
                    setLabelReference(fieldTitle, fileNameURI);;
                }
            }
            
            if (UtilValidate.isNotEmpty(tooltip)) {
                // tooltip with hardcoded labels
                if (!getLabelFromTag(element, fileNameURI, tooltip, uiLabelMap)) {
                    setLabelReference(tooltip, fileNameURI);;
                }
            }
        }
    }
    
    private static void getLabelTag(Element element, String fileNameURI) {
        String labelText = UtilFormatOut.checkNull(element.getAttribute("text"));
        String labelValue = UtilFormatOut.checkNull(UtilXml.elementValue(element));
        
        // label text labels
        if (UtilValidate.isNotEmpty(labelText)) {
            // label text with hardcoded labels
            if (!getLabelFromTag(element, fileNameURI, labelText, uiLabelMap)) {
                setLabelReference(labelText, fileNameURI);;
            }
        // label value labels
        } else if (UtilValidate.isNotEmpty(labelValue)) {
            // label value with hardcoded labels
            if (!getLabelFromTag(element, fileNameURI, labelValue, uiLabelMap)) {
                setLabelReference(labelValue, fileNameURI);;
            }
        }
    }
    
    private static void getMenuItemTag(Element element, String fileNameURI) {
        String menuItemTitle = UtilFormatOut.checkNull(element.getAttribute("title"));
        
        if (UtilValidate.isNotEmpty(menuItemTitle)) {
            // menu item title with hardcoded labels
            if (!getLabelFromTag(element, fileNameURI, menuItemTitle, uiLabelMap)) {
                setLabelReference(menuItemTitle, fileNameURI);;
            }
        }
    }
    
    private static void getFailPropertyTag(Element element, String fileNameURI) {
        String propertyValue = UtilFormatOut.checkNull(element.getAttribute("property"));
        
        if (UtilValidate.isNotEmpty(propertyValue)) {
            // fail-property labels
            setLabelReference(propertyValue, fileNameURI);
        }
    }
    
    private static void getOptionTag(Element element, String fileNameURI) {
        String description = UtilFormatOut.checkNull(element.getAttribute("description"));
        
        if (UtilValidate.isNotEmpty(description)) {
            // option description with hardcoded labels
            if (!getLabelFromTag(element, fileNameURI, description, uiLabelMap)) {
                setLabelReference(description, fileNameURI);;
            }
        }
    }
    
    private static void getLinkTag(Element element, String fileNameURI) {
        String linkText = UtilFormatOut.checkNull(element.getAttribute("text"));
        
        if (UtilValidate.isNotEmpty(linkText)) {
            // link text with hardcoded labels
            if (!getLabelFromTag(element, fileNameURI, linkText, uiLabelMap)) {
                setLabelReference(linkText, fileNameURI);;
            }
        }
    }
    
    private static void getPropertyToFieldTag(Element element, String fileNameURI) {
        String property = UtilFormatOut.checkNull(element.getAttribute("property"));
        
        // property-to-field labels
        if (UtilValidate.isNotEmpty(property)) {
            setLabelReference(property, fileNameURI);
        }
    }
}



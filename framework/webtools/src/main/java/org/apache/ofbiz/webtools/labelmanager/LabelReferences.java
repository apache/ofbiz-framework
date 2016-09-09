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
package org.apache.ofbiz.webtools.labelmanager;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.ofbiz.base.component.ComponentConfig;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.FileUtil;
import org.apache.ofbiz.base.util.UtilFormatOut;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.base.util.UtilXml;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityConfException;
import org.apache.ofbiz.entity.config.model.DelegatorElement;
import org.apache.ofbiz.entity.config.model.EntityConfig;
import org.apache.ofbiz.entity.model.ModelEntity;
import org.apache.ofbiz.entity.model.ModelField;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.ModelParam;
import org.apache.ofbiz.service.ModelService;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

public class LabelReferences {

    public static final String module = LabelReferences.class.getName();
    private static final String bracketedUiLabelMap = "${uiLabelMap.";
    private static final String uiLabelMap = "uiLabelMap.";
    private static final String formFieldTitle = "FormFieldTitle_";
    private static final String getMessage = "UtilProperties.getMessage(";
    private static final String getResourceRegex = "ServiceUtil\\.getResource\\(\\)";
    private static final String getResource = "ServiceUtil.getResource  ";

    protected Map<String, Map<String, Integer>> references = new TreeMap<String, Map<String, Integer>>();
    protected Delegator delegator;
    protected DispatchContext dispatchContext;
    protected Map<String, LabelInfo> labels;
    protected Set<String> labelSet = new HashSet<String>();
    protected Set<String> rootFolders = new HashSet<String>();

    public LabelReferences(Delegator delegator, LabelManagerFactory factory) {
        this.delegator = delegator;
        this.labels = factory.getLabels();
        DelegatorElement delegatorInfo = null;
        try {
            delegatorInfo = EntityConfig.getInstance().getDelegator(delegator.getDelegatorBaseName());
        } catch (GenericEntityConfException e) {
            Debug.logWarning(e, "Exception thrown while getting delegator config: ", module);
        }
        String modelName;
        if (delegatorInfo != null) {
            modelName = delegatorInfo.getEntityModelReader();
        } else {
            modelName = "main";
        }
        // since we do not associate a dispatcher to this DispatchContext, it is important to set a name of an existing entity model reader:
        // in this way it will be possible to retrieve the service models from the cache
        this.dispatchContext = new DispatchContext(modelName, this.getClass().getClassLoader(), null);
        Collection<LabelInfo> infoList = this.labels.values();
        for (LabelInfo labelInfo : infoList) {
            this.labelSet.add(labelInfo.getLabelKey());
        }
        Collection<ComponentConfig> componentConfigs = ComponentConfig.getAllComponents();
        for (ComponentConfig config : componentConfigs) {
            String rootFolder = config.getRootLocation();
            rootFolder = rootFolder.replace('\\', '/');
            if (!rootFolder.endsWith("/")) {
                rootFolder = rootFolder + "/";
            }
            this.rootFolders.add(rootFolder);
        }
    }

    public Map<String, Map<String, Integer>> getLabelReferences() throws IOException, SAXException, ParserConfigurationException, GenericServiceException {
        if (this.labels.size() == 0) {
            // Nothing to search for
            return references;
        }
        // get labels from FTL files
        getLabelsFromFtlFiles();
        // get labels from Java files
        getLabelsFromJavaFiles();
        // get labels from Groovy files
        getLabelsFromGroovyFiles();
        // get labels from simple method files
        getLabelsFromSimpleMethodFiles();
        // get labels from widgets files
        List<File> fileList = new LinkedList<File>();
        for (String rootFolder : this.rootFolders) {
            fileList.addAll(FileUtil.findXmlFiles(rootFolder + "webapp", null, null, null));
            fileList.addAll(FileUtil.findXmlFiles(rootFolder + "widget", null, null, null));
        }
        for (File file : fileList) {
            String inFile = FileUtil.readString("UTF-8", file);
            if (inFile.contains("</forms>")) {
                getLabelsFromFormWidgets(inFile, file);
                findLabelKeyInElement(inFile, file.getPath(), "set");
                continue;
            }
            if (inFile.contains("</screens>") || inFile.contains("</menus>") || inFile.contains("</trees>")) {
                findUiLabelMapInFile(inFile, file.getPath());
                findLabelKeyInElement(inFile, file.getPath(), "set");
                continue;
            }

        }
        // get labels from Ofbiz components files
        getLabelsFromOfbizComponents();
        return references;
    }

    private void setLabelReference(String labelKey, String filePath) {
        Map<String, Integer> reference = references.get(labelKey);
        if (UtilValidate.isEmpty(reference)) {
            reference = new TreeMap<String, Integer>();
            reference.put(filePath, 1);
            references.put(labelKey, reference);
        } else {
            Integer labelsInFile = reference.get(filePath);

            if (UtilValidate.isEmpty(labelsInFile)) {
                labelsInFile = 1;
            } else {
                labelsInFile = labelsInFile + 1;
            }
            reference.put(filePath, labelsInFile);
        }
    }

    private void getLabelsFromFtlFiles() throws IOException {
        for (String rootFolder : this.rootFolders) {
            List<File> ftlFiles = FileUtil.findFiles("ftl", rootFolder, null, null);
            for (File file : ftlFiles) {
                String inFile = FileUtil.readString("UTF-8", file);
                inFile = inFile.replaceAll(getResourceRegex, getResource);
                int pos = inFile.indexOf(bracketedUiLabelMap);
                while (pos >= 0) {
                    int endPos = inFile.indexOf("}", pos);
                    if (endPos >= 0) {
                        String labelKey = inFile.substring(pos + bracketedUiLabelMap.length(), endPos);
                        if (this.labelSet.contains(labelKey)) {
                            setLabelReference(labelKey, file.getPath());
                        }
                        pos = endPos;
                    } else {
                        pos = pos + bracketedUiLabelMap.length();
                    }
                    pos = inFile.indexOf(bracketedUiLabelMap, pos);
                }
            }
        }
    }

    private void getLabelsFromJavaFiles() throws IOException {
        for (String rootFolder : this.rootFolders) {
            List<File> javaFiles = FileUtil.findFiles("java", rootFolder + "src", null, null);
            for (File javaFile : javaFiles) {
                // do not parse this file, else issue with getResourceRegex
                if ("LabelReferences.java".equals(javaFile.getName())) continue;
                String inFile = FileUtil.readString("UTF-8", javaFile);
                inFile = inFile.replaceAll(getResourceRegex, getResource);
                int pos = inFile.indexOf(getMessage);
                while (pos >= 0) {
                    int endLabel = inFile.indexOf(")", pos);
                    if (endLabel >= 0) {
                        String[] args = inFile.substring(pos + getMessage.length(), endLabel).split(",");
                        for (String labelKey : this.labelSet) {
                            String searchString = "\"" + labelKey + "\"";
                            if (searchString.equals(args[1].trim())) {
                                setLabelReference(labelKey, javaFile.getPath());
                            }
                        }
                        pos = endLabel;
                    } else {
                        pos = pos + getMessage.length();
                    }
                    pos = inFile.indexOf(getMessage, pos);
                }
            }
        }
    }

    private void getLabelsFromGroovyFiles() throws IOException {
        for (String rootFolder : this.rootFolders) {
            List<File> groovyFiles = FileUtil.findFiles("groovy", rootFolder + "groovyScripts", null, null);
            for (File file : groovyFiles) {
                String inFile = FileUtil.readString("UTF-8", file);
                findUiLabelMapInFile(inFile, file.getPath());
            }
        }
    }
    

    protected void findUiLabelMapInFile(String inFile, String filePath) {
        int pos = inFile.indexOf(uiLabelMap);
        while (pos >= 0) {
            String endStr = "}";
            if ("\"".equals(inFile.substring(pos - 1, pos))) {
                endStr = "\"";
            }
            int endPos = inFile.indexOf(endStr, pos);
            if (endPos >= 0) {
                String labelKey = inFile.substring(pos + uiLabelMap.length(), endPos);
                if (this.labelSet.contains(labelKey)) {
                    setLabelReference(labelKey, filePath);
                }
                pos = endPos;
            } else {
                pos = pos + uiLabelMap.length();
            }
            pos = inFile.indexOf(uiLabelMap, pos);
        }
    }

    protected void findLabelKeyInElement(String inFile, String filePath, String elementName) {
        String searchString = "<" + elementName;
        int pos = inFile.indexOf(searchString);
        while (pos >= 0) {
            int endLabel = inFile.indexOf(">", pos);
            if (endLabel >= 0) {
                String args = inFile.substring(pos + searchString.length(), endLabel);
                for (String labelKey : this.labelSet) {
                    String arg = "\"" + labelKey + "\"";
                    if (args.contains(arg)) {
                        setLabelReference(labelKey, filePath);
                    }
                }
                pos = endLabel;
            } else {
                pos = pos + searchString.length();
            }
            pos = inFile.indexOf(searchString, pos);
        }
    }

    private void getLabelsFromSimpleMethodFiles() throws IOException {
        for (String rootFolder : this.rootFolders) {
            List<File> simpleMethodsFiles = FileUtil.findFiles("xml", rootFolder + "minilang", null, null);
            for (File file : simpleMethodsFiles) {
                String inFile = FileUtil.readString("UTF-8", file);
                findUiLabelMapInFile(inFile, file.getPath());
                findLabelKeyInElement(inFile, file.getPath(), "set");
                findLabelKeyInElement(inFile, file.getPath(), "fail-property");
            }
        }
    }

    private void getLabelsFromFormWidgets(String inFile, File file) throws MalformedURLException, SAXException, ParserConfigurationException, IOException, GenericServiceException {
        Set<String> fieldNames = new HashSet<String>();
        findUiLabelMapInFile(inFile, file.getPath());
        Document formDocument = UtilXml.readXmlDocument(file.toURI().toURL());
        Element rootElem = formDocument.getDocumentElement();
        for (Element formElement : UtilXml.childElementList(rootElem, "form")) {
            for (Element elem : UtilXml.childElementList(formElement, "auto-fields-service")) {
                getAutoFieldsServiceTag(elem, fieldNames);
            }
            for (Element elem : UtilXml.childElementList(formElement, "auto-fields-entity")) {
                getAutoFieldsEntityTag(elem, fieldNames);
            }
            for (String field : fieldNames) {
                String labelKey = formFieldTitle.concat(field);
                if (this.labelSet.contains(labelKey)) {
                    setLabelReference(labelKey, file.getPath());
                }
            }
        }
    }

    private void getLabelsFromOfbizComponents() throws IOException, SAXException, ParserConfigurationException {
        List<File> componentsFiles = FileUtil.findXmlFiles(null, null, "ofbiz-component", "http://ofbiz.apache.org/dtds/ofbiz-component.xsd");
        for (File componentFile : componentsFiles) {
            String filePath = componentFile.getPath();
            Document menuDocument = UtilXml.readXmlDocument(componentFile.toURI().toURL());
            Element rootElem = menuDocument.getDocumentElement();
            for (Element elem1 : UtilXml.childElementList(rootElem)) {
                checkOfbizComponentTag(elem1, filePath);
                for (Element elem2 : UtilXml.childElementList(elem1)) {
                    checkOfbizComponentTag(elem2, filePath);
                }
            }
        }
    }

    private void checkOfbizComponentTag(Element elem, String filePath) {
        // webapp labels
        if ("webapp".equals(elem.getTagName())) {
            getWebappTag(elem, filePath);
        }
    }

    private void getAutoFieldsEntityTag(Element element, Set<String> fieldNames) {
        String entityName = UtilFormatOut.checkNull(element.getAttribute("entity-name"));
        String defaultFieldType = UtilFormatOut.checkNull(element.getAttribute("default-field-type"));
        if (UtilValidate.isNotEmpty(entityName) && UtilValidate.isNotEmpty(defaultFieldType) && (!("hidden".equals(defaultFieldType)))) {
            ModelEntity entity = delegator.getModelEntity(entityName);
            for (Iterator<ModelField> f = entity.getFieldsIterator(); f.hasNext();) {
                ModelField field = f.next();
                fieldNames.add(field.getName());
            }
        }
    }

    private void getAutoFieldsServiceTag(Element element, Set<String> fieldNames) throws GenericServiceException {
        String serviceName = UtilFormatOut.checkNull(element.getAttribute("service-name"));
        String defaultFieldType = UtilFormatOut.checkNull(element.getAttribute("default-field-type"));
        if (UtilValidate.isNotEmpty(serviceName) && (!("hidden".equals(defaultFieldType)))) {
            ModelService modelService = dispatchContext.getModelService(serviceName);
            List<ModelParam> modelParams = modelService.getInModelParamList();
            Iterator<ModelParam> modelParamIter = modelParams.iterator();
            while (modelParamIter.hasNext()) {
                ModelParam modelParam = modelParamIter.next();
                // skip auto params that the service engine populates...
                if ("userLogin".equals(modelParam.name) || "locale".equals(modelParam.name) || "timeZone".equals(modelParam.name)) {
                    continue;
                }
                if (modelParam.formDisplay) {
                    if (UtilValidate.isNotEmpty(modelParam.entityName) && UtilValidate.isNotEmpty(modelParam.fieldName)) {
                        ModelEntity modelEntity;
                        modelEntity = delegator.getModelEntity(modelParam.entityName);

                        if (modelEntity != null) {
                            ModelField modelField = modelEntity.getField(modelParam.fieldName);

                            if (modelField != null) {
                                fieldNames.add(modelField.getName());
                            }
                        }
                    }
                }
            }
        }
    }

    private void getWebappTag(Element element, String filePath) {
        String title = UtilFormatOut.checkNull(element.getAttribute("title"));
        String appBarDisplay = UtilFormatOut.checkNull(element.getAttribute("app-bar-display"));
        // title labels
        if (UtilValidate.isNotEmpty(title) && UtilValidate.isNotEmpty(appBarDisplay) && "true".equalsIgnoreCase(appBarDisplay)) {
            setLabelReference(title, filePath);
        }
    }

}

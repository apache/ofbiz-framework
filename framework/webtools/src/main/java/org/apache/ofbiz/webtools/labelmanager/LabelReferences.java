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
import java.nio.file.Path;
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

    private static final String MODULE = LabelReferences.class.getName();
    private static final String BRACKETED_UILABEL_MAP = "${uiLabelMap.";
    private static final String UILABEL_MAP = "uiLabelMap.";
    private static final String FORM_FIELD_TITLE = "FormFieldTitle_";
    private static final String GET_MESSAGE = "UtilProperties.GET_MESSAGE(";
    private static final String GET_RES_REGEX = "ServiceUtil\\.getResource\\(\\)";
    private static final String GET_RESOURCE = "ServiceUtil.getResource  ";

    private Map<String, Map<String, Integer>> references = new TreeMap<>();
    private Delegator delegator;
    private DispatchContext dispatchContext;
    private Map<String, LabelInfo> labels;
    private Set<String> labelSet = new HashSet<>();
    private Set<Path> rootFolders = new HashSet<>();

    public LabelReferences(Delegator delegator, LabelManagerFactory factory) {
        this.delegator = delegator;
        this.labels = factory.getLabels();
        DelegatorElement delegatorInfo = null;
        try {
            delegatorInfo = EntityConfig.getInstance().getDelegator(delegator.getDelegatorBaseName());
        } catch (GenericEntityConfException e) {
            Debug.logWarning(e, "Exception thrown while getting delegator config: ", MODULE);
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
            Path rootFolder = config.rootLocation();
            this.rootFolders.add(rootFolder);
        }
    }

    /**
     * Gets label references.
     * @return the label references
     * @throws IOException                  the io exception
     * @throws SAXException                 the sax exception
     * @throws ParserConfigurationException the parser configuration exception
     * @throws GenericServiceException      the generic service exception
     */
    public Map<String, Map<String, Integer>> getLabelReferences() throws IOException, SAXException, ParserConfigurationException,
                                                                         GenericServiceException {
        if (this.labels.isEmpty()) {
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
        List<File> fileList = new LinkedList<>();
        for (Path rootFolder : this.rootFolders) {
            fileList.addAll(FileUtil.findXmlFiles(rootFolder.resolve("webapp").toString(), null, null, null));
            fileList.addAll(FileUtil.findXmlFiles(rootFolder.resolve("widget").toString(), null, null, null));
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
            reference = new TreeMap<>();
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
        for (Path rootFolder : this.rootFolders) {
            List<File> ftlFiles = FileUtil.findFiles("ftl", rootFolder.toString(), null, null);
            for (File file : ftlFiles) {
                String inFile = FileUtil.readString("UTF-8", file);
                inFile = inFile.replaceAll(GET_RES_REGEX, GET_RESOURCE);
                int pos = inFile.indexOf(BRACKETED_UILABEL_MAP);
                while (pos >= 0) {
                    int endPos = inFile.indexOf("}", pos);
                    if (endPos >= 0) {
                        String labelKey = inFile.substring(pos + BRACKETED_UILABEL_MAP.length(), endPos);
                        if (this.labelSet.contains(labelKey)) {
                            setLabelReference(labelKey, file.getPath());
                        }
                        pos = endPos;
                    } else {
                        pos = pos + BRACKETED_UILABEL_MAP.length();
                    }
                    pos = inFile.indexOf(BRACKETED_UILABEL_MAP, pos);
                }
            }
        }
    }
    private void getLabelsFromJavaFiles() throws IOException {
        for (Path rootFolder : this.rootFolders) {
            List<File> javaFiles = FileUtil.findFiles("java", rootFolder.resolve("src").toString(), null, null);
            for (File javaFile : javaFiles) {
                // do not parse this file, else issue with GET_RES_REGEX
                if ("LabelReferences.java".equals(javaFile.getName())) continue;
                String inFile = FileUtil.readString("UTF-8", javaFile);
                inFile = inFile.replaceAll(GET_RES_REGEX, GET_RESOURCE);
                findUiLabelMapInMessage(inFile, javaFile.getPath());
                findUiLabelMapInPattern(inFile, "uiLabelMap.get(\"", javaFile.getPath());
            }
        }
    }
    private void getLabelsFromGroovyFiles() throws IOException {
        for (Path rootFolder : this.rootFolders) {
            List<File> groovyFiles =
                    FileUtil.findFiles("groovy", rootFolder.resolve("groovyScripts").toString(), null, null);
            for (File file : groovyFiles) {
                String inFile = FileUtil.readString("UTF-8", file);
                findUiLabelMapInPattern(inFile, UILABEL_MAP, file.getPath());
                findUiLabelMapInPattern(inFile, "uiLabelMap.get(\"", file.getPath());
                findUiLabelMapInMessage(inFile, file.getPath());
            }
        }
    }

    /**
     * Find ui label map in message.
     * @param inFile   the in file
     * @param filePath the file path
     */
    protected void findUiLabelMapInMessage(String inFile, String filePath) {
        int pos = inFile.indexOf(GET_MESSAGE);
        while (pos >= 0) {
            int endLabel = inFile.indexOf(")", pos);
            if (endLabel >= 0) {
                String[] args = inFile.substring(pos + GET_MESSAGE.length(), endLabel).split(",");
                for (String labelKey : this.labelSet) {
                    String searchString = "\"" + labelKey + "\"";
                    if (searchString.equals(args[1].trim())) {
                        setLabelReference(labelKey, filePath);
                    }
                }
                pos = endLabel;
            } else {
                pos = pos + GET_MESSAGE.length();
            }
            pos = inFile.indexOf(GET_MESSAGE, pos);
        }
    }

    /**
     * Find ui label map in pattern.
     * @param inFile   the in file
     * @param pattern  the pattern
     * @param filePath the file path
     */
    protected void findUiLabelMapInPattern(String inFile, String pattern, String filePath) {
        int pos = inFile.indexOf(pattern);
        while (pos >= 0) {
            String label = inFile.substring(pos + pattern.length());
            String[] realLabel = label.split("\\P{Alpha}+");
            String labelKey = realLabel[0];
            int endPos = pos + labelKey.length();
            if (endPos >= 0) {
                if (this.labelSet.contains(labelKey)) {
                    setLabelReference(labelKey, filePath);
                }
                pos = endPos;
            } else {
                pos = pos + pattern.length();
            }
            pos = inFile.indexOf(pattern, pos);
        }
    }

    /**
     * Find ui label map in file.
     * @param inFile   the in file
     * @param filePath the file path
     */
    protected void findUiLabelMapInFile(String inFile, String filePath) {
        int pos = inFile.indexOf(UILABEL_MAP);
        while (pos >= 0) {
            String endStr = "}";
            if ("\"".equals(inFile.substring(pos - 1, pos))) {
                endStr = "\"";
            }
            int endPos = inFile.indexOf(endStr, pos);
            if (endPos >= 0) {
                String labelKey = inFile.substring(pos + UILABEL_MAP.length(), endPos);
                if (this.labelSet.contains(labelKey)) {
                    setLabelReference(labelKey, filePath);
                }
                pos = endPos;
            } else {
                pos = pos + UILABEL_MAP.length();
            }
            pos = inFile.indexOf(UILABEL_MAP, pos);
        }
    }

    /**
     * Find label key in element.
     * @param inFile      the in file
     * @param filePath    the file path
     * @param elementName the element name
     */
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
        for (Path rootFolder : this.rootFolders) {
            List<File> simpleMethodsFiles =
                    FileUtil.findFiles("xml", rootFolder.resolve("minilang").toString(), null, null);
            for (File file : simpleMethodsFiles) {
                String inFile = FileUtil.readString("UTF-8", file);
                findUiLabelMapInFile(inFile, file.getPath());
                findLabelKeyInElement(inFile, file.getPath(), "set");
                findLabelKeyInElement(inFile, file.getPath(), "fail-property");
                findLabelKeyInElement(inFile, file.getPath(), "property-to-field");
                findLabelKeyInElement(inFile, file.getPath(), "default-message");
            }
        }
    }

    private void getLabelsFromFormWidgets(String inFile, File file) throws MalformedURLException, SAXException, ParserConfigurationException,
                                                                           IOException, GenericServiceException {
        Set<String> fieldNames = new HashSet<>();
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
            for (Element elem : UtilXml.childElementList(formElement, "field")) {
                getAutoFieldsTag(elem, file.getPath());
            }
            for (String field : fieldNames) {
                String labelKey = FORM_FIELD_TITLE.concat(field);
                if (this.labelSet.contains(labelKey)) {
                    setLabelReference(labelKey, file.getPath());
                }
            }
        }
    }

    private void getLabelsFromOfbizComponents() throws IOException, SAXException, ParserConfigurationException {
        List<File> componentsFiles = FileUtil.findXmlFiles(null, null,
                "ofbiz-component", "http://ofbiz.apache.org/dtds/ofbiz-component.xsd");
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
    private void getAutoFieldsTag(Element element, String filePath) {
        String tooltip = UtilFormatOut.checkNull(element.getAttribute("tooltip"));
        if (UtilValidate.isNotEmpty(tooltip)) {
            int pos = tooltip.indexOf(GET_MESSAGE);
            while (pos >= 0) {
                int endLabel = tooltip.indexOf(")", pos);
                if (endLabel >= 0) {
                    String[] args = tooltip.substring(pos + GET_MESSAGE.length(), endLabel).split(",");
                    for (String labelKey : this.labelSet) {
                        String xmlSearchString = "\'" + labelKey + "\'";
                        if (xmlSearchString.equals(args[1].trim())) {
                            setLabelReference(labelKey, filePath);
                        }
                    }
                    pos = endLabel;
                } else {
                    pos = pos + GET_MESSAGE.length();
                }
                pos = tooltip.indexOf(GET_MESSAGE, pos);
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
                if ("userLogin".equals(modelParam.getName()) || "locale".equals(modelParam.getName()) || "timeZone".equals(modelParam.getName())) {
                    continue;
                }
                if (modelParam.isFormDisplay()) {
                    if (UtilValidate.isNotEmpty(modelParam.getEntityName()) && UtilValidate.isNotEmpty(modelParam.getFieldName())) {
                        ModelEntity modelEntity;
                        modelEntity = delegator.getModelEntity(modelParam.getEntityName());

                        if (modelEntity != null) {
                            ModelField modelField = modelEntity.getField(modelParam.getFieldName());

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

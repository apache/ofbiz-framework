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
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import javolution.util.FastList;
import javolution.util.FastMap;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.FileUtil;
import org.ofbiz.base.util.GeneralException;
import org.ofbiz.base.util.UtilFormatOut;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.base.util.UtilXml;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.condition.EntityComparisonOperator;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.condition.EntityConditionList;
import org.ofbiz.entity.condition.EntityExpr;
import org.ofbiz.entity.condition.EntityOperator;
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
    private static final String getEntityLabel = ".get(\"";
    private static final String startExpression = "${";
    private static final String endExpression = "}";
    private static Map<String, Map<String, Integer>> references = null;

    public static Map<String, Map<String, Integer>> getLabelReferences()
            throws GeneralException {
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

        // get labels from tree widgets files
        getLabelsFromTreeWidgets();

        // get labels from Ofbiz components files
        getLabelsFromOfbizComponents();

        return references;
    }

    private static void getLabelsFromFtlFiles() throws GeneralException {
        try {
            List<File> ftlFiles = FileUtil.findFiles("ftl", null, null,
                    uiLabelMap);

            for (File ftlFile : ftlFiles) {
                String fileNameURI = ftlFile.toURI().toString();
                String inFile = FileUtil.readString("UTF-8", ftlFile);
                int pos = 0;
                while (pos >= 0) {
                    pos = inFile.indexOf(uiLabelMap, pos);

                    if (pos >= 0) {
                        int endLabel = inFile.indexOf("}", pos);

                        if (endLabel >= 0) {
                            String labelKey = inFile.substring(pos
                                    + uiLabelMap.length(), endLabel);
                            setLabelReference(labelKey, fileNameURI);
                            pos = endLabel;
                        } else {
                            pos = pos + uiLabelMap.length();
                        }
                    }
                }
            }
            /*
             * ftlFiles = FileUtil.findFiles("ftl", null, null, getEntityLabel);
             *
             * for (File ftlFile: ftlFiles) { getFtlEntityLabels(ftlFile,
             * getEntityLabel); }
             */
        } catch (IOException ioe) {
            throw new GeneralException(ioe.getMessage());
        }
    }

    private static void getLabelsFromJavaFiles() throws GeneralException {
        try {
            List<File> javaFiles = FileUtil.findFiles("java", null, null,
                    getMessage);

            for (File javaFile : javaFiles) {
                getJavaLabels(javaFile, getMessage);
            }
        } catch (IOException ioe) {
            throw new GeneralException(ioe.getMessage());
        }
    }

    private static void getJavaLabels(File javaFile, String message)
            throws GeneralException {
        try {
            String fileNameURI = javaFile.toURI().toString();
            String inFile = FileUtil.readString("UTF-8", javaFile);
            int pos = 0;
            while (pos >= 0) {
                pos = inFile.indexOf(message, pos);

                if (pos >= 0) {
                    int offSet = (pos + 200 > inFile.length()) ? inFile
                            .length() : pos + 200;
                    String searchComma = inFile.substring(pos, offSet);
                    int firstComma = searchComma.indexOf(",\"", 0);

                    if (firstComma < 0) {
                        firstComma = searchComma.indexOf(", \"", 0);
                        pos = pos + firstComma + 3;
                    } else {
                        pos = pos + firstComma + 2;
                    }

                    if (firstComma >= 0) {
                        offSet = (pos + 100 > inFile.length()) ? inFile
                                .length() : pos + 100;
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
                            setLabelReference(inFile.substring(pos, endString),
                                    fileNameURI);
                            pos = endString;
                        }
                    }
                    pos += 1;
                }
            }
        } catch (IOException ioe) {
            throw new GeneralException(ioe.getMessage());
        }
    }

    private static void getFtlEntityLabels(File javaFile, String message)
            throws GeneralException {
        try {
            String fileNameURI = javaFile.toURI().toString();
            String inFile = FileUtil.readString("UTF-8", javaFile);
            int pos = 0;
            while (pos >= 0) {
                pos = inFile.indexOf(message, pos);

                if (pos >= 0) {
                    int offSet = (pos + 200 > inFile.length()) ? inFile
                            .length() : pos + 200;
                    String searchDoubleQuote = inFile
                            .substring(pos + 6, offSet);
                    int firstComma = searchDoubleQuote.indexOf("\"", 0);

                    if (firstComma >= 0) {
                        offSet = (firstComma + 100 > inFile.length()) ? inFile
                                .length() : firstComma + 100;
                        String searchLocale = searchDoubleQuote.substring(
                                firstComma, offSet);
                        int endMethodName = searchLocale
                                .indexOf(", locale)", 0);

                        if (endMethodName < 0) {
                            endMethodName = searchLocale.indexOf(",locale)", 0);
                        }
                        if (endMethodName >= 0) {
                            setLabelReference(inFile.substring(pos + 6, pos + 6
                                    + firstComma), fileNameURI);
                            pos = pos + 6;
                        }
                    }
                    pos += 1;
                }
            }
        } catch (IOException ioe) {
            throw new GeneralException(ioe.getMessage());
        }
    }

    private static void getLabelsFromSimpleMethodFiles()
            throws GeneralException {
        try {
            List<File> simpleMethodsFiles = FileUtil.findXmlFiles(null, null,
                    "simple-methods",
                    "http://ofbiz.apache.org/dtds/simple-methods.xsd");

            for (File simpleMethodFile : simpleMethodsFiles) {
                String fileNameURI = simpleMethodFile.toURI().toString();
                Document simpleMethodDocument = UtilXml
                        .readXmlDocument(simpleMethodFile.toURI().toURL());
                Element rootElem = simpleMethodDocument.getDocumentElement();

                for (Element elem1 : UtilXml.childElementList(rootElem)) {
                    checkSimpleMethodTag(elem1, fileNameURI);
                    for (Element elem2 : UtilXml.childElementList(elem1)) {
                        checkSimpleMethodTag(elem2, fileNameURI);
                        for (Element elem3 : UtilXml.childElementList(elem2)) {
                            checkSimpleMethodTag(elem3, fileNameURI);
                            for (Element elem4 : UtilXml
                                    .childElementList(elem3)) {
                                checkSimpleMethodTag(elem4, fileNameURI);
                                for (Element elem5 : UtilXml
                                        .childElementList(elem4)) {
                                    checkSimpleMethodTag(elem5, fileNameURI);
                                    for (Element elem6 : UtilXml
                                            .childElementList(elem5)) {
                                        checkSimpleMethodTag(elem6, fileNameURI);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (IOException ioe) {
            throw new GeneralException(ioe.getMessage());
        } catch (Exception e) {
            throw new GeneralException(e.getMessage());
        }
    }

    private static void checkSimpleMethodTag(Element elem, String fileNameURI)
            throws GeneralException {
        // fail-property labels
        if ("fail-property".equals(elem.getTagName())) {
            getFailPropertyTag(elem, fileNameURI);
        }
    }

    private static void getLabelsFromFormWidgets() throws GeneralException {
        try {
            List<File> formsFiles = FileUtil.findXmlFiles(null, null, "forms",
                    "http://ofbiz.apache.org/dtds/widget-form.xsd");

            for (File formsFile : formsFiles) {
                String fileNameURI = formsFile.toURI().toString();
                Document formDocument = UtilXml.readXmlDocument(formsFile
                        .toURI().toURL());
                Element rootElem = formDocument.getDocumentElement();

                for (Element elem1 : UtilXml.childElementList(rootElem)) {
                    Map<String, String> autoFieldsEntity = FastMap
                            .newInstance();
                    Map<String, String> autoFieldsService = FastMap
                            .newInstance();
                    checkFormsTag(elem1, fileNameURI, autoFieldsEntity,
                            autoFieldsService);
                    for (Element elem2 : UtilXml.childElementList(elem1)) {
                        checkFormsTag(elem2, fileNameURI, autoFieldsEntity,
                                autoFieldsService);
                        for (Element elem3 : UtilXml.childElementList(elem2)) {
                            checkFormsTag(elem3, fileNameURI, autoFieldsEntity,
                                    autoFieldsService);
                            for (Element elem4 : UtilXml
                                    .childElementList(elem3)) {
                                checkFormsTag(elem4, fileNameURI,
                                        autoFieldsEntity, autoFieldsService);
                                for (Element elem5 : UtilXml
                                        .childElementList(elem4)) {
                                    checkFormsTag(elem5, fileNameURI,
                                            autoFieldsEntity, autoFieldsService);
                                }
                            }
                        }
                    }
                    for (Map.Entry<String, String> entry : autoFieldsEntity
                            .entrySet()) {
                        if ("N".equals(entry.getValue())) {
                            String labelKey = formFieldTitle + entry.getKey();
                            setLabelReference(labelKey, fileNameURI);
                        }
                    }

                    for (Map.Entry<String, String> entry : autoFieldsService
                            .entrySet()) {
                        if ("N".equals(entry.getValue())) {
                            String labelKey = formFieldTitle + entry.getKey();
                            setLabelReference(labelKey, fileNameURI);
                        }
                    }
                }
            }
        } catch (IOException ioe) {
            throw new GeneralException(ioe.getMessage());
        } catch (Exception e) {
            throw new GeneralException(e.getMessage());
        }
    }

    private static void checkFormsTag(Element elem, String fileNameURI,
            Map<String, String> autoFieldsEntity,
            Map<String, String> autoFieldsService) throws GeneralException {
        // auto fields entity labels
        if ("auto-fields-entity".equals(elem.getTagName())) {
            getAutoFieldsEntityTag(elem, fileNameURI, autoFieldsEntity);
            // auto fields service labels
        } else if ("auto-fields-service".equals(elem.getTagName())) {
            getAutoFieldsServiceTag(elem, fileNameURI, autoFieldsService);
            // field labels
        } else if ("field".equals(elem.getTagName())) {
            getFieldTag(elem, fileNameURI, autoFieldsEntity, autoFieldsService);
            // option description labels
        } else if ("option".equals(elem.getTagName())) {
            getOptionTag(elem, fileNameURI);
            // hyperlink/sub-hyperlink description labels
        } else if ("hyperlink".equals(elem.getTagName())
                || "sub-hyperlink".equals(elem.getTagName())) {
            getHyperlinkTag(elem, fileNameURI);
            // entity-options labels
        } else if ("entity-options".equals(elem.getTagName())) {
            getEntityOptionsTag(elem, fileNameURI);
            // display-entity labels
        } else if ("display-entity".equals(elem.getTagName())) {
            getDisplayEntityTag(elem, fileNameURI);
        }
    }

    private static void getLabelsFromScreenWidgets() throws GeneralException {
        try {
            List<File> screensFiles = FileUtil
                    .findXmlFiles(null, null, "screens",
                            "http://ofbiz.apache.org/dtds/widget-screen.xsd");

            for (File screensFile : screensFiles) {
                String fileNameURI = screensFile.toURI().toString();
                Document screenDocument = UtilXml.readXmlDocument(screensFile
                        .toURI().toURL());
                Element rootElem = screenDocument.getDocumentElement();

                for (Element elem1 : UtilXml.childElementList(rootElem)) {
                    checkScreensTag(elem1, fileNameURI);
                    for (Element elem2 : UtilXml.childElementList(elem1)) {
                        checkScreensTag(elem2, fileNameURI);
                        for (Element elem3 : UtilXml.childElementList(elem2)) {
                            checkScreensTag(elem3, fileNameURI);
                            for (Element elem4 : UtilXml
                                    .childElementList(elem3)) {
                                checkScreensTag(elem4, fileNameURI);
                                for (Element elem5 : UtilXml
                                        .childElementList(elem4)) {
                                    checkScreensTag(elem5, fileNameURI);
                                    for (Element elem6 : UtilXml
                                            .childElementList(elem5)) {
                                        checkScreensTag(elem6, fileNameURI);
                                        for (Element elem7 : UtilXml
                                                .childElementList(elem6)) {
                                            checkScreensTag(elem7, fileNameURI);
                                            for (Element elem8 : UtilXml
                                                    .childElementList(elem7)) {
                                                checkScreensTag(elem8,
                                                        fileNameURI);
                                                for (Element elem9 : UtilXml
                                                        .childElementList(elem8)) {
                                                    checkScreensTag(elem9,
                                                            fileNameURI);
                                                    for (Element elem10 : UtilXml
                                                            .childElementList(elem9)) {
                                                        checkScreensTag(elem10,
                                                                fileNameURI);
                                                        for (Element elem11 : UtilXml
                                                                .childElementList(elem10)) {
                                                            checkScreensTag(
                                                                    elem11,
                                                                    fileNameURI);
                                                            for (Element elem12 : UtilXml
                                                                    .childElementList(elem11)) {
                                                                checkScreensTag(
                                                                        elem12,
                                                                        fileNameURI);
                                                                for (Element elem13 : UtilXml
                                                                        .childElementList(elem12)) {
                                                                    checkScreensTag(
                                                                            elem13,
                                                                            fileNameURI);
                                                                    for (Element elem14 : UtilXml
                                                                            .childElementList(elem13)) {
                                                                        checkScreensTag(
                                                                                elem14,
                                                                                fileNameURI);
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
        } catch (IOException ioe) {
            throw new GeneralException(ioe.getMessage());
        } catch (Exception e) {
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
            List<File> menusFiles = FileUtil.findXmlFiles(null, null, "menus",
                    "http://ofbiz.apache.org/dtds/widget-menu.xsd");

            for (File menuFiles : menusFiles) {
                String fileNameURI = menuFiles.toURI().toString();
                Document menuDocument = UtilXml.readXmlDocument(menuFiles
                        .toURI().toURL());
                Element rootElem = menuDocument.getDocumentElement();

                for (Element elem1 : UtilXml.childElementList(rootElem)) {
                    checkMenuTag(elem1, fileNameURI);
                    for (Element elem2 : UtilXml.childElementList(elem1)) {
                        checkMenuTag(elem2, fileNameURI);
                        for (Element elem3 : UtilXml.childElementList(elem2)) {
                            checkMenuTag(elem3, fileNameURI);
                            for (Element elem4 : UtilXml
                                    .childElementList(elem3)) {
                                checkMenuTag(elem4, fileNameURI);
                                for (Element elem5 : UtilXml
                                        .childElementList(elem4)) {
                                    checkMenuTag(elem5, fileNameURI);
                                }
                            }
                        }
                    }
                }
            }
        } catch (IOException ioe) {
            throw new GeneralException(ioe.getMessage());
        } catch (Exception e) {
            throw new GeneralException(e.getMessage());
        }
    }

    private static void checkMenuTag(Element elem, String fileNameURI) {
        // menu-item labels
        if ("menu-item".equals(elem.getTagName())) {
            getMenuItemTag(elem, fileNameURI);
        }
    }

    private static void getLabelsFromTreeWidgets() throws GeneralException {
        try {
            List<File> treeFiles = FileUtil.findXmlFiles(null, null, "menus",
                    "http://ofbiz.apache.org/dtds/widget-tree.xsd");

            for (File treeFile : treeFiles) {
                String fileNameURI = treeFile.toURI().toString();
                Document menuDocument = UtilXml.readXmlDocument(treeFile
                        .toURI().toURL());
                Element rootElem = menuDocument.getDocumentElement();

                for (Element elem1 : UtilXml.childElementList(rootElem)) {
                    checkTreeTag(elem1, fileNameURI);
                    for (Element elem2 : UtilXml.childElementList(elem1)) {
                        checkTreeTag(elem2, fileNameURI);
                        for (Element elem3 : UtilXml.childElementList(elem2)) {
                            checkTreeTag(elem3, fileNameURI);
                            for (Element elem4 : UtilXml
                                    .childElementList(elem3)) {
                                checkTreeTag(elem4, fileNameURI);
                                for (Element elem5 : UtilXml
                                        .childElementList(elem4)) {
                                    checkTreeTag(elem5, fileNameURI);
                                }
                            }
                        }
                    }
                }
            }
        } catch (IOException ioe) {
            throw new GeneralException(ioe.getMessage());
        } catch (Exception e) {
            throw new GeneralException(e.getMessage());
        }
    }

    private static void checkTreeTag(Element elem, String fileNameURI) {
        // link labels
        if ("link".equals(elem.getTagName())) {
            getLinkTag(elem, fileNameURI);
        }
    }

    private static void getLabelsFromOfbizComponents() throws GeneralException {
        try {
            List<File> componentsFiles = FileUtil.findXmlFiles(null, null,
                    "ofbiz-component",
                    "http://ofbiz.apache.org/dtds/ofbiz-component.xsd");

            for (File componentFile : componentsFiles) {
                String fileNameURI = componentFile.toURI().toString();
                Document menuDocument = UtilXml.readXmlDocument(componentFile
                        .toURI().toURL());
                Element rootElem = menuDocument.getDocumentElement();

                for (Element elem1 : UtilXml.childElementList(rootElem)) {
                    checkOfbizComponentTag(elem1, fileNameURI);
                    for (Element elem2 : UtilXml.childElementList(elem1)) {
                        checkOfbizComponentTag(elem2, fileNameURI);
                    }
                }
            }
        } catch (IOException ioe) {
            throw new GeneralException(ioe.getMessage());
        } catch (Exception e) {
            throw new GeneralException(e.getMessage());
        }
    }

    private static void checkOfbizComponentTag(Element elem, String fileNameURI) {
        // webapp labels
        if ("webapp".equals(elem.getTagName())) {
            getWebappTag(elem, fileNameURI);
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
            } else {
                labelsInFile = new Integer(labelsInFile.intValue() + 1);
            }
            reference.put(fileNameURI, labelsInFile);
        }
    }

    private static boolean getLabelFromTag(Element element, String fileNameURI,
            String attributeValue, String stringToSearch) {
        boolean stringFound = false;

        if (UtilValidate.isNotEmpty(attributeValue)) {
            int pos = 0;

            while (pos >= 0) {
                pos = attributeValue.indexOf(stringToSearch, pos);

                if (pos >= 0) {
                    int graph = attributeValue.indexOf("}", pos);

                    if (graph >= 0) {
                        String labelKey = attributeValue.substring(pos
                                + stringToSearch.length(), graph);
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
        String setField = UtilFormatOut
                .checkNull(element.getAttribute("field"));
        String setValue = UtilFormatOut
                .checkNull(element.getAttribute("value"));
        String fromField = UtilFormatOut.checkNull(element
                .getAttribute("from-field"));

        if (UtilValidate.isNotEmpty(setField)) {
            if (UtilValidate.isNotEmpty(setValue)
                    && ("applicationTitle".equals(setField)
                            || "titleProperty".equals(setField) || "title"
                            .equals(setField))) {
                // set field with hardcoded labels
                if (!getLabelFromTag(element, fileNameURI, setValue, uiLabelMap)) {
                    setLabelReference(setValue, fileNameURI);
                }
            } else if (UtilValidate.isNotEmpty(fromField)
                    && ("layoutSettings.companyName".equals(setField) || "layoutSettings.companySubtitle"
                            .equals(setField))) {
                // set field labels
                if (fromField.startsWith(uiLabelMapInLayoutSettings)) {
                    setLabelReference(fromField.substring(
                            uiLabelMapInLayoutSettings.length(), fromField
                                    .length()), fileNameURI);
                    // set field with hardcoded labels
                } else {
                    setLabelReference(fromField, fileNameURI);
                }
            }
        }
    }

    private static void getScreenletTag(Element element, String fileNameURI) {
        String screenTitle = UtilFormatOut.checkNull(element
                .getAttribute("title"));

        if (UtilValidate.isNotEmpty(screenTitle)) {
            // screenlet title with hardcoded labels
            if (!getLabelFromTag(element, fileNameURI, screenTitle, uiLabelMap)) {
                setLabelReference(screenTitle, fileNameURI);
            }
        }
    }

    private static void getAutoFieldsEntityTag(Element element,
            String fileNameURI, Map<String, String> autoFieldsEntity)
            throws GeneralException {
        try {
            String entityName = UtilFormatOut.checkNull(element
                    .getAttribute("entity-name"));
            String defaultFieldType = UtilFormatOut.checkNull(element
                    .getAttribute("default-field-type"));

            if (UtilValidate.isNotEmpty(entityName)
                    && UtilValidate.isNotEmpty(defaultFieldType)
                    && (!("hidden".equals(defaultFieldType)))) {
                ModelEntity entity = LabelManagerFactory.getModelReader()
                        .getModelEntity(entityName);

                for (Iterator<ModelField> f = entity.getFieldsIterator(); f
                        .hasNext();) {
                    ModelField field = f.next();
                    autoFieldsEntity.put(field.getName(), "N");
                }
            }
        } catch (Exception e) {
            throw new GeneralException(e.getMessage());
        }
    }

    private static void getAutoFieldsServiceTag(Element element,
            String fileNameURI, Map<String, String> autoFieldsService)
            throws GeneralException {
        try {
            String serviceName = UtilFormatOut.checkNull(element
                    .getAttribute("service-name"));
            String defaultFieldType = UtilFormatOut.checkNull(element
                    .getAttribute("default-field-type"));

            if (UtilValidate.isNotEmpty(serviceName)
                    && (!("hidden".equals(defaultFieldType)))) {
                ModelService modelService = LabelManagerFactory
                        .getDispatchContext().getModelService(serviceName);
                List<ModelParam> modelParams = modelService
                        .getInModelParamList();
                Iterator<ModelParam> modelParamIter = modelParams.iterator();

                while (modelParamIter.hasNext()) {
                    ModelParam modelParam = modelParamIter.next();
                    // skip auto params that the service engine populates...
                    if ("userLogin".equals(modelParam.name)
                            || "locale".equals(modelParam.name)
                            || "timeZone".equals(modelParam.name)) {
                        continue;
                    }

                    if (modelParam.formDisplay) {
                        if (UtilValidate.isNotEmpty(modelParam.entityName)
                                && UtilValidate
                                        .isNotEmpty(modelParam.fieldName)) {
                            ModelEntity modelEntity;
                            try {
                                modelEntity = LabelManagerFactory
                                        .getModelReader().getModelEntity(
                                                modelParam.entityName);

                                if (modelEntity != null) {
                                    ModelField modelField = modelEntity
                                            .getField(modelParam.fieldName);

                                    if (modelField != null) {
                                        autoFieldsService.put(modelField
                                                .getName(), "N");
                                    }
                                }
                            } catch (GenericEntityException e) {
                                Debug.logError(e, module);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            throw new GeneralException(e.getMessage());
        }
    }

    private static void getHyperlinkTag(Element element, String fileNameURI) {
        String hyperlinkDescription = UtilFormatOut.checkNull(element
                .getAttribute("description"));

        if (UtilValidate.isNotEmpty(hyperlinkDescription)) {
            // hyperlink description with hardcoded labels
            if (!getLabelFromTag(element, fileNameURI, hyperlinkDescription,
                    uiLabelMap)) {
                setLabelReference(hyperlinkDescription, fileNameURI);
            }
        }
    }

    private static void getFieldTag(Element element, String fileNameURI,
            Map<String, String> autoFieldsEntity,
            Map<String, String> autoFieldsService) {
        String fieldName = UtilFormatOut
                .checkNull(element.getAttribute("name"));
        String labelKey = formFieldTitle + fieldName;
        String fieldTitle = UtilFormatOut.checkNull(element
                .getAttribute("title"));
        String tooltip = UtilFormatOut.checkNull(element
                .getAttribute("tooltip"));

        if (UtilValidate.isNotEmpty(autoFieldsEntity)
                && UtilValidate.isNotEmpty(autoFieldsEntity.get(fieldName))) {
            autoFieldsEntity.put(fieldName, "Y");
        }

        if (UtilValidate.isNotEmpty(autoFieldsService)
                && UtilValidate.isNotEmpty(autoFieldsService.get(fieldName))) {
            autoFieldsService.put(fieldName, "Y");
        }

        boolean escludeField = false;

        for (Element fieldTypeElem : UtilXml.childElementList(element)) {
            if ("hidden".equals(fieldTypeElem.getTagName())) {
                escludeField = true;
            } else if ("ignored".equals(fieldTypeElem.getTagName())) {
                escludeField = true;
            }
        }

        if (!escludeField) {
            // field name labels
            if (UtilValidate.isEmpty(fieldTitle)) {
                setLabelReference(labelKey, fileNameURI);
            } else {
                // field title with hardcoded labels
                if (!getLabelFromTag(element, fileNameURI, fieldTitle,
                        uiLabelMap)) {
                    setLabelReference(fieldTitle, fileNameURI);
                }
            }

            if (UtilValidate.isNotEmpty(tooltip)) {
                // tooltip with hardcoded labels
                if (!getLabelFromTag(element, fileNameURI, tooltip, uiLabelMap)) {
                    setLabelReference(tooltip, fileNameURI);
                }
            }
        }
    }

    private static void getLabelTag(Element element, String fileNameURI) {
        String labelText = UtilFormatOut
                .checkNull(element.getAttribute("text"));
        String labelValue = UtilFormatOut.checkNull(UtilXml
                .elementValue(element));

        // label text labels
        if (UtilValidate.isNotEmpty(labelText)) {
            // label text with hardcoded labels
            if (!getLabelFromTag(element, fileNameURI, labelText, uiLabelMap)) {
                setLabelReference(labelText, fileNameURI);
            }
            // label value labels
        } else if (UtilValidate.isNotEmpty(labelValue)) {
            // label value with hardcoded labels
            if (!getLabelFromTag(element, fileNameURI, labelValue, uiLabelMap)) {
                setLabelReference(labelValue, fileNameURI);
            }
        }
    }

    private static void getMenuItemTag(Element element, String fileNameURI) {
        String menuItemTitle = UtilFormatOut.checkNull(element
                .getAttribute("title"));

        if (UtilValidate.isNotEmpty(menuItemTitle)) {
            // menu item title with hardcoded labels
            if (!getLabelFromTag(element, fileNameURI, menuItemTitle,
                    uiLabelMap)) {
                setLabelReference(menuItemTitle, fileNameURI);
            }
        }
    }

    private static void getFailPropertyTag(Element element, String fileNameURI) {
        String propertyValue = UtilFormatOut.checkNull(element
                .getAttribute("property"));

        if (UtilValidate.isNotEmpty(propertyValue)) {
            // fail-property labels
            setLabelReference(propertyValue, fileNameURI);
        }
    }

    private static void getOptionTag(Element element, String fileNameURI) {
        String description = UtilFormatOut.checkNull(element
                .getAttribute("description"));

        if (UtilValidate.isNotEmpty(description)) {
            // option description with hardcoded labels
            if (!getLabelFromTag(element, fileNameURI, description, uiLabelMap)) {
                setLabelReference(description, fileNameURI);
            }
        }
    }

    private static void getLinkTag(Element element, String fileNameURI) {
        String linkText = UtilFormatOut.checkNull(element.getAttribute("text"));

        if (UtilValidate.isNotEmpty(linkText)) {
            // link text with hardcoded labels
            if (!getLabelFromTag(element, fileNameURI, linkText, uiLabelMap)) {
                setLabelReference(linkText, fileNameURI);
            }
        }
    }

    private static void getWebappTag(Element element, String fileNameURI) {
        String title = UtilFormatOut.checkNull(element.getAttribute("title"));
        String appBarDisplay = UtilFormatOut.checkNull(element
                .getAttribute("app-bar-display"));

        // title labels
        if (UtilValidate.isNotEmpty(title)
                && UtilValidate.isNotEmpty(appBarDisplay)
                && "true".equalsIgnoreCase(appBarDisplay)) {
            setLabelReference(title, fileNameURI);
        }
    }

    private static void getEntityOptionsTag(Element element, String fileNameURI) {
        String entityName = UtilFormatOut.checkNull(element
                .getAttribute("entity-name"));
        String description = UtilFormatOut.checkNull(element
                .getAttribute("description"));
        Set<String> fields = new TreeSet<String>();
        Set<String> pkFields = new TreeSet<String>();

        try {

            if (UtilValidate.isNotEmpty(entityName)) {
                GenericDelegator delegator = LabelManagerFactory.getDelegator();
                ModelEntity entity = delegator.getModelEntity(entityName);

                if (UtilValidate.isNotEmpty(entity)
                        && UtilValidate.isNotEmpty(entity
                                .getDefaultResourceName())) {
                    int pos = 0;
                    while (pos >= 0) {
                        pos = description.indexOf(startExpression, pos);

                        if (pos >= 0) {
                            int endLabel = description.indexOf(endExpression,
                                    pos);

                            if (endLabel >= 0) {
                                String fieldName = description.substring(pos
                                        + startExpression.length(), endLabel);
                                if (!fieldName
                                        .startsWith(uiLabelMapInLayoutSettings)) {
                                    for (Map.Entry<String, LabelInfo> e : LabelManagerFactory
                                            .getLabels().entrySet()) {
                                        String keyToSearch = entityName + "."
                                                + fieldName;
                                        if (e.getKey().startsWith(keyToSearch)) {
                                            fields.add(fieldName);
                                        }
                                    }
                                }
                                pos = endLabel;
                            } else {
                                pos = pos + startExpression.length();
                            }
                        }
                    }

                    // Search primary keys of entity
                    Iterator<ModelField> iter = entity.getPksIterator();
                    while (iter != null && iter.hasNext()) {
                        ModelField curField = iter.next();
                        pkFields.add(curField.getName());
                    }
                    Iterator<String> fieldsIt = fields.iterator();
                    while (fieldsIt != null && fieldsIt.hasNext()) {
                        String fieldName = fieldsIt.next();
                        List<EntityExpr> exprs = FastList.newInstance();
                        for (Element entityConstraintElem : UtilXml
                                .childElementList(element)) {
                            if ("entity-constraint".equals(entityConstraintElem
                                    .getTagName())) {
                                String constraintName = UtilFormatOut
                                        .checkNull(entityConstraintElem
                                                .getAttribute("name"));
                                String constraintOperator = UtilFormatOut
                                        .checkNull(entityConstraintElem
                                                .getAttribute("operator"));
                                String constraintValue = UtilFormatOut
                                        .checkNull(entityConstraintElem
                                                .getAttribute("value"));

                                EntityComparisonOperator operator = new EntityComparisonOperator(
                                        EntityOperator.ID_EQUALS, "=");
                                if ("between".equals(constraintValue)) {
                                    operator = new EntityComparisonOperator(
                                            EntityOperator.ID_BETWEEN,
                                            "BETWEEN");
                                } else if ("greater-equals"
                                        .equals(constraintValue)) {
                                    operator = new EntityComparisonOperator(
                                            EntityOperator.ID_GREATER_THAN_EQUAL_TO,
                                            ">=");
                                } else if ("greater".equals(constraintValue)) {
                                    operator = new EntityComparisonOperator(
                                            EntityOperator.ID_GREATER_THAN, ">");
                                } else if ("in".equals(constraintValue)) {
                                    operator = new EntityComparisonOperator(
                                            EntityOperator.ID_IN, "IN");
                                } else if ("less-equals"
                                        .equals(constraintValue)) {
                                    operator = new EntityComparisonOperator(
                                            EntityOperator.ID_LESS_THAN_EQUAL_TO,
                                            "<=");
                                } else if ("less".equals(constraintValue)) {
                                    operator = new EntityComparisonOperator(
                                            EntityOperator.ID_LESS_THAN, "<");
                                } else if ("like".equals(constraintValue)) {
                                    operator = new EntityComparisonOperator(
                                            EntityOperator.ID_LIKE, "LIKE");
                                } else if ("not-equals".equals(constraintValue)) {
                                    operator = new EntityComparisonOperator(
                                            EntityOperator.ID_NOT_EQUAL, "<>");
                                }

                                exprs.add(EntityCondition.makeCondition(
                                        constraintName, operator,
                                        constraintValue));
                            }
                        }

                        EntityConditionList<EntityExpr> ecl = null;
                        if (exprs.size() > 0) {
                            ecl = EntityCondition.makeCondition(exprs,
                                    EntityOperator.AND);
                        }

                        StringBuilder keyBuffer = new StringBuilder();
                        keyBuffer.append(entityName);
                        keyBuffer.append('.');
                        keyBuffer.append(fieldName);
                        List<GenericValue> entityRecords = delegator.findList(
                                entityName, ecl, pkFields, null, null, false);

                        for (GenericValue entityRecord : entityRecords) {
                            boolean pkFound = false;
                            StringBuilder pkBuffer = new StringBuilder(
                                    keyBuffer.toString());
                            Iterator<String> itPkFields = pkFields.iterator();
                            while (itPkFields != null && itPkFields.hasNext()) {
                                String pkField = itPkFields.next();
                                Object pkFieldValue = entityRecord.get(pkField);

                                if (UtilValidate.isNotEmpty(pkFieldValue)) {
                                    pkBuffer.append('.');
                                    pkBuffer.append(pkFieldValue);
                                    pkFound = true;
                                }
                            }
                            if (pkFound) {
                                setLabelReference(pkBuffer.toString(),
                                        fileNameURI);
                            }
                        }
                    }
                }
            }
        } catch (GenericEntityException e) {
            Debug.logError(e, "Problem getting records from " + entityName,
                    module);
        }
    }

    private static void getDisplayEntityTag(Element element, String fileNameURI) {
        String entityName = UtilFormatOut.checkNull(element
                .getAttribute("entity-name"));
        String description = UtilFormatOut.checkNull(element
                .getAttribute("description"));
        Set<String> fields = new TreeSet<String>();
        Set<String> pkFields = new TreeSet<String>();

        try {

            if (UtilValidate.isNotEmpty(entityName)) {
                GenericDelegator delegator = LabelManagerFactory.getDelegator();
                ModelEntity entity = delegator.getModelEntity(entityName);

                if (UtilValidate.isNotEmpty(entity)
                        && UtilValidate.isNotEmpty(entity
                                .getDefaultResourceName())) {
                    int pos = 0;
                    while (pos >= 0) {
                        pos = description.indexOf(startExpression, pos);

                        if (pos >= 0) {
                            int endLabel = description.indexOf(endExpression,
                                    pos);

                            if (endLabel >= 0) {
                                String fieldName = description.substring(pos
                                        + startExpression.length(), endLabel);
                                if (!fieldName
                                        .startsWith(uiLabelMapInLayoutSettings)) {
                                    for (Map.Entry<String, LabelInfo> e : LabelManagerFactory
                                            .getLabels().entrySet()) {
                                        String keyToSearch = entityName + "."
                                                + fieldName;
                                        if (e.getKey().startsWith(keyToSearch)) {
                                            fields.add(fieldName);
                                        }
                                    }
                                }
                                pos = endLabel;
                            } else {
                                pos = pos + startExpression.length();
                            }
                        }
                    }

                    // Search primary keys of entity
                    Iterator<ModelField> iter = entity.getPksIterator();
                    while (iter != null && iter.hasNext()) {
                        ModelField curField = iter.next();
                        pkFields.add(curField.getName());
                    }
                    Iterator<String> fieldsIt = fields.iterator();
                    while (fieldsIt != null && fieldsIt.hasNext()) {
                        String fieldName = fieldsIt.next();
                        StringBuilder keyBuffer = new StringBuilder();
                        keyBuffer.append(entityName);
                        keyBuffer.append('.');
                        keyBuffer.append(fieldName);
                        List<GenericValue> entityRecords = delegator.findList(
                                entityName, null, pkFields, null, null, false);

                        for (GenericValue entityRecord : entityRecords) {
                            boolean pkFound = false;
                            StringBuilder pkBuffer = new StringBuilder(
                                    keyBuffer.toString());
                            Iterator<String> itPkFields = pkFields.iterator();
                            while (itPkFields != null && itPkFields.hasNext()) {
                                String pkField = itPkFields.next();
                                Object pkFieldValue = entityRecord.get(pkField);

                                if (UtilValidate.isNotEmpty(pkFieldValue)) {
                                    pkBuffer.append('.');
                                    pkBuffer.append(pkFieldValue);
                                    pkFound = true;
                                }
                            }
                            if (pkFound) {
                                setLabelReference(pkBuffer.toString(),
                                        fileNameURI);
                            }
                        }
                    }
                }
            }
        } catch (GenericEntityException e) {
            Debug.logError(e, "Problem getting records from " + entityName,
                    module);
        }
    }
}

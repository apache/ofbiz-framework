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

import javolution.util.FastList;

import org.ofbiz.base.component.ComponentConfig;
import org.ofbiz.base.util.FileUtil;
import org.ofbiz.base.util.GeneralException;
import org.ofbiz.base.util.StringUtil;
import org.ofbiz.base.util.UtilGenerics;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilProperties;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.base.util.UtilXml;
import org.ofbiz.base.util.cache.UtilCache;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.model.ModelReader;
import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.GenericServiceException;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.service.ServiceUtil;
import org.w3c.dom.Comment;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

public class LabelManagerFactory {

    public static final String module = LabelManagerFactory.class.getName();
    public static final String resource = "WebtoolsUiLabels";

    public static final String keySeparator = "#";

    protected static UtilCache<String, LabelManagerFactory> labelManagerFactoryCache = new UtilCache<String, LabelManagerFactory>("LabelManagerFactory");

    protected static Map<String, LabelInfo> labels = null;
    protected static Map<String, String> fileNamesFound = null;
    protected static Map<String, String> fileComponent = null;
    protected static Set<String> localesFound = null;
    protected static Set<String> componentNamesFound = null;
    protected static Map<String, Map<String, Integer>> references = null;
    protected static List<LabelInfo> duplicatedLocalesLabelsList = null;
    protected static int duplicatedLocalesLabels = 0;

    protected static GenericDelegator delegator;
    protected static ModelReader entityModelReader;
    protected static DispatchContext dispatchContext;

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
        delegator = GenericDelegator.getGenericDelegator(delegatorName);
        entityModelReader = ModelReader.getModelReader(delegatorName);
        dispatchContext = new DispatchContext("LabelManagerFactoryDispCtx", null, this.getClass().getClassLoader(), null);
        prepareAll();
    }

    private static void prepareAll() throws GeneralException {
        labels = new TreeMap<String, LabelInfo>();
        fileNamesFound = new TreeMap<String, String>();
        fileComponent = new TreeMap<String, String>();
        localesFound = new TreeSet<String>();
        componentNamesFound = new TreeSet<String>();
        duplicatedLocalesLabelsList = FastList.newInstance();
        references = null;
        int duplicatedLocales = 0;

        try {
            boolean sharkComponent = false;
            Collection<ComponentConfig> componentConfigs = ComponentConfig.getAllComponents();

            for (ComponentConfig componentConfig : componentConfigs) {
                String componentName = componentConfig.getComponentName();
                List<File> resourceFiles = FileUtil.findXmlFiles(componentConfig.getRootLocation(), null, "resource", null);
                boolean cycle = true;
                while (cycle) {
                    for (File resourceFile : resourceFiles) {
                        String fileName = resourceFile.getName();
                        Document resourceDocument = UtilXml.readXmlDocument(resourceFile.toURI().toURL());
                        Element resourceElem = resourceDocument.getDocumentElement();
                        String labelKeyComment = "";

                        for (Node propertyNode : UtilXml.childNodeList(resourceElem.getFirstChild())) {
                            if (propertyNode instanceof Element) {
                                Element propertyElem = (Element) propertyNode;
                                String labelKey = StringUtil.defaultWebEncoder.canonicalize(propertyElem.getAttribute("key"));
                                String labelComment = "";

                                for (Node valueNode : UtilXml.childNodeList(propertyElem.getFirstChild())) {
                                    if (valueNode instanceof Element) {
                                        Element valueElem = (Element) valueNode;
                                        String localeName = valueElem.getAttribute("xml:lang");
                                        String labelValue = StringUtil.defaultWebEncoder.canonicalize(UtilXml.nodeValue(valueElem.getFirstChild()));
                                        LabelInfo label = labels.get(labelKey + keySeparator + fileName);

                                        if (UtilValidate.isEmpty(label)) {
                                            label = new LabelInfo(labelKey, labelKeyComment, fileName, componentName, localeName, labelValue, labelComment);
                                            labels.put(labelKey + keySeparator + fileName, label);
                                        } else {
                                            if (label.setLabelValue(localeName, labelValue, labelComment, false)) {
                                                duplicatedLocalesLabelsList.add(label);
                                                duplicatedLocales++;
                                            }
                                        }
                                        localesFound.add(localeName);
                                        componentNamesFound.add(componentName);
                                        fileNamesFound.put(fileName, resourceFile.toURI().toString());
                                        fileComponent.put(fileName, componentName);
                                        labelComment = "";
                                    } else if (valueNode instanceof Comment) {
                                        labelComment = labelComment + StringUtil.defaultWebEncoder.canonicalize(valueNode.getNodeValue());
                                    }
                                }
                                labelKeyComment = "";
                            } else if (propertyNode instanceof Comment) {
                                labelKeyComment = labelKeyComment + StringUtil.defaultWebEncoder.canonicalize(propertyNode.getNodeValue());
                            }
                        }
                    }
                    if (!sharkComponent) {
                        componentName = "shark";
                        resourceFiles = FileUtil.findXmlFiles(System.getProperty("ofbiz.home") + "/specialpurpose/shark", null, "resource", null);
                        sharkComponent = true;
                    } else {
                        cycle = false;
                    }
                }
            }

            // get labels references from sources
            references = LabelReferences.getLabelReferences();
        } catch (IOException ioe) {
            throw new GeneralException(ioe.getMessage());
        } catch (Exception e) {
            throw new GeneralException(e.getMessage());
        }
        duplicatedLocalesLabels = duplicatedLocales;
    }

    public static GenericDelegator getDelegator() {
        return delegator;
    }

    public static ModelReader getModelReader() {
        return entityModelReader;
    }

    public static DispatchContext getDispatchContext() {
        return dispatchContext;
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

    public static Map<String, Map<String, Integer>> getReferences() {
        return references;
    }

    public static Set<String> getLabelsList() {
        return labels.keySet();
    }

    public static Set<String> getReferencesList() {
        return references.keySet();
    }

    public static int getLabelReferenceFile(String key) {
        int refFile = 0;
        boolean keyFound = false;

        if (key == null) {
            key = "";
        }

        for (Map.Entry<String, String> e : fileNamesFound.entrySet()) {
            String keyToSearch = key + keySeparator + e.getKey();

            if (labels.containsKey(keyToSearch)) {
                keyFound = true;
                break;
            }
        }

        if (!keyFound) {
            Map<String, Integer> reference = references.get(key);

            if (UtilValidate.isNotEmpty(reference)) {
                refFile = reference.size();
            }
        }

        return refFile;
    }

    public static int getDuplicatedLocalesLabels() {
        return duplicatedLocalesLabels;
    }

    public static List<LabelInfo> getDuplicatedLocalesLabelsList() {
        return duplicatedLocalesLabelsList;
    }

    public static Map<String, Object> updateLabelKey(DispatchContext dctx, Map<String, ? extends Object> context) {
        String key = (String) context.get("key");
        String keyComment = (String) context.get("keyComment");
        String update_label = (String) context.get("update_label");
        String fileName = (String) context.get("fileName");
        String confirm = (String) context.get("confirm");
        String removeLabel = (String) context.get("removeLabel");
        List<String> localeNames = UtilGenerics.cast(context.get("localeNames"));
        List<String> localeValues = UtilGenerics.cast(context.get("localeValues"));
        List<String> localeComments = UtilGenerics.cast(context.get("localeComments"));
        Locale locale = (Locale) context.get("locale");

        // Remove a Label
        if (UtilValidate.isNotEmpty(removeLabel)) {
            labels.remove(key + keySeparator + fileName);
        } else if (UtilValidate.isNotEmpty(confirm)) {
            LabelInfo label = labels.get(key + keySeparator + fileName);

            // Update a Label
            if (update_label.equalsIgnoreCase("Y")) {
                if (UtilValidate.isNotEmpty(label)) {
                    updateLabelValue(localeNames, localeValues, localeComments, label, key, keyComment, fileName);
                }
                // Insert a new Label
            } else {
                if (UtilValidate.isNotEmpty(label)) {
                    return ServiceUtil.returnError(UtilProperties.getMessage(resource, "WebtoolsLabelManagerNewLabelExisting", UtilMisc.toMap("key", key, "fileName", fileName), locale));
                } else {
                    if (UtilValidate.isEmpty(key)) {
                        return ServiceUtil.returnError(UtilProperties.getMessage(resource, "WebtoolsLabelManagerNewLabelEmptyKey", locale));
                    } else {
                        int notEmptyLabels = updateLabelValue(localeNames, localeValues, localeComments, null, key, keyComment, fileName);
                        if (notEmptyLabels == 0) {
                            return ServiceUtil.returnError(UtilProperties.getMessage(resource, "WebtoolsLabelManagerNewLabelEmpty", locale));
                        }
                    }
                }
            }
        }

        return ServiceUtil.returnSuccess();
    }

    public static Map<String, Object> updateAndSaveLabelKey(DispatchContext dctx, Map<String, ? extends Object> context) {
        String key = (String) context.get("key");
        String keyComment = (String) context.get("keyComment");
        String update_label = (String) context.get("update_label");
        String fileName = (String) context.get("fileName");
        String confirm = (String) context.get("confirm");
        String removeLabel = (String) context.get("removeLabel");
        List<String> localeNames = UtilGenerics.cast(context.get("localeNames"));
        List<String> localeValues = UtilGenerics.cast(context.get("localeValues"));
        List<String> localeComments = UtilGenerics.cast(context.get("localeComments"));
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        LocalDispatcher dispatcher = dctx.getDispatcher();

        Map<String, Object> contextInput = UtilMisc.toMap("key", key, "keyComment", keyComment, "update_label", update_label, "fileName", fileName, "confirm", confirm, "removeLabel", removeLabel,
                "localeNames", localeNames, "localeValues", localeValues, "localeComments", localeComments, "userLogin", userLogin);
        try {
            Map<String, Object> updatedKey = dispatcher.runSync("updateLabelKey", contextInput);

            if (ServiceUtil.isError(updatedKey)) {
                return updatedKey;
            } else {
                return dispatcher.runSync("saveLabelsToXmlFile", UtilMisc.toMap("labelFileName", fileName, "userLogin", userLogin));
            }
        } catch (GenericServiceException e) {
            return ServiceUtil.returnError("error on saving label key :" + key);
        }
    }

    private static int updateLabelValue(List<String> localeNames, List<String> localeValues, List<String> localeComments, LabelInfo label, String key, String keyComment, String fileName) {
        int notEmptyLabels = 0;
        int i = 0;
        while (i < localeNames.size()) {
            String localeName = localeNames.get(i);
            String localeValue = localeValues.get(i);
            String localeComment = localeComments.get(i);

            if (UtilValidate.isNotEmpty(localeValue) || UtilValidate.isNotEmpty(localeComment)) {
                if (label == null) {
                    try {
                        String componentName = getFileComponent(fileName);
                        label = new LabelInfo(key, keyComment, fileName, componentName, localeName, localeValue, localeComment);
                        labels.put(key + keySeparator + fileName, label);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else {
                    label.setLabelKeyComment(keyComment);
                }
                label.setLabelValue(localeName, localeValue, localeComment, true);
                notEmptyLabels++;
            }
            i++;
        }

        return notEmptyLabels;
    }
}

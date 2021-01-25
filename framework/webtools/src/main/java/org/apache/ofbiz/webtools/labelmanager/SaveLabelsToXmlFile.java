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

import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.FileUtil;
import org.apache.ofbiz.base.util.UtilGenerics;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilProperties;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.base.util.UtilXml;
import org.apache.ofbiz.base.util.cache.UtilCache;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.ServiceUtil;
import org.jsoup.parser.Parser;
import org.w3c.dom.Comment;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

public class SaveLabelsToXmlFile {

    private static final String RESOURCE = "WebtoolsUiLabels";
    private static final String MODULE = SaveLabelsToXmlFile.class.getName();

    public static Map<String, Object> saveLabelsToXmlFile(DispatchContext dctx, Map<String, ? extends Object> context) {
        Locale locale = (Locale) context.get("locale");
        String fileName = (String) context.get("fileName");
        if (UtilValidate.isEmpty(fileName)) {
            Debug.logError("labelFileName cannot be empty", MODULE);
            return ServiceUtil.returnFailure(UtilProperties.getMessage(RESOURCE, "saveLabelsToXmlFile.exceptionDuringSaveLabelsToXmlFile", locale));
        }
        String key = (String) context.get("key");
        String keyComment = (String) context.get("keyComment");
        String updateLabel = (String) context.get("update_label");
        String confirm = (String) context.get("confirm");
        String removeLabel = (String) context.get("removeLabel");
        List<String> localeNames = UtilGenerics.cast(context.get("localeNames"));
        List<String> localeValues = UtilGenerics.cast(context.get("localeValues"));
        List<String> localeComments = UtilGenerics.cast(context.get("localeComments"));
        String apacheLicenseText = null;
        URL apache2Header = SaveLabelsToXmlFile.class.getResource("APACHE2_HEADER_FOR_XML");
        try {
            apacheLicenseText = FileUtil.readString("UTF-8", FileUtil.getFile(apache2Header.getPath()));
        } catch (IOException e) {
            Debug.logWarning(e, "Unable to read Apache License text file", MODULE);
        }
        try {
            LabelManagerFactory factory = LabelManagerFactory.getInstance();
            LabelFile labelFile = factory.getLabelFile(fileName);
            if (labelFile == null) {
                Debug.logError("Invalid file name: " + fileName, MODULE);
                return ServiceUtil.returnFailure(UtilProperties.getMessage(RESOURCE, "saveLabelsToXmlFile.exceptionDuringSaveLabelsToXmlFile",
                        locale));
            }
            synchronized (SaveLabelsToXmlFile.class) {
                factory.findMatchingLabels(null, fileName, null, null, false);
                Map<String, LabelInfo> labels = factory.getLabels();
                Set<String> labelsList = factory.getLabelsList();
                Set<String> localesFound = factory.getLocalesFound();
                for (String localeName : localeNames) {
                    localesFound.add(localeName);
                }
                // Remove a Label
                if (UtilValidate.isNotEmpty(removeLabel)) {
                    labels.remove(key + LabelManagerFactory.KEY_SEPARATOR + fileName);
                } else if (UtilValidate.isNotEmpty(confirm)) {
                    LabelInfo label = labels.get(key + LabelManagerFactory.KEY_SEPARATOR + fileName);
                    // Update a Label
                    if ("Y".equalsIgnoreCase(updateLabel)) {
                        if (UtilValidate.isNotEmpty(label)) {
                            factory.updateLabelValue(localeNames, localeValues, localeComments, label, key, keyComment, fileName);
                        }
                        // Insert a new Label
                    } else {
                        if (UtilValidate.isNotEmpty(label)) {
                            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, "WebtoolsLabelManagerNewLabelExisting",
                                    UtilMisc.toMap("key", key, "fileName", fileName), locale));
                        } else {
                            if (UtilValidate.isEmpty(key)) {
                                return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, "WebtoolsLabelManagerNewLabelEmptyKey", locale));
                            } else {
                                int notEmptyLabels = factory.updateLabelValue(localeNames, localeValues, localeComments, null, key,
                                        keyComment, fileName);
                                if (notEmptyLabels == 0) {
                                    return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, "WebtoolsLabelManagerNewLabelEmpty", locale));
                                }
                            }
                        }
                    }
                }
                Document resourceDocument = UtilXml.makeEmptyXmlDocument("resource");
                Element resourceElem = resourceDocument.getDocumentElement();
                resourceElem.setAttribute("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance");
                resourceElem.setAttribute("xsi:noNamespaceSchemaLocation", "http://ofbiz.apache.org/dtds/ofbiz-properties.xsd");
                for (String labelKey : labelsList) {
                    LabelInfo labelInfo = labels.get(labelKey);
                    if (!(labelInfo.getFileName().equalsIgnoreCase(fileName))) {
                        continue;
                    }
                    Element propertyElem = UtilXml.addChildElement(resourceElem, "property", resourceDocument);
                    propertyElem.setAttribute("key", Parser.unescapeEntities(labelInfo.getLabelKey(), true));
                    if (UtilValidate.isNotEmpty(labelInfo.getLabelKeyComment())) {
                        Comment labelKeyComment = resourceDocument.createComment(Parser.unescapeEntities(labelInfo.getLabelKeyComment(), true));
                        Node parent = propertyElem.getParentNode();
                        parent.insertBefore(labelKeyComment, propertyElem);
                    }
                    for (String localeFound : localesFound) {
                        LabelValue labelValue = labelInfo.getLabelValue(localeFound);
                        String valueString = null;
                        if (labelValue != null) {
                            valueString = labelValue.getLabelValue();
                        }
                        if (UtilValidate.isNotEmpty(valueString)) {
                            valueString = Parser.unescapeEntities(valueString, true);
                            Element valueElem = UtilXml.addChildElementValue(propertyElem, "value", valueString, resourceDocument);
                            valueElem.setAttribute("xml:lang", localeFound);
                            if (UtilValidate.isNotEmpty(labelValue.getLabelComment())) {
                                Comment labelComment = resourceDocument.createComment(Parser.unescapeEntities(labelValue.getLabelComment(), true));
                                Node parent = valueElem.getParentNode();
                                parent.insertBefore(labelComment, valueElem);
                            }
                        }
                    }
                    try (FileOutputStream fos = new FileOutputStream(labelFile.getFile())) {
                        if (apacheLicenseText != null) {
                            fos.write(apacheLicenseText.getBytes());
                        }
                        UtilXml.writeXmlDocument(resourceElem, fos, "UTF-8", !(apacheLicenseText == null), true, 4);
                    } finally {
                        // clear cache to see immediately the new labels and
                        // translations in OFBiz
                        UtilCache.clearCache("properties.UtilPropertiesBundleCache");
                    }
                }
            }
        } catch (Exception e) {
            Debug.logError(e, "Exception during save labels to xml file:", MODULE);
            return ServiceUtil.returnFailure(UtilProperties.getMessage(RESOURCE, "saveLabelsToXmlFile.exceptionDuringSaveLabelsToXmlFile", locale));
        }
        return ServiceUtil.returnSuccess();
    }
}

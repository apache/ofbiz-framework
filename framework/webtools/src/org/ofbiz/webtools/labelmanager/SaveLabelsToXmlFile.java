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
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringEscapeUtils;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.FileUtil;
import org.ofbiz.base.util.UtilProperties;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.base.util.UtilXml;
import org.ofbiz.base.util.cache.UtilCache;

import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.ServiceUtil;

import org.w3c.dom.Comment;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

public class SaveLabelsToXmlFile {

    private static final String resource = "WebtoolsUiLabels";
    private static final String module = SaveLabelsToXmlFile.class.getName();

    public static Map<String, Object> saveLabelsToXmlFile(DispatchContext dctx, Map<String, ? extends Object> context) {
        Locale locale = (Locale)context.get("locale");
        String labelFileName = (String)context.get("labelFileName");
        String apacheLicenseText = null;
        try {
            apacheLicenseText = getApacheLicenseText();
        } catch (IOException e) {
            Debug.logWarning(e, "Unable to read Apache License text file", module);
        }
        try {
            LabelManagerFactory.getLabelManagerFactory(dctx.getDelegator().getDelegatorName());
            Map<String, LabelInfo> labels = LabelManagerFactory.getLabels();
            Map<String, String> fileNamesFound = LabelManagerFactory.getFileNamesFound();
            Set<String> labelsList = LabelManagerFactory.getLabelsList();
            Set<String> localesFound = LabelManagerFactory.getLocalesFound();
            for (String fileName : fileNamesFound.keySet()) {
                if (UtilValidate.isNotEmpty(labelFileName) && !(labelFileName.equalsIgnoreCase(fileName))) {
                    continue;
                }
                String uri = fileNamesFound.get(fileName);
                Document resourceDocument = UtilXml.makeEmptyXmlDocument("resource");
                Element resourceElem = resourceDocument.getDocumentElement();
                resourceElem.setAttribute("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance");
                for (String labelKey : labelsList) {
                    LabelInfo labelInfo = labels.get(labelKey);
                    if (!(labelInfo.getFileName().equalsIgnoreCase(fileName))) {
                        continue;
                    }
                    Element propertyElem = UtilXml.addChildElement(resourceElem, "property", resourceDocument);
                    propertyElem.setAttribute("key", StringEscapeUtils.unescapeHtml(labelInfo.getLabelKey()));
                    if (UtilValidate.isNotEmpty(labelInfo.getLabelKeyComment())) {
                        Comment labelKeyComment = resourceDocument.createComment(StringEscapeUtils.unescapeHtml(labelInfo.getLabelKeyComment()));
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
                            valueString = StringEscapeUtils.unescapeHtml(valueString);
                            Element valueElem = UtilXml.addChildElementValue(propertyElem, "value", valueString, resourceDocument);;
                            valueElem.setAttribute("xml:lang", localeFound);
                            if (valueString.trim().length() == 0) {
                                valueElem.setAttribute("xml:space", "preserve");
                            }
                            if (UtilValidate.isNotEmpty(labelValue.getLabelComment())) {
                                Comment labelComment = resourceDocument.createComment(StringEscapeUtils.unescapeHtml(labelValue.getLabelComment()));
                                Node parent = valueElem.getParentNode();
                                parent.insertBefore(labelComment, valueElem);
                            }
                        }
                    }
                }
                if (UtilValidate.isNotEmpty(uri)) {
                    File outFile = new File(new URI(uri));
                    FileOutputStream fos = new FileOutputStream(outFile);
                    try {
                        if (apacheLicenseText != null) {
                            fos.write(apacheLicenseText.getBytes());
                        }
                        UtilXml.writeXmlDocument(resourceElem, fos, "UTF-8", !(apacheLicenseText == null), true, 4);
                    } finally {
                        fos.close();
                        // clear cache to see immediately the new labels and translations in OFBiz
                        UtilCache.clearCache("properties.UtilPropertiesBundleCache");
                    }
                }
            }
        } catch (Exception e) {
            Debug.logError(e, "Exception during save labels to xml file:", module);
            return ServiceUtil.returnFailure(UtilProperties.getMessage(resource, "saveLabelsToXmlFile.exceptionDuringSaveLabelsToXmlFile", locale));
        }
        return ServiceUtil.returnSuccess();
    }
    
    public static String getApacheLicenseText() throws IOException {
        String apacheLicenseText = null;
        String basePath = System.getProperty("ofbiz.home");
        if (UtilValidate.isNotEmpty(basePath)) {
            String apacheLicenseFileName = basePath + "/framework/webtools/config/APACHE2_HEADER_FOR_XML";
            File apacheLicenseFile = new File(apacheLicenseFileName);
            apacheLicenseText = FileUtil.readString("UTF-8", apacheLicenseFile);
        }
        return apacheLicenseText;
    }
}

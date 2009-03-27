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
import java.net.URI;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.apache.xml.serialize.OutputFormat;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.FileUtil;
import org.ofbiz.base.util.StringUtil;
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

                String uri = (String)fileNamesFound.get(fileName);
                Document resourceDocument = UtilXml.makeEmptyXmlDocument("resource");
                Element resourceElem = resourceDocument.getDocumentElement();
                resourceElem.setAttribute("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance");

                for (String labelKey : labelsList) {
                    LabelInfo labelInfo = (LabelInfo)labels.get(labelKey);

                    if (!(labelInfo.getFileName().equalsIgnoreCase(fileName))) {
                        continue;
                    }

                    Element propertyElem = UtilXml.addChildElement(resourceElem, "property", resourceDocument);
                    propertyElem.setAttribute("key", StringUtil.fromHtmlToSpecialChars(labelInfo.getLabelKey(), true, true, false));

                    if (UtilValidate.isNotEmpty(labelInfo.getLabelKeyComment())) {
                        Comment labelKeyComment = resourceDocument.createComment(StringUtil.fromHtmlToSpecialChars(labelInfo.getLabelKeyComment(), true, true, false));
                        Node parent = propertyElem.getParentNode();
                        parent.insertBefore(labelKeyComment, propertyElem);
                    }

                    for (String localeFound : localesFound) {
                        LabelValue labelValue = labelInfo.getLabelValue(localeFound);

                        if (UtilValidate.isNotEmpty(labelValue)) {
                             Element valueElem = UtilXml.addChildElementValue(propertyElem, "value", StringUtil.fromHtmlToSpecialChars(labelValue.getLabelValue(), true, true, false), resourceDocument);
                            valueElem.setAttribute("xml:lang", localeFound);

                            if (UtilValidate.isNotEmpty(labelValue.getLabelComment())) {
                                Comment labelComment = resourceDocument.createComment(StringUtil.fromHtmlToSpecialChars(labelValue.getLabelComment(), true, true, false));
                                Node parent = valueElem.getParentNode();
                                parent.insertBefore(labelComment, valueElem);
                            }
                        }
                    }
                }

                if (UtilValidate.isNotEmpty(resourceElem) && UtilValidate.isNotEmpty(uri)) {
                    File outFile = new File(new URI(uri));
                    FileOutputStream fos = new FileOutputStream(outFile);
                    OutputFormat format = new OutputFormat(resourceDocument.getDocumentElement().getOwnerDocument(), "UTF-8", true);

                    try {
                        format.setIndent(4);
                        format.setOmitXMLDeclaration(true);
                        UtilXml.writeXmlDocument(fos, resourceElem, format);
                    } finally {
                        if (UtilValidate.isNotEmpty(fos)) {
                               fos.close();

                            // workaround to insert the Apache License Header at top of the file
                            // because the comment on top the xml file has been not written
                            String outBuffer = FileUtil.readString("UTF-8", outFile);
                            String basePath = System.getProperty("ofbiz.home");

                            if (UtilValidate.isNotEmpty(basePath)) {
                                String apacheHeaderFileName = basePath + "/framework/webtools/config/APACHE2_HEADER_FOR_XML";
                                String apacheHeaderBuffer = "";
                                File apacheHeaderFile = new File(apacheHeaderFileName);

                                if (UtilValidate.isNotEmpty(apacheHeaderFile)) {
                                    apacheHeaderBuffer = FileUtil.readString("UTF-8", apacheHeaderFile);
                                }

                                FileUtil.writeString("UTF-8", apacheHeaderBuffer + outBuffer, outFile);

                                // clear cache to see immediately the new labels and translations in OFBiz
                                UtilCache.clearCache("properties.UtilPropertiesBundleCache");
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            Debug.logError(e, "Exception during save labels to xml file:", module);
            return ServiceUtil.returnFailure(UtilProperties.getMessage(resource, "saveLabelsToXmlFile.exceptionDuringSaveLabelsToXmlFile", locale));
        }
        return ServiceUtil.returnSuccess();
    }
}

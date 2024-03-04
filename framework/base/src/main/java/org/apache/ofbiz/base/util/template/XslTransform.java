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
package org.apache.ofbiz.base.util.template;

import java.io.IOException;
import java.io.StringWriter;

import javax.xml.XMLConstants;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.cache.UtilCache;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

public final class XslTransform {

    private static final String MODULE = XslTransform.class.getName();
    private static final UtilCache<String, Templates> XSL_TEMPLATE_CACHE = UtilCache.createUtilCache("XsltTemplates", 0, 0);

    /**
     * @param template the content or url of the xsl template
     * @param data the content or url of the xml data file
     * @throws TransformerException
     */
    public static String renderTemplate(String template, String data) throws TransformerException {
        String result = null;
        TransformerFactory tfactory = TransformerFactory.newInstance();
        tfactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        tfactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        tfactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");
        if (tfactory.getFeature(SAXSource.FEATURE)) {
            // setup for xml data file preprocessing to be able to xinclude
            SAXParserFactory pfactory = SAXParserFactory.newInstance();
            pfactory.setNamespaceAware(true);
            pfactory.setValidating(false);
            pfactory.setXIncludeAware(true);
            XMLReader reader = null;
            try {
                reader = pfactory.newSAXParser().getXMLReader();
            } catch (Exception e) {
                throw new TransformerException("Error creating SAX parser/reader", e);
            }
            // do the actual preprocessing
            SAXSource source = new SAXSource(reader, new InputSource(data));
            // compile the xsl template
            Transformer transformer = tfactory.newTransformer(new StreamSource(template));
            // and apply the xsl template to the source document and save in a result string
            try (StringWriter sw = new StringWriter()) {
                StreamResult sr = new StreamResult(sw);
                transformer.transform(source, sr);
                result = sw.toString();
            } catch (IOException e) {
                Debug.logError(e, MODULE);
            }
        } else {
            Debug.logError("tfactory does not support SAX features!", MODULE);
        }
        return result;
    }
}

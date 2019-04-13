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
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.URL;
import java.net.URLConnection;
import java.util.Map;

import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.Source;
import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.ofbiz.base.location.FlexibleLocation;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.GeneralException;
import org.apache.ofbiz.base.util.URLConnector;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.base.util.UtilXml;
import org.apache.ofbiz.base.util.cache.UtilCache;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

public final class XslTransform {

    public static final String module = XslTransform.class.getName();
    private static final UtilCache<String, Templates> xslTemplatesCache = UtilCache.createUtilCache("XsltTemplates", 0, 0);

    /**
     * @param template the content or url of the xsl template
     * @param data the content or url of the xml data file
     * @throws TransformerException
     */
    public static String renderTemplate(String template, String data)
    throws TransformerException {
        String result = null;
        TransformerFactory tfactory = TransformerFactory.newInstance();
        if (tfactory.getFeature(SAXSource.FEATURE)) {
            // setup for xml data file preprocessing to be able to xinclude
            SAXParserFactory pfactory= SAXParserFactory.newInstance();
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
            } catch (IOException e) {}
        } else {
            Debug.logError("tfactory does not support SAX features!", module);
        }
        return result;
    }

    /*
     *  it does not look like the rest of this file is working or used..........better set it to deprecated
     *  @deprecated
     */
    @Deprecated
    public static Document transform(Map<String, Object> context, Map<String, Object> params)
        throws GeneralException, IOException, TransformerConfigurationException, TransformerException {
        Document outputDocument = null;
        TransformerFactory tFactory = TransformerFactory.newInstance();
        Templates translet = null;
        String templateName = (String)context.get("templateName");
        if (UtilValidate.isNotEmpty(templateName)) {
            translet = xslTemplatesCache.get(templateName);
        }

        if (translet == null) {
            String templateUrl = (String)context.get("templateUrl");
            String templateString = (String)context.get("templateString");
            Document templateDocument = (Document)context.get("templateDocument");
            Source templateSource = getSource(templateDocument, templateUrl, templateString);
            translet = tFactory.newTemplates(templateSource);
            if (UtilValidate.isNotEmpty(templateName)) {
                translet = xslTemplatesCache.putIfAbsentAndGet(templateName, translet);
            }
        }
        if (translet != null) {
            Transformer transformer = translet.newTransformer();
            if (params != null) {
                for (Map.Entry<String, Object> entry: params.entrySet()) {
                       String key = entry.getKey();
                    Object val = entry.getValue();
                    transformer.setParameter(key, val);
               }
            }

            DOMResult outputResult = new DOMResult(UtilXml.makeEmptyXmlDocument());

            String inputUrl = (String)context.get("inputUrl");
            String inputString = (String)context.get("inputString");
            Document inputDocument = (Document)context.get("inputDocument");
            Source inputSource = getSource(inputDocument, inputUrl, inputString);

            transformer.transform(inputSource, outputResult);
            Node nd = outputResult.getNode();
            outputDocument = (Document)nd;
        }

        return outputDocument;
    }

    /*
     *  it does not look like the rest of this file is working or used..........better set it to deprecated
     *  @deprecated
     */
    @Deprecated
    private static Source getSource(Document inputDocument, String inputUrl, String inputString) throws IOException {
        Source source = null;
        if (inputDocument != null) {
            source = new DOMSource(inputDocument);
        } else if (UtilValidate.isNotEmpty(inputString)) {
            source = new StreamSource(new StringReader(inputString));
        } else if (UtilValidate.isNotEmpty(inputUrl)) {
            URL url = FlexibleLocation.resolveLocation(inputUrl);
            URLConnection conn = URLConnector.openConnection(url);
            try (InputStream in = conn.getInputStream()) {
            source = new StreamSource(in);
            }
        }
        return source;
    }
}

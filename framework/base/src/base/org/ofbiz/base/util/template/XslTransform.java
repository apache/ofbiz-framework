/*
 * $Id: XslTransform.java 5462 2005-08-05 18:35:48Z jonesde $
 *
 * Copyright (c) 2005 The Open For Business Project - www.ofbiz.org
 *
 * Permission is hereby granted, free of charge, to any person obtaining a
 * copy of this software and associated documentation files (the "Software"),
 * to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
 * OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY
 * CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT
 * OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR
 * THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package org.ofbiz.base.util.template;

import java.util.Map;
import java.util.Set;
import java.util.Iterator;
import java.io.StringReader;
import java.net.URL;
import java.net.URLConnection;
import java.io.InputStream;

import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.base.util.UtilXml;
import org.ofbiz.base.util.URLConnector;
import org.ofbiz.base.util.cache.UtilCache;
import org.ofbiz.base.location.FlexibleLocation;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.Templates;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.ofbiz.base.util.GeneralException;
import java.io.IOException;

import javax.xml.transform.stream.StreamSource;

/**
 * XslTransform
 * 
 * This utility takes an input document and a XSL stylesheet and performs the
 * transform, returning the output document.
 * The input for both the input document and stylesheet can be in one of three forms
 * - a URL to the doc, the doc in string form and the doc in DOM Document form.
 * It keeps its own cache for storing the compiled transforms.
 *
 * @author     <a href="mailto:byersa@automationgroups.com">Al Byers</a>
 * @version    $Rev$
 * @since      3.2
 */
public final class XslTransform {

    public static final String module = XslTransform.class.getName();
    public static UtilCache xslTemplatesCache = new UtilCache("XsltTemplates", 0, 0);

    public static Document transform(Map context, Map params) 
        throws GeneralException, IOException, TransformerConfigurationException, TransformerException {
        Document outputDocument = null;
        TransformerFactory tFactory = TransformerFactory.newInstance();
        Templates translet = null;
        String templateName = (String)context.get("templateName");
        if (UtilValidate.isNotEmpty(templateName)) {
            translet = (Templates) xslTemplatesCache.get(templateName);
        }

        if (translet == null ) {
            String templateUrl = (String)context.get("templateUrl");
        	String templateString = (String)context.get("templateString");
        	Document templateDocument = (Document)context.get("templateDocument");
            Source templateSource = getSource(templateDocument, templateUrl, templateString);
            translet = tFactory.newTemplates(templateSource);
            if (UtilValidate.isNotEmpty(templateName)) {
                    xslTemplatesCache.put(templateName, translet);
            }
        }
        if (translet != null ) {
            Transformer transformer = translet.newTransformer();
        	if (params != null) {
               Set entrySet = params.entrySet(); 
               Iterator iter = entrySet.iterator();
               while (iter.hasNext()) {
               	    Map.Entry entry = (Map.Entry)iter.next(); 
               	    String key = (String)entry.getKey();
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
    
    private static Source getSource(Document inputDocument, String inputUrl, String inputString) throws GeneralException, IOException {
        Source source = null;
        if (inputDocument != null) {
            source = new DOMSource(inputDocument);
        } else if (UtilValidate.isNotEmpty(inputString)) {
            source = new StreamSource(new StringReader(inputString));
        } else if (UtilValidate.isNotEmpty(inputUrl)) {
            URL url = FlexibleLocation.resolveLocation(inputUrl);
            URLConnection conn = URLConnector.openConnection(url);
            InputStream in = conn.getInputStream();
            source = new StreamSource(in);
        }
        return source;
    }
}

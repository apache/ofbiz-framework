/*
 * $Id: SimpleMapProcessor.java 5462 2005-08-05 18:35:48Z jonesde $
 *
 *  Copyright (c) 2001, 2002 The Open For Business Project - www.ofbiz.org
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a
 *  copy of this software and associated documentation files (the "Software"),
 *  to deal in the Software without restriction, including without limitation
 *  the rights to use, copy, modify, merge, publish, distribute, sublicense,
 *  and/or sell copies of the Software, and to permit persons to whom the
 *  Software is furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included
 *  in all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
 *  OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 *  MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 *  IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY
 *  CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT
 *  OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR
 *  THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package org.ofbiz.minilang;

import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.ofbiz.base.util.UtilURL;
import org.ofbiz.base.util.UtilXml;
import org.ofbiz.base.util.cache.UtilCache;
import org.ofbiz.minilang.operation.MapProcessor;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * SimpleMapProcessor Mini Language
 *
 * @author     <a href="mailto:jonesde@ofbiz.org">David E. Jones</a>
 * @version    $Rev$
 * @since      2.0
 */
public class SimpleMapProcessor {

    protected static UtilCache simpleMapProcessorsResourceCache = new UtilCache("minilang.SimpleMapProcessorsResource", 0, 0);
    protected static UtilCache simpleMapProcessorsURLCache = new UtilCache("minilang.SimpleMapProcessorsURL", 0, 0);

    public static void runSimpleMapProcessor(String xmlResource, String name, Map inMap, Map results, List messages, Locale locale) throws MiniLangException {
        runSimpleMapProcessor(xmlResource, name, inMap, results, messages, locale, null);
    }

    public static void runSimpleMapProcessor(String xmlResource, String name, Map inMap, Map results, List messages, Locale locale, ClassLoader loader) throws MiniLangException {
        if (loader == null)
            loader = Thread.currentThread().getContextClassLoader();

        Map mapProcessors = getProcessors(xmlResource, name, loader);
        MapProcessor processor = (MapProcessor) mapProcessors.get(name);

        if (processor == null) {
            throw new MiniLangException("Could not find SimpleMapProcessor named " + name + " in XML document resource: " + xmlResource);
        }

        if (processor != null)
            processor.exec(inMap, results, messages, locale, loader);
    }

    public static void runSimpleMapProcessor(URL xmlURL, String name, Map inMap, Map results, List messages, Locale locale, ClassLoader loader) throws MiniLangException {
        if (loader == null)
            loader = Thread.currentThread().getContextClassLoader();

        Map mapProcessors = getProcessors(xmlURL, name);
        MapProcessor processor = (MapProcessor) mapProcessors.get(name);

        if (processor == null) {
            throw new MiniLangException("Could not find SimpleMapProcessor named " + name + " in XML document: " + xmlURL.toString());
        }

        if (processor != null)
            processor.exec(inMap, results, messages, locale, loader);
    }

    protected static Map getProcessors(String xmlResource, String name, ClassLoader loader) throws MiniLangException {
        Map simpleMapProcessors = (Map) simpleMapProcessorsResourceCache.get(xmlResource);

        if (simpleMapProcessors == null) {
            synchronized (SimpleMapProcessor.class) {
                simpleMapProcessors = (Map) simpleMapProcessorsResourceCache.get(xmlResource);
                if (simpleMapProcessors == null) {
                    URL xmlURL = UtilURL.fromResource(xmlResource, loader);

                    if (xmlURL == null) {
                        throw new MiniLangException("Could not find SimpleMapProcessor XML document in resource: " + xmlResource);
                    }
                    simpleMapProcessors = getAllProcessors(xmlURL);

                    // put it in the cache
                    simpleMapProcessorsResourceCache.put(xmlResource, simpleMapProcessors);
                }
            }
        }

        return simpleMapProcessors;
    }

    protected static Map getProcessors(URL xmlURL, String name) throws MiniLangException {
        Map simpleMapProcessors = (Map) simpleMapProcessorsURLCache.get(xmlURL);

        if (simpleMapProcessors == null) {
            synchronized (SimpleMapProcessor.class) {
                simpleMapProcessors = (Map) simpleMapProcessorsURLCache.get(xmlURL);
                if (simpleMapProcessors == null) {
                    simpleMapProcessors = getAllProcessors(xmlURL);

                    // put it in the cache
                    simpleMapProcessorsURLCache.put(xmlURL, simpleMapProcessors);
                }
            }
        }

        return simpleMapProcessors;
    }

    protected static Map getAllProcessors(URL xmlURL) throws MiniLangException {
        Map mapProcessors = new HashMap();

        // read in the file
        Document document = null;

        try {
            document = UtilXml.readXmlDocument(xmlURL, true);
        } catch (java.io.IOException e) {
            throw new MiniLangException("Could not read XML file", e);
        } catch (org.xml.sax.SAXException e) {
            throw new MiniLangException("Could not parse XML file", e);
        } catch (javax.xml.parsers.ParserConfigurationException e) {
            throw new MiniLangException("XML parser not setup correctly", e);
        }

        if (document == null) {
            throw new MiniLangException("Could not find SimpleMapProcessor XML document: " + xmlURL.toString());
        }

        Element rootElement = document.getDocumentElement();
        List simpleMapProcessorElements = UtilXml.childElementList(rootElement, "simple-map-processor");
        Iterator strProcorIter = simpleMapProcessorElements.iterator();

        while (strProcorIter.hasNext()) {
            Element simpleMapProcessorElement = (Element) strProcorIter.next();
            MapProcessor processor = new MapProcessor(simpleMapProcessorElement);

            mapProcessors.put(simpleMapProcessorElement.getAttribute("name"), processor);
        }

        return mapProcessors;
    }
}

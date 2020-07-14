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
package org.apache.ofbiz.base.config;

import java.io.InputStream;
import java.net.URL;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilURL;
import org.apache.ofbiz.base.util.UtilXml;
import org.apache.ofbiz.base.util.cache.UtilCache;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Loads resources using dynamically specified resource loader classes.
 */
public abstract class ResourceLoader {

    private static final String MODULE = ResourceLoader.class.getName();
    private static final UtilCache<String, ResourceLoader> loaderCache = UtilCache.createUtilCache("resource.ResourceLoaders", 0, 0);
    // This cache is temporary - we will use it until the framework has been refactored to eliminate DOM tree caching, then it can be removed.
    private static final UtilCache<String, Document> domCache = UtilCache.createUtilCache("resource.DomTrees", 0, 0);

    public static InputStream loadResource(String xmlFilename, String location, String loaderName) throws GenericConfigException {
        ResourceLoader loader = getLoader(xmlFilename, loaderName);
        if (loader == null) {
            throw new IllegalArgumentException("ResourceLoader not found with name [" + loaderName + "] in " + xmlFilename);
        }
        return loader.loadResource(location);
    }

    public static URL getURL(String xmlFilename, String location, String loaderName) throws GenericConfigException {
        ResourceLoader loader = getLoader(xmlFilename, loaderName);
        if (loader == null) {
            throw new IllegalArgumentException("ResourceLoader not found with name [" + loaderName + "] in " + xmlFilename);
        }
        return loader.getURL(location);
    }

    public static ResourceLoader getLoader(String xmlFilename, String loaderName) throws GenericConfigException {
        String cacheKey = xmlFilename.concat("#").concat(loaderName);
        ResourceLoader loader = loaderCache.get(cacheKey);
        if (loader == null) {
            loader = getLoader(xmlFilename, loaderName, cacheKey);
        }
        return loader;
    }

    private static ResourceLoader getLoader(String xmlFilename, String loaderName, String cacheKey)
            throws GenericConfigException {
        ResourceLoader loader;
        Element rootElement;
        URL xmlUrl = UtilURL.fromResource(xmlFilename);
        if (xmlUrl == null) {
            throw new GenericConfigException("Could not find the " + xmlFilename + " file");
        }
        try {
            rootElement = UtilXml.readXmlDocument(xmlUrl, true, true).getDocumentElement();
        } catch (Exception e) {
            throw new GenericConfigException("Exception thrown while reading " + xmlFilename + ": ", e);
        }
        Element loaderElement = UtilXml.firstChildElement(rootElement, "resource-loader", "name", loaderName);
        if (loaderElement == null) {
            throw new GenericConfigException("The " + xmlFilename + " file is missing the <resource-loader> element with the name " + loaderName);
        }
        if (loaderElement.getAttribute("class").isEmpty()) {
            throw new GenericConfigException("The " + xmlFilename + " file <resource-loader> element with the name " + loaderName + " is missing the class attribute");
        }
        loader = loaderCache.putIfAbsentAndGet(cacheKey, makeLoader(loaderElement));
        return loader;
    }

    /** This method should be avoided. DOM object trees take a lot of memory and they are not
     * thread-safe, so they should not be cached.
     * @deprecated use {@link #readXmlRootElement(String)}
     */
    @Deprecated
    public static Element getXmlRootElement(String xmlFilename) throws GenericConfigException {
        Document document = ResourceLoader.getXmlDocument(xmlFilename);

        if (document == null) {
            return null;
        }
        return document.getDocumentElement();
    }

    public static Element readXmlRootElement(String xmlFilename) throws GenericConfigException {
        Document document = ResourceLoader.readXmlDocument(xmlFilename);

        if (document == null) {
            return null;
        }
        return document.getDocumentElement();
    }

    public static void invalidateDocument(String xmlFilename) {
        UtilCache.clearCachesThatStartWith(xmlFilename);
    }

    /** This method should be avoided. DOM object trees take a lot of memory and they are not
     * thread-safe, so they should not be cached.
     * @deprecated use {@link #readXmlDocument(String)}
     */
    @Deprecated
    public static Document getXmlDocument(String xmlFilename) throws GenericConfigException {
        Document document = domCache.get(xmlFilename);

        if (document == null) {
            document = readXmlDocument(xmlFilename);

            if (document != null) {
                document = domCache.putIfAbsentAndGet(xmlFilename, document);
            }
        }
        return document;
    }

    public static Document readXmlDocument(String xmlFilename) throws GenericConfigException {
        URL confUrl = UtilURL.fromResource(xmlFilename);

        if (confUrl == null) {
            throw new GenericConfigException("ERROR: could not find the [" + xmlFilename + "] XML file on the classpath");
        }

        try {
            return UtilXml.readXmlDocument(confUrl, true, true);
        } catch (org.xml.sax.SAXException | javax.xml.parsers.ParserConfigurationException | java.io.IOException e) {
            throw new GenericConfigException("Error reading " + xmlFilename + "", e);
        }
    }

    private static ResourceLoader makeLoader(Element loaderElement) throws GenericConfigException {
        String loaderName = loaderElement.getAttribute("name");
        String className = loaderElement.getAttribute("class");
        ResourceLoader loader;
        try {
            Class<?> lClass = null;
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            lClass = classLoader.loadClass(className);
            loader = (ResourceLoader) lClass.getDeclaredConstructor().newInstance();
            loader.init(loaderName, loaderElement.getAttribute("prefix"), loaderElement.getAttribute("prepend-env"));
            return loader;
        } catch (ReflectiveOperationException e) {
            throw new GenericConfigException("Exception thrown while loading ResourceLoader class \"" + className
                    + "\" ", e);
        }
    }

    private String prefix;
    private String envName;

    protected ResourceLoader() { }

    private void init(String name, String prefix, String envName) {
        this.prefix = prefix;
        this.envName = envName;
    }

    /**
     * Just a utility method to be used in loadResource by the implementing class.
     * @param location
     * @return the built-up full location
     */
    public String fullLocation(String location) {
        StringBuilder buf = new StringBuilder();
        if (!envName.isEmpty()) {
            String propValue = System.getProperty(envName);
            if (propValue == null) {
                String errMsg = "The Java environment (-Dxxx=yyy) variable with name " + envName + " is not set, cannot load resource.";
                Debug.logError(errMsg, MODULE);
                throw new IllegalArgumentException(errMsg);
            }
            buf.append(propValue);
        }
        buf.append(prefix);
        buf.append(location);
        return buf.toString();
    }

    public abstract InputStream loadResource(String location) throws GenericConfigException;
    public abstract URL getURL(String location) throws GenericConfigException;
}

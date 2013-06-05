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
package org.ofbiz.base.config;

import java.io.InputStream;
import java.net.URL;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilURL;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.base.util.UtilXml;
import org.ofbiz.base.util.cache.UtilCache;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Loads resources using dynamically specified resource loader classes.
 */
public abstract class ResourceLoader {

    public static final String module = ResourceLoader.class.getName();
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
            Element rootElement = null;
            URL xmlUrl = UtilURL.fromResource(xmlFilename);
            if (xmlUrl == null) {
                throw new GenericConfigException("ERROR: could not find the [" + xmlFilename + "] XML file on the classpath");
            }
            try {
                rootElement = UtilXml.readXmlDocument(xmlUrl, true, true).getDocumentElement();
            } catch (Exception e) {
                throw new GenericConfigException("Error reading " + xmlFilename + ": ", e);
            }
            Element loaderElement = UtilXml.firstChildElement(rootElement, "resource-loader", "name", loaderName);
            loader = makeLoader(loaderElement);
            if (loader != null) {
                loader = loaderCache.putIfAbsentAndGet(cacheKey, loader);
            }
        }
        return loader;
    }

    // This method should be avoided. DOM object trees take a lot of memory and they are not
    // thread-safe, so they should not be cached.
    @Deprecated
    public static Element getXmlRootElement(String xmlFilename) throws GenericConfigException {
        Document document = ResourceLoader.getXmlDocument(xmlFilename);

        if (document != null) {
            return document.getDocumentElement();
        } else {
            return null;
        }
    }

    public static void invalidateDocument(String xmlFilename) throws GenericConfigException {
        UtilCache.clearCachesThatStartWith(xmlFilename);
    }

    // This method should be avoided. DOM object trees take a lot of memory and they are not
    // thread-safe, so they should not be cached.
    @Deprecated
    public static Document getXmlDocument(String xmlFilename) throws GenericConfigException {
        Document document = domCache.get(xmlFilename);

        if (document == null) {
            URL confUrl = UtilURL.fromResource(xmlFilename);

            if (confUrl == null) {
                throw new GenericConfigException("ERROR: could not find the [" + xmlFilename + "] XML file on the classpath");
            }

            try {
                document = UtilXml.readXmlDocument(confUrl, true, true);
            } catch (org.xml.sax.SAXException e) {
                throw new GenericConfigException("Error reading " + xmlFilename + "", e);
            } catch (javax.xml.parsers.ParserConfigurationException e) {
                throw new GenericConfigException("Error reading " + xmlFilename + "", e);
            } catch (java.io.IOException e) {
                throw new GenericConfigException("Error reading " + xmlFilename + "", e);
            }

            if (document != null) {
                document = (Document) domCache.putIfAbsentAndGet(xmlFilename, document);
            }
        }
        return document;
    }

    public static ResourceLoader makeLoader(Element loaderElement) throws GenericConfigException {
        if (loaderElement == null)
            return null;

        String loaderName = loaderElement.getAttribute("name");
        String className = loaderElement.getAttribute("class");
        ResourceLoader loader = null;

        try {
            Class<?> lClass = null;

            if (UtilValidate.isNotEmpty(className)) {
                try {
                    ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
                    lClass = classLoader.loadClass(className);
                } catch (ClassNotFoundException e) {
                    throw new GenericConfigException("Error loading Resource Loader class \"" + className + "\"", e);
                }
            }

            try {
                loader = (ResourceLoader) lClass.newInstance();
            } catch (IllegalAccessException e) {
                throw new GenericConfigException("Error loading Resource Loader class \"" + className + "\"", e);
            } catch (InstantiationException e) {
                throw new GenericConfigException("Error loading Resource Loader class \"" + className + "\"", e);
            }
        } catch (SecurityException e) {
            throw new GenericConfigException("Error loading Resource Loader class \"" + className + "\"", e);
        }

        if (loader != null) {
            loader.init(loaderName, loaderElement.getAttribute("prefix"), loaderElement.getAttribute("prepend-env"));
        }

        return loader;
    }

    // FIXME: This class is not thread-safe.
    protected String name;
    protected String prefix;
    protected String envName;

    protected ResourceLoader() {}

    public void init(String name, String prefix, String envName) {
        this.name = name;
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

        if (UtilValidate.isNotEmpty(envName)) {
            String propValue = System.getProperty(envName);
            if (propValue == null) {
                String errMsg = "The Java environment (-Dxxx=yyy) variable with name " + envName + " is not set, cannot load resource.";
                Debug.logError(errMsg, module);
                throw new IllegalArgumentException(errMsg);
            }
            buf.append(propValue);
        }
        if (UtilValidate.isNotEmpty(prefix)) {
            buf.append(prefix);
        }
        buf.append(location);
        return buf.toString();
    }

    public abstract InputStream loadResource(String location) throws GenericConfigException;
    public abstract URL getURL(String location) throws GenericConfigException;
}

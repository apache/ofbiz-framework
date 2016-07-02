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
package org.ofbiz.base.util;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * URL Utilities - Simple Class for flexibly working with properties files
 *
 */
public final class UtilURL {

    public static final String module = UtilURL.class.getName();
    private static final Map<String, URL> urlMap = new ConcurrentHashMap<String, URL>();

    private UtilURL() {}

    public static <C> URL fromClass(Class<C> contextClass) {
        String resourceName = contextClass.getName();
        int dotIndex = resourceName.lastIndexOf('.');

        if (dotIndex != -1) resourceName = resourceName.substring(0, dotIndex);
        resourceName += ".properties";

        return fromResource(contextClass, resourceName);
    }

    /**
     * Returns a <code>URL</code> instance from a resource name. Returns
     * <code>null</code> if the resource is not found.
     * <p>This method uses various ways to locate the resource, and in all
     * cases it tests to see if the resource exists - so it
     * is very inefficient.</p>
     * 
     * @param resourceName
     * @return
     */
    public static URL fromResource(String resourceName) {
        return fromResource(resourceName, null);
    }

    public static <C> URL fromResource(Class<C> contextClass, String resourceName) {
        if (contextClass == null)
            return fromResource(resourceName, null);
        else
            return fromResource(resourceName, contextClass.getClassLoader());
    }

    /**
     * Returns a <code>URL</code> instance from a resource name. Returns
     * <code>null</code> if the resource is not found.
     * <p>This method uses various ways to locate the resource, and in all
     * cases it tests to see if the resource exists - so it
     * is very inefficient.</p>
     * 
     * @param resourceName
     * @param loader
     * @return
     */
    public static URL fromResource(String resourceName, ClassLoader loader) {
        URL url = urlMap.get(resourceName);
        if (url != null) {
            try {
                return new URL(url.toString());
            } catch (MalformedURLException e) {
                Debug.logWarning(e, "Exception thrown while copying URL: ", module);
            }
        }
        if (loader == null) {
            try {
                loader = Thread.currentThread().getContextClassLoader();
            } catch (SecurityException e) {
                // Huh? The new object will be created by the current thread, so how is this any different than the previous code?
                UtilURL utilURL = new UtilURL();
                loader = utilURL.getClass().getClassLoader();
            }
        }
        url = loader.getResource(resourceName);
        if (url != null) {
            // Do not cache URLs from ClassLoader - interferes with EntityClassLoader operation
            //urlMap.put(resourceName, url);
            return url;
        }
        url = ClassLoader.getSystemResource(resourceName);
        if (url != null) {
            urlMap.put(resourceName, url);
            return url;
        }
        url = fromFilename(resourceName);
        if (url != null) {
            urlMap.put(resourceName, url);
            return url;
        }
        url = fromOfbizHomePath(resourceName);
        if (url != null) {
            urlMap.put(resourceName, url);
            return url;
        }
        url = fromUrlString(resourceName);
        if (url != null) {
            urlMap.put(resourceName, url);
        }
        return url;
    }

    public static URL fromFilename(String filename) {
        if (filename == null) return null;
        File file = new File(filename);
        URL url = null;

        try {
            if (file.exists()) url = file.toURI().toURL();
        } catch (java.net.MalformedURLException e) {
            e.printStackTrace();
            url = null;
        }
        return url;
    }

    public static URL fromUrlString(String urlString) {
        URL url = null;
        try {
            url = new URL(urlString);
        } catch (MalformedURLException e) {
        }

        return url;
    }

    public static URL fromOfbizHomePath(String filename) {
        String ofbizHome = System.getProperty("ofbiz.home");
        if (ofbizHome == null) {
            Debug.logWarning("No ofbiz.home property set in environment", module);
            return null;
        }
        String newFilename = ofbizHome;
        if (!newFilename.endsWith("/") && !filename.startsWith("/")) {
            newFilename = newFilename + "/";
        }
        newFilename = newFilename + filename;
        return fromFilename(newFilename);
    }

    public static String getOfbizHomeRelativeLocation(URL fileUrl) {
        String ofbizHome = System.getProperty("ofbiz.home");
        String path = fileUrl.getPath();
        if (path.startsWith(ofbizHome)) {
            // note: the +1 is to remove the leading slash
            path = path.substring(ofbizHome.length()+1);
        }
        return path;
    }
}

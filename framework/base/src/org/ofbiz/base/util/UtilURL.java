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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * URL Utilities - Simple Class for flexibly working with properties files
 *
 */
public class UtilURL {

    public static final String module = UtilURL.class.getName();

    public static <C> URL fromClass(Class<C> contextClass) {
        String resourceName = contextClass.getName();
        int dotIndex = resourceName.lastIndexOf('.');

        if (dotIndex != -1) resourceName = resourceName.substring(0, dotIndex);
        resourceName += ".properties";

        return fromResource(contextClass, resourceName);
    }

    public static URL fromResource(String resourceName) {
        return fromResource(resourceName, null);
    }

    public static <C> URL fromResource(Class<C> contextClass, String resourceName) {
        if (contextClass == null)
            return fromResource(resourceName, null);
        else
            return fromResource(resourceName, contextClass.getClassLoader());
    }

    public static URL fromResource(String resourceName, ClassLoader loader) {
        URL url = null;

        if (loader != null && url == null) url = loader.getResource(resourceName);
        if (loader != null && url == null) url = loader.getResource(resourceName + ".properties");

        if (loader == null && url == null) {
            try {
                loader = Thread.currentThread().getContextClassLoader();
            } catch (SecurityException e) {
                UtilURL utilURL = new UtilURL();
                loader = utilURL.getClass().getClassLoader();
            }
        }

        if (url == null) url = loader.getResource(resourceName);
        if (url == null) url = loader.getResource(resourceName + ".properties");

        if (url == null) url = ClassLoader.getSystemResource(resourceName);
        if (url == null) url = ClassLoader.getSystemResource(resourceName + ".properties");

        if (url == null) url = fromFilename(resourceName);
        if (url == null) url = fromOfbizHomePath(resourceName);
        if (url == null) url = fromUrlString(resourceName);

        //Debug.logInfo("[fromResource] got URL " + (url == null ? "[NotFound]" : url.toExternalForm()) + " from resourceName " + resourceName);
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

    public static String readUrlText(URL url) throws IOException {
        InputStream stream = url.openStream();

        StringBuilder buf = new StringBuilder();
        BufferedReader in = null;
        try {
            in = new BufferedReader(new InputStreamReader(stream));

            String str;
            while ((str = in.readLine()) != null) {
                buf.append(str);
                buf.append(System.getProperty("line.separator"));
            }
        } catch (IOException e) {
            Debug.logError(e, "Error reading text from URL [" + url + "]: " + e.toString(), module);
            throw e;
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    Debug.logError(e, "Error closing after reading text from URL [" + url + "]: " + e.toString(), module);
                }
            }
        }

        return buf.toString();
    }
}

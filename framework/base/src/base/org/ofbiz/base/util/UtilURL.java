/*
 * $Id: UtilURL.java 5462 2005-08-05 18:35:48Z jonesde $
 *
 *  Copyright (c) 2001-2005 The Open For Business Project - www.ofbiz.org
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
package org.ofbiz.base.util;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * URL Utilities - Simple Class for flexibly working with properties files
 *
 * @author     <a href="mailto:jonesde@ofbiz.org">David E. Jones</a>
 * @version    $Rev$
 * @since      2.0
 */
public class UtilURL {

    public static final String module = UtilURL.class.getName();

    public static URL fromClass(Class contextClass) {
        String resourceName = contextClass.getName();
        int dotIndex = resourceName.lastIndexOf('.');

        if (dotIndex != -1) resourceName = resourceName.substring(0, dotIndex);
        resourceName += ".properties";

        return fromResource(contextClass, resourceName);
    }

    public static URL fromResource(String resourceName) {
        return fromResource(resourceName, null);
    }

    public static URL fromResource(Class contextClass, String resourceName) {
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

        //Debug.log("[fromResource] got URL " + (url == null ? "[NotFound]" : url.toExternalForm()) + " from resourceName " + resourceName);
        return url;
    }

    public static URL fromFilename(String filename) {
        if (filename == null) return null;
        File file = new File(filename);
        URL url = null;

        try {
            if (file.exists()) url = file.toURL();
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
}

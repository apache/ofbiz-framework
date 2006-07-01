/*
 * $Id: GenericHelperFactory.java 5462 2005-08-05 18:35:48Z jonesde $
 *
 * Copyright (c) 2001-2005 The Open For Business Project - www.ofbiz.org
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
 *
 */
package org.ofbiz.entity.datasource;

import java.util.HashMap;
import java.util.Map;

import org.ofbiz.base.util.Debug;
import org.ofbiz.entity.config.DatasourceInfo;
import org.ofbiz.entity.config.EntityConfigUtil;

/**
 * Generic Entity Helper Factory Class
 *
 * @author     <a href="mailto:jonesde@ofbiz.org">David E. Jones</a>
 * @version    $Rev$
 * @since      2.0
 */
public class GenericHelperFactory {
    
    public static final String module = GenericHelperFactory.class.getName();

    // protected static UtilCache helperCache = new UtilCache("entity.GenericHelpers", 0, 0);
    protected static Map helperCache = new HashMap();

    public static GenericHelper getHelper(String helperName) {
        GenericHelper helper = (GenericHelper) helperCache.get(helperName);

        if (helper == null) // don't want to block here
        {
            synchronized (GenericHelperFactory.class) {
                // must check if null again as one of the blocked threads can still enter
                helper = (GenericHelper) helperCache.get(helperName);
                if (helper == null) {
                    try {
                        DatasourceInfo datasourceInfo = EntityConfigUtil.getDatasourceInfo(helperName);

                        if (datasourceInfo == null) {
                            throw new IllegalStateException("Could not find datasource definition with name " + helperName);
                        }
                        String helperClassName = datasourceInfo.helperClass;
                        Class helperClass = null;

                        if (helperClassName != null && helperClassName.length() > 0) {
                            try {
                                ClassLoader loader = Thread.currentThread().getContextClassLoader();
                                helperClass = loader.loadClass(helperClassName);
                            } catch (ClassNotFoundException e) {
                                Debug.logWarning(e, module);
                                throw new IllegalStateException("Error loading GenericHelper class \"" + helperClassName + "\": " + e.getMessage());
                            }
                        }

                        Class[] paramTypes = new Class[] {String.class};
                        Object[] params = new Object[] {helperName};

                        java.lang.reflect.Constructor helperConstructor = null;

                        if (helperClass != null) {
                            try {
                                helperConstructor = helperClass.getConstructor(paramTypes);
                            } catch (NoSuchMethodException e) {
                                Debug.logWarning(e, module);
                                throw new IllegalStateException("Error loading GenericHelper class \"" + helperClassName + "\": " + e.getMessage());
                            }
                        }
                        try {
                            helper = (GenericHelper) helperConstructor.newInstance(params);
                        } catch (IllegalAccessException e) {
                            Debug.logWarning(e, module);
                            throw new IllegalStateException("Error loading GenericHelper class \"" + helperClassName + "\": " + e.getMessage());
                        } catch (InstantiationException e) {
                            Debug.logWarning(e, module);
                            throw new IllegalStateException("Error loading GenericHelper class \"" + helperClassName + "\": " + e.getMessage());
                        } catch (java.lang.reflect.InvocationTargetException e) {
                            Debug.logWarning(e, module);
                            throw new IllegalStateException("Error loading GenericHelper class \"" + helperClassName + "\": " + e.getMessage());
                        }

                        if (helper != null)
                            helperCache.put(helperName, helper);
                    } catch (SecurityException e) {
                        Debug.logError(e, module);
                        throw new IllegalStateException("Error loading GenericHelper class: " + e.toString());
                    }
                }
            }
        }
        return helper;
    }
}

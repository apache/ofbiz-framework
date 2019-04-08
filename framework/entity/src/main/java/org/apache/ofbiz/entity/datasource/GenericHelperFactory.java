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
package org.apache.ofbiz.entity.datasource;

import java.util.HashMap;
import java.util.Map;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.config.model.Datasource;
import org.apache.ofbiz.entity.config.model.EntityConfig;

/**
 * Generic Entity Helper Factory Class
 *
 */
public class GenericHelperFactory {

    public static final String module = GenericHelperFactory.class.getName();

    // protected static UtilCache helperCache = new UtilCache("entity.GenericHelpers", 0, 0);
    protected static Map<String, GenericHelper> helperCache = new HashMap<String, GenericHelper>();

    public static GenericHelper getHelper(GenericHelperInfo helperInfo) {
        GenericHelper helper = helperCache.get(helperInfo.getHelperFullName());

        if (helper == null) { // don't want to block here
            synchronized (GenericHelperFactory.class) {
                // must check if null again as one of the blocked threads can still enter
                helper = helperCache.get(helperInfo.getHelperFullName());
                if (helper == null) {
                    try {
                        Datasource datasourceInfo = EntityConfig.getDatasource(helperInfo.getHelperBaseName());

                        if (datasourceInfo == null) {
                            throw new IllegalStateException("Could not find datasource definition with name " + helperInfo.getHelperBaseName());
                        }
                        String helperClassName = datasourceInfo.getHelperClass();
                        Class<?> helperClass = null;

                        if (UtilValidate.isNotEmpty(helperClassName)) {
                            try {
                                ClassLoader loader = Thread.currentThread().getContextClassLoader();
                                helperClass = loader.loadClass(helperClassName);
                            } catch (ClassNotFoundException e) {
                                Debug.logWarning(e, module);
                                throw new IllegalStateException("Error loading GenericHelper class \"" + helperClassName + "\": " + e.getMessage());
                            }
                        }

                        Class<?>[] paramTypes = new Class<?>[] {GenericHelperInfo.class};
                        Object[] params = new Object[] {helperInfo};

                        java.lang.reflect.Constructor<?> helperConstructor = null;

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
                            helperCache.put(helperInfo.getHelperFullName(), helper);
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

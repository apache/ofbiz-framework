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
package org.ofbiz.security.authz.da;

import java.util.Set;
import java.util.Iterator;
import java.util.ServiceLoader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.ObjectType;
import org.ofbiz.base.util.cache.UtilCache;
import org.ofbiz.entity.Delegator;

public class DynamicAccessFactory {

    /**
     * Cache to store the DynamicAccess implementations
     */
    private static UtilCache<String,DynamicAccessHandler> dynamicAccessHandlerCache = UtilCache.createUtilCache("security.DynamicAccessHandlerCache");
    private static final String module = DynamicAccessFactory.class.getName();

    public static DynamicAccessHandler getDynamicAccessHandler(Delegator delegator, String accessString) {
        if (dynamicAccessHandlerCache.size() == 0) { // should always be at least 1
            loadAccessHandlers(delegator);
        }

        Set<? extends String> patterns = dynamicAccessHandlerCache.getCacheLineKeys();
        for (String pattern : patterns) {
            if (!pattern.equals("*")) { // ignore the default pattern for now
                Debug.logInfo("Checking DOH pattern : " + pattern, module);
                Pattern p = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE);
                Matcher m = p.matcher(accessString);
                if (m.find()) {
                    Debug.logInfo("Pattern [" + pattern + "] matched -- " + accessString, module);
                    return dynamicAccessHandlerCache.get(pattern);
                }
            }
        }

        return dynamicAccessHandlerCache.get("*");
    }

    private static void loadAccessHandlers(Delegator delegator) {
        Iterator<DynamicAccessHandler> it = ServiceLoader.load(DynamicAccessHandler.class, DynamicAccessFactory.class.getClassLoader()).iterator();
        while (it.hasNext()) {
            DynamicAccessHandler handler = it.next();
            handler.setDelegator(delegator);
            dynamicAccessHandlerCache.put(handler.getPattern(), handler);
        }
    }

    @SuppressWarnings("unchecked")
    public static DynamicAccess loadDynamicAccessObject(Delegator delegator, String accessString) {
        DynamicAccess da = null;
        Class<DynamicAccess> clazz;
        try {
            clazz = (Class<DynamicAccess>) ObjectType.loadClass(accessString);
        } catch (ClassNotFoundException e) {
            Debug.logError(e, module);
            return null;
        } catch (ClassCastException e) {
            Debug.logError(e, module);
            return null;
        }

        if (clazz != null) {
            try {
                da = clazz.newInstance();
                da.setDelegator(delegator);
            } catch (InstantiationException e) {
                Debug.logError(e, module);
                return null;
            } catch (IllegalAccessException e) {
                Debug.logError(e, module);
                return null;
            }
        }

        return da;
    }
}

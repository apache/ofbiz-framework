/*
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
 */
package org.ofbiz.entity;

import java.util.concurrent.ConcurrentHashMap;

import org.ofbiz.base.lang.Factory;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilObject;

/** <code>Delegator</code> factory abstract class. */
public abstract class DelegatorFactory implements Factory<Delegator, String> {
    public static final String module = DelegatorFactoryImpl.class.getName();
    private static final ConcurrentHashMap<String, Delegator> delegatorCache = new ConcurrentHashMap<String, Delegator>();

    public static Delegator getDelegator(String delegatorName) {
        if (delegatorName == null) {
            delegatorName = "default";
            //Debug.logWarning(new Exception("Location where getting delegator with null name"), "Got a getGenericDelegator call with a null delegatorName, assuming default for the name.", module);
        }
        do {
            Delegator delegator = delegatorCache.get(delegatorName);

            if (delegator != null) {
                // setup the Entity ECA Handler
                delegator.initEntityEcaHandler();
                //Debug.logInfo("got delegator(" + delegatorName + ") from cache", module);
                return delegator;
            }
            try {
                delegator = UtilObject.getObjectFromFactory(DelegatorFactory.class, delegatorName);
            } catch (ClassNotFoundException e) {
                Debug.logError(e, module);
            }
            //Debug.logInfo("putting delegator(" + delegatorName + ") into cache", module);
            delegatorCache.putIfAbsent(delegatorName, delegator);
        } while (true);
    }
}

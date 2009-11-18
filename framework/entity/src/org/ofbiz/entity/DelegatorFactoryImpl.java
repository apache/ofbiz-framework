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

import org.ofbiz.base.util.Debug;

/** A <code>DelegatorFactory</code> implementation that returns an
 * instance of <code>GenericDelegator</code>. */
public class DelegatorFactoryImpl extends DelegatorFactory {

    public static final String module = DelegatorFactoryImpl.class.getName();

    public Delegator getInstance(String delegatorName) {
        if (delegatorName == null) {
            delegatorName = "default";
            Debug.logWarning(new Exception("Location where getting delegator with null name"), "Got a getGenericDelegator call with a null delegatorName, assuming default for the name.", module);
        }
        GenericDelegator delegator = GenericDelegator.delegatorCache.get(delegatorName);
        if (delegator == null) {
            synchronized (GenericDelegator.delegatorCache) {
                // must check if null again as one of the blocked threads can still enter
                delegator = GenericDelegator.delegatorCache.get(delegatorName);
                if (delegator == null) {
                    if (Debug.infoOn()) Debug.logInfo("Creating new delegator [" + delegatorName + "] (" + Thread.currentThread().getName() + ")", module);
                    //Debug.logInfo(new Exception(), "Showing stack where new delegator is being created...", module);
                    try {
                        delegator = new GenericDelegator(delegatorName);
                    } catch (GenericEntityException e) {
                        Debug.logError(e, "Error creating delegator", module);
                    }
                    if (delegator != null) {
                        GenericDelegator.delegatorCache.put(delegatorName, delegator);
                    } else {
                        Debug.logError("Could not create delegator with name " + delegatorName + ", constructor failed (got null value) not sure why/how.", module);
                    }
                }
            }
        }
        return delegator;
    }

}

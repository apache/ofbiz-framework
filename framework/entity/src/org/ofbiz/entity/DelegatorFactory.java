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

/** GenericDelegator Factory Class. */
public class DelegatorFactory {

    public static final String module = DelegatorFactory.class.getName();

    public static GenericDelegator getGenericDelegator(String delegatorName) {
        if (delegatorName == null) {
            delegatorName = "default";
            Debug.logWarning(new Exception("Location where getting delegator with null name"), "Got a getGenericDelegator call with a null delegatorName, assuming default for the name.", module);
        }
        DelegatorData delegatorData = null;
        try {
            delegatorData = DelegatorData.getInstance(delegatorName);
        } catch (GenericEntityException e) {
            Debug.logError(e, "Could not create delegator with name \"" + delegatorName + "\": ", module);
        }
        if (delegatorData != null) {
            return new DelegatorImpl(delegatorData);
        }
        return null;
    }
}

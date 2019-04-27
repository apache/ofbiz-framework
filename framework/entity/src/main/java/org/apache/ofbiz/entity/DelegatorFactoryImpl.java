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
package org.apache.ofbiz.entity;

import org.apache.ofbiz.base.util.Debug;

/** A <code>DelegatorFactory</code> implementation that returns an
 * instance of <code>GenericDelegator</code>. */
public class DelegatorFactoryImpl extends DelegatorFactory {

    public static final String module = DelegatorFactoryImpl.class.getName();

    // TODO: this method should propagate the GenericEntityException
    @Override
    public Delegator getInstance(String delegatorName) {
        if (Debug.infoOn()) {
            Debug.logInfo("Creating new delegator [" + delegatorName + "] (" + Thread.currentThread().getName() + ")", module);
        }
        try {
            return new GenericDelegator(delegatorName);
        } catch (GenericEntityException e) {
            Debug.logError(e, "Error creating delegator: " + e.getMessage(), module);
            return null;
        }
    }
}

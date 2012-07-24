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
package org.ofbiz.service;

import org.ofbiz.entity.Delegator;

/**
 * A default {@link LocalDispatcherFactory} implementation.
 */
public class GenericDispatcherFactory implements LocalDispatcherFactory {
    @Override
    public LocalDispatcher createLocalDispatcher(String name, Delegator delegator) {
        // attempts to retrieve an already registered DispatchContext with the name "name"
        LocalDispatcher dispatcher = ServiceDispatcher.getLocalDispatcher(name, delegator);
        // if not found then create a new GenericDispatcher object; the constructor will also register a new DispatchContext in the ServiceDispatcher with name "dispatcherName"
        if (dispatcher == null) {
            dispatcher = new GenericDispatcher(name, delegator);
        }
        return dispatcher;
    }
}

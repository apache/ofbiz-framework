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
package org.apache.ofbiz.service;

import org.apache.ofbiz.entity.Delegator;

/**
 * A {@link LocalDispatcher} factory. Implementations register themselves in the ofbiz-component.xml file.
 */
public interface LocalDispatcherFactory {
    /**
     * Creates a <code>LocalDispatcher</code> instance based on <code>name</code> and <code>delegator</code>.
     * If a matching <code>LocalDispatcher</code> was already created, then it will be returned.
     * @param name
     * @param delegator
     */
    LocalDispatcher createLocalDispatcher(String name, Delegator delegator);
}

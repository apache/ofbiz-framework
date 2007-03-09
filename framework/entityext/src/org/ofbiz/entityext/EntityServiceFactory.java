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
package org.ofbiz.entityext;

import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.GenericDispatcher;
import org.ofbiz.service.LocalDispatcher;

/**
 * EntityEcaUtil
 */
public class EntityServiceFactory {

    public static final String module = EntityServiceFactory.class.getName();

    public static LocalDispatcher getLocalDispatcher(GenericDelegator delegator) {
        LocalDispatcher dispatcher = GenericDispatcher.getLocalDispatcher("entity-" + delegator.getDelegatorName(), delegator);
        return dispatcher;
    }
    
    public static DispatchContext getDispatchContext(GenericDelegator delegator) {
        LocalDispatcher dispatcher = getLocalDispatcher(delegator);
        if (dispatcher == null) return null;
        return dispatcher.getDispatchContext();
    }
}

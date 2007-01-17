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
package org.ofbiz.shark.mapping;

import org.enhydra.shark.api.internal.working.CallbackUtilities;
import org.enhydra.shark.api.internal.scriptmappersistence.ScriptMappingManager;
import org.enhydra.shark.api.RootException;
import org.enhydra.shark.api.ScriptMappingTransaction;
import org.enhydra.shark.api.TransactionException;

/**
 * Shark Script Mappings Implementation
 */
public class EntityScriptMappingMgr implements ScriptMappingManager {

    public static final String module = EntityScriptMappingMgr.class.getName();
    protected CallbackUtilities callBack = null;

    public void configure(CallbackUtilities callbackUtilities) throws RootException {
        this.callBack = callbackUtilities;
    }

    public ScriptMappingTransaction getScriptMappingTransaction() throws TransactionException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }
}

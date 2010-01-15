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

import java.util.Map;

import org.ofbiz.base.util.cache.UtilCache;
import org.ofbiz.entity.Delegator;

public class ObjectDaHandler implements DynamicAccessHandler {

    private static UtilCache<String,DynamicAccess> dynamicAccessCache = UtilCache.createUtilCache("security.DynamicAccessCache");

    protected Delegator delegator;

    public void setDelegator(Delegator delegator) {
        this.delegator = delegator;
    }

    public String getPattern() {
        // returns "*" as a fall back pattern (catch all)
        // if no other handler comes back this handler will catch
        return "*";
    }

    public boolean handleDynamicAccess(String accessString, String userId, String permission, Map<String, ? extends Object> context) {
        DynamicAccess da = getDynamicAccessObject(accessString);
        if (da != null) {
            return da.hasPermission(userId, permission, context);
        }
        return false;
    }

    private DynamicAccess getDynamicAccessObject(String name) {
        DynamicAccess da = dynamicAccessCache.get(name);

        if (da == null) {
            da = DynamicAccessFactory.loadDynamicAccessObject(delegator, name);
            if (da != null) {
                dynamicAccessCache.put(name, da);
            }
        }

        return da;
    }
}

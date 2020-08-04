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
package org.apache.ofbiz.service.test;

import java.util.Map;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.ServiceUtil;

public class ServiceEngineTestPermissionServices {

    private static final String MODULE = ServiceEngineTestPermissionServices.class.getName();
    private static final String RESOURCE = "ServiceErrorUiLabels";

    public static Map<String, Object> genericTestService(DispatchContext dctx, Map<String, ? extends Object> context) {
        return ServiceUtil.returnSuccess();
    }

    public static Map<String, Object> testPermissionPing(DispatchContext dctx, Map<String, ? extends Object> context) {
        Map<String, Object> result = ServiceUtil.returnSuccess();
        result.put("hasPermission", "Y".equalsIgnoreCase((String) context.get("givePermission")));
        return result;
    }

}
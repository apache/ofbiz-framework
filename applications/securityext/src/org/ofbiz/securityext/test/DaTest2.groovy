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

 package org.ofbiz.securityext.test;

import org.ofbiz.base.util.Debug;

String recordNumber = permission.substring(permission.lastIndexOf(":") + 1)
if ("system".equals(userId) && "2000".equals(recordNumber)) {
    Debug.log("Matched approval requirements {system} - {2000}; returning true");
    return true;
}

Debug.logInfo("Did not match expected requirements; returning false", "groovy");
return false;

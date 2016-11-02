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

import org.apache.ofbiz.base.util.UtilMisc


// Allow containing screens to specify URL parameters to be included in calendar navigation links
List urlParameterNames = context.urlParameterNames
if (urlParameterNames == null) {
    urlParameterNames = UtilMisc.toList("fixedAssetId", "partyId", "workEffortTypeId", "calendarType", "hideEvents", "portalPageId")
}
StringBuilder sb = new StringBuilder()
for (entry in parameters.entrySet()) {
    if (urlParameterNames.contains(entry.getKey())) {
        sb.append("&").append(entry.getKey()).append("=").append(entry.getValue())
    }
}
context.put("urlParam", sb.toString())

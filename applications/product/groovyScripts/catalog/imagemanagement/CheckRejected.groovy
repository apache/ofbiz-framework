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

import org.ofbiz.base.util.*;

Map paramMap = UtilHttp.getParameterMap(request);
def rejected = false
int rowCount = UtilHttp.getMultiFormRowCount(paramMap);
if (rowCount > 1) {
    for (int i = 0; i < rowCount; i++) {
        String thisSuffix = UtilHttp.MULTI_ROW_DELIMITER + i;
        if(paramMap.get("checkStatusId" +thisSuffix)){
            def temp = paramMap.get("checkStatusId" +thisSuffix)
            def splitTemp = temp.split("/")
            if(splitTemp[0].equals("IM_REJECTED")){
                rejected = true
            }
        }
    }
}
else {
    def temp = paramMap.get("checkStatusId_o_0")
    def splitTemp = temp.split("/")
    if(splitTemp[0].equals("IM_REJECTED")){
        rejected = true
    }
}

if(rejected){
    return "rejected"
}
else {
    return "approved"
}

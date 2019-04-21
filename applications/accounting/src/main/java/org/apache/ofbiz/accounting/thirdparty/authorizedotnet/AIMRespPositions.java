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

package org.apache.ofbiz.accounting.thirdparty.authorizedotnet;

import java.util.LinkedHashMap;
import java.util.Map;

public class AIMRespPositions extends AuthorizeResponse.RespPositions {

    // AIM v3.1 response positions
    private static Map<String, Integer> positions = new LinkedHashMap<>();
    static {
        positions.put(AuthorizeResponse.RESPONSE_CODE, 1);
        positions.put(AuthorizeResponse.REASON_CODE, 3);
        positions.put(AuthorizeResponse.REASON_TEXT, 4);
        positions.put(AuthorizeResponse.AUTHORIZATION_CODE, 5);
        positions.put(AuthorizeResponse.AVS_RESULT_CODE, 6);
        positions.put(AuthorizeResponse.CVV_RESULT_CODE, 39);
        positions.put(AuthorizeResponse.TRANSACTION_ID, 7);
        positions.put(AuthorizeResponse.AMOUNT, 10);
    }
    
    @Override
    public int getPosition(String name) {
        if (positions.containsKey(name)) {
            return positions.get(name);
        } else {
            return -1;
        }              
    }
    
    @Override
    public String getApprovalString() {
        return "1";
    }
}

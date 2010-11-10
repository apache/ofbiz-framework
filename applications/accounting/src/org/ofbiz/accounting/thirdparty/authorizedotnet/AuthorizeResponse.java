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

package org.ofbiz.accounting.thirdparty.authorizedotnet;

import java.math.BigDecimal;
import java.util.*;

import org.ofbiz.base.util.UtilValidate;

public class AuthorizeResponse {

    private List<String> response = new ArrayList<String>();
    private RespPositions pos;
    private String rawResp;
     
    // response types
    public static final int AIM_RESPONSE = 1;
    public static final int CP_RESPONSE = 2;
    
    // status constants
    public static final String APPROVED = "Approved";
    public static final String DECLINED = "Declined";
    public static final String ERROR    = "Error";
    
    // positions of the result
    public static final String RESPONSE_CODE = "RESPONSE_CODE";
    public static final String REASON_CODE = "REASON_CODE";
    public static final String REASON_TEXT = "REASON_TEXT";
    public static final String AUTHORIZATION_CODE = "AUTHORIZATION_CODE";
    public static final String AVS_RESULT_CODE = "AVS_RESULT_CODE";
    public static final String CVV_RESULT_CODE = "CVV_RESULT_CODE";
    public static final String TRANSACTION_ID = "TRANSACTION_ID";
    public static final String AMOUNT = "AMOUNT";    
    
    // singletons
    private static final AIMRespPositions aimPos = new AIMRespPositions();
    private static final CPRespPositions cpPos = new CPRespPositions();
    
    public AuthorizeResponse(String resp, int responseType) {
        this(resp, "|", responseType);
    }

    public AuthorizeResponse(String resp, String delim, int responseType) {
        this.rawResp = resp;
        this.response = splitResp(resp, delim);
        if (responseType == CP_RESPONSE) {
            pos = cpPos;
        } else {
            pos = aimPos;
        }
    }
        
    public boolean isApproved() {
        return pos.getApprovalString().equals(getResponseCode());
    }
    
    public String getTransactionId() {
        return getResponseField(TRANSACTION_ID);
    }
    
    public String getAuthorizationCode() {
        return getResponseField(AUTHORIZATION_CODE);
    }
    
    public String getResponseCode() {
        return getResponseField(RESPONSE_CODE);
    }

    public String getReasonCode() {
        return getResponseField(REASON_CODE);
    }

    public String getReasonText() {
        return getResponseField(REASON_TEXT);
    }
    
    public String getAvsResult() {
        return getResponseField(AVS_RESULT_CODE);
    }
    
    public String getCvResult() {
        return getResponseField(CVV_RESULT_CODE);
    }
    
    public BigDecimal getAmount() {
        BigDecimal amount = BigDecimal.ZERO;
        String amtStr = getResponseField(AMOUNT);
        if (UtilValidate.isNotEmpty(amtStr) && !UtilValidate.isAlphabetic(amtStr)) {
            amount = new BigDecimal(amtStr);
        }
        return amount;
    }
            
    public String getRawResponse() {
        return this.rawResp;
    }

    private String getResponseField(String field) {
        int position = pos.getPosition(field);
        if (position == -1) 
            return null;
        return getResponseField(position);
    }
    
    private String getResponseField(int position) {
        if (response.size() < position) {
            return null;
        } else {
            return response.get(position);
        }
    }
    
    private List<String> splitResp(String r, String delim) {
        int s1=0, s2=-1;
        List<String> out = new ArrayList<String>(40);
        out.add("empty");
        while (true) {
            s2 = r.indexOf(delim, s1);
            if (s2 != -1) {
                out.add(r.substring(s1, s2));
            } else {
                //the end part of the string (string not pattern terminated)
                String _ = r.substring(s1);
                if (_ != null && !_.equals("")) {
                    out.add(_);
                }
                break;
            }
            s1 = s2;
            s1 += delim.length();
        }
        return out;
    }

    @Override
    public String toString() {
        return response.toString();
    }
    
    public static abstract class RespPositions {        
        public abstract int getPosition(String name);
        public abstract String getApprovalString();
    }
}

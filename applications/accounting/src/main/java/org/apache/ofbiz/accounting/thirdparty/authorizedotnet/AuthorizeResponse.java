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

import java.math.BigDecimal;
import java.util.Arrays;

import org.apache.ofbiz.base.util.UtilValidate;

public class AuthorizeResponse {

    private String[] response;
    private RespPositions pos;
    private String rawResp;
    // response types
    public static final int AIM_RESPONSE = 1;
    public static final int CP_RESPONSE = 2;
    // status constants
    public static final String ERROR = "Error";
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
    private static final AIMRespPositions AIM_POS = new AIMRespPositions();
    private static final CPRespPositions CP_POS = new CPRespPositions();

    public AuthorizeResponse(String resp, int responseType) {
        this(resp, "\\|", responseType);
    }
    public AuthorizeResponse(String resp, String delim, int responseType) {
        this.rawResp = resp;
        this.response = resp.split(delim);
        if (responseType == CP_RESPONSE) {
            pos = CP_POS;
        } else {
            pos = AIM_POS;
        }
    }

    /**
     * Is approved boolean.
     * @return the boolean
     */
    public boolean isApproved() {
        return pos.getApprovalString().equals(getResponseCode());
    }

    /**
     * Gets transaction id.
     * @return the transaction id
     */
    public String getTransactionId() {
        return getResponseField(TRANSACTION_ID);
    }

    /**
     * Gets authorization code.
     * @return the authorization code
     */
    public String getAuthorizationCode() {
        return getResponseField(AUTHORIZATION_CODE);
    }

    /**
     * Gets response code.
     * @return the response code
     */
    public String getResponseCode() {
        return getResponseField(RESPONSE_CODE);
    }

    /**
     * Gets reason code.
     * @return the reason code
     */
    public String getReasonCode() {
        return getResponseField(REASON_CODE);
    }

    /**
     * Gets reason text.
     * @return the reason text
     */
    public String getReasonText() {
        return getResponseField(REASON_TEXT);
    }

    /**
     * Gets avs result.
     * @return the avs result
     */
    public String getAvsResult() {
        return getResponseField(AVS_RESULT_CODE);
    }

    /**
     * Gets cv result.
     * @return the cv result
     */
    public String getCvResult() {
        return getResponseField(CVV_RESULT_CODE);
    }

    /**
     * Gets amount.
     * @return the amount
     */
    public BigDecimal getAmount() {
        BigDecimal amount = BigDecimal.ZERO;
        String amtStr = getResponseField(AMOUNT);
        if (UtilValidate.isNotEmpty(amtStr) && !UtilValidate.isAlphabetic(amtStr)) {
            amount = new BigDecimal(amtStr);
        }
        return amount;
    }

    /**
     * Gets raw response.
     * @return the raw response
     */
    public String getRawResponse() {
        return this.rawResp;
    }
    private String getResponseField(String field) {
        int position = pos.getPosition(field);
        if (position == -1) {
            return null;
        }
        return getResponseField(position);
    }
    private String getResponseField(int position) {
        if (response.length < position) {
            return null;
        } else {
            // positions always start at 1; arrays start at 0
            return response[position - 1];
        }
    }
    @Override
    public String toString() {
        return Arrays.toString(response);
    }
    public abstract static class RespPositions {
        public abstract int getPosition(String name);
        public abstract String getApprovalString();
    }
}

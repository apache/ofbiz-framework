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

import java.util.*;

public class AuthorizeResponse {

    private String rawResp = null;
    private Vector response = new Vector();
    private String respCode = "";
    private String reasonCode = "";
    private String reasonText = "";
    private String version = "3.0";
    private int maxPos = 39; //maximum number of field positions in response. there are more, but currently none are used.

    //constant names for response fields
    public static int RESPONSE_CODE           = 1;
    public static int RESPONSE_SUBCODE        = 2;
    public static int RESPONSE_REASON_CODE    = 3;
    public static int RESPONSE_REASON_TEXT    = 4;
    public static int APPROVAL_CODE           = 5;
    public static int AUTHORIZATION_CODE      = 5;
    public static int AVS_RESULT_CODE         = 6;
    public static int TRANSACTION_ID          = 7;

    // 8 - 37 echoed from request
    public static int INVOICE_NUMBER          = 8;
    public static int DESCRIPTION             = 9;
    public static int AMOUNT                  = 10;
    public static int METHOD                  = 11;
    public static int TRANSACTION_TYPE        = 12;
    public static int CUSTOMER_ID             = 13;
    public static int CARDHOLDER_FIRST_NAME   = 14;
    public static int CARDHOLDER_LAST_NAME    = 15;
    public static int COMPANY                 = 16;
    public static int BILLING_ADDRESS         = 17;
    public static int CITY                    = 18;
    public static int STATE                   = 19;
    public static int ZIP                     = 20;
    public static int COUNTRY                 = 21;
    public static int PHONE                   = 22;
    public static int FAX                     = 23;
    public static int EMAIL                   = 24;
    public static int SHIP_TO_FIRST_NAME      = 25;
    public static int SHIP_TO_LAST_NAME       = 26;
    public static int SHIP_TO_COMPANY         = 27;
    public static int SHIP_TO_ADDRESS         = 28;
    public static int SHIP_TO_CITY            = 29;
    public static int SHIP_TO_STATE           = 30;
    public static int SHIP_TO_ZIP             = 31;
    public static int SHIP_TO_COUNTRY         = 32;
    public static int TAX_AMOUNT              = 33;
    public static int DUTY_AMOUNT             = 34;
    public static int FREIGHT_AMOUNT          = 35;
    public static int TAX_EXEMPT_FLAG         = 36;
    public static int PO_NUMBER               = 37;
    public static int MD5_HASH                = 38;
    public static int CID_RESPONSE_CODE       = 39;
    //public static int CAVV_RESPONSE_CODE    = 40;

    //some other constants
    public static String APPROVED = "Approved";
    public static String DECLINED = "Declined";
    public static String ERROR    = "Error";


    public AuthorizeResponse(String resp) {
        this(resp, "|");
    }

    public AuthorizeResponse(String resp, String delim) {
        this.rawResp = resp;
        this.response = splitResp(resp, delim);
        setApproval();
    }

    private void setApproval() {
        String rc = (String)response.get(RESPONSE_CODE);

        if(rc.equals("1")) {
            this.respCode = APPROVED;
        }

        if(rc.equals("2")) {
            this.respCode = DECLINED;
        }

        if(rc.equals("3")) {
            this.respCode = ERROR;
        }

        this.reasonCode = (String)response.get(RESPONSE_REASON_CODE);
        this.reasonText = (String)response.get(RESPONSE_REASON_TEXT);

    }

    public void setVersion(String version)
    {
        if (version != null && version.length() > 0)
        {
            if (version.equals("3.0") || version.equals("3.1"))
                this.version = version;
        }

    }

    public String getResponseCode() {
        return this.respCode;
    }

    public String getReasonCode() {
        return this.reasonCode;
    }

    public String getReasonText() {
        return this.reasonText;
    }

    public String getResponseField(int posNum) {

        if (this.version.equals("3.0"))
        {
            if (posNum == CID_RESPONSE_CODE)
                return "M";
        }
        if(posNum < 1 || posNum > maxPos) {
            return "unknown_field";
        }
        else {
            return (String)response.get(posNum);
        }
    }

    public String getRawResponse() {
        return this.rawResp;
    }

    private Vector splitResp(String r, String delim) {
        int s1=0, s2=-1;
        Vector out = new Vector(40);
        out.addElement("empty");
        while(true){
            s2 = r.indexOf(delim, s1);
            if(s2 != -1){
                out.addElement(r.substring(s1, s2));
            }else{
                //the end part of the string (string not pattern terminated)
                String _ = r.substring(s1);
                if(_ != null && !_.equals("")){
                    out.addElement(_);
                }
                break;
            }
            s1 = s2;
            s1 += delim.length();
        }
        return out;
    }

    public String toString() {
        return response.toString();
    }

}

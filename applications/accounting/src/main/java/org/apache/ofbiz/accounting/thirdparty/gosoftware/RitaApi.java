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
package org.apache.ofbiz.accounting.thirdparty.gosoftware;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.GeneralException;
import org.apache.ofbiz.base.util.HttpClient;
import org.apache.ofbiz.base.util.HttpClientException;
import org.apache.ofbiz.base.util.ObjectType;

public class RitaApi {

    public static final String module = RitaApi.class.getName();
    public static final String xschema = "x-schema:..\\dtd\\stnd.xdr";
    public static final String rootElement = "XML_FILE";
    public static final String reqElement = "XML_REQUEST";

    // request fields
    public static final String FUNCTION_TYPE = "FUNCTION_TYPE";
    public static final String PAYMENT_TYPE = "PAYMENT_TYPE";
    public static final String USER_ID = "USER_ID";
    public static final String USER_PW = "USER_PW";
    public static final String COMMAND = "COMMAND";
    public static final String CLIENT_ID = "CLIENT_ID";

    public static final String ACCT_NUM = "ACCT_NUM";
    public static final String EXP_MONTH = "EXP_MONTH";
    public static final String EXP_YEAR = "EXP_YEAR";
    public static final String TRANS_AMOUNT = "TRANS_AMOUNT";

    public static final String CARDHOLDER = "CARDHOLDER";
    public static final String TRACK_DATA = "TRACK_DATA";
    public static final String INVOICE = "INVOICE";
    public static final String PRESENT_FLAG = "PRESENT_FLAG";
    public static final String CUSTOMER_STREET = "CUSTOMER_STREET";
    public static final String CUSTOMER_ZIP = "CUSTOMER_ZIP";
    public static final String CVV2 = "CVV2";
    public static final String TAX_AMOUNT = "TAX_AMOUNT";
    public static final String PURCHASE_ID = "PURCHASE_ID";
    public static final String FORCE_FLAG = "FORCE_FLAG";
    public static final String ORIG_SEQ_NUM = "ORIG_SEQ_NUM";

    // response fields
    public static final String TERMINATION_STATUS = "TERMINATION_STATUS";
    public static final String INTRN_SEQ_NUM = "INTRN_SEQ_NUM";
    public static final String RESULT = "RESULT";
    public static final String RESULT_CODE = "RESULT_CODE";
    public static final String RESPONSE_TEXT = "RESPONSE_TEXT";

    public static final String AUTH_CODE = "AUTH_CODE";
    public static final String AVS_CODE = "AVS_CODE";
    public static final String CVV2_CODE = "CVV2_CODE";
    public static final String REFERENCE = "REFERENCE";
    public static final String TRANS_DATE = "TRANS_DATE";
    public static final String TRANS_TIME = "TRANS_TIME";
    public static final String ORIG_TRANS_AMOUNT = "ORIG_TRANS_AMOUNT";

    // IN/OUT validation array
    private static final String[] validOut = { TERMINATION_STATUS, INTRN_SEQ_NUM, RESULT, RESULT_CODE, RESPONSE_TEXT,
            AUTH_CODE, AVS_CODE, CVV2_CODE, REFERENCE, TRANS_DATE, TRANS_TIME,
            ORIG_TRANS_AMOUNT };

    private static final String[] validIn = { FUNCTION_TYPE, PAYMENT_TYPE, USER_ID, USER_PW, COMMAND, CLIENT_ID,
            ACCT_NUM, EXP_MONTH, EXP_YEAR, TRANS_AMOUNT, CARDHOLDER, TRACK_DATA,
            INVOICE, PRESENT_FLAG, CUSTOMER_STREET, CUSTOMER_ZIP, CVV2, TAX_AMOUNT,
            PURCHASE_ID, FORCE_FLAG, ORIG_TRANS_AMOUNT, ORIG_SEQ_NUM };

    // mode definition
    protected static final int MODE_OUT = 20;
    protected static final int MODE_IN = 10;

    // instance variables
    protected Map<String, String> document = null;
    protected String host = null;
    protected boolean ssl = false;
    protected int port = 0;
    protected int mode = 0;

    public RitaApi(Map<String, String> document) {
        this.document = new HashMap<>();
        this.document.putAll(document);
        this.mode = MODE_OUT;
    }

    public RitaApi() {
        this.document = new HashMap<>();
        this.mode = MODE_IN;
    }

    public RitaApi(String host, int port, boolean ssl) {
        this();
        this.host = host;
        this.port = port;
        this.ssl = ssl;
    }

    public void set(String name, Object value) {
        if (!checkIn(name)) {
            throw new IllegalArgumentException("Field [" + name + "] is not a valid IN parameter");
        }

        String objString = null;
        try {
            objString = (String) ObjectType.simpleTypeConvert(value, "java.lang.String", null, null);
        } catch (GeneralException | ClassCastException e) {
            Debug.logError(e, module);
            throw new IllegalArgumentException("Unable to convert value to String");
        }
        if (objString == null && value != null) {
            throw new IllegalArgumentException("Unable to convert value to String");
        } else if (objString == null) {
            objString = "";
        }

        // append to the XML document
        document.put(name, objString);
    }

    public String get(String name) {
        if (!checkOut(name)) {
            throw new IllegalArgumentException("Field [" + name + "] is not a valid OUT parameter");
        }

        return document.get(name);
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();
        for (Map.Entry<String, String> entry : document.entrySet()) {
            String name = entry.getKey();
            String value = entry.getValue();
            buf.append(name);
            buf.append(" ");
            buf.append(value);
            buf.append("\r\n");
        }
        buf.append(".\r\n");
        return buf.toString();
    }

    public Map<String, String> getDocument() {
        return this.document;
    }

    public RitaApi send() throws IOException, GeneralException {
        if (host == null || port == 0) {
            throw new GeneralException("TCP transaction not supported without valid host/port configuration");
        }

        if (mode == MODE_IN) {
            String stream = this.toString() + "..\r\n";
            Debug.logInfo("Sending - \n" + stream, module);
            String urlString = "http://" + host + ":" + port;
            HttpClient http = new HttpClient(urlString);
            http.setDebug(true);

            Map<String, String> docMap = new HashMap<>();
            String resp = null;
            try {
                resp = http.post(stream);
            } catch (HttpClientException e) {
                Debug.logError(e, module);
                throw new IOException(e.getMessage());
            }

            String[] lines = resp.split("\n");
            for (int i = 0; i < lines.length; i++) {
                Debug.logInfo(lines[i], module);
                if (!".".equals(lines[i].trim())) {
                    String[] lineSplit = lines[i].trim().split(" ", 2);
                    if (lineSplit != null && lineSplit.length == 2) {
                        docMap.put(lineSplit[0], lineSplit[1]);
                    } else {
                        Debug.logWarning("Line split error - " + lines[i], module);
                    }
                } else {
                    break;
                }
            }
            RitaApi out = new RitaApi(docMap);
            return out;
        }
        throw new IllegalStateException("Cannot send output object");
    }

    private boolean checkIn(String name) {
        for (String element : validOut) {
            if (name.equals(element)) {
                return false;
            }
        }
        return true;
    }

    private boolean checkOut(String name) {
        for (String element : validIn) {
            if (name.equals(element)) {
                return false;
            }
        }
        return true;
    }
}

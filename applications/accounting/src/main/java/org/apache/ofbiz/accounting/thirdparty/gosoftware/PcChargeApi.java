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

import java.io.DataInputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.Socket;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.GeneralException;
import org.apache.ofbiz.base.util.ObjectType;
import org.apache.ofbiz.base.util.UtilXml;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;


public class PcChargeApi {

    public static final String module = PcChargeApi.class.getName();
    public static final String xschema = "x-schema:..\\dtd\\stnd.xdr";
    public static final String rootElement = "XML_FILE";
    public static final String reqElement = "XML_REQUEST";

    public static final String USER_ID = "USER_ID";
    public static final String USER_PW = "USER_PW";
    public static final String COMMAND = "COMMAND";
    public static final String TROUTD = "TROUTD";
    public static final String PROCESSOR_ID = "PROCESSOR_ID";
    public static final String MERCH_NUM = "MERCH_NUM";
    public static final String ACCT_NUM = "ACCT_NUM";
    public static final String EXP_DATE = "EXP_DATE";
    public static final String MANUAL_FLAG = "MANUAL_FLAG";
    public static final String TRANS_AMOUNT = "TRANS_AMOUNT";
    public static final String REFERENCE = "REFERENCE";
    public static final String TRACK_DATA = "TRACK_DATA";
    public static final String CUSTOMER_CODE = "CUSTOMER_CODE";
    public static final String TAX_AMOUNT = "TAX_AMOUNT";
    public static final String PRINT_RECEIPTS_FLAG = "PRINT_RECEIPTS_FLAG";
    public static final String PERIODIC_PAYMENT_FLAG = "PERIODIC_PAYMENT_FLAG";
    public static final String OFFLINE_FLAG = "OFFLINE_FLAG";
    public static final String VOID_FLAG = "VOID_FLAG";
    public static final String ZIP_CODE = "ZIP_CODE";
    public static final String STREET = "STREET";
    public static final String TICKET_NUM = "TICKET_NUM";
    public static final String CARDHOLDER = "CARDHOLDER";
    public static final String TRANS_STORE = "TRANS_STORE";
    public static final String TRANS_ID = "TRANS_ID";
    public static final String TOTAL_AUTH = "TOTAL_AUTH";
    public static final String MULTI_FLAG = "MULTI_FLAG";
    public static final String PRESENT_FLAG = "PRESENT_FLAG";
    public static final String CVV2 = "CVV2";

    public static final String AUTH_CODE = "AUTH_CODE";
    public static final String RESULT = "RESULT";
    public static final String AVS_CODE = "AVS_CODE";
    public static final String TRANS_DATE = "TRANS_DATE";
    public static final String TICKET = "TICKET";
    public static final String CARD_ID_CODE = "CARD_ID_CODE";
    public static final String CVV2_CODE = "CVV2_CODE";

    protected static final String[] validOut = { RESULT, TRANS_DATE, AVS_CODE, CVV2_CODE, CARD_ID_CODE, TICKET };
    protected static final String[] validIn = { PROCESSOR_ID, MERCH_NUM, ACCT_NUM, EXP_DATE, TRANS_AMOUNT, TRACK_DATA,
            CUSTOMER_CODE, TAX_AMOUNT, PRINT_RECEIPTS_FLAG, PERIODIC_PAYMENT_FLAG, OFFLINE_FLAG, VOID_FLAG, ZIP_CODE,
            STREET, TICKET_NUM, CARDHOLDER, TRANS_STORE, TOTAL_AUTH, MULTI_FLAG, PRESENT_FLAG, CVV2 };

    protected static final int MODE_OUT = 20;
    protected static final int MODE_IN = 10;

    protected Document document = null;
    protected Element req = null;
    protected String host = null;
    protected int port = 0;
    protected int mode = 0;

    public PcChargeApi(Document document) {
        this.document = document;
        Element rootElement = this.document.getDocumentElement();
        if (reqElement.equals(rootElement.getNodeName())) {
            this.req = rootElement;
        } else {
            this.req = UtilXml.firstChildElement(rootElement, reqElement);
        }
        this.mode = MODE_OUT;
    }

    public PcChargeApi(boolean isFile) {
        // initialize the document
        String initialElement = rootElement;
        if (!isFile) {
            initialElement = reqElement;
        }

        this.document = UtilXml.makeEmptyXmlDocument(initialElement);
        Element root = this.document.getDocumentElement();
        if (isFile) {
            root.setAttribute("xmlns", xschema);
            this.req = UtilXml.addChildElement(root, reqElement, document);
        } else {
            this.req = root;
        }
        this.mode = MODE_IN;
    }

    public PcChargeApi(String host, int port) {
        this(false);
        this.host = host;
        this.port = port;
    }

    public PcChargeApi() {
        this(true);
    }

    public void set(String name, Object value) {
        if (!checkIn(name)) {
            throw new IllegalArgumentException("Field [" + name + "] is not a valid IN parameter");
        }

        String objString = null;
        try {
            objString = (String) ObjectType.simpleTypeConvert(value, "java.lang.String", null, null);
        } catch (GeneralException e) {
            Debug.logError(e, module);
            throw new IllegalArgumentException("Unable to convert value to String");
        } catch (ClassCastException e) {
            Debug.logError(e, module);
            throw new IllegalArgumentException("Unable to convert value to String");
        }
        if (objString == null && value != null) {
            throw new IllegalArgumentException("Unable to convert value to String");
        } else if (objString == null) {
            objString = "";
        }

        // append to the XML document
        UtilXml.addChildElementValue(req, name, objString, document);
    }

    public String get(String name) {
        if (!checkOut(name)) {
            throw new IllegalArgumentException("Field [" + name + "] is not a valid OUT parameter");
        }

        return UtilXml.childElementValue(req, name);
    }

    @Override
    public String toString() {
        try {
            return UtilXml.writeXmlDocument(document);
        } catch (IOException e) {
            Debug.logError(e, module);
            throw new IllegalStateException("Unable to write document as String");
        }
    }

    public Document getDocument() {
        return this.document;
    }

    public PcChargeApi send() throws IOException, GeneralException {
        if (host == null || port == 0) {
            throw new GeneralException("TCP transaction not supported without valid host/port configuration");
        }

        byte readBuffer[] = new byte[2250];
        if (mode == MODE_IN) {
            try (Socket sock = new Socket(host, port);
                    PrintStream ps =  new PrintStream(sock.getOutputStream());
                    DataInputStream dis = new DataInputStream(sock.getInputStream())) {
             
                ps.print(this.toString());
                ps.flush();

                StringBuilder buf = new StringBuilder();
                int size;
                while ((size = dis.read(readBuffer)) > -1) {
                    buf.append(new String(readBuffer, 0, size));
                }
                Document outDoc = null;
                try {
                    outDoc = UtilXml.readXmlDocument(buf.toString(), false);
                } catch (ParserConfigurationException e) {
                    throw new GeneralException(e);
                } catch (SAXException e) {
                    throw new GeneralException(e);
                }
                PcChargeApi out = new PcChargeApi(outDoc);
                return out;
            }
        } else {
            throw new IllegalStateException("Cannot send output object");
        }
        
    }

    private boolean checkIn(String name) {
        for (int i = 0; i < validOut.length; i++) {
            if (name.equals(validOut[i])) {
                return false;
            }
        }
        return true;
    }

    private boolean checkOut(String name) {
        for (int i = 0; i < validIn.length; i++) {
            if (name.equals(validIn[i])) {
                return false;
            }
        }
        return true;
    }
}

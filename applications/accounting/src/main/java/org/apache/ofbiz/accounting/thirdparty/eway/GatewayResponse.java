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
package org.apache.ofbiz.accounting.thirdparty.eway;

import java.io.InputStream;
import java.math.BigDecimal;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.ofbiz.base.util.Debug;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;

/**
 * A class representing the payment gateway's response to a request. It holds 
 * fields of the response which are filled in when the response arrives and 
 * available through getter methods. This response class supports all 3 payment
 * methods.
 * 
 * Based on public domain sample code provided by eWay.com.au
 */
public class GatewayResponse {

    private static final String module = GatewayResponse.class.getName();
    
    // private field definitions, values are set to default
    
    private double txBeagleScore = -1;
    private int txReturnAmount = 0;
    
    private boolean txTrxnStatus = false;
    
    private String txTrxnNumber = "";
    private String txTrxnReference = "";
    private String txTrxnOption1 = "";
    private String txTrxnOption2 = "";
    private String txTrxnOption3 = "";    
    private String txAuthCode = "";
    private String txTrxnError = "";
    
    // getter methods for the response fields
    
    public String getTrxnNumber() {
        return txTrxnNumber;
    }

    public String getTrxnReference() {
        return txTrxnReference;
    }

    public String getTrxnOption1() {
        return txTrxnOption1;
    }

    public String getTrxnOption2() {
        return txTrxnOption2;
    }

    public String getTrxnOption3() {
        return txTrxnOption3;
    }

    public String getAuthCode() {
        return txAuthCode;
    }

    public String getTrxnError() {
        return txTrxnError;
    }

    public int getReturnAmount() {
        return txReturnAmount;
    }
    
    public BigDecimal getTransactionAmount() {
        BigDecimal amt = new BigDecimal(getReturnAmount());
        amt = amt.divide(new BigDecimal(100));
        return amt.setScale(2, BigDecimal.ROUND_HALF_UP);
    }

    public boolean getTrxnStatus() {
        return txTrxnStatus;
    }

    /**
     * Gets the beagle score. Defaults to -1 in case of non-Beagle payment 
     * methods or if the response does not contain this field.
     * 
     * @return The beagle score or -1 if it was not defined in the response
     */
    public double getBeagleScore() {
        return txBeagleScore;
    }

    /**
     * Creates the GatewayResponse object by parsing an xml from a stream. Fills
     * in the fields of the object that are available through getters after this
     * method returns.
     * 
     * @param xmlstream
     *            the stream to parse the response from
     * @throws Exception
     *             if the xml contains a root element with a bad name or an
     *             unknown element, or if the xml is badly formatted
     */
    public GatewayResponse(InputStream xmlstream, GatewayRequest req) throws Exception {

        DocumentBuilderFactory builderFactory = DocumentBuilderFactory
                .newInstance();
        DocumentBuilder builder = builderFactory.newDocumentBuilder();
        Document doc = builder.parse(xmlstream);

        // get the root node
        Node rootnode = doc.getDocumentElement();
        String root = rootnode.getNodeName();

        if (root != "ewayResponse") {
            throw new Exception("Bad root element in response: " + root);
        }

        // get all elements
        NodeList list = doc.getElementsByTagName("*");
        for (int i = 0; i < list.getLength(); i++) {            
            Node node = list.item(i);                        
            String name = node.getNodeName();
            if (name == "ewayResponse")
                continue;
            Text textnode = (Text) node.getFirstChild();
            String value = "";
            if (textnode != null)
                value = textnode.getNodeValue();

            if (name == "ewayTrxnError")
                txTrxnError = value;
            else if (name == "ewayTrxnStatus") {
                if (value.toLowerCase().trim().equals("true")) {
                    txTrxnStatus = true;
                }
            } 
            else if (name == "ewayTrxnNumber")
                txTrxnNumber = value;
            else if (name == "ewayTrxnOption1")
                txTrxnOption1 = value;
            else if (name == "ewayTrxnOption2")
                txTrxnOption2 = value;
            else if (name == "ewayTrxnOption3")
                txTrxnOption3 = value;
            else if (name == "ewayReturnAmount") {
                if (!value.equals("")) {
                    txReturnAmount = Integer.parseInt(value);
                }
            }
            else if (name == "ewayAuthCode")
                txAuthCode = value;
            else if (name == "ewayTrxnReference")
                txTrxnReference = value;
            else if (name == "ewayBeagleScore") {
                if (!value.equals("")) {
                    txBeagleScore = Double.parseDouble(value);
                }
            }
            else {
                throw new Exception("Unknown field in response: " + name);
            }
        }
        
        if (req.isTestMode()) {
            Debug.logInfo("[eWay Reply]\n" + this.toString(), module);
        }
    }
    
    @Override
    public String toString() {
        StringBuffer buf = new StringBuffer();
        buf.append("<ewayResponse>").append("\n");
        buf.append("\t<ewayTrxnError>").append(txTrxnError).append("</ewayTrxnError>\n");
        buf.append("\t<ewayTrxnStatus>").append(txTrxnStatus).append("</ewayTrxnStatus>\n");
        buf.append("\t<ewayTrxnNumber>").append(txTrxnNumber).append("</ewayTrxnNumber>\n");
        buf.append("\t<ewayTrxnOption1>").append(txTrxnOption1).append("</ewayTrxnOption1>\n");
        buf.append("\t<ewayTrxnOption2>").append(txTrxnOption2).append("</ewayTrxnOption2>\n");
        buf.append("\t<ewayTrxnOption3>").append(txTrxnOption3).append("</ewayTrxnOption3>\n");
        buf.append("\t<ewayReturnAmount>").append(txReturnAmount).append("</ewayReturnAmount>\n");
        buf.append("\t<ewayAuthCode>").append(txAuthCode).append("</ewayAuthCode>\n");
        buf.append("\t<ewayBeagleScore>").append(txBeagleScore).append("</ewayBeagleScore>\n");
        buf.append("\t<ewayTrxnReference>").append(txTrxnReference).append("</ewayTrxnReference>\n");        
        buf.append("</ewayResponse>").append("\n");
        return buf.toString();
    }
}

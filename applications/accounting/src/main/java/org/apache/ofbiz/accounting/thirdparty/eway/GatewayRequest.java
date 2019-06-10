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

import java.math.BigDecimal;

import org.apache.ofbiz.base.util.Debug;

/**
 * A class representing a payment request. It holds the fields of the request,
 * provides setters and getters to manipulate them. It also holds information
 * about the type of the request: the request can be of Real-time, CVN, and
 * Beagle type, or the combination of the latter two. You set the request type
 * in the constructor of the object.
 *
 * Based on public domain sample code provided by eWay.com.au
 */
public class GatewayRequest {

    private static final String module = GatewayRequest.class.getName();

    // request urls
    public static final String REQUEST_URL_REFUND_TEST = "";
    public static final String REQUEST_URL_REFUND = "https://www.eway.com.au/gateway/xmlpaymentrefund.asp";

    public static final String REQUEST_URL_BEAGLE_TEST = "https://www.eway.com.au/gateway_cvn/xmltest/testpage.asp";
    public static final String REQUEST_URL_BEAGLE = "https://www.eway.com.au/gateway_cvn/xmlbeagle.asp";

    public static final String REQUEST_URL_CVN_TEST = "https://www.eway.com.au/gateway_cvn/xmltest/testpage.asp";
    public static final String REQUEST_URL_CVN = "https://www.eway.com.au/gateway_cvn/xmlpayment.asp";

    public static final String REQUEST_URL_RT_TEST = "https://www.eway.com.au/gateway/xmltest/testpage.asp";
    public static final String REQUEST_URL_RT = "https://www.eway.com.au/gateway/xmlpayment.asp";

    /**
     * Constant value to be used with the CVN payment method
     */
    public static final int REQUEST_METHOD_CVN = 1;

    /**
     * Constant value to be used with the Beagle (GeoIP) payment method
     */
    public static final int REQUEST_METHOD_BEAGLE = 2;

    /**
     * Constant value to be used with the Refund method
     */
    public static final int REQUEST_METHOD_REFUND = 4;

    /**
     * The request method used in the transaction, set in the constructor. This
     * value is the boolean combination of the values REQUEST_METHOD_CVN and
     * REQUEST_METHOD_BEAGLE. Defaults to 0, meaning that Real-time payment method
     * is used.
     */

    private BigDecimal txTotalAmount = BigDecimal.ZERO;
    private boolean isTestMode = false;
    private int requestMethod = 0;

    private String txCustomerID = "";
    private String txCardHoldersName = "";
    private String txCardNumber = "";
    private String txCardExpiryMonth = "";
    private String txCardExpiryYear = "";
    private String txTrxnNumber = "";
    private String txCustomerFirstName = "";
    private String txCustomerLastName = "";
    private String txCustomerEmailAddress = "";
    private String txCustomerAddress = "";
    private String txCustomerPostcode = "";
    private String txCustomerInvoiceRef = "";
    private String txCustomerInvoiceDescription = "";
    private String txCVN = "";
    private String txOption1 = "";
    private String txOption2 = "";
    private String txOption3 = "";
    private String txCustomerIPAddress = "";
    private String txCustomerBillingCountry = "";
    private String txRefundPassword = "";

    /**
     * Default constructor to be used with the Real-Time payment method. The same as
     * calling <code>GatewayRequest(0)</code>;
     */
    public GatewayRequest() {
        requestMethod = 0;
    }

    /**
     * Constructor to be used with the CVN and Beagle payment methods.
     *
     * @param method
     *            Logical combination of the REQUEST_METHOD_CVN and
     *            REQUEST_METHOD_BEAGLE constants.
     */
    public GatewayRequest(int method) {
        requestMethod = method;
    }

    /**
     * Gets the request method given when constructing the object.
     *
     * @return the request method as a logical combination of the REQUEST_METHOD_CVN
     *         and REQUEST_METHOD_BEAGLE constants.
     */
    public int getRequestMethod() {
        return requestMethod;
    }

    /**
     * Gets the URL for the configured request
     */
    public String getUrl() {
        if ((requestMethod & REQUEST_METHOD_REFUND) != 0) {
            if (isTestMode()) {
                return null;
            }
            return REQUEST_URL_REFUND;
        } else if ((requestMethod & REQUEST_METHOD_BEAGLE) != 0) {
            if (isTestMode()) {
                return REQUEST_URL_BEAGLE_TEST;
            }
            return REQUEST_URL_BEAGLE;
        } else if ((requestMethod & REQUEST_METHOD_CVN) != 0) {
            if (isTestMode()) {
                return REQUEST_URL_CVN_TEST;
            }
            return REQUEST_URL_CVN;
        } else {
            if (isTestMode()) {
                return REQUEST_URL_RT_TEST;
            }
            return REQUEST_URL_RT;
        }
    }

    /*
     * Getters and setters follow for each defined field of the request.
     */

    public String getCustomerID() {
        return txCustomerID;
    }

    public void setCustomerID(String value) {
        txCustomerID = value;
    }

    public String getRefundPassword() {
        return txRefundPassword;
    }

    public void setRefundPassword(String value) {
        txRefundPassword = value;
    }

    public BigDecimal getTotalAmount() {
        return txTotalAmount;
    }

    public void setTotalAmount(BigDecimal value) {
        txTotalAmount = value;
    }

    public String getCardHoldersName() {
        return txCardHoldersName;
    }

    public void setCardHoldersName(String value) {
        txCardHoldersName = value;
    }

    public String getCardNumber() {
        return txCardNumber;
    }

    public void setCardNumber(String value) {
        txCardNumber = value;
    }

    public String getCardExpiryMonth() {
        return txCardExpiryMonth;
    }

    public void setCardExpiryMonth(String value) {
        txCardExpiryMonth = value;
    }

    public String getCardExpiryYear() {
        return txCardExpiryYear;
    }

    public void setCardExpiryYear(String value) {
        txCardExpiryYear = value;
    }

    public String getTrxnNumber() {
        return txTrxnNumber;
    }

    public void setTrxnNumber(String value) {
        txTrxnNumber = value;
    }

    public String getCustomerFirstName() {
        return txCustomerFirstName;
    }

    public void setCustomerFirstName(String value) {
        txCustomerFirstName = value;
    }

    public String getCustomerLastName() {
        return txCustomerLastName;
    }

    public void setCustomerLastName(String value) {
        txCustomerLastName = value;
    }

    public String getCustomerEmailAddress() {
        return txCustomerEmailAddress;
    }

    public void setCustomerEmailAddress(String value) {
        txCustomerEmailAddress = value;
    }

    public String getCustomerAddress() {
        return txCustomerAddress;
    }

    public void setCustomerAddress(String value) {
        txCustomerAddress = value;
    }

    public String getCustomerPostcode() {
        return txCustomerPostcode;
    }

    public void setCustomerPostcode(String value) {
        txCustomerPostcode = value;
    }

    public String getCustomerInvoiceRef() {
        return txCustomerInvoiceRef;
    }

    public void setCustomerInvoiceRef(String value) {
        txCustomerInvoiceRef = value;
    }

    public String getCustomerInvoiceDescription() {
        return txCustomerInvoiceDescription;
    }

    public void setCustomerInvoiceDescription(String value) {
        txCustomerInvoiceDescription = value;
    }

    public String getCVN() {
        return txCVN;
    }

    public void setCVN(String value) {
        txCVN = value;
    }

    public String getOption1() {
        return txOption1;
    }

    public void setOption1(String value) {
        txOption1 = value;
    }

    public String getOption2() {
        return txOption2;
    }

    public void setOption2(String value) {
        txOption2 = value;
    }

    public String getOption3() {
        return txOption3;
    }

    public void setOption3(String value) {
        txOption3 = value;
    }

    public String getCustomerIPAddress() {
        return txCustomerIPAddress;
    }

    public void setCustomerIPAddress(String value) {
        txCustomerIPAddress = value;
    }

    public String getCustomerBillingCountry() {
        return txCustomerBillingCountry;
    }

    public void setCustomerBillingCountry(String value) {
        txCustomerBillingCountry = value;
    }

    public boolean isTestMode() {
        return isTestMode;
    }

    public void setTestMode(boolean b) {
        isTestMode = b;
    }

    /**
     * Gives the xml representation of this object. This xml will be sent to the
     * gateway. This method is public only for debugging purposes, you might wish to
     * examine the xml content. The special fields of the CVN and Beagle requests
     * are added only if the request belongs to the CVN or Beagle types,
     * respectively.
     *
     * @return The GatewayRequest object as an xml string.
     */
    public String toXml() {
        // We don't really need the overhead of creating an XML DOM object
        // just to concatenate a String together.

        Integer totalInt = txTotalAmount.multiply(new BigDecimal(100)).intValue();

        StringBuffer xml = new StringBuffer("<ewaygateway>");
        xml.append(createNode("ewayCustomerID", txCustomerID));
        xml.append(createNode("ewayTotalAmount", "" + totalInt));
        xml.append(createNode("ewayCustomerInvoiceRef", txCustomerInvoiceRef));
        xml.append(createNode("ewayCardExpiryMonth", txCardExpiryMonth));
        xml.append(createNode("ewayCardExpiryYear", txCardExpiryYear));

        // all charge methods (not refund)
        if (requestMethod != REQUEST_METHOD_REFUND) {
            xml.append(createNode("ewayCardHoldersName", txCardHoldersName));
            xml.append(createNode("ewayCardNumber", txCardNumber));
            xml.append(createNode("ewayTrxnNumber", txTrxnNumber));
            xml.append(createNode("ewayCustomerInvoiceDescription", txCustomerInvoiceDescription));
            xml.append(createNode("ewayCustomerFirstName", txCustomerFirstName));
            xml.append(createNode("ewayCustomerLastName", txCustomerLastName));
            xml.append(createNode("ewayCustomerEmail", txCustomerEmailAddress));
            xml.append(createNode("ewayCustomerAddress", txCustomerAddress));
            xml.append(createNode("ewayCustomerPostcode", txCustomerPostcode));
        }

        // fill in also CVN data if the request is of CVN type
        if (requestMethod == REQUEST_METHOD_CVN || requestMethod == REQUEST_METHOD_BEAGLE) {
            xml.append(createNode("ewayCVN", txCVN));
        }

        xml.append(createNode("ewayOption1", txOption1));
        xml.append(createNode("ewayOption2", txOption2));
        xml.append(createNode("ewayOption3", txOption3));

        // fill in also Beagle data if the request is of Beagle type
        if (requestMethod == REQUEST_METHOD_BEAGLE) {
            xml.append(createNode("ewayCustomerIPAddress", txCustomerIPAddress));
            xml.append(createNode("ewayCustomerBillingCountry",
                    txCustomerBillingCountry));
        }

        // fill in the refund password if REFUND type
        if (requestMethod == REQUEST_METHOD_REFUND) {
            xml.append(createNode("ewayOriginalTrxnNumber", txTrxnNumber));
            xml.append(createNode("ewayRefundPassword", txRefundPassword));
        }

        xml.append("</ewaygateway>");

        // log the request for test mode
        if (isTestMode()) {
            Debug.logInfo("[eWay Request] : " + xml.toString(), module);
        }

        return xml.toString();
    }

    /**
     * Helper method to build an XML node.
     *
     * @param nodeName
     *            The name of the node being created.
     * @param nodeValue
     *            The value of the node being created.
     * @return An XML node as a String in <nodName>nodeValue</nodeName> format
     */
    private static String createNode(String nodeName, String nodeValue) {
        return "<" + nodeName + ">" + nodeValue + "</" + nodeName + ">";
    }
}

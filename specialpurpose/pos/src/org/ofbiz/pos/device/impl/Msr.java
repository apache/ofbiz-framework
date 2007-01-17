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
package org.ofbiz.pos.device.impl;

import jpos.JposException;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.pos.adaptor.DataEventAdaptor;
import org.ofbiz.pos.adaptor.ErrorEventAdaptor;
import org.ofbiz.pos.device.GenericDevice;
import org.ofbiz.pos.screen.PosScreen;

public class Msr extends GenericDevice {

    public static final String module = Msr.class.getName();
    public static final int MSR_CREDIT_CARD = 100;
    public static final int MSR_GIFT_CARD = 101;
    public static final int MSR_ATM_CARD = 102;
    public static final int MSR_CUST_CARD = 701;
    public static final int MSR_CLERK_CARD = 801;
    public static final int MSR_UNKNOWN = 999;

    public Msr(String deviceName, int timeout) {
        super(deviceName, timeout);
        this.control = new jpos.MSR();
    }

    protected void initialize() throws JposException {
        Debug.logInfo("MSR [" + control.getPhysicalDeviceName() + "] Claimed : " + control.getClaimed(), module);
        final jpos.MSR msr = (jpos.MSR) control;
        msr.setDecodeData(true);
        msr.setTracksToRead(2);

        // create the data listner
        msr.addDataListener(new DataEventAdaptor() {

            public void dataOccurred(jpos.events.DataEvent event) {
                String[] decodedData = new String[7];
                byte[] track1 = null;
                byte[] track2 = null;

                try {
                    // get the raw track data
                    track1 = msr.getTrack1Data();
                    track2 = msr.getTrack2Data();

                    // get the decoded data
                    decodedData[0] = msr.getTitle();
                    decodedData[1] = msr.getFirstName();
                    decodedData[2] = msr.getMiddleInitial();
                    decodedData[3] = msr.getSurname();
                    decodedData[4] = msr.getSuffix();
                    decodedData[5] = msr.getAccountNumber();

                    // verify the acct num exists
                    if (UtilValidate.isEmpty(decodedData[5])) {
                        PosScreen.currentScreen.showDialog("dialog/error/cardreaderror");
                        msr.clearInput();
                        return;
                    }

                    // fix expDate (reversed)
                    if (msr.getExpirationDate() != null && msr.getExpirationDate().length() > 3) {
                        decodedData[6] = msr.getExpirationDate().substring(2) + msr.getExpirationDate().substring(0, 2);
                    } else {
                        PosScreen.currentScreen.showDialog("dialog/error/cardreaderror");
                        msr.clearInput();
                        return;
                    }

                    msr.clearInput();
                } catch (jpos.JposException e) {
                    Debug.logError(e, module);
                }

                processMsrData(decodedData, track1, track2);
            }
        });

        // create the error listener
        msr.addErrorListener(new ErrorEventAdaptor() {

            public void errorOccurred(jpos.events.ErrorEvent event) {
                Debug.log("Error Occurred : " + event.getErrorCodeExtended(), module);
                PosScreen.currentScreen.showDialog("dialog/error/cardreaderror");
                try {
                    msr.clearInput();
                } catch (jpos.JposException e) {
                    Debug.logError(e, module);
                }
            }
        });
    }

    protected void processMsrData(String[] decodedData, byte[] track1, byte[] track2) {
        StringBuffer msrStr = new StringBuffer();
        msrStr.append(decodedData[5]);
        msrStr.append("|");
        msrStr.append(decodedData[6]);
        msrStr.append("|");
        msrStr.append(decodedData[1]);
        msrStr.append("|");
        msrStr.append(decodedData[3]);
        Debug.log("Msr Info : " + msrStr.toString(), module);

        // implemented validation
        int msrType = MSR_UNKNOWN;
        try {
            if (UtilValidate.isAnyCard(decodedData[5])) {
                msrType = MSR_CREDIT_CARD;
            } else if (UtilValidate.isGiftCard(decodedData[5])) {
                msrType = MSR_GIFT_CARD;
            }
        } catch (NumberFormatException e) {            
        }

        // all implemented types
        switch (msrType) {
            case MSR_CREDIT_CARD:
                // make sure we are on the POS pay screen
                this.setPayPanel();
                PosScreen.currentScreen.getButtons().setLock(true);

                String[] credInfo = PosScreen.currentScreen.getInput().getFunction("CREDIT");
                if (credInfo == null) {
                    PosScreen.currentScreen.getInput().setFunction("CREDIT", "");
                }
                PosScreen.currentScreen.getInput().setFunction("MSRINFO", msrStr.toString());
                PosScreen.currentScreen.getOutput().print("Credit Card Read");
                PosScreen.currentScreen.getInput().clearInput();
                this.callEnter();
                break;
            case MSR_GIFT_CARD:
                // make sure we are on the POS pay screen
                this.setPayPanel();
                PosScreen.currentScreen.getButtons().setLock(true);

                PosScreen.currentScreen.getInput().setFunction("MSRINFO", msrStr.toString());
                PosScreen.currentScreen.getOutput().print("Gift Card Read");
                PosScreen.currentScreen.getInput().clearInput();
                this.callEnter();
                break;
            case MSR_UNKNOWN:
                PosScreen.currentScreen.showDialog("dialog/error/unknowncardtype");
                break;
        }
    }

    private void setPayPanel() {
        if (!"main/paypanel".equals(PosScreen.currentScreen.getName())) {
            PosScreen pos = PosScreen.currentScreen.showPage("paypanel", false);            
            pos.getInput().setFunction("TOTAL", "");
            pos.refresh();
            Debug.log("Switched to paypanel.xml; triggered TOTAL function", module);
        }
    }
}

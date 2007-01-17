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
package org.ofbiz.pos.jpos.service;

import jpos.JposException;
import jpos.MSRConst;
import jpos.JposConst;
import jpos.events.DataEvent;
import jpos.events.ErrorEvent;

import org.ofbiz.pos.adaptor.KeyboardAdaptor;
import org.ofbiz.pos.adaptor.KeyboardReceiver;

public class MsrKybService extends BaseService implements jpos.services.MSRService17, KeyboardReceiver {

    public static final String module = MsrKybService.class.getName();
    public static final int JPOS_MSR_ACCT_ERR = 100;
    public static final int JPOS_MSR_EXPD_ERR = 101;

    protected String title = new String();
    protected String firstname = new String();
    protected String middle = new String();
    protected String surname = new String();
    protected String suffix = new String();

    protected String[] accountNumber = new String[2];
    protected String[] expireDate = new String[2];
    protected String serviceCode = new String();

    protected byte[] track1DiscretionaryData = new byte[0];
    protected byte[] track2DiscretionaryData = new byte[0];
    protected byte[] track1Data = new byte[0];
    protected byte[] track2Data = new byte[0];
    protected byte[] track3Data = new byte[0];
    protected int[] sentinels = new int[0];
    protected int[] lrc = new int[0];

    protected boolean parseDecodeData = true;
    protected boolean decodeData = true;
    protected boolean autoDisable = false;
    protected boolean sendSentinels = true;

    protected int tracksToRead = MSRConst.MSR_TR_1_2_3;
    protected int errorType = MSRConst.MSR_ERT_CARD;

    public MsrKybService() {
        KeyboardAdaptor.getInstance(this, KeyboardAdaptor.MSR_DATA);    
    }

    // MSRService12
    public boolean getCapISO() throws JposException {
        // the type of cards this reader supports (ISO only)
        return true;
    }

    public boolean getCapJISOne() throws JposException {
        // the type of cards this reader supports (ISO only)
        return false;
    }

    public boolean getCapJISTwo() throws JposException {
        // the type of cards this reader supports (ISO only)
        return false;
    }

    public String getAccountNumber() throws JposException {
        return this.accountNumber[1];
    }

    public boolean getAutoDisable() throws JposException {
        return this.autoDisable;
    }

    public void setAutoDisable(boolean b) throws JposException {
        this.autoDisable = b;
    }

    public boolean getDecodeData() throws JposException {
        return this.decodeData;
    }

    public void setDecodeData(boolean b) throws JposException {
        this.decodeData = b;
        if (!b) {
            this.parseDecodeData = false;
        }
    }

    public int getErrorReportingType() throws JposException {
        return this.errorType;
    }

    public void setErrorReportingType(int i) throws JposException {
        this.errorType = i;
    }

    public String getExpirationDate() throws JposException {
        return this.expireDate[1];
    }

    public String getFirstName() throws JposException {
        return this.firstname;
    }

    public String getMiddleInitial() throws JposException {
        return this.middle;
    }

    public boolean getParseDecodeData() throws JposException {
        return this.parseDecodeData;
    }

    public void setParseDecodeData(boolean b) throws JposException {
        this.parseDecodeData = b;
    }

    public String getServiceCode() throws JposException {
        return this.serviceCode;
    }

    public String getSuffix() throws JposException {
        return this.suffix;
    }

    public String getSurname() throws JposException {
        return this.surname;
    }

    public String getTitle() throws JposException {
        return this.title;
    }

    public byte[] getTrack1Data() throws JposException {
        return this.track1Data;
    }

    public byte[] getTrack1DiscretionaryData() throws JposException {
        return this.track1DiscretionaryData;
    }

    public byte[] getTrack2Data() throws JposException {
        return this.track2Data;
    }

    public byte[] getTrack2DiscretionaryData() throws JposException {
        return this.track2DiscretionaryData;
    }

    public byte[] getTrack3Data() throws JposException {
        return this.track3Data;
    }

    public int getTracksToRead() throws JposException {
        return this.tracksToRead;
    }

    public void setTracksToRead(int i) throws JposException {
        this.tracksToRead = i;
    }

    public void clearInput() throws JposException {
        this.title = null;
        this.firstname = new String();
        this.middle = new String();
        this.surname = new String();
        this.suffix = new String();
        this.serviceCode = new String();
        this.accountNumber = new String[2];
        this.expireDate = new String[2];
        this.track1Data = new byte[0];
        this.track2Data = new byte[0];
        this.track3Data = new byte[0];
        this.track1DiscretionaryData = new byte[0];
        this.track2DiscretionaryData = new byte[0];
    }

    // MSRService13
    public int getCapPowerReporting() throws JposException {
        return 0;  // not used
    }

    public int getPowerNotify() throws JposException {
        return 0;  // not used
    }

    public void setPowerNotify(int i) throws JposException {
        // not used
    }

    public int getPowerState() throws JposException {
        return 0;  // not used
    }

    // MSRService15
    public boolean getCapTransmitSentinels() throws JposException {
        return true;
    }

    public byte[] getTrack4Data() throws JposException {
        return new byte[0];  // not implemented
    }

    public boolean getTransmitSentinels() throws JposException {
        return this.sendSentinels;
    }

    public void setTransmitSentinels(boolean b) throws JposException {
        this.sendSentinels = b;
    }

    // KeyboardReceiver
    public synchronized void receiveData(int[] codes, char[] chars) {
        String data = new String(chars);
        this.parseMsrString(data);

        DataEvent event = new DataEvent(this, 0);
        this.fireEvent(event);
    }

    private void parseMsrString(String str) {
        if (str.indexOf(";") == -1 || str.indexOf("^") == -1) {
            ErrorEvent error = new ErrorEvent(this, JposConst.JPOS_E_FAILURE, -1,
                    JposConst.JPOS_EL_INPUT, JposConst.JPOS_ER_CLEAR);
            this.fireEvent(error);
            return;
        }

        try {
            // parse the tracks from the character string
            String track1 = str.substring(0, str.indexOf(";"));
            String track2 = str.substring(str.indexOf(";"), str.length() - 1);

            // track 1 data
            int firstCarrot = track1.indexOf("^", 2);
            int nextCarrot = track1.indexOf("^", firstCarrot + 1);

            String tr1BegChar = track1.substring(0, 1);
            String formatCode = track1.substring(1, 2);
            String tr1AcctNum = track1.substring(2, firstCarrot);
            String cardHolder = track1.substring(firstCarrot + 1, nextCarrot);
            String tr1ExpDate = track1.substring(nextCarrot + 1, nextCarrot + 5);
            String tr1OptData = track1.substring(nextCarrot + 4, track1.length() - 2);
            String tr1EndChar = track1.substring(track1.length() - 2, track1.length() - 1);
            String tr1LrcCode = track1.substring(track1.length() - 1, track1.length());

            // track 2 data
            String tr2BegChar = track2.substring(0, 1);
            String tr2AcctNum = track2.substring(1, track2.indexOf("="));
            String tr2ExpDate = track2.substring((track2.indexOf("=") + 1), track2.indexOf("=") + 5);
            String tr2OptData = track2.substring(track2.indexOf("=") + 5, track2.length() - 2);
            String tr2EndChar = track2.substring(track2.length() - 2, track2.length() - 1);
            String tr2LrcCode = track2.substring(track2.length() - 1, track2.length());

            // see if we need to remove the sentinels
            if (this.sendSentinels) {
                track1 = track1.substring(0, track1.length() - 1);
                track2 = track2.substring(0, track2.length() - 1);
            } else {
                track1 = track1.substring(1, track1.length() - 2);
                track2 = track2.substring(0, track2.length() - 2);
            }

            // set the raw track data
            this.track1Data = track1.getBytes();
            this.track2Data = track2.getBytes();

            // parse the decoded data
            if (this.parseDecodeData) {
                // set the sentinels
                this.sentinels = new int[4];
                this.sentinels[0] = (int) tr1BegChar.charAt(0);
                this.sentinels[1] = (int) tr1EndChar.charAt(0);
                this.sentinels[2] = (int) tr2BegChar.charAt(0);
                this.sentinels[3] = (int) tr2EndChar.charAt(0);

                // set the lrcs
                this.lrc = new int[2];
                this.lrc[0] = (int) tr1LrcCode.charAt(0);
                this.lrc[1] = (int) tr2LrcCode.charAt(0);

                // set the account number
                this.accountNumber = new String[2];
                this.accountNumber[0] = tr1AcctNum.trim();
                this.accountNumber[1] = tr2AcctNum.trim();

                // set the expiration date
                this.expireDate = new String[2];
                this.expireDate[0] = tr1ExpDate.trim();
                this.expireDate[1] = tr2ExpDate.trim();

                // track discretionar info
                this.track1DiscretionaryData = tr1OptData.getBytes();
                this.track2DiscretionaryData = tr2OptData.getBytes();
                this.track1Data = track1.getBytes();
                this.track2Data = track2.getBytes();

                // other field data (i.e. name)
                this.serviceCode = formatCode.trim();
                this.surname = (cardHolder.substring(0, cardHolder.indexOf("/"))).trim();
                this.firstname = (cardHolder.substring((cardHolder.indexOf("/") + 1))).trim();
                // not sure how to support title, middle initial, and suffix
            }
        } catch (Exception e) {
            ErrorEvent error = new ErrorEvent(this, JposConst.JPOS_E_FAILURE, -1,
                    JposConst.JPOS_EL_INPUT, JposConst.JPOS_ER_CLEAR);
            this.fireEvent(error);
            return;    
        }
    }
}

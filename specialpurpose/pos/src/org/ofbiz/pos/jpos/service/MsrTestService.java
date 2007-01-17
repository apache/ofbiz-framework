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

import jpos.MSRConst;
import jpos.JposException;
import jpos.events.DataEvent;

import org.ofbiz.base.util.GeneralException;

public class MsrTestService extends BaseService implements jpos.services.MSRService17 {

    public static final String module = MsrTestService.class.getName();
    public static final int JPOS_MSR_ACCT_ERR = 100;
    public static final int JPOS_MSR_EXPD_ERR = 101;
    protected static MsrTestService instance = null;

    protected String title = "";
    protected String firstname = "John";
    protected String middle = "";
    protected String surname = "Doe";
    protected String suffix = "";

    protected String[] accountNumber = {"4111111111111111", "4111111111111111" };
    protected String[] expireDate = { "0909", "0909" };
    protected String serviceCode = "";

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

    public MsrTestService() {
        instance = this;
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

    public static void sendTest() throws GeneralException {
        if (instance == null) {
            throw new GeneralException("MsrTestService instance is null; make sure 'TestMsr' is configured in pos-containers.xml");
        }
        DataEvent event = new DataEvent(instance, 0);
        instance.fireEvent(event);
    }
}

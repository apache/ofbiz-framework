/*
 * Copyright 2001-2006 The Apache Software Foundation
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package org.ofbiz.pos.device.impl;

import jpos.JposException;
import jpos.ScannerConst;

import org.ofbiz.base.util.Debug;
import org.ofbiz.pos.adaptor.DataEventAdaptor;
import org.ofbiz.pos.device.GenericDevice;
import org.ofbiz.pos.screen.PosScreen;
import org.ofbiz.pos.event.MenuEvents;

public class Scanner extends GenericDevice {

    public static final String module = Scanner.class.getName();

    protected String deviceName = null;
    protected int timeout = -1;

    public Scanner(String deviceName, int timeout) {
        super(deviceName, timeout);
        this.control = new jpos.Scanner();
    }

    protected void initialize() throws JposException {
        Debug.logInfo("Scanner [" + control.getPhysicalDeviceName() + "] Claimed : " + control.getClaimed(), module);
        final jpos.Scanner scanner = (jpos.Scanner) control;

        // tell the driver to decode the scanned data
        scanner.setDecodeData(true);

        // create the new listner
        scanner.addDataListener(new DataEventAdaptor() {

            public void dataOccurred(jpos.events.DataEvent event) {
                byte[] scanData = null;
                int dataType = ScannerConst.SCAN_SDT_UNKNOWN;

                try {
                    dataType = scanner.getScanDataType();
                    scanData = scanner.getScanDataLabel();
                    if (scanData == null || scanData.length == 0) {
                        Debug.logWarning("Scanner driver does not support decoding data; the raw result is used instead", module);
                        scanData = scanner.getScanData();
                    }
                    
                    scanner.clearInput();
                } catch (jpos.JposException e) {
                    Debug.logError(e, module);
                }

                processScanData(scanData, dataType);
            }
        });
    }

    protected void processScanData(byte[] data, int dataType) {
        if (data != null) {
            // make sure we are on the main POS screen
            if (!"main/pospanel".equals(PosScreen.currentScreen.getName())) {
                PosScreen.currentScreen.showPage("pospanel");
            }
            
            // we can add some type checking here if needed (i.e. type of barcode; type of SKU, etc)
            if (dataType == ScannerConst.SCAN_SDT_UNKNOWN) {
                Debug.logWarning("Scanner type checking problems - check scanner driver", module);
            }

            // stuff the data to the Input component
            PosScreen.currentScreen.getInput().clearInput();
            PosScreen.currentScreen.getInput().appendString(new String(data));

            // call the ENTER event
            //this.callEnter();
            MenuEvents.addItem(PosScreen.currentScreen, null);
        }
    }
}


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
import jpos.ScannerConst;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilProperties;
import org.ofbiz.pos.adaptor.DataEventAdaptor;
import org.ofbiz.pos.device.GenericDevice;
import org.ofbiz.pos.screen.PosScreen;
import org.ofbiz.pos.event.MenuEvents;

public class Scanner extends GenericDevice {

    public static final String module = Scanner.class.getName();

    protected String deviceName = null;
    protected int timeout = -1;
    private static final boolean MULTI_BARCODES_ALLOWED = UtilProperties.propertyValueEqualsIgnoreCase("jpos.properties", "MultiBarCodesAllowed", "Y");

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

            // This deals with multi Bar Codes in one event alone. 
            // For the moment it works only with barcode id  of 1 char (see ScannerKybService).
            // I thought that javapos AutoDisable option should be the way but does not seem to work.
            // At least with my Zebex handheld and seem also not implemented though present
            // in Msr, Kbd ans Scanner services (see autoDisable, getAutoDisable, setAutoDisable).
            // I also tried to use setDataEventEnabled around getScanDataLabel/Type without success
            // Perhaps I'm missing something here, but have no more time to search...
            // I saw in JavaPOS Doc somehting about supplemental barcode. I think it's ok
            // because it seems that in this case the scanner is able to deliver only one label.
            String toInput = new String(data) + "\n  "; 
            while (toInput.indexOf("\n") > -1) {
                int posCR = toInput.indexOf("\n");            
                // stuff the data to the Input component                
                PosScreen.currentScreen.getInput().clearInput();
                PosScreen.currentScreen.getInput().appendString(toInput.substring(0, posCR));
                
                // At least one product recognized
                MenuEvents.addItem(PosScreen.currentScreen, null);
        
                if (!MULTI_BARCODES_ALLOWED) {
                    break;
                }
                // More products to add
                toInput = toInput.substring(posCR+3);
            }
        }
    }
}


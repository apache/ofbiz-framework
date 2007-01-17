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

import java.util.List;

import jpos.JposException;

import org.ofbiz.base.util.Debug;
import org.ofbiz.pos.adaptor.DataEventAdaptor;
import org.ofbiz.pos.config.ButtonEventConfig;
import org.ofbiz.pos.device.GenericDevice;
import org.ofbiz.pos.screen.PosScreen;

/**
 * Keyboard Key -> Button Mapping Tool
 *
 * This class will invoke button events based on a key press.
 * The key -> code mapping is handled in the jpos.xml file.
 * The code -> button mapping is handled in the buttonevents.xml file.
 * It is advised to map to key codes > 200.
 */
public class Keyboard extends GenericDevice {

    public static final String module = CashDrawer.class.getName();

    public Keyboard(String deviceName, int timeout) {
        super(deviceName, timeout);
        this.control = new jpos.POSKeyboard();
    }

    protected void initialize() throws JposException {
        Debug.logInfo("Keyboard [" + control.getPhysicalDeviceName() + "] Claimed : " + control.getClaimed(), module);
        final jpos.POSKeyboard keyboard = (jpos.POSKeyboard) control;

        keyboard.addDataListener(new DataEventAdaptor() {
            public void dataOccurred(jpos.events.DataEvent event) {
                Debug.log("POSKeyboard DataEvent - " + event.getWhen(), module);
                try {
                    int keyCode = keyboard.getPOSKeyData();
                    Debug.log("Received KeyCode From POSKeyboard DataEvent : " + keyCode, module);

                    // -1 is not valid
                    if (keyCode == -1) {
                        return;
                    }

                    // check for button mapping
                    if (PosScreen.currentScreen.isLocked() && 500 != keyCode) {
                        Debug.log("PosScreen is locked; not running POSKeyboard Event!", module);
                        return;
                    }

                    List buttonEvents = ButtonEventConfig.findButtonKeyAssign(keyCode);
                    if (buttonEvents != null && buttonEvents.size() > 0) {

                        Debug.log("Key -> Button Mapping(s) Found [" + keyCode + "]", module);
                        try {
                            ButtonEventConfig.invokeButtonEvents(buttonEvents, PosScreen.currentScreen);
                        } catch (ButtonEventConfig.ButtonEventNotFound e) {
                            Debug.logError(e, module);
                        } catch (ButtonEventConfig.ButtonEventException e) {
                            Debug.logError(e, module);
                        }
                    } else {
                        Debug.logWarning("No key-code button mappings found for key-code [" + keyCode + "]", module);
                    }
                } catch (JposException e) {
                    Debug.logError(e, module);
                }
            }
        });
    }
}

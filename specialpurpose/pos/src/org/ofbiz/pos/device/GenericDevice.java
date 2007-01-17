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
package org.ofbiz.pos.device;

import jpos.BaseControl;
import jpos.JposException;

import org.ofbiz.base.util.Debug;
import org.ofbiz.pos.event.MenuEvents;
import org.ofbiz.pos.screen.PosScreen;

public abstract class GenericDevice implements JposDevice {

    public static final String module = GenericDevice.class.getName();

    protected BaseControl control = null;
    protected String deviceName = null;
    protected int timeout = -1;

    public GenericDevice(String deviceName, int timeout) {
        this.deviceName = deviceName;
        this.timeout = timeout;
    }

    public void open() throws JposException {
        if (deviceName != null && control != null) {
            if (!"[NOT IMPLEMENTED]".equals(deviceName) && !"[DISABLED]".equals(deviceName)) {
                control.open(deviceName);
                control.claim(timeout);
                this.enable(true);
                this.initialize();
            }
        } else {
            Debug.logWarning("No device named [" + deviceName + "] available", module);
        }
    }

    public void close() throws JposException {
        control.release();
        control.close();
        control = null;
    }

    public boolean isEnabled() {
        try {
            return control.getDeviceEnabled();
        } catch (JposException e) {
            Debug.logError(e, module);
            return false;
        }
    }

    public void enable(boolean enable) {
        try {
            control.setDeviceEnabled(enable);
        } catch (JposException e) {
            Debug.logError(e, module);
        }
    }

    protected void callEnter() {
        // first invoke the enter event
        MenuEvents.triggerEnter(PosScreen.currentScreen, null);
        MenuEvents.triggerClear(PosScreen.currentScreen);
    }
    
    protected abstract void initialize() throws JposException;
}

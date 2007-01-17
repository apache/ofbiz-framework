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
import org.ofbiz.pos.device.GenericDevice;
import org.ofbiz.pos.screen.PosScreen;
import org.ofbiz.pos.screen.DialogCallback;
import org.ofbiz.pos.screen.PosDialog;

public class CashDrawer extends GenericDevice implements Runnable, DialogCallback {

    public static final String module = CashDrawer.class.getName();

    protected boolean openCalled = false;
    protected boolean waiting = false;
    protected Thread waiter = null;
    protected long startTime = -1;
    protected int comError = 0;

    public CashDrawer(String deviceName, int timeout) {
        super(deviceName, timeout);
        this.control = new jpos.CashDrawer();
    }

    protected void initialize() throws JposException {
        Debug.logInfo("CashDrawer [" + control.getPhysicalDeviceName() + "] Claimed : " + control.getClaimed(), module);
    }

    public void receiveDialogCb(PosDialog dialog) {
        if (this.openCalled) {
            this.openDrawer();
        }
    }

    public void resetComError() {
        this.comError = 0;
    }
    
    public void openDrawer() {
        if (this.comError > 2) {
            // only attempt this 3 times
            return;
        }
        try {
            this.openCalled = true;
            ((jpos.CashDrawer) control).openDrawer();
            this.openCalled = false;
            //this.startWaiter();
        } catch (JposException e) {
            Debug.logError(e, module);
            this.comError++;
            PosScreen.currentScreen.showDialog("dialog/error/drawererror", this);
        }
    }

    public boolean isDrawerOpen() {
        try {
            return ((jpos.CashDrawer) control).getDrawerOpened();
        } catch (JposException e) {
            Debug.logError(e, module);
        }
        return false;
    }

    protected synchronized void startWaiter() {
        if (!this.isDrawerOpen()) {
            this.waiter = new Thread(this);
            this.waiter.setDaemon(false);
            this.waiter.setName(this.getClass().getName());
            this.waiting = true;
            this.waiter.start();
        } else {
            Debug.logWarning("Drawer already open!", module);
        }
    }

    public void run() {
        Debug.log("Starting Waiter Thread", module);
        this.startTime = System.currentTimeMillis();
        while (waiting) {
            boolean isOpen = true;
            try {
                isOpen = ((jpos.CashDrawer) control).getDrawerOpened();
            } catch (JposException e) {
                Debug.logError(e, module);
                this.waiting = false;
                PosScreen.currentScreen.showDialog("dialog/error/drawererror");
            }
            if (isOpen) {
                long now = (System.currentTimeMillis() - startTime);
                if ((now > 4499) && (now % 500 == 0)) {
                    java.awt.Toolkit.getDefaultToolkit().beep();
                }
                if ((now > 4499) && (now % 5000 == 0)) {
                    PosScreen.currentScreen.showDialog("dialog/error/draweropen");
                }
            } else {
                this.waiting = false;
            }
        }
        this.startTime = -1;
        Debug.log("Waiter finished", module);
    }
}


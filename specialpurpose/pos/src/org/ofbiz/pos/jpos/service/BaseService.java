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

import java.util.ArrayList;
import java.util.List;
import java.util.Iterator;

import jpos.services.EventCallbacks;
import jpos.JposException;
import jpos.JposConst;
import jpos.events.DataEvent;
import jpos.events.ErrorEvent;
import jpos.events.DirectIOEvent;
import jpos.events.OutputCompleteEvent;
import jpos.events.StatusUpdateEvent;
import jpos.config.JposEntry;

public class BaseService implements jpos.services.BaseService, jpos.loader.JposServiceInstance {

    public static final String module = BaseService.class.getName();
    protected static boolean claimed = false;

    protected List eventQueue = new ArrayList();
    protected JposEntry entry = null;

    protected boolean freezeEvents = false;
    protected boolean deviceEnabled = false;
    protected boolean eventsEnabled = true;

    protected String deviceName = null;
    protected String healthText = null;
    protected String physicalName = null;
    protected String physicalDesc = null;
    protected String serviceDesc = null;

    protected int serviceVer = 1007000;
    protected int state = JposConst.JPOS_S_CLOSED;

    private EventCallbacks ecb = null;

    // open/close methods
    public void open(String deviceName, EventCallbacks ecb) throws JposException {
        this.deviceName = deviceName;
        this.ecb = ecb;
        this.healthText = "OK";
        this.state = JposConst.JPOS_S_IDLE;
        this.serviceDesc = entry.getProp(JposEntry.DEVICE_CATEGORY_PROP_NAME).getValueAsString();
        this.physicalDesc = entry.getProp(JposEntry.PRODUCT_DESCRIPTION_PROP_NAME).getValueAsString();
        this.physicalName = entry.getProp(JposEntry.PRODUCT_NAME_PROP_NAME).getValueAsString();
    }

    public void claim(int i) throws JposException {
        BaseService.claimed = true;
    }

    public void release() throws JposException {
        BaseService.claimed = false;
    }

    public void close() throws JposException {
        BaseService.claimed = false;
        this.freezeEvents = false;
        this.deviceEnabled = false;
        this.ecb = null;
        this.healthText = "CLOSED";
        this.state = JposConst.JPOS_S_CLOSED;
    }

    // field methods
    public String getCheckHealthText() throws JposException {
        return this.healthText;
    }

    public boolean getClaimed() throws JposException {
        return BaseService.claimed;
    }

    public int getDataCount() throws JposException {
        return this.eventQueue.size();
    }

    public boolean getDataEventEnabled() throws JposException {
        return this.eventsEnabled;
    }

    public void setDataEventEnabled(boolean b) throws JposException {
        boolean fireEvents = false;
        if (!this.eventsEnabled && b) {
            fireEvents = true;
        }
        this.eventsEnabled = b;

        if (fireEvents) {
            this.fireQueuedEvents();
        }
    }

    public boolean getDeviceEnabled() throws JposException {
        return this.deviceEnabled;
    }

    public void setDeviceEnabled(boolean b) throws JposException {
        this.deviceEnabled = b;
    }

    public String getDeviceServiceDescription() throws JposException {
        return this.serviceDesc;
    }

    public int getDeviceServiceVersion() throws JposException {
        return this.serviceVer;
    }

    public boolean getFreezeEvents() throws JposException {
        return this.freezeEvents;
    }

    public void setFreezeEvents(boolean b) throws JposException {
        this.freezeEvents = b;
    }

    public String getPhysicalDeviceDescription() throws JposException {
        return this.physicalDesc;
    }

    public String getPhysicalDeviceName() throws JposException {
        return this.physicalName;
    }

    public int getState() throws JposException {
        return this.state;
    }

    public void checkHealth(int i) throws JposException {
        // This method is not used since there is no physical device to check
    }

    public void directIO(int i, int[] ints, Object o) throws JposException {
        // This method is not used since there is no physical IO to be performed
    }

    public void setEntry(JposEntry entry) {
        this.entry = entry;
    }

    // JposServiceInstance
    public void deleteInstance() throws JposException {
        // TODO: Implement Me!
    }

    protected void fireEvent(Object ev) {
        if (this.eventsEnabled && this.ecb != null) {
            if (ev instanceof DataEvent) {
                this.ecb.fireDataEvent((DataEvent) ev);
            } else if (ev instanceof DirectIOEvent) {
                this.ecb.fireDirectIOEvent((DirectIOEvent) ev);
            } else if (ev instanceof DirectIOEvent) {
                this.ecb.fireErrorEvent((ErrorEvent) ev);
            } else if (ev instanceof DirectIOEvent) {
                this.ecb.fireOutputCompleteEvent((OutputCompleteEvent) ev);
            } else if (ev instanceof DirectIOEvent) {
                this.ecb.fireStatusUpdateEvent((StatusUpdateEvent) ev);
            }
        } else {
            this.eventQueue.add(ev);
        }
    }

    private void fireQueuedEvents() {
        List queuedList = new ArrayList(eventQueue);
        this.eventQueue = new ArrayList();
        Iterator i = queuedList.iterator();

        while (i.hasNext()) {
            Object obj = i.next();
            i.remove();
            this.fireEvent(obj);
        }
    }
}

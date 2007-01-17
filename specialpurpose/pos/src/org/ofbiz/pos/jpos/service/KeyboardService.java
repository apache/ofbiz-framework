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

import java.util.Map;
import java.util.HashMap;
import java.util.Enumeration;
import java.awt.event.KeyListener;
import java.awt.event.KeyEvent;
import java.lang.reflect.Field;

import jpos.JposException;
import jpos.POSKeyboardConst;
import jpos.JposConst;
import jpos.events.DataEvent;
import jpos.services.EventCallbacks;

import org.ofbiz.pos.adaptor.KeyboardReceiver;
import org.ofbiz.pos.adaptor.KeyboardAdaptor;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilValidate;

public class KeyboardService extends BaseService implements jpos.services.POSKeyboardService17, KeyboardReceiver, KeyListener {

    public static final String module = KeyboardService.class.getName();

    protected boolean autoDisable = false;
    protected boolean received = false;

    protected int eventTypes = POSKeyboardConst.KBD_ET_DOWN;
    protected int keyEvent = -1;
    protected int keyData = -1;

    protected KeyEvent lastEvent = null;
    protected Map keyMapping = null;

    public KeyboardService() {
        KeyboardAdaptor.getInstance(this, KeyboardAdaptor.KEYBOARD_DATA);
    }

    public void open(String deviceName, EventCallbacks ecb) throws JposException {
        super.open(deviceName, ecb);

        // setup the key mapping
        this.keyMapping = new HashMap();
        Enumeration props = entry.getPropertyNames();
        while (props.hasMoreElements()) {
            String propName = (String) props.nextElement();
            if (propName.startsWith("key.")) {
				String propValue = (String) entry.getPropertyValue(propName);
                propName = propName.substring(4);

                PosKey key = new PosKey(propName, propValue);
                keyMapping.put(new Integer(key.hashCode()), key);
            }
        }
    }

    // POSKeyboardService12
    public boolean getCapKeyUp() throws JposException {
        // we only support key down events
        return false;
    }

    public boolean getAutoDisable() throws JposException {
        return this.autoDisable;
    }

    public void setAutoDisable(boolean b) throws JposException {
        this.autoDisable = b;
    }

    public int getEventTypes() throws JposException {
        return this.eventTypes;
    }

    public void setEventTypes(int i) throws JposException {
        if (i == POSKeyboardConst.KBD_ET_DOWN)
            this.eventTypes = i;
    }

    public int getPOSKeyData() throws JposException {
        if (!received) {
            throw new JposException(JposConst.JPOS_PS_UNKNOWN, "No data received");
        }
        return keyData;
    }

    public int getPOSKeyEventType() throws JposException {
        if (!received) {
            throw new JposException(JposConst.JPOS_PS_UNKNOWN, "No data received");
        }
        return this.keyEvent;
    }

    public void clearInput() throws JposException {
        this.keyEvent = -1;
        this.keyData = -1;
        this.received = false;
    }

    // POSKeyboardService13
    public int getCapPowerReporting() throws JposException {
        return 0;
    }

    public int getPowerNotify() throws JposException {
        return 0;
    }

    public void setPowerNotify(int i) throws JposException {
    }

    public int getPowerState() throws JposException {
        return 0;
    }

    // KeyboardReceiver
    public synchronized void receiveData(int[] codes, char[] chars) {
        if (lastEvent != null) {
            KeyEvent thisEvent = lastEvent;
            PosKey thisKey = new PosKey(thisEvent);
            PosKey mappedKey = (PosKey) keyMapping.get(new Integer(thisKey.hashCode()));
            if (mappedKey != null && mappedKey.checkModifiers(thisEvent.getModifiersEx())) {
                this.received = true;
                this.keyData = mappedKey.getMappedCode();

                // fire off the event notification
                DataEvent event = new DataEvent(this, 0);
                this.fireEvent(event);
            }
        } else {
            Debug.log("Last Event is null??", module);
        }
    }

    // KeyListener
    public void keyPressed(KeyEvent event) {
        this.keyEvent = POSKeyboardConst.KBD_KET_KEYDOWN;
        this.lastEvent = event;        
    }

    public void keyTyped(KeyEvent event) {
    }

    public void keyReleased(KeyEvent event) {
        // currently this is not enabled
        if (this.eventTypes == POSKeyboardConst.KBD_ET_DOWN_UP) {
            this.keyEvent = POSKeyboardConst.KBD_KET_KEYDOWN;
            this.lastEvent = event;
        }
    }

    class PosKey {

        private int keyCode, mappedCode;
        private boolean alt, ctrl, shift;

        public PosKey(KeyEvent event) {
            this.keyCode = event.getKeyCode();
            this.mappedCode = -1;

            int modifiersEx = event.getModifiersEx();
            this.shift = this.checkShift(modifiersEx);
            this.ctrl = this.checkCtrl(modifiersEx);
            this.alt = this.checkAlt(modifiersEx);
        }

        public PosKey(String keyName, String mappedValue) throws JposException {
            String keyDef = null;
            String keyMod = null;
            if (keyName.indexOf("+") != -1) {
                keyDef = keyName.substring (0, keyName.indexOf("+")).trim();
                keyMod = keyName.substring(keyName.indexOf("+") + 1);
            } else {
                keyDef = keyName;
            }

            // set the keycode
            if (keyDef.startsWith("0x")) {
                try {
                    this.keyCode = Integer.parseInt(keyDef.substring(2), 16);
                } catch (Throwable t) {
                    Debug.logError(t, module);
                    throw new JposException(JposConst.JPOS_E_ILLEGAL, "Illegal hex code key definition [" + keyName + "]");
                }
            } else if (keyDef.startsWith("VK_")) {
                try {
                    Field kef = KeyEvent.class.getField(keyDef);
                    this.keyCode = kef.getInt(kef);
                } catch (Throwable t) {
                    Debug.logError(t, module);
                    throw new JposException(JposConst.JPOS_E_ILLEGAL, "Illegal virtual key definition [" + keyName + "]");
                }
            } else {
                try {
                    this.keyCode = Integer.parseInt(keyDef);
                } catch (Throwable t) {
                    Debug.logError(t, module);
                    throw new JposException(JposConst.JPOS_E_ILLEGAL, "Illegal key code definition [" + keyName + "]");
                }
            }

            // set the key modifiers
            String[] modifiers = null;
            if (keyMod != null && keyMod.length() > 0) {
                if (keyMod.indexOf("+") != -1) {
                    modifiers = keyMod.split("\\+");
                } else {
                    modifiers = new String[1];
                    modifiers[0] = keyMod;
                }
                for (int i = 0; i < modifiers.length; i++) {
                    if ("SHIFT".equalsIgnoreCase(modifiers[i])) {
                        this.shift = true;
                    } else {
                        this.shift = false;
                    }
                    if ("CTRL".equalsIgnoreCase(modifiers[i])) {
                        this.ctrl = true;
                    } else {
                        this.ctrl = false;
                    }
                    if ("ALT".equalsIgnoreCase(modifiers[i])) {
                        this.alt = true;
                        this.alt = false;
                    }
                }
            }

            // set the mapped value
            if (UtilValidate.isNotEmpty(mappedValue)) {
                try {
                    this.mappedCode = Integer.parseInt(mappedValue);
                } catch (Throwable t) {
                    Debug.logError(t, module);
                    throw new JposException(JposConst.JPOS_E_ILLEGAL, "Illegal key code mapping [" + mappedValue + "]");
                }
            } else {
                this.mappedCode = keyCode;
            }
        }

        public int getKeyCode() {
            return keyCode;
        }

        public int getMappedCode() {
            return mappedCode;
        }

        public int hashCode() {
            int code = this.keyCode;
            if (shift) code += KeyEvent.SHIFT_DOWN_MASK;
            if (ctrl) code += KeyEvent.CTRL_DOWN_MASK;
            if (alt) code += KeyEvent.ALT_DOWN_MASK;
            return code;
        }

        public boolean checkModifiers(int mod) {
            if (shift && !checkShift(mod)) {
                return false;
            }
            if (ctrl && !checkCtrl(mod)) {
                return false;
            }
            if (alt && !checkAlt(mod)) {
                return false;
            }
            return true;
        }

        public boolean checkShift(int mod) {
            return ((mod & KeyEvent.SHIFT_DOWN_MASK) > 0);
        }

        public boolean checkCtrl(int mod) {
            return ((mod & KeyEvent.CTRL_DOWN_MASK) > 0);
        }

        public boolean checkAlt(int mod) {
            return ((mod & KeyEvent.ALT_DOWN_MASK) > 0);
        }
    }
}

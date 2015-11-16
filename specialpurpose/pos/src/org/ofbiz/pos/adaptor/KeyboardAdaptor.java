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
package org.ofbiz.pos.adaptor;

import java.awt.Component;
import java.awt.Container;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilProperties;


/**
 * KeyboardAdaptor - Handles reading keyboard input
 *
 */
public class KeyboardAdaptor {

    public static final String module = KeyboardAdaptor.class.getName();

    public static final int EVENT_RELEASED = 2;
    public static final int EVENT_PRESSED = 1;
    public static final int EVENT_TYPED = 3;

    public static final int KEYBOARD_DATA = 100;
    public static final int SCANNER_DATA = 101;
    public static final int MSR_DATA = 102;
    public static final int ALL_DATA = 999;

    protected static List<Component> loadedComponents = new LinkedList<Component>();
    protected static Map<KeyboardReceiver, Integer> receivers = new LinkedHashMap<KeyboardReceiver, Integer>();
    protected static KeyboardAdaptor adaptor = null;
    protected static boolean running = true;

    protected KeyboardListener listener = null;

    public static KeyboardAdaptor getInstance(KeyboardReceiver receiver, int dataType) {
        if (adaptor == null) {
            synchronized(KeyboardAdaptor.class) {
                if (adaptor == null) {
                    adaptor = new KeyboardAdaptor();
                }
            }
        }

        if (receiver != null && dataType > -1) {
            receivers.put(receiver, dataType);
        }
        return adaptor;
    }

    public static KeyboardAdaptor getInstance() {
        return getInstance(null, -1);
    }

    public static void attachComponents(Component[] coms, boolean recurse) {
        // check the adaptor
        if (adaptor == null) {
            KeyboardAdaptor.getInstance();
        }

        // add the new ones to listen on
        if (adaptor != null && coms != null) {
            adaptor.addComponents(coms, recurse);
        }
    }

    public static void attachComponents(Component[] coms) {
        KeyboardAdaptor.attachComponents(coms, true);
    }

    public static void attachComponents(Container parent, boolean recurse) {
        KeyboardAdaptor.attachComponents(new Component[] { parent }, recurse);
    }

    public static void attachComponents(Container parent) {
        KeyboardAdaptor.attachComponents(parent, true);
    }

    public static void stop() {
        running = false;
    }

    private KeyboardAdaptor() {
        this.listener = new KeyboardListener();
        this.listener.setDaemon(false);
        this.listener.setName(listener.toString());
        this.listener.start();
        KeyboardAdaptor.adaptor = this;
    }

    private void addComponents(Component[] coms, boolean recurse) {
        listener.reader.configureComponents(coms, recurse);
    }

    private class KeyboardListener extends Thread {

        public final Long MAX_WAIT_SCANNER = new Long(Long.parseLong(UtilProperties.getPropertyValue("jpos", "MaxWaitScanner", "100")));
        public final Long MAX_WAIT_KEYBOARD = new Long(Long.parseLong(UtilProperties.getPropertyValue("jpos", "MaxWaitKeyboard", "10")));
        // By default keyboard entry (login & password 1st)
        public Long MAX_WAIT = MAX_WAIT_KEYBOARD;

        private List<Integer> keyCodeData = new LinkedList<Integer>();
        private List<Character> keyCharData = new LinkedList<Character>();
        private long lastKey = -1;
        private KeyReader reader = null;

        public KeyboardListener() {
            this.reader = new KeyReader(this);
        }

        private int checkDataType(char[] chars) {
            if (chars.length == 0) {
                // non-character data from keyboard interface (i.e. FN keys, enter, esc, etc)
                return KEYBOARD_DATA;
            } else if ((chars[0]) == 2 && (chars[chars.length - 1]) == 10) {
                // test for scanner data
                return SCANNER_DATA;
            } else if ((chars[0]) == 37 && (chars[chars.length - 1]) == 10) {
                // test for MSR data
                return MSR_DATA;
            } else {
                // otherwise it's keyboard data
                return KEYBOARD_DATA;
            }
        }

        protected synchronized void receiveCode(int keycode) {
            keyCodeData.add(keycode);
        }

        protected synchronized void receiveChar(char keychar) {
            keyCharData.add(new Character(keychar));
            if (keychar == '\2') {
                MAX_WAIT = MAX_WAIT_SCANNER;
            }
        }

        protected synchronized void sendData() {
            if (KeyboardAdaptor.receivers.size() > 0) {
                if (keyCharData.size() > 0 || keyCodeData.size() > 0) {
                    char[] chars = new char[keyCharData.size()];
                    int[] codes = new int[keyCodeData.size()];

                    for (int i = 0; i < codes.length; i++) {
                        Integer itg = keyCodeData.get(i);
                        codes[i] = itg.intValue();
                    }

                    for (int i = 0; i < chars.length; i++) {
                        Character ch = keyCharData.get(i);
                        chars[i] = ch.charValue();
                    }

                    for (KeyboardReceiver receiver : receivers.keySet()) {
                        int receiverType = (receivers.get(receiver)).intValue();
                        int thisDataType = this.checkDataType(chars);
                        if (receiverType == ALL_DATA || receiverType == thisDataType) {
                            receiver.receiveData(codes, chars);
                        }
                    }

                    keyCharData = new LinkedList<Character>();
                    keyCodeData = new LinkedList<Integer>();
                    lastKey = -1;
                    MAX_WAIT = MAX_WAIT_KEYBOARD;
                }
            } else {
                Debug.logWarning("No receivers configured for key input", module);
            }
        }

        protected synchronized void sendEvent(int eventType, KeyEvent event) {
            lastKey = System.currentTimeMillis();
            if (KeyboardAdaptor.receivers.size() > 0) {
                for (KeyboardReceiver receiver : KeyboardAdaptor.receivers.keySet()) {
                    if (receiver instanceof KeyListener) {
                        switch (eventType) {
                            case 1:
                                ((KeyListener) receiver).keyPressed(event);
                                break;
                            case 2:
                                ((KeyListener) receiver).keyTyped(event);
                                break;
                            case 3:
                                ((KeyListener) receiver).keyReleased(event);
                                break;
                            default:
                                break;
                        }
                    }
                }
            }
        }

        @Override
        public void run() {
            while (running) {
                long now = System.currentTimeMillis();
                if ((lastKey > -1) && (now - lastKey) >= MAX_WAIT.intValue()) {
                    this.sendData();
                }

                if (!running) {
                    break;
                } else {
                    try {
                        Thread.sleep(MAX_WAIT.intValue());
                    } catch (InterruptedException e) {
                    }
                }
            }
        }
    }

    class KeyReader implements KeyListener {

        private KeyboardListener k;

        public KeyReader(KeyboardListener k) {
            this.k = k;
        }

        private void configureComponents(Component[] coms, boolean recurse) {
            for (int i = 0; i < coms.length; i++) {
                if (!loadedComponents.contains(coms[i])) {
                    coms[i].addKeyListener(this);
                    Debug.logInfo("Added [" + coms[i].getName() + "] to KeyboardAdaptor", module);
                }
                if (recurse && coms[i] instanceof Container) {
                    Component[] nextComs = ((Container) coms[i]).getComponents();
                    configureComponents(nextComs, true);
                }
            }
        }

        public void keyTyped(KeyEvent e) {
            k.receiveChar(e.getKeyChar());
            k.sendEvent(EVENT_TYPED, e);
        }

        public void keyPressed(KeyEvent e) {
            k.receiveCode(e.getKeyCode());
            k.sendEvent(EVENT_PRESSED, e);
        }

        public void keyReleased(KeyEvent e) {
            k.sendEvent(EVENT_RELEASED, e);
        }
    }
}

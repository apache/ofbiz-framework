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
package org.ofbiz.pos.component;

import java.awt.Component;
import java.awt.Container;
import java.awt.AWTEvent;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import net.xoetrope.swing.XButton;
import net.xoetrope.xui.helper.SwingWorker;

import org.ofbiz.base.config.GenericConfigException;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.pos.config.ButtonEventConfig;
import org.ofbiz.pos.screen.PosScreen;

public class PosButton {

    public static final String module = PosButton.class.getName();

    protected Map loadedXButtons = new HashMap();
    protected PosScreen pos = null;

    public PosButton(PosScreen pos) {
        this.pos = pos;
        this.loadButtons(pos.getComponents());

        try {
            ButtonEventConfig.loadButtonConfig();
        } catch (GenericConfigException e) {
            Debug.logError(e, module);
        }
    }

    private void loadButtons(Component[] component) {
        for (int i = 0; i < component.length; i++) {
            if (component[i] instanceof XButton) {
                XButton button = (XButton) component[i];
                String buttonName = button.getName();
                String styleName = buttonName == null ? null : (String) pos.getAttribute("style", buttonName);
                PosButtonWrapper wrapper = new PosButtonWrapper(button, styleName);
                if (UtilValidate.isEmpty(buttonName)) {
                    wrapper.setEnabled(false);
                } else {
                    pos.addActionHandler(button, PosScreen.BUTTON_ACTION_METHOD);
                    loadedXButtons.put(button.getName(), wrapper);
                }
            }
            if (component[i] instanceof Container) {
                Component[] subComponents = ((Container) component[i]).getComponents();
                loadButtons(subComponents);
            }
        }
    }

    public boolean isLockable(String name) {
        if (!loadedXButtons.containsKey(name)) {
            return false;
        }

        return ButtonEventConfig.isLockable(name);
    }

    public void setLock(boolean lock) {
        Iterator i = loadedXButtons.keySet().iterator();
        while (i.hasNext()) {
            String buttonName = (String) i.next();
            if (this.isLockable(buttonName) && lock) {
                this.setLock(buttonName, lock);
            } else {
                this.setLock(buttonName, false);
            }
        }
    }

    public void setLock(String buttonName, boolean lock) {
        PosButtonWrapper button = (PosButtonWrapper) loadedXButtons.get(buttonName);
        button.setEnabled(!lock);
    }

    public void buttonPressed(final PosScreen pos, final AWTEvent event) {
        if (pos == null) {
            Debug.logWarning("Received a null PosScreen object in buttonPressed event", module);
            return;
        }
        if (event == null) {
            Debug.logWarning("Received a null AWTEvent object in buttonPressed event", module);
            return;
        }
        final String buttonName = ButtonEventConfig.getButtonName(event);
        final ClassLoader cl = this.getClassLoader(pos);

        if (buttonName != null) {
            final SwingWorker worker = new SwingWorker() {
                public Object construct() {
                    if (cl != null) {
                        Thread.currentThread().setContextClassLoader(cl);
                    }
                    try {
                        ButtonEventConfig.invokeButtonEvent(buttonName, pos, event);
                    } catch (ButtonEventConfig.ButtonEventNotFound e) {
                        Debug.logWarning(e, "Button not found - " + buttonName, module);
                    } catch (ButtonEventConfig.ButtonEventException e) {
                        Debug.logError(e, "Button invocation exception - " + buttonName, module);
                    }
                    return null;
                }
            };
            worker.start();
        } else {
            Debug.logWarning("No button name found for buttonPressed event", module);
        }
    }

    private ClassLoader getClassLoader(PosScreen pos) {
        ClassLoader cl = pos.getClassLoader();
        if (cl == null) {
            try {
                cl = Thread.currentThread().getContextClassLoader();
            } catch (Throwable t) {
            }
            if (cl == null) {
                Debug.log("No context classloader available; using class classloader", module);
                try {
                    cl = this.getClass().getClassLoader();
                } catch (Throwable t) {
                    Debug.logError(t, module);
                }
            }
        }
        
        return cl;
    }
}

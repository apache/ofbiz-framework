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
package org.ofbiz.pos.screen;


import java.awt.AWTEvent;
import java.awt.Frame;
import java.awt.Window;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

import java.util.*;

import net.xoetrope.builder.NavigationHelper;
import net.xoetrope.xui.XPage;
import net.xoetrope.xui.XProjectManager;

import org.ofbiz.base.splash.SplashLoader;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilFormatOut;
import org.ofbiz.base.util.UtilProperties;
import org.ofbiz.guiapp.xui.XuiContainer;
import org.ofbiz.guiapp.xui.XuiSession;
import org.ofbiz.pos.PosTransaction;
import org.ofbiz.pos.adaptor.KeyboardAdaptor;
import org.ofbiz.pos.component.Input;
import org.ofbiz.pos.component.Journal;
import org.ofbiz.pos.component.Operator;
import org.ofbiz.pos.component.Output;
import org.ofbiz.pos.component.PosButton;
import org.ofbiz.pos.device.DeviceLoader;

public class PosScreen extends NavigationHelper implements Runnable, DialogCallback, FocusListener {

    public static final String module = PosScreen.class.getName();
    public static final Frame appFrame = XProjectManager.getCurrentProject().getAppFrame();
    public static final Window appWin = XProjectManager.getCurrentProject().getAppWindow();
    public static final String BUTTON_ACTION_METHOD = "buttonPressed";
    public static final long MAX_INACTIVITY = 1800000;
    public static PosScreen currentScreen;

    protected static boolean monitorRunning = false;
    protected static boolean firstInit = false;
    protected static long lastActivity = 0;
    protected static Thread activityMonitor = null;

    protected ClassLoader classLoader = null;
    protected XuiSession session = null;
    protected Output output = null;
    protected Input input = null;
    protected Journal journal = null;
    protected Operator operator = null;
    protected PosButton buttons = null;
    protected String scrLocation = null;
    protected boolean isLocked = false;
    protected boolean inDialog = false;

    private Locale defaultLocale = Locale.getDefault();
    
    public PosScreen() {
        super();
        this.classLoader = Thread.currentThread().getContextClassLoader();
        this.addFocusListener(this);
    }

    public void pageCreated() {
        super.pageCreated();

        // initial settings                
        this.setEnabled(false);
        this.setVisible(false);

        // setup the shared components
        this.session = XuiContainer.getSession();
        this.output = new Output(this);
        this.input = new Input(this);
        this.journal = new Journal(this);
        this.operator = new Operator(this);
        this.setLastActivity(System.currentTimeMillis());

        if (!firstInit) {
            firstInit = true;           
            
            // pre-load a few screens
            XProjectManager.getPageManager().loadPage(this.getScreenLocation() + "/paypanel");
            XProjectManager.getPageManager().loadPage(this.getScreenLocation() + "/mgrpanel");
            XProjectManager.getPageManager().loadPage(this.getScreenLocation() + "/promopanel");

            // start the shared monitor thread
            if (activityMonitor == null) {
                monitorRunning = true;
                activityMonitor = new Thread(this);
                activityMonitor.setDaemon(false);
                activityMonitor.start();
            }

            // configure the frame/window listeners
            KeyboardAdaptor.attachComponents(appFrame, false);
            KeyboardAdaptor.attachComponents(appWin, false);
            appFrame.addFocusListener(this);
            appWin.addFocusListener(this);

            // close the splash screen
            SplashLoader.close();            
        }

        // buttons are different per screen
        this.buttons = new PosButton(this);

        // make sure all components have the keyboard set
        KeyboardAdaptor.attachComponents(this);
    }

    public void pageActivated() {
        super.pageActivated();

        this.setLastActivity(System.currentTimeMillis());

        if (session.getUserLogin() == null) {
            this.setLock(true);
        } else {
            this.setLock(isLocked);
        }

        currentScreen = this;
        this.refresh();      
    }

    public void pageDeactivated() {
        super.pageDeactivated();

        if (Debug.verboseOn()) {
            this.logInfo();
        }
    }

    public void logInfo() {
        Debug.log("App Frame :", module);
        Debug.log("name      - " + appFrame.getName(), module);
        Debug.log("title     - " + appFrame.getTitle(), module);
        Debug.log("active    - " + appFrame.isActive(), module);
        Debug.log("enabled   - " + appFrame.isEnabled(), module);
        Debug.log("visible   - " + appFrame.isVisible(), module);
        Debug.log("showing   - " + appFrame.isShowing(), module);
        Debug.log("opaque    - " + appFrame.isOpaque(), module);
        Debug.log("focusable - " + appFrame.isFocusable(), module);
        Debug.log("focused   - " + appFrame.isFocused(), module);
        Debug.log("hasFocus  - " + appFrame.hasFocus(), module);

        Debug.log("", module);
        Debug.log("App Window :", module);
        Debug.log("name      - " + appWin.getName(), module);
        Debug.log("active    - " + appWin.isActive(), module);
        Debug.log("enabled   - " + appWin.isEnabled(), module);
        Debug.log("visible   - " + appWin.isVisible(), module);
        Debug.log("showing   - " + appWin.isShowing(), module);
        Debug.log("opaque    - " + appWin.isOpaque(), module);
        Debug.log("focusable - " + appWin.isFocusable(), module);
        Debug.log("focused   - " + appWin.isFocused(), module);
        Debug.log("hasFocus  - " + appWin.hasFocus(), module);

        Debug.log("", module);

        Debug.log("POS Screen :", module);
        Debug.log("name      - " + this.getName(), module);
        Debug.log("enabled   - " + this.isEnabled(), module);
        Debug.log("visible   - " + this.isVisible(), module);
        Debug.log("showing   - " + this.isShowing(), module);
        Debug.log("opaque    - " + this.isOpaque(), module);
        Debug.log("focusable - " + this.isFocusable(), module);
        Debug.log("focused   - " + this.hasFocus(), module);
    }

    public void refresh() {
        this.refresh(true);
    }

    public void refresh(boolean updateOutput) {
        PosTransaction trans = PosTransaction.getCurrentTx(this.getSession());
        if (trans == null) {
            updateOutput = false;
        }

        appFrame.requestFocus();
        this.lockScreenButton(this);
        //this.requestFocus();

        if (!isLocked) {
            this.setEnabled(true);
            this.setVisible(true);
            journal.refresh(this);
            input.clearInput();
            operator.refresh();
            if (updateOutput) {
                if (input.isFunctionSet("PAID")) {
                    output.print(UtilProperties.getMessage("pos","ULOGIN",defaultLocale)
                            + UtilFormatOut.formatPrice(trans.getTotalDue() * -1));
                } else if (input.isFunctionSet("TOTAL")) {
                    if (trans.getTotalDue() > 0) {
                        output.print(UtilProperties.getMessage("pos","TOTALD",defaultLocale) + " " + UtilFormatOut.formatPrice(trans.getTotalDue()));
                    } else {
                        output.print(UtilProperties.getMessage("pos","PAYFIN",defaultLocale));
                    }
                } else {
                    if (PosTransaction.getCurrentTx(session).isOpen()) {
                        output.print(UtilProperties.getMessage("pos","ISOPEN",defaultLocale));
                    } else {
                        output.print(UtilProperties.getMessage("pos","ISCLOSED",defaultLocale));
                    }
                }
            }
            //journal.focus();
        } else {
            output.print(UtilProperties.getMessage("pos","ULOGIN",defaultLocale));
            //input.focus();
        }

        this.repaint();
        //this.logInfo();
    }

    public boolean isLocked() {
        return isLocked;
    }

    public void setLock(boolean lock) {
        this.buttons.setLock(lock);
        this.input.setLock(lock);
        this.output.setLock(lock);
        this.journal.setLock(lock);
        this.operator.setLock(lock);
        this.isLocked = lock;
        if (lock) {
            this.input.setFunction("LOGIN");
        }
        DeviceLoader.enable(!lock);
    }

    public XuiSession getSession() {
        return this.session;
    }

    public Input getInput() {
        return this.input;
    }

    public Output getOutput() {
        return this.output;
    }

    public Journal getJournal() {
        return this.journal;
    }

    public PosButton getButtons() {
        return this.buttons;
    }

    public void setLastActivity(long l) {
        lastActivity = l;
    }

    public ClassLoader getClassLoader() {
        return classLoader;
    }

    // generic button XUI event calls into PosButton to lookup the external reference
    public synchronized void buttonPressed() {
        this.setLastActivity(System.currentTimeMillis());
        buttons.buttonPressed(this, (AWTEvent)this.getCurrentEvent());
        journal.focus();
    }

    // generic page display methods - extends those in XPage
    public PosScreen showPage(String pageName) {
        return this.showPage(pageName, true);
    }

    public PosScreen showPage(String pageName, boolean refresh) {
        if (pageName.startsWith("/")) {
            pageName = pageName.substring(1);
        }
        XPage newPage = (XPage)XProjectManager.getPageManager().showPage(this.getScreenLocation() + "/" + pageName);         
        if (newPage instanceof PosScreen) {
            if (refresh) ((PosScreen) newPage).refresh();
            return (PosScreen) newPage;
        }
        return null;
    }

    public void lockScreenButton(PosScreen pos) {
        if ((this.getScreenLocation() + "/pospanel").equals(pos.getName())) {
            pos.getButtons().setLock("menuMain", true);
        } else if ((this.getScreenLocation() + "/mgrpanel").equals(pos.getName())) {
            pos.getButtons().setLock("menuMgr", true);
        } else if ((this.getScreenLocation() + "/paypanel").equals(pos.getName())) {
            pos.getButtons().setLock("menuPay", true);
        } else if ((this.getScreenLocation() + "/promopanel").equals(pos.getName())) {
            pos.getButtons().setLock("menuPromo", true);
        }
    }

    public PosDialog showDialog(String pageName) {
        return showDialog(pageName, this, null);
    }

    public PosDialog showDialog(String pageName, String text) {
        return showDialog(pageName, this, text);
    }

    public PosDialog showDialog(String pageName, DialogCallback cb) {
        return showDialog(pageName, cb, null);    
    }

    public PosDialog showDialog(String pageName, DialogCallback cb, String text) {
        if (pageName.startsWith("/")) {
            pageName = pageName.substring(1);
        }
        XPage dialogPage = (XPage)XProjectManager.getPageManager().loadPage(this.getScreenLocation() + "/" + pageName);        
        PosDialog dialog = PosDialog.getInstance(dialogPage, true, 0);
        dialog.showDialog(this, cb, text);
        return dialog;
    }

    // PosDialog Callback method
    public void receiveDialogCb(PosDialog dialog) {
        Debug.log("Dialog closed; refreshing screen", module);
        this.refresh();
    }

    // run method for auto-locking POS on inactivity
    public void run() {
        while (monitorRunning) {
            if (!isLocked && (System.currentTimeMillis() - lastActivity) > MAX_INACTIVITY) {
                Debug.log("POS terminal auto-lock activated", module);
                PosScreen.currentScreen.showPage("pospanel").setLock(true);
            }
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                Debug.logError(e, module);
            }
        }
    }

    public void focusGained(FocusEvent event) {
        if (Debug.verboseOn()) {
            String from = event != null && event.getOppositeComponent() != null ? event.getOppositeComponent().getName() : "??";
            Debug.log(event.getSource() + " focus gained from " + from, module);
        }
    }

    public void focusLost(FocusEvent event) {
        if (Debug.verboseOn()) {
            String to = event != null && event.getOppositeComponent() != null ? event.getOppositeComponent().getName() : "??";
            Debug.log(event.getSource() + " focus lost to " + to, module);
        }
    }

    public String getScreenLocation() {
        if (this.scrLocation == null) {
            synchronized(this) {
                if (this.scrLocation == null) {
                    String xuiProps = this.getSession().getContainer().getXuiPropertiesName();
                    String startClass = UtilProperties.getPropertyValue(xuiProps, "StartClass", "default/pospanel");
                    this.scrLocation = startClass.substring(0, startClass.indexOf("/"));
                }
            }
        }
        return this.scrLocation;
    }
    
    public void setWaitCursor() {
        setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.WAIT_CURSOR));        
    }
    public void setNormalCursor() {
        setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.DEFAULT_CURSOR));        
    }
}

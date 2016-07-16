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
package org.apache.ofbiz.pos.screen;


import java.awt.AWTEvent;
import java.awt.Frame;
import java.awt.Window;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.math.BigDecimal;
import java.util.Locale;

import net.xoetrope.xui.XPage;
import net.xoetrope.xui.XProject;
import net.xoetrope.xui.XProjectManager;

import org.apache.ofbiz.base.splash.SplashLoader;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilFormatOut;
import org.apache.ofbiz.base.util.UtilProperties;
import org.apache.ofbiz.guiapp.xui.XuiContainer;
import org.apache.ofbiz.guiapp.xui.XuiSession;
import org.apache.ofbiz.pos.PosTransaction;
import org.apache.ofbiz.pos.adaptor.KeyboardAdaptor;
import org.apache.ofbiz.pos.component.InputWithPassword;
import org.apache.ofbiz.pos.component.Journal;
import org.apache.ofbiz.pos.component.Operator;
import org.apache.ofbiz.pos.component.Output;
import org.apache.ofbiz.pos.component.PosButton;
import org.apache.ofbiz.pos.component.PromoStatusBar;
import org.apache.ofbiz.pos.device.DeviceLoader;

@SuppressWarnings("serial")
public class PosScreen extends XPage implements Runnable, DialogCallback, FocusListener {


    public static final String module = PosScreen.class.getName();
    public static final Frame appFrame = XProjectManager.getCurrentProject().getAppFrame();
    public static final Window appWin = XProjectManager.getCurrentProject().getAppWindow();
    public static final String BUTTON_ACTION_METHOD = "buttonPressed";
    public static final long MAX_INACTIVITY = Long.valueOf(UtilProperties.getPropertyValue(PosTransaction.resource, "MaxInactivity", "1800000"));
    public static PosScreen currentScreen;

    protected XProject currentProject = XProjectManager.getCurrentProject();
    protected static boolean monitorRunning = false;
    protected static boolean firstInit = false;
    protected static long lastActivity = 0;
    protected static Thread activityMonitor = null;

    protected ClassLoader classLoader = null;
    protected XuiSession session = null;
    protected Output output = null;
    protected InputWithPassword input = null;
    protected Journal journal = null;
    protected Operator operator = null;
    protected PosButton buttons = null;
    protected String scrLocation = null;
    protected boolean isLocked = false;
    protected boolean inDialog = false;
    protected PromoStatusBar promoStatusBar = null;

    private Locale defaultLocale = Locale.getDefault();

    public PosScreen() {
        super();
        this.classLoader = Thread.currentThread().getContextClassLoader();
        this.addFocusListener(this);
    }

    @Override
    public void pageCreated() {
        super.pageCreated();

        // initial settings
        this.setEnabled(false);
        this.setVisible(false);

        // setup the shared components
        this.session = XuiContainer.getSession();
        this.output = new Output(this);
        this.input = new InputWithPassword(this);
        this.journal = new Journal(this);
        this.operator = new Operator(this);
        this.promoStatusBar = new PromoStatusBar(this);
        this.setLastActivity(System.currentTimeMillis());

        if (!firstInit) {
            firstInit = true;

            // pre-load a few screens

            currentProject.getPageManager().loadPage(this.getScreenLocation() + "/paypanel");
            currentProject.getPageManager().loadPage(this.getScreenLocation() + "/mgrpanel");
            currentProject.getPageManager().loadPage(this.getScreenLocation() + "/promopanel");

            // start the shared monitor thread
            if (activityMonitor == null) {
                monitorRunning = true;
                activityMonitor = new Thread(this);
                activityMonitor.setDaemon(false);
                activityMonitor.start();
            }

            // configure the window listener
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

    @Override
    public void pageActivated() {
        super.pageActivated();

        this.setLastActivity(System.currentTimeMillis());

        // The input receives keyboard events even when it's page is deactivated, so
        // we'll clear it early to prevent the characters from displaying momentarily
        input.clearInput();

        if (session.getUserLogin() == null) {
            this.setLock(true);
        } else {
            this.setLock(isLocked);
        }

        currentScreen = this;
        this.refresh();
    }

    @Override
    public void pageDeactivated() {
        super.pageDeactivated();

        if (Debug.verboseOn()) {
            this.logInfo();
        }
    }

    public void logInfo() {
        Debug.logInfo("App Frame :", module);
        Debug.logInfo("name      - " + appFrame.getName(), module);
        Debug.logInfo("title     - " + appFrame.getTitle(), module);
        Debug.logInfo("active    - " + appFrame.isActive(), module);
        Debug.logInfo("enabled   - " + appFrame.isEnabled(), module);
        Debug.logInfo("visible   - " + appFrame.isVisible(), module);
        Debug.logInfo("showing   - " + appFrame.isShowing(), module);
        Debug.logInfo("opaque    - " + appFrame.isOpaque(), module);
        Debug.logInfo("focusable - " + appFrame.isFocusable(), module);
        Debug.logInfo("focused   - " + appFrame.isFocused(), module);
        Debug.logInfo("hasFocus  - " + appFrame.hasFocus(), module);

        Debug.logInfo("", module);
        Debug.logInfo("App Window :", module);
        Debug.logInfo("name      - " + appWin.getName(), module);
        Debug.logInfo("active    - " + appWin.isActive(), module);
        Debug.logInfo("enabled   - " + appWin.isEnabled(), module);
        Debug.logInfo("visible   - " + appWin.isVisible(), module);
        Debug.logInfo("showing   - " + appWin.isShowing(), module);
        Debug.logInfo("opaque    - " + appWin.isOpaque(), module);
        Debug.logInfo("focusable - " + appWin.isFocusable(), module);
        Debug.logInfo("focused   - " + appWin.isFocused(), module);
        Debug.logInfo("hasFocus  - " + appWin.hasFocus(), module);

        Debug.logInfo("", module);

        Debug.logInfo("POS Screen :", module);
        Debug.logInfo("name      - " + this.getName(), module);
        Debug.logInfo("enabled   - " + this.isEnabled(), module);
        Debug.logInfo("visible   - " + this.isVisible(), module);
        Debug.logInfo("showing   - " + this.isShowing(), module);
        Debug.logInfo("opaque    - " + this.isOpaque(), module);
        Debug.logInfo("focusable - " + this.isFocusable(), module);
        Debug.logInfo("focused   - " + this.hasFocus(), module);
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
                    output.print(UtilProperties.getMessage(PosTransaction.resource,"PosChange",defaultLocale)
                            + UtilFormatOut.formatPrice(trans.getTotalDue().negate()));
                } else if (input.isFunctionSet("TOTAL")) {
                    if (trans.getTotalDue().compareTo(BigDecimal.ZERO) > 0) {
                        output.print(UtilProperties.getMessage(PosTransaction.resource,"PosTotalD",defaultLocale) + " " + UtilFormatOut.formatPrice(trans.getTotalDue()));
                    } else {
                        output.print(UtilProperties.getMessage(PosTransaction.resource,"PosPayFin",defaultLocale));
                    }
                } else {
                    if (PosTransaction.getCurrentTx(session).isOpen()) {
                        output.print(UtilProperties.getMessage(PosTransaction.resource,"PosIsOpen",defaultLocale));
                    } else {
                        output.print(UtilProperties.getMessage(PosTransaction.resource,"PosIsClosed",defaultLocale));
                    }
                }
            } else {
                promoStatusBar.clear();
            }
            //journal.focus();
        } else {
            output.print(UtilProperties.getMessage(PosTransaction.resource,"PosULogin",defaultLocale));
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

    public InputWithPassword getInput() {
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

    public PromoStatusBar getPromoStatusBar() {
        return this.promoStatusBar;
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
    @Override
    public PosScreen showPage(String pageName) {
        return this.showPage(pageName, true);
    }

    public PosScreen showPage(String pageName, boolean refresh) {
        if (pageName.startsWith("/")) {
            pageName = pageName.substring(1);
        }
        XPage newPage = (XPage)currentProject.getPageManager().showPage(this.getScreenLocation() + "/" + pageName);
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
        XPage dialogPage = (XPage)currentProject.getPageManager().loadPage(this.getScreenLocation() + "/" + pageName);
        PosDialog dialog = PosDialog.getInstance(dialogPage, true, 0);
        dialog.showDialog(this, cb, text);
        return dialog;
    }

    // PosDialog Callback method
    public void receiveDialogCb(PosDialog dialog) {
        Debug.logInfo("Dialog closed; refreshing screen", module);
        this.refresh();
    }

    // run method for auto-locking POS on inactivity
    public void run() {
        while (monitorRunning) {
            if (!isLocked && (System.currentTimeMillis() - lastActivity) > MAX_INACTIVITY) {
                Debug.logInfo("POS terminal auto-lock activated", module);
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
            Debug.logInfo(event.getSource() + " focus gained from " + from, module);
        }
    }

    public void focusLost(FocusEvent event) {
        if (Debug.verboseOn()) {
            String to = event != null && event.getOppositeComponent() != null ? event.getOppositeComponent().getName() : "??";
            Debug.logInfo(event.getSource() + " focus lost to " + to, module);
        }
    }

    public String getScreenLocation() {
        if (this.scrLocation == null) {
            synchronized(this) {
                if (this.scrLocation == null) {
                    String xuiProps = this.getSession().getContainer().getXuiPropertiesName();
                    String startClass = UtilProperties.getPropertyValue(xuiProps, "StartClass", "default/pospanel");
                    this.scrLocation = startClass.substring(0, startClass.lastIndexOf("/"));
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

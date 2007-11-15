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

import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.ContainerListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.util.Locale;

import net.xoetrope.swing.XButton;
import net.xoetrope.swing.XDialog;
import net.xoetrope.swing.XEdit;
import net.xoetrope.xui.XPage;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilProperties;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.pos.PosTransaction;


public class PromoCode extends XPage {

    public static final String module = PromoCode.class.getName();
    protected static PosScreen m_pos = null;
    protected XDialog m_dialog = null;
    protected XEdit m_codeEdit = null;
    protected XButton m_cancel = null;
    protected XButton m_load = null;
    protected static PosTransaction m_trans = null;
    private boolean promoLoaded = false;

    public PromoCode(PosTransaction trans, PosScreen page) {
        m_trans = trans;
        m_pos = page;
    }

    public void openDlg() {
        m_dialog = (XDialog) pageMgr.loadPage(m_pos.getScreenLocation() + "/dialog/promocode");
        m_dialog.setCaption(UtilProperties.getMessage("pos", "LoadAPromoCode", Locale.getDefault()));
        m_codeEdit = (XEdit) m_dialog.findComponent("codeEdit");
        m_cancel = (XButton) m_dialog.findComponent("BtnCancel");
        m_load = (XButton) m_dialog.findComponent("BtnLoad");
        addMouseHandler(m_cancel, "cancel");
        addMouseHandler(m_load, "loadPromoCode");
        promoLoaded = false;

        m_dialog.pack();

        m_dialog.showDialog(this);
    }

    public synchronized void cancel() {
        if (this.wasMouseClicked()) {
            m_codeEdit.setText(null);
            closeDlg();
        }
    }

    public synchronized void loadPromoCode() {
        if (wasMouseClicked()) {
            String code = this.m_codeEdit.getText();
            if (UtilValidate.isNotEmpty(code)) {
                String response = addProductPromoCode(code);
                if (response != null) {
                    Debug.logError(response, module);
                    m_pos.showDialog("dialog/error/exception", response);
                } else {
                    promoLoaded = true;
                }
                this.m_codeEdit.setText(null);
                closeDlg();
            }
        }
    }

    private String addProductPromoCode(String code) {
        final ClassLoader cl = this.getClassLoader(m_pos);
        Thread.currentThread().setContextClassLoader(cl);
        return m_trans.addProductPromoCode(code, m_pos);
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

    private void closeDlg() {
        m_dialog.closeDlg();
    }
    
    public boolean isPromoLoaded() {
        return promoLoaded;
    }
}
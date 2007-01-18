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

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import net.xoetrope.swing.XButton;
//import org.ofbiz.pos.screen.XFocusDialog;
import net.xoetrope.swing.XEdit;
import net.xoetrope.swing.XDialog;
import net.xoetrope.xui.XPage;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilProperties;
import org.ofbiz.pos.PosTransaction;


public class SaveSale extends XPage {

    /**
     * To save a sale. 2 modes : save and keep the current sale or save and clear the current sale.  
     */
    public static final String module = SaveSale.class.getName();
    protected static PosScreen m_pos = null;
    protected XDialog m_dialog = null;
    protected XEdit m_saleName = null;
    protected XButton m_cancel = null;
    protected XButton m_save = null;
    protected XButton m_saveAndClear = null;
    protected static PosTransaction m_trans = null;
    public static SimpleDateFormat sdf = new SimpleDateFormat(UtilProperties.getMessage("pos","DateTimeFormat",Locale.getDefault()));

    //TODO : make getter and setter for members (ie m_*) if needed (extern calls).  For that in Eclipse use Source/Generate Getters and setters

    public SaveSale(PosTransaction trans, PosScreen page) {
        m_trans = trans;
        m_pos = page;
    }

    public void openDlg() {
        m_dialog = (XDialog) pageMgr.loadPage(m_pos.getScreenLocation() + "/dialog/savesale");
        m_saleName = (XEdit) m_dialog.findComponent("saleName");        
        //m_dialog.setM_focused(m_saleName); 
        m_saleName.setText(m_pos.session.getUserId() + " " + sdf.format(new Date()));
        m_dialog.setCaption(UtilProperties.getMessage("pos", "SaveASale", Locale.getDefault()));

        m_cancel = (XButton) m_dialog.findComponent("BtnCancel");
        m_save = (XButton) m_dialog.findComponent("BtnSave");
        m_saveAndClear = (XButton) m_dialog.findComponent("BtnSaveAndClear");

        addMouseHandler(m_cancel, "cancel");
        addMouseHandler(m_save, "save");
        addMouseHandler(m_saveAndClear, "saveAndClear");

        m_dialog.pack();
        m_dialog.showDialog(this);
    }

    public synchronized void cancel()
    {
        if (wasMouseClicked()) {
            this.m_dialog.closeDlg();
        }
    }

    public synchronized void save() {
        if (wasMouseClicked()) {
            String sale = m_saleName.getText();
            if (null != sale) {
                saveSale(sale);
            }
        }
    }

    public synchronized void saveAndClear() {
        if (wasMouseClicked()) {
            String sale = m_saleName.getText();
            if (null != sale) {
                saveSale(sale);
                m_trans.voidSale();
                m_pos.refresh();
            }
        }
    }

    private void saveSale(String sale) {
        final ClassLoader cl = this.getClassLoader(m_pos);
        Thread.currentThread().setContextClassLoader(cl);
        m_trans.saveSale(sale, m_pos);
        this.m_dialog.closeDlg();
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
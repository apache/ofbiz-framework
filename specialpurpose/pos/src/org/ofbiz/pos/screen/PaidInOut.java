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

import java.util.ResourceBundle;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Locale;
import java.util.Map;

import javax.swing.DefaultComboBoxModel;

import net.xoetrope.swing.XButton;
import net.xoetrope.swing.XDialog;
import net.xoetrope.swing.XEdit;
import net.xoetrope.swing.XLabel;
import net.xoetrope.swing.XComboBox;
import net.xoetrope.xui.XPage;

import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilProperties;
import org.ofbiz.pos.PosTransaction;


public class PaidInOut extends XPage {

    /**
     * To allow creating or choising a reason for a PAID IN or OUT 
     */
    public static final String module = PaidInOut.class.getName();
    protected static PosScreen m_pos = null;
    protected XDialog m_dialog = null;
    protected XLabel m_amoutLabel = null;    
    protected XEdit m_amountEdit = null;
    protected XLabel m_reasonLabel = null;
    static protected Hashtable m_reasonMap = new Hashtable();
    protected XComboBox m_reasonsCombo = null;
    protected XButton m_cancel = null;
    protected XButton m_ok = null;
    protected DefaultComboBoxModel m_comboModel = null; 
    protected static PosTransaction m_trans = null;
    protected String m_type = null; 

    //TODO : make getter and setter for members (ie m_*) if needed (extern calls). For that in Eclipse use Source/Generate Getters and setters

    public PaidInOut(PosTransaction trans, PosScreen page, String type) {
        m_trans = trans;
        m_pos = page;
        m_type = type;
    }

    public Map openDlg() {
        m_dialog = (XDialog) pageMgr.loadPage(m_pos.getScreenLocation() + "/dialog/PaidInOut");
        m_amountEdit = (XEdit) m_dialog.findComponent("amountEdit");
        m_reasonsCombo = (XComboBox) m_dialog.findComponent("ReasonsCombo");

        m_cancel = (XButton) m_dialog.findComponent("BtnCancel");
        m_ok = (XButton) m_dialog.findComponent("BtnOk");
        m_amoutLabel = (XLabel) m_dialog.findComponent("amoutLabel");        
        m_reasonLabel = (XLabel) m_dialog.findComponent("reasonLabel");
        addMouseHandler(m_cancel, "cancel");
        addMouseHandler(m_ok, "verify");

        m_comboModel = new DefaultComboBoxModel();
        ResourceBundle reasons = null;
        Enumeration reasonsKeys = null; 
        
        if (m_type.equals("IN")) {
            m_dialog.setCaption(UtilProperties.getMessage("pos", "PaidInTitle", Locale.getDefault()));
            reasons = ResourceBundle.getBundle(m_pos.getScreenLocation() + "/dialog/PaidIn", Locale.getDefault());
            reasonsKeys = reasons.getKeys();
        }
        else { // OUT
            m_dialog.setCaption(UtilProperties.getMessage("pos", "PaidOutTitle", Locale.getDefault()));
            reasons = ResourceBundle.getBundle(m_pos.getScreenLocation() + "/dialog/PaidOut", Locale.getDefault());
            reasonsKeys = reasons.getKeys();
        }
            
        while (reasonsKeys.hasMoreElements()) {
            String key = (String)reasonsKeys.nextElement();
            String val = reasons.getString(key);
            m_comboModel.addElement(val);
        }
        m_reasonsCombo.setModel(m_comboModel);
        m_reasonsCombo.setToolTipText(UtilProperties.getMessage("pos", "CreateOrChooseReasonInOut", Locale.getDefault()));

        m_dialog.pack();
        m_reasonsCombo.requestFocusInWindow();
        m_dialog.showDialog(this);
        return UtilMisc.toMap("amount", m_amountEdit.getText(), "reason", (String) m_reasonsCombo.getSelectedItem());        
    }

    public synchronized void cancel() {
        if (wasMouseClicked()) {            
            m_dialog.closeDlg();
        }
    }

    public synchronized void verify() {
        if (wasMouseClicked()) {
            String amount = m_amountEdit.getText();
            String reason = (String) m_reasonsCombo.getSelectedItem();
            if (null != amount && amount.length() > 0 && null != reason && reason.length() > 0 ) {                
                m_dialog.closeDlg();
            }
        }
    }
}

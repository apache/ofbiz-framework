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

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.swing.DefaultComboBoxModel;

import javolution.util.FastList;
import net.xoetrope.swing.XButton;
import net.xoetrope.swing.XComboBox;
import net.xoetrope.swing.XDialog;
import net.xoetrope.swing.XEdit;
import net.xoetrope.swing.XLabel;
import net.xoetrope.xui.XPage;
import net.xoetrope.xui.events.XEventHelper;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilProperties;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.pos.PosTransaction;


@SuppressWarnings("serial")
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
    protected XComboBox m_reasonCombo = null;
    protected XLabel m_reasonCommentLabel = null;
    protected XEdit m_reasonCommentEdit = null;
    protected XButton m_cancel = null;
    protected XButton m_ok = null;
    protected DefaultComboBoxModel m_comboModel = null;
    protected static PosTransaction m_trans = null;
    protected String m_type = null;
    protected boolean cancelled = false;
    private static boolean showKeyboardInSaveSale = UtilProperties.propertyValueEqualsIgnoreCase("parameters", "ShowKeyboardInSaveSale", "Y");

    //TODO : make getter and setter for members (ie m_*) if needed (extern calls). For that in Eclipse use Source/Generate Getters and setters

    public PaidInOut(PosTransaction trans, PosScreen page, String type) {
        m_trans = trans;
        m_pos = page;
        m_type = type;
    }

    public Map<String, String> openDlg() {
        m_dialog = (XDialog) pageMgr.loadPage(m_pos.getScreenLocation() + "/dialog/PaidInOut");
        m_amoutLabel = (XLabel) m_dialog.findComponent("amoutLabel");
        m_amountEdit = (XEdit) m_dialog.findComponent("amountEdit");

        m_reasonLabel = (XLabel) m_dialog.findComponent("reasonLabel");
        m_reasonCombo = (XComboBox) m_dialog.findComponent("reasonCombo");

        m_reasonCommentLabel = (XLabel) m_dialog.findComponent("reasonCommentLabel");
        m_reasonCommentEdit = (XEdit) m_dialog.findComponent("reasonCommentEdit");

        m_cancel = (XButton) m_dialog.findComponent("BtnCancel");
        m_ok = (XButton) m_dialog.findComponent("BtnOk");
        Locale locale = Locale.getDefault();

        XEventHelper.addMouseHandler(this, m_cancel, "cancel");
        XEventHelper.addMouseHandler(this, m_ok, "verify");
        XEventHelper.addMouseHandler(this, m_amountEdit, "editAmount");

        m_comboModel = new DefaultComboBoxModel();
        List<GenericValue> posPaidReasons = FastList.newInstance();
        if (m_type.equals("IN")) {
            m_dialog.setCaption(UtilProperties.getMessage(PosTransaction.resource, "PosPaidInTitle", locale));
            try {
                posPaidReasons = m_trans.getSession().getDelegator().findByAndCache("Enumeration", UtilMisc.toMap("enumTypeId", "POS_PAID_REASON_IN"));
            } catch (GenericEntityException e) {
                Debug.logError(e, module);
            }
        } else { // OUT
            m_dialog.setCaption(UtilProperties.getMessage(PosTransaction.resource, "PosPaidOutTitle", locale));
            try {
                posPaidReasons = m_trans.getSession().getDelegator().findByAndCache("Enumeration", UtilMisc.toMap("enumTypeId", "POS_PAID_REASON_OUT"));
            } catch (GenericEntityException e) {
                Debug.logError(e, module);            }
        }
        for (GenericValue reason : posPaidReasons) {
            m_comboModel.addElement(reason.get("description", locale));
        }
        m_reasonCombo.setModel(m_comboModel);
        m_reasonCombo.setToolTipText(UtilProperties.getMessage(PosTransaction.resource, "PosCreateOrChooseReasonInOut", locale));

        m_dialog.pack();
        m_reasonCombo.requestFocusInWindow();
        m_dialog.showDialog(this);
        if (cancelled) {
            return new HashMap<String, String>();
        } else {
            return UtilMisc.toMap("amount", m_amountEdit.getText(),
                    "reason", (String)(posPaidReasons.get(m_reasonCombo.getSelectedIndex())).get("enumId"),
                    "reasonComment", (String) m_reasonCommentEdit.getText());
            }
    }

    public synchronized void cancel() {
        if (wasMouseClicked()) {
            cancelled = true;
            m_dialog.closeDlg();
        }
    }

    public synchronized void verify() {
        if (wasMouseClicked()) {
            String amount = m_amountEdit.getText();
            String reason = (String) m_reasonCombo.getSelectedItem();
            if (UtilValidate.isNotEmpty(amount)&& UtilValidate.isNotEmpty(reason)) {
                m_dialog.closeDlg();
            }
        }
    }

    public synchronized void editAmount() {
        if (wasMouseClicked() && showKeyboardInSaveSale) {
            try {
                NumericKeypad numericKeypad = new NumericKeypad(m_pos);
                numericKeypad.setMinus(true);
                numericKeypad.setPercent(false);
                m_amountEdit.setText(numericKeypad.openDlg());
            } catch (Exception e) {
                Debug.logError(e, module);
            }
            m_dialog.repaint();
        }
        return;
    }
}

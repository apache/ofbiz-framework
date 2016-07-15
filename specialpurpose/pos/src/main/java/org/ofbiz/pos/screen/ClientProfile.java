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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.swing.DefaultComboBoxModel;

import net.xoetrope.swing.XButton;
import net.xoetrope.swing.XComboBox;
import net.xoetrope.swing.XDialog;
import net.xoetrope.swing.XEdit;
import net.xoetrope.swing.XLabel;
import net.xoetrope.swing.XRadioButton;
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
public class ClientProfile extends XPage implements ActionListener {

    /**
     * To allow searching, creating or editing a client profile (ie for now : Name, Email Address, Phone Number, Membership Card Number)
     */

    public static final String module = ClientProfile.class.getName();
    protected static PosScreen m_pos = null;
    protected XDialog m_dialog = null;
    protected XLabel m_nameLabel = null;
    protected XEdit m_nameEdit = null;
    protected XRadioButton m_nameRadioEquals = null;
    protected XRadioButton m_nameRadioContains = null;
    protected XLabel m_emailLabel = null;
    protected XEdit m_emailEdit = null;
    protected XLabel m_phoneLabel = null;
    protected XEdit m_phoneEdit = null;
    protected XLabel m_cardLabel = null;
    protected XEdit m_cardEdit = null;
    protected XLabel m_clientListLabel = null;
    protected XComboBox m_clientListCombo = null;
    protected List<String> m_clientListBidingCombo = new LinkedList<String>();
    protected XLabel m_tipLoginPwdLabel = null;
    protected XButton m_search = null;
    protected XButton m_create = null;
    protected XButton m_edit = null;
    protected XButton m_select = null;
    protected XButton m_cancel = null;
    protected DefaultComboBoxModel m_comboModel = null;
    protected static PosTransaction m_trans = null;
    protected String m_type = null;
    protected boolean cancelled = false;
    private static final Boolean SWIP_WITH_CARD = UtilProperties.propertyValueEqualsIgnoreCase("parameters", "SwipWithCard", "Y");
    private static final String START_SENTINEL = UtilProperties.getPropertyValue("parameters", "StartSentinel", ";");
    private static final String END_SENTINEL = UtilProperties.getPropertyValue("parameters", "EndSentinel", "?");
    private static Locale locale = Locale.getDefault();
    private String m_partyId = null;

    //TODO : make getter and setter for members (ie m_*) if needed (extern calls). For that in Eclipse use Source/Generate Getters and setters

    public ClientProfile(PosTransaction trans, PosScreen page) {
        m_trans = trans;
        m_pos = page;
    }

    public void openDlg() {
        m_dialog = (XDialog) pageMgr.loadPage(m_pos.getScreenLocation() + "/dialog/ClientProfile");

        m_nameEdit = (XEdit) m_dialog.findComponent("nameEdit"); // 1st for focus (still does not work)
        m_nameLabel = (XLabel) m_dialog.findComponent("nameLabel");
        m_nameRadioEquals = (XRadioButton) m_dialog.findComponent("NameRadioEquals");
        m_nameRadioEquals.setToolTipText(UtilProperties.getMessage(PosTransaction.resource, "PosTipNameRadioEquals", locale));
        m_nameRadioContains = (XRadioButton) m_dialog.findComponent("NameRadioContain");
        m_nameRadioContains.setToolTipText(UtilProperties.getMessage(PosTransaction.resource, "PosTipNameRadioContains", locale));

        m_emailLabel = (XLabel) m_dialog.findComponent("emailLabel");
        m_emailEdit = (XEdit) m_dialog.findComponent("emailEdit");
        m_phoneLabel = (XLabel) m_dialog.findComponent("phoneLabel");
        m_phoneEdit = (XEdit) m_dialog.findComponent("phoneEdit");
        m_cardLabel = (XLabel) m_dialog.findComponent("cardLabel");
        m_cardEdit = (XEdit) m_dialog.findComponent("cardEdit");

        m_clientListLabel = (XLabel) m_dialog.findComponent("clientListLabel");
        m_clientListCombo = (XComboBox) m_dialog.findComponent("clientListCombo");

        m_tipLoginPwdLabel = (XLabel) m_dialog.findComponent("tipLoginPwdLabel");

        m_search = (XButton) m_dialog.findComponent("BtnSearch");
        m_create = (XButton) m_dialog.findComponent("BtnCreate");
        m_edit = (XButton) m_dialog.findComponent("BtnEdit");
        m_select = (XButton) m_dialog.findComponent("BtnSelect");
        m_cancel = (XButton) m_dialog.findComponent("BtnCancel");

        XEventHelper.addMouseHandler(this, m_search, "search");
        XEventHelper.addMouseHandler(this, m_create, "edit(create)");
        XEventHelper.addMouseHandler(this, m_edit, "edit(update)");
        XEventHelper.addMouseHandler(this, m_select, "select");
        XEventHelper.addMouseHandler(this, m_cancel, "cancel");
        XEventHelper.addMouseHandler(this, m_nameEdit, "editName");
        XEventHelper.addMouseHandler(this, m_emailEdit, "editEmail");
        XEventHelper.addMouseHandler(this, m_phoneEdit, "editPhone");
        XEventHelper.addMouseHandler(this, m_cardEdit, "editCard");

        m_comboModel = new DefaultComboBoxModel();
        m_dialog.setCaption(UtilProperties.getMessage(PosTransaction.resource, "PosClientProfile", locale));
        m_clientListCombo.setModel(m_comboModel);
        m_clientListCombo.setToolTipText(UtilProperties.getMessage(PosTransaction.resource, "PosSelectClientToEdit", locale));
        m_clientListCombo.addActionListener(this);

        m_dialog.pack();
        m_nameEdit.requestFocusInWindow();
        m_dialog.showDialog(this);
        if (!cancelled) {
            GenericValue  person = null;
            try {
                person = m_trans.getSession().getDelegator().findOne("Person", UtilMisc.toMap("partyId", m_partyId), false);
            } catch (GenericEntityException e) {
                Debug.logError(e, module);
            }
            if (UtilValidate.isNotEmpty(person)) {
                String cardId = person.getString("cardId");
                if (UtilValidate.isNotEmpty(cardId)) {
                    String partyId = m_trans.getPartyId();
                    m_trans.setPartyId(m_partyId);
                    String result = m_trans.addProductPromoCode(cardId);
                    if (UtilValidate.isEmpty(result)) {
                        m_pos.getPromoStatusBar().displayClient(person.getString("lastName"));
                        m_pos.getPromoStatusBar().addPromoCode(cardId);
                    } else {
                        m_trans.setPartyId(partyId);
                        m_pos.showDialog("dialog/error/exception", result);
                    }
                }
            }
        }
    }

    public synchronized void cancel() {
        if (wasMouseClicked()) {
            cancelled = true;
            m_dialog.closeDlg();
        }
    }

    public synchronized void editName() {
        if (wasMouseClicked() && UtilProperties.propertyValueEqualsIgnoreCase("parameters", "ShowKeyboardInSaveSale", "Y")) {
            try {
                Keyboard keyboard = new Keyboard(m_pos);
                keyboard.setText(m_nameEdit.getText());
                m_nameEdit.setText(keyboard.openDlg());
            } catch (Exception e) {
                Debug.logError(e, module);
            }
            m_dialog.repaint();
        }
        return;
    }

    public synchronized void editEmail() {
        if (wasMouseClicked() && UtilProperties.propertyValueEqualsIgnoreCase("parameters", "ShowKeyboardInSaveSale", "Y")) {
            try {
                Keyboard keyboard = new Keyboard(m_pos);
                keyboard.setText(m_emailEdit.getText());
                m_emailEdit.setText(keyboard.openDlg());
            } catch (Exception e) {
                Debug.logError(e, module);
            }
            m_dialog.repaint();
        }
        return;
    }

    public synchronized void editPhone() {
        if (wasMouseClicked() && UtilProperties.propertyValueEqualsIgnoreCase("parameters", "ShowKeyboardInSaveSale", "Y")) {
            try {
                NumericKeypad numericKeypad = new NumericKeypad(m_pos);
                numericKeypad.setMinus(false); // this order must be respected
                numericKeypad.setPercent(false);
                numericKeypad.setText(m_phoneEdit.getText());
                m_phoneEdit.setText(numericKeypad.openDlg());
            } catch (Exception e) {
                Debug.logError(e, module);
            }
            m_dialog.repaint();
        }
        return;
    }



    public synchronized void editCard() {
        if (wasMouseClicked() && UtilProperties.propertyValueEqualsIgnoreCase("parameters", "ShowKeyboardInSaveSale", "Y") && !SWIP_WITH_CARD) {
            try {
                NumericKeypad numericKeypad = new NumericKeypad(m_pos);
                numericKeypad.setMinus(false); // this order must be respected
                numericKeypad.setPercent(false);
                numericKeypad.setText(m_cardEdit.getText());
                m_cardEdit.setText(numericKeypad.openDlg());
            } catch (Exception e) {
                Debug.logError(e, module);
            }
            m_dialog.repaint();
        } else {
            String card = m_cardEdit.getText();
            if (SWIP_WITH_CARD && UtilValidate.isNotEmpty(m_cardEdit.getText())
                    && (card.startsWith(START_SENTINEL) && card.endsWith(END_SENTINEL))) {
                m_cardEdit.setText(card.substring(1, card.length() - 1));
            }
        }

        return;
    }

    public synchronized void search() {
        if (wasMouseClicked()) {
            String name = m_nameEdit.getText().trim();
            String email = m_emailEdit.getText().trim();
            String phone = m_phoneEdit.getText().trim();
            String card = m_cardEdit.getText().trim();
            searchClientProfile(name, email, phone, card);
        }
    }

    public synchronized void edit(String editType) {
        if (wasMouseClicked()) {
            String name = m_nameEdit.getText().trim();
            String email = m_emailEdit.getText().trim();
            String phone = m_phoneEdit.getText().trim();
            String card = m_cardEdit.getText().trim();
            if (UtilValidate.isNotEmpty(name)) {
                if (UtilValidate.isNotEmpty(card) && SWIP_WITH_CARD &&  card.startsWith(START_SENTINEL) && card.endsWith(END_SENTINEL)) {
                    card = card.substring(1, card.length() - 1);
                }
                editClientProfile(name, email, phone, card, editType, m_partyId);
            } else {
                m_pos.showDialog("dialog/error/exception", UtilProperties.getMessage(PosTransaction.resource, "PosFieldsRequired", locale));
            }
        }
    }

    private void searchClientProfile(String name, String email, String  phone, String card) {
        final ClassLoader cl = this.getClassLoader(m_pos);
        Thread.currentThread().setContextClassLoader(cl);
        Boolean equalsName = m_nameRadioEquals.isSelected();
        List<Map<String, String>> partyList = m_trans.searchClientProfile(name, email, phone, card, m_pos, equalsName);
        boolean first = true;
        m_clientListCombo.removeAll();
        m_clientListBidingCombo.clear();
        if (UtilValidate.isNotEmpty(partyList)) {
            for (Map<String, String> party : partyList) {
                name = party.get("lastName");
                email = party.get("infoString");
                phone = party.get("contactNumber");
                String partyId = party.get("partyId");
                m_clientListBidingCombo.add(partyId);
                card = party.get("cardId");
                name = name == null ? "" : name;
                email = email == null ? "" : email;
                phone = phone == null ? "" : phone;
                card = card == null ? "" : card;
                if (first) { // Most of the time the 1st is enough...
                    m_nameEdit.setText(name);
                    m_emailEdit.setText(email);
                    m_phoneEdit.setText(phone);
                    m_cardEdit.setText(card);
                    m_partyId = partyId;
                }
                m_clientListCombo.addItem(name + " | " + email + " | " + phone + " | " + card);

                first = false;
            }
        }
    }

    public void actionPerformed(ActionEvent e) {
        XComboBox clientListCombo = (XComboBox) e.getSource();
        String client = (String) clientListCombo.getSelectedItem();
        if (UtilValidate.isNotEmpty(client)) {
            String[] clientInfos = client.split(" \\| ");
            String name = clientInfos.length > 0 ? clientInfos[0] : "";
            String email = clientInfos.length > 1 ? clientInfos[1] : "";
            String phone = clientInfos.length > 2 ? clientInfos[2] : "";
            String card = clientInfos.length > 3 ? clientInfos[3] : "";
            if (UtilValidate.isNotEmpty(card) && SWIP_WITH_CARD &&  card.startsWith(START_SENTINEL) && card.endsWith(END_SENTINEL)) {
                card = card.substring(1, card.length() - 1);
            }
            m_nameEdit.setText(name);
            m_emailEdit.setText(email);
            m_phoneEdit.setText(phone);
            m_cardEdit.setText(card);
            m_partyId = m_clientListBidingCombo.get(clientListCombo.getSelectedIndex());
        }
    }

    private void editClientProfile(String name, String email,String  phone, String card, String editType, String partyId) {
        final ClassLoader cl = this.getClassLoader(m_pos);
        Thread.currentThread().setContextClassLoader(cl);
        String result = m_trans.editClientProfile(name, email, phone, card, m_pos, editType, partyId);
        if (UtilValidate.isNotEmpty(result)) { // new party ?
            m_partyId = result;
        }
        searchClientProfile(name, email, phone, card); // Only to update the combo
    }

    public synchronized void select() {
        if (wasMouseClicked() && UtilValidate.isNotEmpty(m_partyId)) {
            m_dialog.closeDlg();
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
                Debug.logInfo("No context classloader available; using class classloader", module);
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

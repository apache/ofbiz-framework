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

import java.util.Locale;

import net.xoetrope.swing.XEdit;

import org.ofbiz.base.util.UtilProperties;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.pos.PosTransaction;
import org.ofbiz.pos.screen.PosScreen;

public class PromoStatusBar {
    protected static PosScreen m_pos = null;
    protected XEdit statusbarPromoCode = null;
    protected XEdit statusbarClient = null;
    private String customer = (UtilProperties.getMessage(PosTransaction.resource, "PosCustomer", Locale.getDefault()));
    private String promoCode = (UtilProperties.getMessage(PosTransaction.resource, "PosPromoCode", Locale.getDefault()));


    public PromoStatusBar(PosScreen page) {
        m_pos = page;
        statusbarClient = (XEdit) page.findComponent("statusbarClient");
        statusbarClient.setFocusable(false);
        statusbarPromoCode = (XEdit) page.findComponent("statusbarPromoCode");
        statusbarPromoCode.setFocusable(false);
    }

    public void addPromoCode(String message) {
        String promoCodes = statusbarPromoCode.getText();
        if (UtilValidate.isNotEmpty(promoCodes)) {
            statusbarPromoCode.setText(promoCodes + ", " + message);
        } else {
            statusbarPromoCode.setText(promoCode + " " + message);
        }
    }

    public void displayClient(String message) {
        statusbarClient.setText(customer + " " + message);
    }

    public void clear() {
        if (UtilValidate.isEmpty(statusbarPromoCode.getText())) { // to handle when on another screen
            PosScreen newPos = m_pos.showPage("promopanel");
            PromoStatusBar promoStatusBar = newPos.getPromoStatusBar();
            XEdit statusbarPromoCode = promoStatusBar.getStatusbarPromoCode();
            statusbarPromoCode.setText("");
            XEdit statusbarClient = promoStatusBar.getStatusbarClient();
            statusbarClient.setText("");
            m_pos.showPage("paypanel");
        } else {
            statusbarPromoCode.setText("");
            statusbarClient.setText("");
        }
    }
    
    private XEdit getStatusbarPromoCode() {
        return statusbarPromoCode;
    }
    
    private XEdit getStatusbarClient() {
        return statusbarClient;
    }
}

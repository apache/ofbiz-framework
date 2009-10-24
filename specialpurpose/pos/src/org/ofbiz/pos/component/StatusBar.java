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
import org.ofbiz.pos.PosTransaction;
import org.ofbiz.pos.screen.PosScreen;

public class StatusBar {

    protected XEdit statusbarPromoCode = null;
    protected XEdit statusbarClient = null;
    private String customer = (UtilProperties.getMessage(PosTransaction.resource, "PosCustomer", Locale.getDefault()));
    private String promoCode = (UtilProperties.getMessage(PosTransaction.resource, "PosPromoCode", Locale.getDefault()));


    public StatusBar(PosScreen page) {
        statusbarClient = (XEdit) page.findComponent("statusbarClient");
        statusbarClient.setFocusable(false);
        statusbarPromoCode = (XEdit) page.findComponent("statusbarPromoCode");
        statusbarPromoCode.setFocusable(false);
        clearClient();
        clearPromoCode();
    }

    public void printPromoCode(String message) {
        statusbarPromoCode.setText(promoCode + " " + message);
    }

    public void printClient(String message) {
        statusbarClient.setText(customer + " " + 
                message);
    }

    public void clearPromoCode() {
        statusbarPromoCode.setText("");
    }

    public void clearClient() {
        statusbarClient.setText("");
    }
}

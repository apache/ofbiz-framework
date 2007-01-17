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

import net.xoetrope.swing.XEdit;
import org.ofbiz.pos.screen.PosScreen;
import org.ofbiz.pos.PosTransaction;
import org.ofbiz.base.util.UtilProperties;
import org.ofbiz.guiapp.xui.XuiSession;

import java.util.Locale;

public class Output {

    public static final String module = Output.class.getName();

    // login labels

    private Locale defaultLocale = Locale.getDefault();
    /* Useless since i18n, see pos.properties
    public static final String ULOGIN = "Enter User ID:"; 
    public static final String UPASSW = "Enter Password:";

    // open/close labels
    public static final String OPDRAM = "Starting Drawer Amount:";
    public static final String ENTCAS = "Enter Cash Amount:";
    public static final String ENTCHK = "Enter Check Amount:";
    public static final String ENTCRC = "Enter Credit Card Amount:";
    public static final String ENTGFC = "Enter Gift Card Amount:";
    public static final String ENTOTH = "Enter Other Payment Amount:";

    // complete sale labels
    public static final String PAYFIN = "Press Finish To Complete Sale";
    public static final String TOTALD = "Total Due: ";
    public static final String CHANGE = "Change Due: ";

    // payment (credit/check/gc) labels
    public static final String CREDNO = "Enter Card Number:";
    public static final String CREDEX = "Enter Expiration Date (MMYY):";
    public static final String CREDCF = "Enter Last 4 Digits:";
    public static final String CREDZP = "Enter Billing ZipCode:";
    public static final String REFNUM = "Enter Reference Number:";
    public static final String AUTHCD = "Enter Auth Code:";

    // standard messages
    public static final String ISCLOSED = "Register Is Closed";
    public static final String ISOPEN = "Register Is Open";*/

    protected XuiSession session = null;
    protected XEdit output = null;

    public Output(PosScreen page) {
        this.output = (XEdit) page.findComponent("pos_output");
        this.session = page.getSession();
        this.output.setFocusable(false);
        this.clear();
    }

    public void setLock(boolean lock) {
        if (lock) {
            this.print(UtilProperties.getMessage("pos","ULOGIN",defaultLocale));
        } else {
            if (PosTransaction.getCurrentTx(session).isOpen()) {
                this.print(UtilProperties.getMessage("pos","ISOPEN",defaultLocale));
            } else {
            	this.print(UtilProperties.getMessage("pos","ISCLOSED",defaultLocale));
            }
        }
    }

    public void print(String message) {
        this.output.setText(message);
    }

    public void clear() {
        output.setText("");
    }
}

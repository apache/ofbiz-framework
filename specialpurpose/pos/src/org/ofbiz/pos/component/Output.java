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

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

import net.xoetrope.swing.XPassword;
import org.ofbiz.pos.screen.PosScreen;

public class InputWithPassword extends Input {
    
    protected javax.swing.JTextField savedInput;
    protected XPassword password = null;
    
    public InputWithPassword( PosScreen page) {
        super( page);
        this.savedInput = super.input;
        this.password = (XPassword)page.findComponent("pos_inputpassword");
        if( this.password == null) {
            this.password = new XPassword();
        }
        this.password.setVisible(false);
        this.password.setFocusable(false);
    }
    public void setPasswordInput(boolean isPasswordInput) {
        if( isPasswordInput) {
            this.savedInput.setVisible(false);
            this.password.setText("");
            this.password.setVisible( true);
            super.input = this.password;
        } else {
            this.password.setVisible(false);
            this.savedInput.setVisible( true);
            super.input = this.savedInput;
        }
    }
}

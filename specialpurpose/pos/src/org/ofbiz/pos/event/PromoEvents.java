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
package org.ofbiz.pos.event;

import java.util.Locale;

import org.ofbiz.base.util.UtilProperties;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.pos.PosTransaction;
import org.ofbiz.pos.component.Input;
import org.ofbiz.pos.component.Output;
import org.ofbiz.pos.screen.PosScreen;

public class PromoEvents {

 
    public static final String module = PromoEvents.class.getName();

    public static synchronized void addPromoCode(PosScreen pos) {
        PosTransaction trans = PosTransaction.getCurrentTx(pos.getSession());
        if (!trans.isOpen()) {
            pos.showDialog("dialog/error/terminalclosed");
            return;
        }
        Input input = pos.getInput();
        String[] lastFunc = input.getLastFunction();
        if (lastFunc == null || !"PROMOCODE".equals(lastFunc[0])) {
            Output output = pos.getOutput();
            input.setFunction("PROMOCODE");
            output.print(UtilProperties.getMessage(PosTransaction.resource,"PosEntPromoCode",Locale.getDefault()));
        } else if ("PROMOCODE".equals(lastFunc[0])) {
            String promoCode = input.value();
            if (UtilValidate.isNotEmpty(promoCode)) {
                String result = trans.addProductPromoCode(promoCode, pos);
                if (result != null) {
                    pos.showDialog("dialog/error/exception", result);
                    input.clearFunction("PROMOCODE");
                } else {
                    input.clearFunction("PROMOCODE");
                    NavagationEvents.showPosScreen(pos);
                    pos.refresh();
                }
            }
        }
    }
}

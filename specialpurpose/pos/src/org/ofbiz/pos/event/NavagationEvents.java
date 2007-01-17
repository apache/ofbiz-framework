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

import org.ofbiz.pos.PosTransaction;
import org.ofbiz.pos.screen.PosScreen;

public class NavagationEvents {

    public static void showPosScreen(PosScreen pos) {
        ManagerEvents.mgrLoggedIn = false;
        pos.showPage("pospanel");
        PosScreen.currentScreen.getInput().clear();
    }

    public static void showPayScreen(PosScreen pos) {
        ManagerEvents.mgrLoggedIn = false;
        PosTransaction trans = PosTransaction.getCurrentTx(pos.getSession());
        if (trans.isEmpty()) {
            pos.showDialog("dialog/error/noitems");
        } else {
            PosScreen newPos = pos.showPage("paypanel");
            newPos.getInput().setFunction("TOTAL");
            newPos.refresh();
        }
    }

    public static void showPromoScreen(PosScreen pos) {
        ManagerEvents.mgrLoggedIn = false;
        pos.showPage("promopanel");
        PosScreen.currentScreen.getInput().clear();        
    }
}


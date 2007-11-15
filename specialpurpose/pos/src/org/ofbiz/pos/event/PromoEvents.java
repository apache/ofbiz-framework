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

import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.sql.Timestamp;
import java.util.Locale;
import java.text.DecimalFormat;
import java.text.ParseException;

import net.xoetrope.xui.XProjectManager;

import org.ofbiz.base.util.cache.UtilCache;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilDateTime;
import org.ofbiz.base.util.UtilFormatOut;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.pos.device.DeviceLoader;
import org.ofbiz.pos.device.impl.Receipt;
import org.ofbiz.pos.screen.PosScreen;
import org.ofbiz.pos.screen.PaidInOut;
import org.ofbiz.pos.screen.PromoCode;
import org.ofbiz.pos.PosTransaction;
import org.ofbiz.pos.adaptor.SyncCallbackAdaptor;
import org.ofbiz.pos.component.Input;
import org.ofbiz.pos.component.Output;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.util.EntityListIterator;
import org.ofbiz.entity.condition.EntityExpr;
import org.ofbiz.entity.condition.EntityOperator;
import org.ofbiz.entity.condition.EntityConditionList;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.service.GenericServiceException;
import org.ofbiz.service.ServiceUtil;
import org.ofbiz.guiapp.xui.XuiSession;
import org.ofbiz.base.util.UtilProperties;

public class PromoEvents {

    public static final String module = PromoEvents.class.getName();

    public static void promoCode(PosScreen pos) {
        
        PosTransaction trans = PosTransaction.getCurrentTx(pos.getSession());
        if (!trans.isOpen()) {
            pos.showDialog("dialog/error/terminalclosed");
            return;
        }
        
        PromoCode promoCode = new PromoCode(trans, pos);
        promoCode.openDlg();
        if (promoCode.isPromoLoaded()) {
            NavagationEvents.showPosScreen(pos);
        }
    }    
}

/*
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
 */

import org.ofbiz.entity.*;
import org.ofbiz.base.util.*;

paymentSetup = from("WebSitePaymentSettingView").orderBy("webSiteId", "paymentMethodTypeId").queryList();
context.paymentSetups = paymentSetup;

webSiteId = parameters.webSiteId;
paymentMethodTypeId = parameters.paymentMethodTypeId;

webSitePayment = null;
if (webSiteId && paymentMethodTypeId) {
    webSitePayment = from("WebSitePaymentSettingView").where("webSiteId", webSiteId, "paymentMethodTypeId", paymentMethodTypeId).queryOne();
}
context.webSitePayment = webSitePayment;

webSites = from("WebSite").orderBy("siteName").queryList();
context.webSites = webSites;

paymentMethodTypes = from("PaymentMethodType").orderBy("description").queryList();
context.paymentMethodTypes = paymentMethodTypes;

payInfo = UtilHttp.getParameterMap(request);
if (webSitePayment) {
    payInfo = webSitePayment;
}
context.payInfo = payInfo;

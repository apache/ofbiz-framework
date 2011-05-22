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

import org.ofbiz.webpos.WebPosEvents;
import org.ofbiz.webpos.session.WebPosSession;
import org.ofbiz.webpos.transaction.WebPosTransaction;

webPosSession = WebPosEvents.getWebPosSession(request, null);
if (webPosSession) {
    context.shoppingCartSize = webPosSession.getCart().size();
    context.isManagerLoggedIn = webPosSession.isManagerLoggedIn();
    webPosTransaction = webPosSession.getCurrentTransaction();

    if (webPosTransaction) {
        context.isOpen = webPosTransaction.isOpen();
    }
    context.cart = webPosSession.getCart();
    context.totalDue = webPosSession.getCurrentTransaction().getTotalDue();
    context.totalPayments = webPosSession.getCurrentTransaction().getPaymentTotal();
} else {
    context.shoppingCartSize = 0;
}
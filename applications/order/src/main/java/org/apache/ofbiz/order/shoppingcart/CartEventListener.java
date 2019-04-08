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
package org.apache.ofbiz.order.shoppingcart;

import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.DelegatorFactory;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.transaction.TransactionUtil;
import org.apache.ofbiz.webapp.stats.VisitHandler;

/**
 * HttpSessionListener that saves information about abandoned carts
 */
public class CartEventListener implements HttpSessionListener {

    public static final String module = CartEventListener.class.getName();

    public CartEventListener() {}

    public void sessionCreated(HttpSessionEvent event) {
        //for this one do nothing when the session is created...
    }

    public void sessionDestroyed(HttpSessionEvent event) {
        HttpSession session = event.getSession();
        ShoppingCart cart = (ShoppingCart) session.getAttribute("shoppingCart");
        if (cart == null) {
            Debug.logInfo("No cart to save, doing nothing.", module);
            return;
        }

        String delegatorName = (String) session.getAttribute("delegatorName");
        Delegator delegator = null;
        if (UtilValidate.isNotEmpty(delegatorName)) {
            delegator = DelegatorFactory.getDelegator(delegatorName);
        }
        if (delegator == null) {
            Debug.logError("Could not find delegator with delegatorName in session, not saving abandoned cart info.", module);
            return;
        }

        boolean beganTransaction = false;
        try {
            beganTransaction = TransactionUtil.begin();

            GenericValue visit = VisitHandler.getVisit(session);
            if (visit == null) {
                Debug.logInfo("Could not get the current visit, not saving abandoned cart info.", module);
                return;
            }

            Debug.logInfo("Saving abandoned cart", module);
            int seqId = 1;
            for (ShoppingCartItem cartItem : cart) {
                GenericValue cartAbandonedLine = delegator.makeValue("CartAbandonedLine");

                cartAbandonedLine.set("visitId", visit.get("visitId"));
                cartAbandonedLine.set("cartAbandonedLineSeqId", (Integer.valueOf(seqId)).toString());
                cartAbandonedLine.set("productId", cartItem.getProductId());
                cartAbandonedLine.set("prodCatalogId", cartItem.getProdCatalogId());
                cartAbandonedLine.set("quantity", cartItem.getQuantity());
                cartAbandonedLine.set("reservStart", cartItem.getReservStart());
                cartAbandonedLine.set("reservLength", cartItem.getReservLength());
                cartAbandonedLine.set("reservPersons", cartItem.getReservPersons());
                cartAbandonedLine.set("unitPrice", cartItem.getBasePrice());
                cartAbandonedLine.set("reserv2ndPPPerc", cartItem.getReserv2ndPPPerc());
                cartAbandonedLine.set("reservNthPPPerc", cartItem.getReservNthPPPerc());
                if (cartItem.getConfigWrapper() != null) {
                    cartAbandonedLine.set("configId", cartItem.getConfigWrapper().getConfigId());
                }
                cartAbandonedLine.set("totalWithAdjustments", cartItem.getItemSubTotal());
                //not doing pre-reservations now, so this is always N
                cartAbandonedLine.set("wasReserved", "N");
                cartAbandonedLine.create();

                seqId++;
            }
        } catch (GenericEntityException e) {
            try {
                // only rollback the transaction if we started one...
                TransactionUtil.rollback(beganTransaction, "Error saving abandoned cart info", e);
            } catch (GenericEntityException e2) {
                Debug.logError(e2, "Could not rollback transaction: " + e2.toString(), module);
            }

            Debug.logError(e, "An entity engine error occurred while saving abandoned cart information", module);
        } finally {
            // only commit the transaction if we started one... this will throw an exception if it fails
            try {
                TransactionUtil.commit(beganTransaction);
            } catch (GenericEntityException e) {
                Debug.logError(e, "Could not commit transaction for entity engine error occurred while saving abandoned cart information", module);
            }
        }
    }
}

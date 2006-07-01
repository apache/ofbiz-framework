/*
 * $Id: CartEventListener.java 5462 2005-08-05 18:35:48Z jonesde $
 *
 *  Copyright (c) 2001-2005 The Open For Business Project - www.ofbiz.org
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a
 *  copy of this software and associated documentation files (the "Software"),
 *  to deal in the Software without restriction, including without limitation
 *  the rights to use, copy, modify, merge, publish, distribute, sublicense,
 *  and/or sell copies of the Software, and to permit persons to whom the
 *  Software is furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included
 *  in all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
 *  OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 *  MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 *  IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY
 *  CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT
 *  OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR
 *  THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package org.ofbiz.order.shoppingcart;

import java.util.Iterator;

import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.webapp.stats.VisitHandler;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.transaction.TransactionUtil;

/**
 * HttpSessionListener that saves information about abandoned carts
 *
 * @author     <a href="mailto:jonesde@ofbiz.org">David E. Jones</a>
 * @version    $Rev$
 * @since      2.0
 */
public class CartEventListener implements HttpSessionListener {

    public static final String module = CartEventListener.class.getName();

    public CartEventListener() {}

    public void sessionCreated(HttpSessionEvent event) {
        //for this one do nothing when the session is created...
        //HttpSession session = event.getSession();
    }

    public void sessionDestroyed(HttpSessionEvent event) {
        HttpSession session = event.getSession();        
        ShoppingCart cart = (ShoppingCart) session.getAttribute("shoppingCart");       
        if (cart == null) {
            Debug.logInfo("No cart to save, doing nothing.", module);
            return;
        }
        
        String delegatorName = (String) session.getAttribute("delegatorName");
        GenericDelegator delegator = null;
        if (UtilValidate.isNotEmpty(delegatorName)) {
            delegator = GenericDelegator.getGenericDelegator(delegatorName);
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
                Debug.logError("Could not get the current visit, not saving abandoned cart info.", module);
                return;
            }
            
            Debug.logInfo("Saving abandoned cart", module);
            Iterator cartItems = cart.iterator();
            int seqId = 1;
            while (cartItems.hasNext()) {
                ShoppingCartItem cartItem = (ShoppingCartItem) cartItems.next();
                GenericValue cartAbandonedLine = delegator.makeValue("CartAbandonedLine", null);

                cartAbandonedLine.set("visitId", visit.get("visitId"));
                cartAbandonedLine.set("cartAbandonedLineSeqId", (new Integer(seqId)).toString());
                cartAbandonedLine.set("productId", cartItem.getProductId());
                cartAbandonedLine.set("prodCatalogId", cartItem.getProdCatalogId());
                cartAbandonedLine.set("quantity", new Double(cartItem.getQuantity()));
                cartAbandonedLine.set("reservStart", cartItem.getReservStart());
                cartAbandonedLine.set("reservLength", new Double(cartItem.getReservLength()));
                cartAbandonedLine.set("reservPersons", new Double(cartItem.getReservPersons()));
                cartAbandonedLine.set("unitPrice", new Double(cartItem.getBasePrice()));
                cartAbandonedLine.set("reserv2ndPPPerc", new Double(cartItem.getReserv2ndPPPerc()));
                cartAbandonedLine.set("reservNthPPPerc", new Double(cartItem.getReservNthPPPerc()));
                cartAbandonedLine.set("totalWithAdjustments", new Double(cartItem.getItemSubTotal()));
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

/*
 * $Id: WebShoppingCart.java 7478 2006-05-02 11:15:01Z jonesde $
 *
 *  Copyright (c) 2001, 2002 The Open For Business Project - www.ofbiz.org
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

import java.util.Locale;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.ofbiz.base.util.UtilHttp;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.product.store.ProductStoreWorker;
import org.ofbiz.webapp.website.WebSiteWorker;

/**
 * WebShoppingCart.java
 *
 * Extension of {@link org.ofbiz.order.shoppingcart.ShoppingCart ShoppingCart}
 * class which provides web presentation layer specific functionality
 * related specifically to user session information. 
 *
 * @author     <a href="mailto:jaz@ofbiz.org">Andy Zeneski</a>
 * @author     <a href="mailto:jonesde@ofbiz.org">David E. Jones</a>
 * @author     <a href="mailto:tristana@twibble.org">Tristan Austin</a>
 * @version    $Rev$
 * @since      2.0
 */
public class WebShoppingCart extends ShoppingCart {
    public WebShoppingCart(HttpServletRequest request, Locale locale, String currencyUom) {
        // for purchase orders, bill to customer partyId must be set - otherwise, no way to know who we're purchasing for.  supplierPartyId is furnished 
        // by order manager for PO entry.
        // TODO: refactor constructor and the getCartObject method which calls them to multiple constructors for different types of orders
        super((GenericDelegator)request.getAttribute("delegator"), ProductStoreWorker.getProductStoreId(request),
                WebSiteWorker.getWebSiteId(request), (locale != null ? locale : ProductStoreWorker.getStoreLocale(request)), 
                (currencyUom != null ? currencyUom : ProductStoreWorker.getStoreCurrencyUomId(request)), 
                request.getParameter("billToCustomerPartyId"),
                (request.getParameter("supplierPartyId") != null ? request.getParameter("supplierPartyId") : request.getParameter("billFromVendorPartyId")));

        HttpSession session = request.getSession(true);
        this.userLogin = (GenericValue) session.getAttribute("userLogin");
        this.autoUserLogin = (GenericValue) session.getAttribute("autoUserLogin");
        this.orderPartyId = (String) session.getAttribute("orderPartyId");
    }

    public WebShoppingCart(HttpServletRequest request) {
        this(request, UtilHttp.getLocale(request), UtilHttp.getCurrencyUom(request));        
    }
    
    /** Creates a new cloned ShoppingCart Object. */
    public WebShoppingCart(ShoppingCart cart) {
        super(cart);
    }
}

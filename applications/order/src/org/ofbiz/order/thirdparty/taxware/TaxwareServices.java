/*
 * $Id: TaxwareServices.java 5462 2005-08-05 18:35:48Z jonesde $
 *
 * Copyright (c) 2001, 2002 The Open For Business Project - www.ofbiz.org
 *
 * Permission is hereby granted, free of charge, to any person obtaining a
 * copy of this software and associated documentation files (the "Software"),
 * to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
 * OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY
 * CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT
 * OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR
 * THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package org.ofbiz.order.thirdparty.taxware;


import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.ofbiz.entity.GenericValue;
import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.ModelService;

/**
 * TaxwareServices
 *
 * @author     <a href="mailto:jaz@ofbiz.org">Andy Zeneski</a>
 * @version    $Rev$
 * @since      2.0
 */
public class TaxwareServices {

    public static Map calcTax(DispatchContext dctx, Map context) {
        Map result = new HashMap();
        List items = (List) context.get("itemProductList");
        List amnts = (List) context.get("itemAmountList");
        List ishpn = (List) context.get("itemShippingList");
        Double shipping = (Double) context.get("orderShippingAmount");
        GenericValue address = (GenericValue) context.get("shippingAddress");

        if (items.size() != amnts.size()) {
            result.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_ERROR);
            result.put(ModelService.ERROR_MESSAGE, "ERROR: Items, Amount, or ItemShipping lists are not valid size.");
            return result;
        }

        try {
            TaxwareUTL utl = new TaxwareUTL();

            utl.setShipping(shipping != null ? shipping.doubleValue() : 0.0);
            utl.setShipAddress(address);
            for (int i = 0; i < items.size(); i++) {
                GenericValue p = (GenericValue) items.get(i);
                Double amount = (Double) amnts.get(i);
                Double ishp = ishpn != null ? (Double) ishpn.get(i) : new Double(0.0);

                utl.addItem(p, amount.doubleValue(), ishp.doubleValue());
            }

            int resp = utl.process();

            if (resp == 0) {
                result.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_ERROR);
                result.put(ModelService.ERROR_MESSAGE, "ERROR: No records processed.");
                return result;
            }

            result.put("orderAdjustments", utl.getOrderAdjustments());
            result.put("itemAdjustments", utl.getItemAdjustments());

        } catch (TaxwareException e) {
            e.printStackTrace();
            result.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_ERROR);
            result.put(ModelService.ERROR_MESSAGE, "ERROR: Taxware problem (" + e.getMessage() + ").");
        }

        return result;
    }

    public static Map verifyZip(DispatchContext dctx, Map context) {

        return new HashMap();
    }
}

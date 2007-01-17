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

import java.util.Hashtable;
import java.util.List;
import java.util.ListIterator;
import java.awt.AWTEvent;

import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.GeneralException;
import org.ofbiz.order.shoppingcart.CartItemModifyException;
import org.ofbiz.order.shoppingcart.ItemNotFoundException;
import org.ofbiz.pos.PosTransaction;
import org.ofbiz.pos.config.ButtonEventConfig;
import org.ofbiz.pos.component.Input;
import org.ofbiz.pos.component.Journal;
import org.ofbiz.pos.screen.SelectProduct;
import org.ofbiz.pos.screen.PosScreen;
import org.ofbiz.entity.GenericValue;

public class MenuEvents {

    public static final String module = MenuEvents.class.getName();

    // extended number events
    public static void triggerClear(PosScreen pos) {
        // clear the pieces
        String[] totalFunc = pos.getInput().getFunction("TOTAL");
        String[] paidFunc = pos.getInput().getFunction("PAID");
        if (paidFunc != null) {
            pos.getInput().clear();
            pos.showPage("pospanel");
        } else {
            if (UtilValidate.isEmpty(pos.getInput().value())) {
                pos.getInput().clear();
            }
            if (totalFunc != null) {
                pos.getInput().setFunction("TOTAL", totalFunc[1]);
            }
        }

        // refresh the current screen
        pos.refresh();

        // clear out the manual locks
        if (!pos.isLocked()) {
            pos.getInput().setLock(false);
            pos.getButtons().setLock(false);
        } else {
            // just re-call set lock
            pos.setLock(true);
        }
    }

    public static void triggerQty(PosScreen pos) {
        pos.getInput().setFunction("QTY");
    }

    public static void triggerEnter(PosScreen pos, AWTEvent event) {
        // enter key maps to various different events; depending on the function
        Input input = pos.getInput();
        String[] lastFunc = input.getLastFunction();
        if (lastFunc != null) {
            if ("MGRLOGIN".equals(lastFunc[0])) {
                SecurityEvents.mgrLogin(pos);
            } else if ("LOGIN".equals(lastFunc[0])) {
                SecurityEvents.login(pos);
            } else if ("OPEN".equals(lastFunc[0])) {
                ManagerEvents.openTerminal(pos);
            } else if ("CLOSE".equals(lastFunc[0])) {
                ManagerEvents.closeTerminal(pos);
            } else if ("PAID_IN".equals(lastFunc[0])) {
                ManagerEvents.paidOutAndIn(pos, "IN");
            } else if ("PAID_OUT".equals(lastFunc[0])) {
                ManagerEvents.paidOutAndIn(pos, "OUT");
            } else if ("VOID".equals(lastFunc[0])) {
                ManagerEvents.voidOrder(pos);
            } else if ("REFNUM".equals(lastFunc[0])) {
                PaymentEvents.setRefNum(pos);
            } else if ("CREDIT".equals(lastFunc[0])) {
                PaymentEvents.payCredit(pos);
            } else if ("CHECK".equals(lastFunc[0])) {
                PaymentEvents.payCheck(pos);
            } else if ("GIFTCARD".equals(lastFunc[0])) {
                PaymentEvents.payGiftCard(pos);
            } else if ("MSRINFO".equals(lastFunc[0])) {
                if (input.isFunctionSet("CREDIT")) {
                    PaymentEvents.payCredit(pos);
                } else if (input.isFunctionSet("GIFTCARD")) {
                    PaymentEvents.payGiftCard(pos);
                }
            } else if ("SKU".equals(lastFunc[0])) {
                MenuEvents.addItem(pos, event);
            }
        } else if (input.value().length() > 0) {
            MenuEvents.addItem(pos, event);
        }
    }

    public static void addItem(PosScreen pos, AWTEvent event) {
        PosTransaction trans = PosTransaction.getCurrentTx(pos.getSession());
        Input input = pos.getInput();
        String[] func = input.getFunction("QTY");
        String value = input.value();

        // no value; just return
        if (event != null && UtilValidate.isEmpty(value)) {
            String buttonName = ButtonEventConfig.getButtonName(event);
            if (UtilValidate.isNotEmpty(buttonName)) {
                if (buttonName.startsWith("SKU.")) {
                    value = buttonName.substring(4);
                }
            }
            if (UtilValidate.isEmpty(value)) {
                return;
            }
        }

        if (!trans.isOpen()) {
            pos.showDialog("dialog/error/terminalclosed");
        } else {

            // check for quantity
            double quantity = 1;
            if (func != null && "QTY".equals(func[0])) {
                try {
                    quantity = Double.parseDouble(func[1]);
                } catch (NumberFormatException e) {
                    quantity = 1;
                }
            }

            // locate the product ID
            String productId = null;
            try {
                List items = trans.lookupItem(value);
                if (items != null) {
                    ListIterator it = items.listIterator();
                    if (it.hasNext()) {
                        GenericValue product = (GenericValue) it.next();
                        productId = product.getString("productId");
                        Hashtable productsMap = new Hashtable();
                        productsMap.put(product.getString("productId"), product.get("internalName"));
                        while (it.hasNext()) {
                            product = (GenericValue) it.next();
                            if (!productId.equals(product.getString("productId"))) {
                                productsMap.put(product.getString("productId"), product.get("internalName"));
                            }
                        }
                        if (productsMap.size() > 1 && ButtonEventConfig.getButtonName(event).equals("menuSku")) {
                            SelectProduct SelectProduct = new SelectProduct(productsMap, trans, pos);
                            productId = SelectProduct.openDlg();
                        }
                    }
                }
            } catch (GeneralException e) {
                Debug.logError(e, module);
                pos.showDialog("dialog/error/producterror");
            }

            // add the item to the cart; report any errors to the user
            if (productId != null) {
                try {
                    trans.addItem(productId, quantity);
                } catch (CartItemModifyException e) {
                    Debug.logError(e, module);
                    pos.showDialog("dialog/error/producterror");
                } catch (ItemNotFoundException e) {
                    pos.showDialog("dialog/error/productnotfound");
                }
            } else {
                pos.showDialog("dialog/error/productnotfound");
            }
        }

        // clear the qty flag
        input.clearFunction("QTY");

        // re-calc tax
        trans.calcTax();

        // refresh the others
        pos.refresh();
    }

    public static void changeQty(PosScreen pos) {
        PosTransaction trans = PosTransaction.getCurrentTx(pos.getSession());
        String sku = null;
        try {
            sku = getSelectedItem(pos);
        } catch (ArrayIndexOutOfBoundsException e) {
        }

        if (sku == null) {
            pos.getOutput().print("Invalid Selection!");
            pos.getJournal().refresh(pos);
            pos.getInput().clear();
        }

        Input input = pos.getInput();
        String value = input.value();

        boolean increment = true;
        double quantity = 1;
        if (UtilValidate.isNotEmpty(value)) {
            try {
                quantity = Double.parseDouble(value);
            } catch (NumberFormatException e) {
                quantity = 1;
            }
        } else {
            String[] func = input.getLastFunction();
            if (func != null && "QTY".equals(func[0])) {
                increment = false;
                try {
                    quantity = Double.parseDouble(func[1]);
                } catch (NumberFormatException e) {
                    quantity = trans.getItemQuantity(sku);
                }
            }
        }

        // adjust the quantity
        quantity = (increment ? trans.getItemQuantity(sku) + quantity : quantity);

        try {
            trans.modifyQty(sku, quantity);
        } catch (CartItemModifyException e) {
            Debug.logError(e, module);
            pos.showDialog("dialog/error/producterror");
        }

        // clear the qty flag
        input.clearFunction("QTY");

        // re-calc tax
        trans.calcTax();

        // refresh the others
        pos.refresh();
    }

    public static void saleDiscount(PosScreen pos) {
        PosTransaction trans = PosTransaction.getCurrentTx(pos.getSession());
        if (!trans.isOpen()) {
            pos.showDialog("dialog/error/terminalclosed");
        } else {
            Input input = pos.getInput();
            String value = input.value();
            if (UtilValidate.isNotEmpty(value)) {
                double amount = 0.00;
                boolean percent = false;
                if (value.endsWith("%")) {
                    percent = true;
                    value = value.substring(0, value.length() - 1);
                }
                try {
                    amount = Double.parseDouble(value);
                } catch (NumberFormatException e) {
                }

                amount = (amount / 100) * -1;
                trans.addDiscount(null, amount, percent);
                trans.calcTax();
            }
        }
        pos.refresh();
    }

    public static void itemDiscount(PosScreen pos) {
        PosTransaction trans = PosTransaction.getCurrentTx(pos.getSession());
        if (!trans.isOpen()) {
            pos.showDialog("dialog/error/terminalclosed");
        } else {
            String sku = null;
            try {
                sku = getSelectedItem(pos);
            } catch (ArrayIndexOutOfBoundsException e) {
            }

            if (sku == null) {
                pos.getOutput().print("Invalid Selection!");
                pos.getJournal().refresh(pos);
                pos.getInput().clear();
            }

            Input input = pos.getInput();
            String value = input.value();
            if (UtilValidate.isNotEmpty(value)) {
                double amount = 0.00;
                boolean percent = false;
                if (value.endsWith("%")) {
                    percent = true;
                    value = value.substring(0, value.length() - 1);
                }
                try {
                    amount = Double.parseDouble(value);
                } catch (NumberFormatException e) {
                }

                amount = (amount / 100) * -1;
                trans.addDiscount(sku, amount, percent);
                trans.calcTax();
            }
        }
        pos.refresh();
    }

    public static void clearDiscounts(PosScreen pos) {
        PosTransaction trans = PosTransaction.getCurrentTx(pos.getSession());
        trans.clearDiscounts();
        trans.calcTax();
        pos.refresh();
    }

    public static void calcTotal(PosScreen pos) {
        PosTransaction trans = PosTransaction.getCurrentTx(pos.getSession());
        trans.calcTax();

        pos.getInput().setFunction("TOTAL");
        pos.getJournal().refresh(pos);
    }

    public static void voidItem(PosScreen pos) {
        PosTransaction trans = PosTransaction.getCurrentTx(pos.getSession());
        String sku = null;
        try {
            sku = getSelectedItem(pos);
        } catch (ArrayIndexOutOfBoundsException e) {
        }

        if (sku == null) {
            pos.getOutput().print("Invalid Selection!");
            pos.getJournal().refresh(pos);
            pos.getInput().clear();
        }

        try {
            trans.voidItem(sku);
        } catch (CartItemModifyException e) {
            pos.getOutput().print(e.getMessage());
        }

        // re-calc tax
        trans.calcTax();
        pos.refresh();
    }

    public static void voidAll(PosScreen pos) {
        PosTransaction trans = PosTransaction.getCurrentTx(pos.getSession());
        trans.voidSale();
        pos.refresh();
    }

    public static void saveSale(PosScreen pos) {
        PosTransaction trans = PosTransaction.getCurrentTx(pos.getSession());
        trans.saveSale(pos);
    }

    public static void loadSale(PosScreen pos) {
        PosTransaction trans = PosTransaction.getCurrentTx(pos.getSession());
        trans.loadSale(pos);
    }

    public static String getSelectedItem(PosScreen pos) {
        Journal journal = pos.getJournal();
        return journal.getSelectedSku();
    }
}

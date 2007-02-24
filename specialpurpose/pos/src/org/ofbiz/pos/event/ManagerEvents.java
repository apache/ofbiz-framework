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

public class ManagerEvents {

    public static final String module = ManagerEvents.class.getName();
    public static boolean mgrLoggedIn = false;
    static DecimalFormat priceDecimalFormat = new DecimalFormat("#,##0.00");

    public static void modifyPrice(PosScreen pos) {
        PosTransaction trans = PosTransaction.getCurrentTx(pos.getSession());
        String sku = null;
        try {
            sku = MenuEvents.getSelectedItem(pos);
        } catch (ArrayIndexOutOfBoundsException e) {
        }

        if (sku == null) {
            pos.getOutput().print(UtilProperties.getMessage("pos","Invalid_Selection",Locale.getDefault()));
            pos.getJournal().refresh(pos);
            pos.getInput().clear();
        }

        Input input = pos.getInput();
        String value = input.value();
        if (UtilValidate.isNotEmpty(value)) {
            double price = 0.00;
            boolean parsed = false;
            try {
                price = Double.parseDouble(value);
                parsed = true;
            } catch (NumberFormatException e) {
            }

            if (parsed) {
                price = price / 100;
                trans.modifyPrice(sku, price);

                // re-calc tax
                trans.calcTax();
            }
        }

        // refresh the other components
        pos.refresh();
    }

    public static void openTerminal(PosScreen pos) {
        if (!mgrLoggedIn) {
            pos.showDialog("dialog/error/mgrnotloggedin");
            return;
        }

        PosTransaction trans = PosTransaction.getCurrentTx(pos.getSession());
        Input input = pos.getInput();
        if (!trans.isOpen()) {
            if (input.isFunctionSet("OPEN")) {
                String amountStr = input.value();
                if (UtilValidate.isNotEmpty(amountStr))
                {
                    try {
                        double amt = Double.parseDouble(amountStr);
                        amt = amt / 100;
                        amountStr = UtilFormatOut.formatPrice(amt);
                    } catch (NumberFormatException e)
                    {
                        Debug.logError(e, module);
                    }
                }
                GenericValue state = pos.getSession().getDelegator().makeValue("PosTerminalState", null);
                state.set("posTerminalId", pos.getSession().getId());
                state.set("openedDate", UtilDateTime.nowTimestamp());
                state.set("openedByUserLoginId", pos.getSession().getUserId());
                state.set("startingTxId", trans.getTransactionId());
                try
                {
                    state.set("startingDrawerAmount", new Double(priceDecimalFormat.parse(amountStr).doubleValue()));
                }
                catch (ParseException pe)
                {
                    Debug.logError(pe, module);
                }
                try {
                    state.create();
                } catch (GenericEntityException e) {
                    Debug.logError(e, module);
                    pos.showDialog("dialog/error/exception", e.getMessage());
                }
                NavagationEvents.showPosScreen(pos);
            } else {
                input.clear();
                input.setFunction("OPEN");
                pos.getOutput().print(UtilProperties.getMessage("pos","OPDRAM",Locale.getDefault()));
                return;
            }
        } else {
            pos.showPage("pospanel");
        }
    }

    public static void closeTerminal(PosScreen pos) {
        if (!mgrLoggedIn) {
            pos.showDialog("dialog/error/mgrnotloggedin");
            return;
        }

        PosTransaction trans = PosTransaction.getCurrentTx(pos.getSession());
        if (!trans.isOpen()) {
            pos.showDialog("dialog/error/terminalclosed");
            return;
        }

        Output output = pos.getOutput();
        Input input = pos.getInput();
        if (input.isFunctionSet("CLOSE")) {
            String[] func = input.getFunction("CLOSE");
            String lastValue = input.value();
            if (UtilValidate.isNotEmpty(lastValue)) {
                try {
                    double dbl = Double.parseDouble(lastValue);
                    dbl = dbl / 100;
                    lastValue = UtilFormatOut.formatPrice(dbl);
                } catch (NumberFormatException e) {
                    Debug.logError(e, module);
                }
                if (UtilValidate.isNotEmpty(func[1])) {
                    func[1] = func[1] + "|";
                }
                func[1] = func[1] + lastValue;
                input.setFunction("CLOSE", func[1]);
            }

            String[] closeInfo = new String[0];
            if (UtilValidate.isNotEmpty(func[1])) {
                closeInfo = func[1].split("\\|");
            }
            switch (closeInfo.length) {
                case 0:
                    output.print(UtilProperties.getMessage("pos","ENTCAS",Locale.getDefault()));
                    break;
                case 1:
                    output.print(UtilProperties.getMessage("pos","ENTCHK",Locale.getDefault()));
                    break;
                case 2:
                    output.print(UtilProperties.getMessage("pos","ENTCRC",Locale.getDefault()));
                    break;
                case 3:
                    output.print(UtilProperties.getMessage("pos","ENTGFC",Locale.getDefault()));
                    break;
                case 4:
                    output.print(UtilProperties.getMessage("pos","ENTOTH",Locale.getDefault()));
                    break;
                case 5:
                    GenericValue state = trans.getTerminalState();
                    state.set("closedDate", UtilDateTime.nowTimestamp());
                    state.set("closedByUserLoginId", pos.getSession().getUserId());
                    try
                    {
                        state.set("actualEndingCash", new Double(priceDecimalFormat.parse(closeInfo[0]).doubleValue()));
                        state.set("actualEndingCheck", new Double(priceDecimalFormat.parse(closeInfo[1]).doubleValue()));
                        state.set("actualEndingCc", new Double(priceDecimalFormat.parse(closeInfo[2]).doubleValue()));
                        state.set("actualEndingGc", new Double(priceDecimalFormat.parse(closeInfo[3]).doubleValue()));
                        state.set("actualEndingOther", new Double(priceDecimalFormat.parse(closeInfo[4]).doubleValue()));
                    }
                    catch (ParseException pe)
                    {
                        Debug.logError(pe, module);
                    }
                    state.set("endingTxId", trans.getTransactionId());
                    Debug.log("Updated State - " + state, module);
                    try {
                        state.store();
                        state.refresh();
                    } catch (GenericEntityException e) {
                        Debug.logError(e, module);
                        pos.showDialog("dialog/error/exception", e.getMessage());
                    }

                    // print the totals report
                    output.print(UtilProperties.getMessage("pos","WaitingFinalSales",Locale.getDefault()));
                    //pos.showDialog("dialog/error/terminalclosed"); JLR 14/11/06 : Pb with that don't know why, useless => commented out
                    printTotals(pos, state, true);

                    // lock the terminal for the moment
                    pos.getInput().setLock(true);
                    pos.getButtons().setLock(true);
                    pos.refresh(false);

                    // transmit final data to server
                    GenericValue terminal = null;
                    try {
                        terminal = state.getRelatedOne("PosTerminal");
                    } catch (GenericEntityException e) {
                        Debug.logError(e, module);
                        pos.showDialog("dialog/error/exception", e.getMessage());
                    }
                    if (terminal != null && terminal.get("pushEntitySyncId") != null) {
                        String syncId = terminal.getString("pushEntitySyncId");
                        SyncCallbackAdaptor cb = new SyncCallbackAdaptor(pos, syncId, state.getTimestamp("lastUpdatedTxStamp"));
                        pos.getSession().getDispatcher().registerCallback("runEntitySync", cb);
                    } else {
                        // no sync setting; just logout
                        SecurityEvents.logout(pos);
                    }
                    // unlock the terminal
                    pos.getInput().setLock(false);
                    pos.getButtons().setLock(false);
                    pos.refresh(true);

            }
        } else {
            trans.popDrawer();
            input.clear();
            input.setFunction("CLOSE");
            output.print(UtilProperties.getMessage("pos","ENTCAS",Locale.getDefault()));
        }
    }

    public static void voidOrder(PosScreen pos) {
        if (!mgrLoggedIn) {
            pos.showDialog("dialog/error/mgrnotloggedin");
            return;
        }

        XuiSession session = pos.getSession();
        PosTransaction trans = PosTransaction.getCurrentTx(session);
        if (!trans.isOpen()) {
            pos.showDialog("dialog/error/terminalclosed");
            return;
        }

        Output output = pos.getOutput();
        Input input = pos.getInput();
        boolean lookup = false;

        if (input.isFunctionSet("VOID")) {
            lookup = true;
        } else if (UtilValidate.isNotEmpty(input.value())) {
            lookup = true;
        }

        if (lookup) {
            GenericValue state = trans.getTerminalState();
            Timestamp openDate = state.getTimestamp("openedDate");

            String orderId = input.value();
            GenericValue orderHeader = null;
            try {
                orderHeader = session.getDelegator().findByPrimaryKey("OrderHeader", UtilMisc.toMap("orderId", orderId));
            } catch (GenericEntityException e) {
                Debug.logError(e, module);
            }
            if (orderHeader == null) {
                input.clear();
                pos.showDialog("dialog/error/ordernotfound");
                return;
            } else {
                Timestamp orderDate = orderHeader.getTimestamp("orderDate");
                if (orderDate.after(openDate)) {
                    LocalDispatcher dispatcher = session.getDispatcher();
                    Map returnResp = null;
                    try {
                        returnResp = dispatcher.runSync("quickReturnOrder", UtilMisc.toMap("orderId", orderId,
                                                        "returnHeaderTypeId", "CUSTOMER_RETURN", "userLogin", session.getUserLogin()));
                    } catch (GenericServiceException e) {
                        Debug.logError(e, module);
                        pos.showDialog("dialog/error/exception", e.getMessage());
                        pos.refresh();
                        return;
                    }
                    if (returnResp != null && ServiceUtil.isError(returnResp)) {
                        pos.showDialog("dialog/error/exception", ServiceUtil.getErrorMessage(returnResp));
                        pos.refresh();
                        return;
                    }

                    // todo print void receipt

                    trans.setTxAsReturn((String) returnResp.get("returnId"));
                    input.clear();
                    pos.showDialog("dialog/error/salevoided");
                    pos.refresh();
                } else {
                    input.clear();
                    pos.showDialog("dialog/error/ordernotfound");
                    return;
                }
            }
        } else {
            input.setFunction("VOID");
            output.print(UtilProperties.getMessage("pos","VOID",Locale.getDefault()));
        }
    }

    public static void reprintLastTx(PosScreen pos) {
        if (!mgrLoggedIn) {
            pos.showDialog("dialog/error/mgrnotloggedin");
            return;
        }
        DeviceLoader.receipt.reprintReceipt(true);
        pos.refresh();
    }

    public static void popDrawer(PosScreen pos) {
        if (!mgrLoggedIn) {
            pos.showDialog("dialog/error/mgrnotloggedin");
        } else {
            PosTransaction trans = PosTransaction.getCurrentTx(pos.getSession());
            trans.popDrawer();
            pos.refresh();
        }
    }

    public static void clearCache(PosScreen pos) {
        if (!mgrLoggedIn) {
            pos.showDialog("dialog/error/mgrnotloggedin");
        } else {
            UtilCache.clearAllCaches();
            pos.refresh();
        }
    }

    public static void resetXui(PosScreen pos) {
        if (!mgrLoggedIn) {
            pos.showDialog("dialog/error/mgrnotloggedin");
        } else {
            XProjectManager.getPageManager().reset();
            pos.refresh();
        }
    }

    public static void shutdown(PosScreen pos) {
        if (!mgrLoggedIn) {
            pos.showDialog("dialog/error/mgrnotloggedin");
        } else {
            pos.getOutput().print(UtilProperties.getMessage("pos","Shutting_down",Locale.getDefault()));
            PosTransaction.getCurrentTx(pos.getSession()).closeTx();
            System.exit(0);
        }
    }

    public static void totalsReport(PosScreen pos) {
        if (!mgrLoggedIn) {
            pos.showDialog("dialog/error/mgrnotloggedin");
            return;
        }
        printTotals(pos, null, false);
    }

    public static void paidOut(PosScreen pos) {
        paidOutAndIn(pos, "OUT");        
    }

    public static void paidIn(PosScreen pos) {
        paidOutAndIn(pos, "IN");        
    }
                    
    public static void paidOutAndIn(PosScreen pos, String type) {
        if (!mgrLoggedIn) {
            pos.showDialog("dialog/error/mgrnotloggedin");
            return;
        }
        
        PosTransaction trans = PosTransaction.getCurrentTx(pos.getSession());
        if (!trans.isOpen()) {
            pos.showDialog("dialog/error/terminalclosed");
            return;
        }
        
        PaidInOut PaidInOut = new PaidInOut(trans, pos, type);
        Map mapInOut = PaidInOut.openDlg();
        if (null != mapInOut.get("amount")) {
            String amount = (String) mapInOut.get("amount");
            try {
                double dbl = Double.parseDouble(amount);
            } catch (NumberFormatException e) {
                Debug.logError(e, module);
                return;
            }

            GenericValue internTx = pos.getSession().getDelegator().makeValue("PosTerminalInternTx", null);
            internTx.set("posTerminalLogId", trans.getTerminalLogId());                        
            try
            {
                internTx.set("paidAmount", new Double(priceDecimalFormat.parse(amount).doubleValue() / 100));
            }
            catch (ParseException pe)
            {
                Debug.logError(pe, module);
                return;
            }
            internTx.set("reasonComment", mapInOut.get("reason"));
            try {
                internTx.create();
            } catch (GenericEntityException e) {
                Debug.logError(e, module);
                pos.showDialog("dialog/error/exception", e.getMessage());
                return;
            }                        
            //save the TX Log 
            trans.paidInOut(type);               
            NavagationEvents.showPosScreen(pos);                                    
        }    
    }
    
    private static void printTotals(PosScreen pos, GenericValue state, boolean runBalance) {
        PosTransaction trans = PosTransaction.getCurrentTx(pos.getSession());
        if (!trans.isOpen()) {
            pos.showDialog("dialog/error/terminalclosed");
            return;
        }
        if (state == null) {
            state = trans.getTerminalState();
        }

        double checkTotal = 0.00;
        double cashTotal = 0.00;
        double gcTotal = 0.00;
        double ccTotal = 0.00;
        double othTotal = 0.00;
        double total = 0.00;

        GenericDelegator delegator = pos.getSession().getDelegator();
        List exprs = UtilMisc.toList(new EntityExpr("originFacilityId", EntityOperator.EQUALS, trans.getFacilityId()),
                new EntityExpr("terminalId", EntityOperator.EQUALS, trans.getTerminalId()));
        EntityListIterator eli = null;

        try {
            eli = delegator.findListIteratorByCondition("OrderHeaderAndPaymentPref", new EntityConditionList(exprs, EntityOperator.AND), null, null);
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
        }

        Timestamp dayStart = state.getTimestamp("openedDate");
        Timestamp dayEnd = state.getTimestamp("closedDate");
        if (dayEnd == null) {
            dayEnd = UtilDateTime.nowTimestamp();
        }

        if (eli != null) {
            GenericValue ohpp;
            while (((ohpp = (GenericValue) eli.next()) != null)) {
                Timestamp orderDate = ohpp.getTimestamp("orderDate");
                if (orderDate.after(dayStart) && orderDate.before(dayEnd)) {
                    String pmt = ohpp.getString("paymentMethodTypeId");
                    Double amt = ohpp.getDouble("maxAmount");

                    if ("CASH".equals(pmt)) {
                        cashTotal += amt.doubleValue();
                    } else  if ("CHECK".equals(pmt)) {
                        checkTotal += amt.doubleValue();
                    } else if ("GIFT_CARD".equals(pmt)) {
                        gcTotal += amt.doubleValue();
                    } else if ("CREDIT_CARD".equals(pmt)) {
                        ccTotal += amt.doubleValue();
                    } else {
                        othTotal += amt.doubleValue();
                    }
                    total += amt.doubleValue();
                }
            }

            try {
                eli.close();
            } catch (GenericEntityException e) {
                Debug.logWarning(e, "Trouble closing ELI", module);
            }
        }

        Map reportMap = new HashMap();
        String reportTemplate = "totals.txt";

        // miscellaneous
        reportMap.put("term", UtilFormatOut.padString(UtilProperties.getMessage("pos","term",Locale.getDefault()), 20, false, ' '));
        reportMap.put("draw", UtilFormatOut.padString(UtilProperties.getMessage("pos","draw",Locale.getDefault()), 20, false, ' '));
        reportMap.put("clerk", UtilFormatOut.padString(UtilProperties.getMessage("pos","clerk",Locale.getDefault()), 20, false, ' '));
        reportMap.put("total_report", UtilFormatOut.padString(UtilProperties.getMessage("pos","total_report",Locale.getDefault()), 20, false, ' '));

        // titles
        reportMap.put("cashTitle", UtilFormatOut.padString(UtilProperties.getMessage("pos","CASH",Locale.getDefault()), 20, false, ' '));
        reportMap.put("checkTitle", UtilFormatOut.padString(UtilProperties.getMessage("pos","CHECK",Locale.getDefault()), 20, false, ' '));
        reportMap.put("giftCardTitle", UtilFormatOut.padString(UtilProperties.getMessage("pos","GIFT_CARD",Locale.getDefault()), 20, false, ' '));
        reportMap.put("creditCardTitle", UtilFormatOut.padString(UtilProperties.getMessage("pos","CREDIT_CARD",Locale.getDefault()), 20, false, ' '));
        reportMap.put("otherTitle", UtilFormatOut.padString(UtilProperties.getMessage("pos","OTHER",Locale.getDefault()), 20, false, ' '));
        reportMap.put("grossSalesTitle", UtilFormatOut.padString(UtilProperties.getMessage("pos","GROSS_SALES",Locale.getDefault()), 20, false, ' '));
        reportMap.put("+/-", UtilFormatOut.padString("+/-", 20, false, ' '));
        reportMap.put("spacer", UtilFormatOut.padString("", 20, false, ' '));

        // logged
        reportMap.put("cashTotal", UtilFormatOut.padString(UtilFormatOut.formatPrice(cashTotal), 8, false, ' '));
        reportMap.put("checkTotal", UtilFormatOut.padString(UtilFormatOut.formatPrice(checkTotal), 8, false, ' '));
        reportMap.put("ccTotal", UtilFormatOut.padString(UtilFormatOut.formatPrice(ccTotal), 8, false, ' '));
        reportMap.put("gcTotal", UtilFormatOut.padString(UtilFormatOut.formatPrice(gcTotal), 8, false, ' '));
        reportMap.put("otherTotal", UtilFormatOut.padString(UtilFormatOut.formatPrice(othTotal), 8, false, ' '));
        reportMap.put("grossTotal", UtilFormatOut.padString(UtilFormatOut.formatPrice(total), 8, false, ' '));

        if (runBalance) {
            // actuals
            double cashEnd = state.getDouble("actualEndingCash").doubleValue();
            double checkEnd = state.getDouble("actualEndingCheck").doubleValue();
            double ccEnd = state.getDouble("actualEndingCc").doubleValue();
            double gcEnd = state.getDouble("actualEndingGc").doubleValue();
            double othEnd = state.getDouble("actualEndingOther").doubleValue();
            double grossEnd = cashEnd + checkEnd + ccEnd + gcEnd + othEnd;

            reportMap.put("cashEnd", UtilFormatOut.padString(UtilFormatOut.formatPrice(cashEnd), 8, false, ' '));
            reportMap.put("checkEnd", UtilFormatOut.padString(UtilFormatOut.formatPrice(checkEnd), 8, false, ' '));
            reportMap.put("ccEnd", UtilFormatOut.padString(UtilFormatOut.formatPrice(ccEnd), 8, false, ' '));
            reportMap.put("gcEnd", UtilFormatOut.padString(UtilFormatOut.formatPrice(gcEnd), 8, false, ' '));
            reportMap.put("otherEnd", UtilFormatOut.padString(UtilFormatOut.formatPrice(othEnd), 8, false, ' '));
            reportMap.put("grossEnd", UtilFormatOut.padString(UtilFormatOut.formatPrice(grossEnd), 8, false, ' '));

            // diffs
            double cashDiff = cashEnd - cashTotal;
            double checkDiff = checkEnd - checkTotal;
            double ccDiff = ccEnd - ccTotal;
            double gcDiff = gcEnd - gcTotal;
            double othDiff = othEnd - othTotal;
            double grossDiff = cashDiff + checkDiff + ccDiff + gcDiff + othDiff;

            reportMap.put("cashDiff", UtilFormatOut.padString(UtilFormatOut.formatPrice(cashDiff), 8, false, ' '));
            reportMap.put("checkDiff", UtilFormatOut.padString(UtilFormatOut.formatPrice(checkDiff), 8, false, ' '));
            reportMap.put("ccDiff", UtilFormatOut.padString(UtilFormatOut.formatPrice(ccDiff), 8, false, ' '));
            reportMap.put("gcDiff", UtilFormatOut.padString(UtilFormatOut.formatPrice(gcDiff), 8, false, ' '));
            reportMap.put("otherDiff", UtilFormatOut.padString(UtilFormatOut.formatPrice(othDiff), 8, false, ' '));
            reportMap.put("grossDiff", UtilFormatOut.padString(UtilFormatOut.formatPrice(grossDiff), 8, false, ' '));

            // set the report template
            reportTemplate = "balance.txt";
        }

        Receipt receipt = DeviceLoader.receipt;
        if (receipt.isEnabled()) {
            receipt.printReport(trans, reportTemplate, reportMap);
        }
    }
}

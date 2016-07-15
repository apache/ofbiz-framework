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
package org.ofbiz.pos.event;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import net.xoetrope.xui.XProjectManager;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilDateTime;
import org.ofbiz.base.util.UtilFormatOut;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilNumber;
import org.ofbiz.base.util.UtilProperties;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.base.util.cache.UtilCache;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.transaction.GenericTransactionException;
import org.ofbiz.entity.transaction.TransactionUtil;
import org.ofbiz.entity.util.EntityListIterator;
import org.ofbiz.entity.util.EntityQuery;
import org.ofbiz.guiapp.xui.XuiSession;
import org.ofbiz.pos.PosTransaction;
import org.ofbiz.pos.adaptor.SyncCallbackAdaptor;
import org.ofbiz.pos.component.Input;
import org.ofbiz.pos.component.Output;
import org.ofbiz.pos.device.DeviceLoader;
import org.ofbiz.pos.device.impl.Receipt;
import org.ofbiz.pos.screen.PaidInOut;
import org.ofbiz.pos.screen.PosScreen;
import org.ofbiz.service.GenericServiceException;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.service.ServiceUtil;

public class ManagerEvents {


    public static final String module = ManagerEvents.class.getName();
    public static boolean mgrLoggedIn = false;
    static DecimalFormat priceDecimalFormat = new DecimalFormat("#,##0.00");

    // scales and rounding modes for BigDecimal math
    public static final int scale = UtilNumber.getBigDecimalScale("order.decimals");
    public static final int rounding = UtilNumber.getBigDecimalRoundingMode("order.rounding");
    public static final BigDecimal ZERO = (BigDecimal.ZERO).setScale(scale, rounding);

    public static synchronized void modifyPrice(PosScreen pos) {
        PosTransaction trans = PosTransaction.getCurrentTx(pos.getSession());
        String cartIndex = null;
        try {
            cartIndex = MenuEvents.getSelectedIdx(pos);
        } catch (ArrayIndexOutOfBoundsException e) {
        }

        if (cartIndex == null) {
            pos.getOutput().print(UtilProperties.getMessage(PosTransaction.resource,"PosInvalidSelection",Locale.getDefault()));
            pos.getJournal().refresh(pos);
            pos.getInput().clear();
        }

        Input input = pos.getInput();
        String value = input.value();
        if (UtilValidate.isNotEmpty(value)) {
            BigDecimal price = ZERO;
            boolean parsed = false;
            try {
                price = new BigDecimal(value);
                parsed = true;
            } catch (NumberFormatException e) {
            }

            if (parsed) {
                price = price.movePointLeft(2);
                trans.modifyPrice(cartIndex, price);

                // re-calc tax
                trans.calcTax();
            }
        }

        // refresh the other components
        pos.refresh();
    }

    public static synchronized void openTerminal(PosScreen pos) {
        if (!mgrLoggedIn) {
            pos.showDialog("dialog/error/mgrnotloggedin");
            return;
        }

        PosTransaction trans = PosTransaction.getCurrentTx(pos.getSession());
        Input input = pos.getInput();
        if (!trans.isOpen()) {
            if (input.isFunctionSet("OPEN")) {
                BigDecimal amt = ZERO;
                String amountStr = input.value();
                if (UtilValidate.isNotEmpty(amountStr)) {
                    try {
                        amt = new BigDecimal(amountStr);
                        amt = amt.movePointLeft(2);
                    } catch (NumberFormatException e)
                    {
                        Debug.logError(e, module);
                    }
                }
                GenericValue state = pos.getSession().getDelegator().makeValue("PosTerminalState");
                state.set("posTerminalId", pos.getSession().getId());
                state.set("openedDate", UtilDateTime.nowTimestamp());
                state.set("openedByUserLoginId", pos.getSession().getUserId());
                state.set("startingTxId", trans.getTransactionId());
                state.set("startingDrawerAmount", amt);
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
                pos.getOutput().print(UtilProperties.getMessage(PosTransaction.resource,"PosOpDrAm",Locale.getDefault()));
                return;
            }
        } else {
            pos.showPage("pospanel");
        }
    }

    public static synchronized void closeTerminal(PosScreen pos) {
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
                    BigDecimal amt = new BigDecimal(lastValue);
                    amt = amt.movePointLeft(2);
                    lastValue = amt.toString();
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
                    output.print(UtilProperties.getMessage(PosTransaction.resource,"PosEntCas",Locale.getDefault()));
                    break;
                case 1:
                    output.print(UtilProperties.getMessage(PosTransaction.resource,"PosEntChk",Locale.getDefault()));
                    break;
                case 2:
                    output.print(UtilProperties.getMessage(PosTransaction.resource,"PosEntCrc",Locale.getDefault()));
                    break;
                case 3:
                    output.print(UtilProperties.getMessage(PosTransaction.resource,"PosEntGfc",Locale.getDefault()));
                    break;
                case 4:
                    output.print(UtilProperties.getMessage(PosTransaction.resource,"PosEntOth",Locale.getDefault()));
                    break;
                case 5:
                    GenericValue state = trans.getTerminalState();
                    state.set("closedDate", UtilDateTime.nowTimestamp());
                    state.set("closedByUserLoginId", pos.getSession().getUserId());
                    state.set("actualEndingCash", new BigDecimal(closeInfo[0]));
                    state.set("actualEndingCheck", new BigDecimal(closeInfo[1]));
                    state.set("actualEndingCc", new BigDecimal(closeInfo[2]));
                    state.set("actualEndingGc", new BigDecimal(closeInfo[3]));
                    state.set("actualEndingOther", new BigDecimal(closeInfo[4]));
                    state.set("endingTxId", trans.getTransactionId());
                    Debug.logInfo("Updated State - " + state, module);
                    try {
                        state.store();
                        state.refresh();
                    } catch (GenericEntityException e) {
                        Debug.logError(e, module);
                        pos.showDialog("dialog/error/exception", e.getMessage());
                    }

                    // print the totals report
                    output.print(UtilProperties.getMessage(PosTransaction.resource,"PosWaitingFinalSales",Locale.getDefault()));
                    //pos.showDialog("dialog/error/terminalclosed"); JLR 14/11/06 : Pb with that don't know why, useless => commented out
                    printTotals(pos, state, true);

                    // lock the terminal for the moment
                    pos.getInput().setLock(true);
                    pos.getButtons().setLock(true);
                    pos.refresh(false);

                    // transmit final data to server
                    GenericValue terminal = null;
                    try {
                        terminal = state.getRelatedOne("PosTerminal", false);
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
            output.print(UtilProperties.getMessage(PosTransaction.resource,"PosEntCas",Locale.getDefault()));
        }
    }

    public static synchronized void voidOrder(PosScreen pos) {
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
                orderHeader = session.getDelegator().findOne("OrderHeader", UtilMisc.toMap("orderId", orderId), false);
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
                    Map<String, Object> returnResp = null;
                    try {
                        returnResp = dispatcher.runSync("quickReturnOrder", UtilMisc.<String, Object>toMap("orderId", orderId,
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
            output.print(UtilProperties.getMessage(PosTransaction.resource,"PosVoid",Locale.getDefault()));
        }
    }

    public static synchronized void reprintLastTx(PosScreen pos) {
        if (!mgrLoggedIn) {
            pos.showDialog("dialog/error/mgrnotloggedin");
            return;
        }
        DeviceLoader.receipt.reprintReceipt(true);
        pos.refresh();
    }

    public static synchronized void popDrawer(PosScreen pos) {
        if (!mgrLoggedIn) {
            pos.showDialog("dialog/error/mgrnotloggedin");
        } else {
            PosTransaction trans = PosTransaction.getCurrentTx(pos.getSession());
            trans.popDrawer();
            pos.refresh();
        }
    }

    public static synchronized void clearCache(PosScreen pos) {
        if (!mgrLoggedIn) {
            pos.showDialog("dialog/error/mgrnotloggedin");
        } else {
            UtilCache.clearAllCaches();
            pos.refresh();
        }
    }

    public static synchronized void resetXui(PosScreen pos) {
        if (!mgrLoggedIn) {
            pos.showDialog("dialog/error/mgrnotloggedin");
        } else {
            XProjectManager.getCurrentProject().getPageManager().reset();
            pos.refresh();
        }
    }

    public static synchronized void SwapKeyboard(PosScreen pos) {
        if (!mgrLoggedIn) {
            pos.showDialog("dialog/error/mgrnotloggedin");
        } else {
            String showKeyboardInSaveSale = null;
            showKeyboardInSaveSale = UtilProperties.getPropertyValue("parameters", "ShowKeyboardInSaveSale");
            if ("N".equalsIgnoreCase(showKeyboardInSaveSale)) {
                UtilProperties.setPropertyValueInMemory("parameters", "ShowKeyboardInSaveSale", "Y");
            } else {
                UtilProperties.setPropertyValueInMemory("parameters", "ShowKeyboardInSaveSale", "N");
            }
        }
    }



    public static synchronized void shutdown(PosScreen pos) {
        if (!mgrLoggedIn) {
            pos.showDialog("dialog/error/mgrnotloggedin");
        } else {
            pos.getOutput().print(UtilProperties.getMessage(PosTransaction.resource,"PosShuttingDown",Locale.getDefault()));
            PosTransaction.getCurrentTx(pos.getSession()).closeTx();
            System.exit(0);
        }
    }

    public static synchronized void totalsReport(PosScreen pos) {
        if (!mgrLoggedIn) {
            pos.showDialog("dialog/error/mgrnotloggedin");
            return;
        }
        printTotals(pos, null, false);
    }

    public static synchronized void paidOut(PosScreen pos) {
        paidOutAndIn(pos, "OUT");
    }

    public static synchronized void paidIn(PosScreen pos) {
        paidOutAndIn(pos, "IN");
    }

    public static synchronized void paidOutAndIn(PosScreen pos, String type) {
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
        Map<String, String> mapInOut = PaidInOut.openDlg();
        if (null != mapInOut.get("amount")) {
            String amount = mapInOut.get("amount");
            BigDecimal amt = ZERO;
            try {
                amt = new BigDecimal(amount);
                amt = amt.movePointLeft(2);
            } catch (NumberFormatException e) {
                Debug.logError(e, module);
                return;
            }

            GenericValue internTx = pos.getSession().getDelegator().makeValue("PosTerminalInternTx");
            internTx.set("posTerminalLogId", trans.getTerminalLogId());
            internTx.set("paidAmount", amt);
            internTx.set("reasonComment", mapInOut.get("reasonComment"));
            internTx.set("reasonEnumId", mapInOut.get("reason"));
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

    private static synchronized void printTotals(PosScreen pos, GenericValue state, boolean runBalance) {
        PosTransaction trans = PosTransaction.getCurrentTx(pos.getSession());
        if (!trans.isOpen()) {
            pos.showDialog("dialog/error/terminalclosed");
            return;
        }
        if (state == null) {
            state = trans.getTerminalState();
        }

        BigDecimal checkTotal = ZERO;
        BigDecimal cashTotal = ZERO;
        BigDecimal gcTotal = ZERO;
        BigDecimal ccTotal = ZERO;
        BigDecimal othTotal = ZERO;
        BigDecimal total = ZERO;

        boolean beganTransaction = false;
        try {
            beganTransaction = TransactionUtil.begin();

            Delegator delegator = pos.getSession().getDelegator();
            EntityListIterator eli = null;

            try {
                eli = EntityQuery.use(delegator).from("OrderHeaderAndPaymentPref").where("originFacilityId", trans.getFacilityId(), "terminalId", trans.getTerminalId()).queryIterator();
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
                while (((ohpp = eli.next()) != null)) {
                    Timestamp orderDate = ohpp.getTimestamp("orderDate");
                    if (orderDate.after(dayStart) && orderDate.before(dayEnd)) {
                        String pmt = ohpp.getString("paymentMethodTypeId");
                        BigDecimal amt = ohpp.getBigDecimal("maxAmount");

                        if ("CASH".equals(pmt)) {
                            cashTotal = cashTotal.add(amt);
                        } else  if ("PERSONAL_CHECK".equals(pmt)) {
                            checkTotal = checkTotal.add(amt);
                        } else if ("GIFT_CARD".equals(pmt)) {
                            gcTotal = gcTotal.add(amt);
                        } else if ("CREDIT_CARD".equals(pmt)) {
                            ccTotal = ccTotal.add(amt);
                        } else {
                            othTotal = othTotal.add(amt);
                        }
                        total = total.add(amt);
                    }
                }

                try {
                    eli.close();
                } catch (GenericEntityException e) {
                    Debug.logWarning(e, "Trouble closing ELI", module);
                    pos.showDialog("dialog/error/exception", e.getMessage());
                }
            }
        } catch (GenericTransactionException e) {
            Debug.logError(e, module);
            try {
                TransactionUtil.rollback(beganTransaction, e.getMessage(), e);
            } catch (GenericTransactionException e2) {
                Debug.logError(e2, "Unable to rollback transaction", module);
                pos.showDialog("dialog/error/exception", e2.getMessage());
            }
            pos.showDialog("dialog/error/exception", e.getMessage());
        } finally {
            try {
                TransactionUtil.commit(beganTransaction);
            } catch (GenericTransactionException e) {
                Debug.logError(e, "Unable to commit transaction", module);
                pos.showDialog("dialog/error/exception", e.getMessage());
            }
        }


        Map<String, String> reportMap = new HashMap<String, String>();
        String reportTemplate = "totals.txt";

        // miscellaneous
        reportMap.put("term", UtilFormatOut.padString(UtilProperties.getMessage(PosTransaction.resource,"PosTerm",Locale.getDefault()), 20, false, ' '));
        reportMap.put("draw", UtilFormatOut.padString(UtilProperties.getMessage(PosTransaction.resource,"PosDraw",Locale.getDefault()), 20, false, ' '));
        reportMap.put("clerk", UtilFormatOut.padString(UtilProperties.getMessage(PosTransaction.resource,"PosClerk",Locale.getDefault()), 20, false, ' '));
        reportMap.put("total_report", UtilFormatOut.padString(UtilProperties.getMessage(PosTransaction.resource,"PosTotalReport",Locale.getDefault()), 20, false, ' '));

        // titles
        reportMap.put("cashTitle", UtilFormatOut.padString(UtilProperties.getMessage(PosTransaction.resource,"PosCash",Locale.getDefault()), 20, false, ' '));
        reportMap.put("checkTitle", UtilFormatOut.padString(UtilProperties.getMessage(PosTransaction.resource,"PosCheck",Locale.getDefault()), 20, false, ' '));
        reportMap.put("giftCardTitle", UtilFormatOut.padString(UtilProperties.getMessage(PosTransaction.resource,"PosGiftCard",Locale.getDefault()), 20, false, ' '));
        reportMap.put("creditCardTitle", UtilFormatOut.padString(UtilProperties.getMessage(PosTransaction.resource,"PosCreditCard",Locale.getDefault()), 20, false, ' '));
        reportMap.put("otherTitle", UtilFormatOut.padString(UtilProperties.getMessage(PosTransaction.resource,"PosOther",Locale.getDefault()), 20, false, ' '));
        reportMap.put("grossSalesTitle", UtilFormatOut.padString(UtilProperties.getMessage(PosTransaction.resource,"PosGrossSales",Locale.getDefault()), 20, false, ' '));
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
            BigDecimal cashEnd = state.getBigDecimal("actualEndingCash");
            BigDecimal checkEnd = state.getBigDecimal("actualEndingCheck");
            BigDecimal ccEnd = state.getBigDecimal("actualEndingCc");
            BigDecimal gcEnd = state.getBigDecimal("actualEndingGc");
            BigDecimal othEnd = state.getBigDecimal("actualEndingOther");
            BigDecimal grossEnd = cashEnd.add(checkEnd.add(ccEnd.add(gcEnd.add(othEnd))));

            reportMap.put("cashEnd", UtilFormatOut.padString(UtilFormatOut.formatPrice(cashEnd), 8, false, ' '));
            reportMap.put("checkEnd", UtilFormatOut.padString(UtilFormatOut.formatPrice(checkEnd), 8, false, ' '));
            reportMap.put("ccEnd", UtilFormatOut.padString(UtilFormatOut.formatPrice(ccEnd), 8, false, ' '));
            reportMap.put("gcEnd", UtilFormatOut.padString(UtilFormatOut.formatPrice(gcEnd), 8, false, ' '));
            reportMap.put("otherEnd", UtilFormatOut.padString(UtilFormatOut.formatPrice(othEnd), 8, false, ' '));
            reportMap.put("grossEnd", UtilFormatOut.padString(UtilFormatOut.formatPrice(grossEnd), 8, false, ' '));

            // diffs
            BigDecimal cashDiff = cashEnd.subtract(cashTotal);
            BigDecimal checkDiff = checkEnd.subtract(checkTotal);
            BigDecimal ccDiff = ccEnd.subtract(ccTotal);
            BigDecimal gcDiff = gcEnd.subtract(gcTotal);
            BigDecimal othDiff = othEnd.subtract(othTotal);
            BigDecimal grossDiff = cashDiff.add(checkDiff.add(ccDiff.add(gcDiff.add(othDiff))));

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

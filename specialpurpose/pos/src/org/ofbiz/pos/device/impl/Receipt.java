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
package org.ofbiz.pos.device.impl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import jpos.JposException;
import jpos.POSPrinter;
import jpos.POSPrinterConst;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilFormatOut;
import org.ofbiz.base.util.UtilURL;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.base.util.string.FlexibleStringExpander;
import org.ofbiz.pos.PosTransaction;
import org.ofbiz.pos.device.GenericDevice;
import org.ofbiz.pos.screen.DialogCallback;
import org.ofbiz.pos.screen.PosDialog;
import org.ofbiz.pos.screen.PosScreen;

import org.apache.commons.collections.map.LinkedMap;

public class Receipt extends GenericDevice implements DialogCallback {

    public static final String module = Receipt.class.getName();

    protected static final String ESC = ((char) 0x1b) + "";
    protected static final String LF = ((char) 0x0a) + "";

    protected static final String ALIGN_CENTER = ESC + "|cA";
    protected static final String ALIGN_RIGHT = ESC + "|rA";

    protected static final String TEXT_DOUBLE_HEIGHT = ESC + "|4C";
    protected static final String TEXT_UNDERLINE = ESC + "|uC";
    protected static final String TEXT_BOLD = ESC + "|bC";

    protected static final String PAPER_CUT = ESC + "|100fP";

    protected SimpleDateFormat[] dateFormat = null;
    protected String[] storeReceiptTmpl = null;
    protected String[] custReceiptTmpl = null;
    protected LinkedMap reportTmpl = new LinkedMap();

    protected String[] dateFmtStr = { "EEE, d MMM yyyy HH:mm:ss z", "EEE, d MMM yyyy HH:mm:ss z", "EEE, d MMM yyyy HH:mm:ss z" };
    protected int[] priceLength = { 7, 7, 7 };
    protected int[] qtyLength = { 5, 5, 5 };
    protected int[] descLength = { 25, 25, 0 };
    protected int[] pridLength = { 25, 25, 0 };
    protected int[] infoLength = { 34, 34, 0 };

    protected PosTransaction lastTransaction = null;

    public Receipt(String deviceName, int timeout) {
        super(deviceName, timeout);
        this.control = new jpos.POSPrinter();
    }

    protected void initialize() throws JposException {
        Debug.logInfo("Receipt [" + control.getPhysicalDeviceName() + "] Claimed : " + control.getClaimed(), module);
        // set map mode to metric - all dimensions specified in 1/100mm units
        // unit = 1/100 mm - i.e. 1 cm = 10 mm = 10 * 100 units
        ((jpos.POSPrinter) control).setMapMode(POSPrinterConst.PTR_MM_METRIC);
    }

    public void println() {
        this.println("");
    }

    public void println(String p) {
        try {
            ((POSPrinter) control).printNormal(POSPrinterConst.PTR_S_RECEIPT, p + LF);
        } catch (jpos.JposException e) {
            Debug.logError(e, module);
        }
    }

    public void printBarcode(String barcode) {
        // print the orderId bar code (Code 3 of 9) centered (1cm tall, 6cm wide)
        try {
            ((POSPrinter) control).printBarCode(POSPrinterConst.PTR_S_RECEIPT, barcode, POSPrinterConst.PTR_BCS_Code39,
                    10 * 100, 60 * 100, POSPrinterConst.PTR_BC_CENTER, POSPrinterConst.PTR_BC_TEXT_NONE);
        } catch (JposException e) {
            Debug.logError(e, module);
        }
    }

    public void printReport(PosTransaction trans, String resource, Map context) {
        Debug.log("Print Report Requested", module);
        String[] report = this.readReportTemplate(resource);

        if (report != null) {
            for (int i = 0; i < report.length; i++) {
                if (report[i] != null) {
                    this.printInfo(report[i], context, trans, 2);
                }
            }

            this.println();
            this.println();
            this.println(PAPER_CUT);
        }
    }

    public void reprintReceipt() {
        this.reprintReceipt(false);
    }

    public void reprintReceipt(boolean reprintStoreCopy) {
        if (lastTransaction != null) {
            this.printReceipt(lastTransaction, reprintStoreCopy);
        }
    }

    public void printReceipt(PosTransaction trans, boolean printStoreCopy) {
        Debug.log("Print Receipt Requested : " + trans.getTransactionId(), module);
        POSPrinter printer = (POSPrinter) control;
        this.lastTransaction = trans;

        try {
            if (!checkState(printer)) {
                return;
            }
        } catch (JposException e) {
            Debug.logError(e, module);
        }

        if (printStoreCopy) {
            String[] storeReceipt = this.readStoreTemplate();
            int payments = trans.getNumberOfPayments();
            for (int i = 0; i < payments; i++) {
                Map info = trans.getPaymentInfo(i);
                if (info.containsKey("cardNumber")) {
                    this.printReceipt(trans, storeReceipt, 1, info);
                }
                try {
                    Thread.sleep(3000);
                } catch (Exception e) {
                }
            }                        
        }

        // print the customer receipt
        String[] custReceipt = this.readCustomerTemplate();
        this.printReceipt(trans, custReceipt, 0, null);
    }

    private void printReceipt(PosTransaction trans, String[] template, int type, Map payInfo) {
        try {
            ((POSPrinter) control).transactionPrint(POSPrinterConst.PTR_S_RECEIPT, POSPrinterConst.PTR_TP_TRANSACTION);
        } catch (Exception e) {
        }

        if (template != null) {
            for (int i = 0; i < template.length; i++) {
                if (template[i] != null) {
                    if ("[ORDER_BARCODE]".equals(template[i])) {
                        this.printBarcode(trans.getOrderId());
                    } else if (template[i].startsWith("[DLOOP]")) {
                        this.printDetail(template[i], trans, type);
                    } else if (template[i].startsWith("[PLOOP]")) {
                        this.printPayInfo(template[i], trans, type);
                    } else if (payInfo != null) {
                        this.printPayInfo(template[i], trans, type, payInfo);
                    } else {
                        this.printInfo(template[i], trans, type);
                    }
                }
            }

            this.println();
            this.println();
            this.println(PAPER_CUT);
        }
        try {
            ((POSPrinter) control).transactionPrint(POSPrinterConst.PTR_S_RECEIPT, POSPrinterConst.PTR_TP_NORMAL);
        } catch (Exception e) {
        }
    }

    private synchronized String[] readStoreTemplate() {
        if (this.storeReceiptTmpl == null) {
            this.storeReceiptTmpl = new String[7];
            this.readTemplate(storeReceiptTmpl, "storereceipt.txt", 1);
        }

        return this.storeReceiptTmpl;
    }

    private synchronized String[] readCustomerTemplate() {
        if (this.custReceiptTmpl == null) {
            this.custReceiptTmpl = new String[7];
            this.readTemplate(custReceiptTmpl, "custreceipt.txt", 0);
        }

        return this.custReceiptTmpl;
    }

    private synchronized String[] readReportTemplate(String resource) {
        String[] template = (String[]) reportTmpl.get(resource);
        if (template == null) {
            template = new String[7];
            this.readTemplate(template, resource, 2);
            reportTmpl.put(resource, template);
        }

        return template;
    }

    private String[] readTemplate(String[] template, String resource, int type) {
        int currentPart = 0;

        URL fileUrl = UtilURL.fromResource(resource);
        StringBuffer buf = new StringBuffer();

        try {
            InputStream in = fileUrl.openStream();
            BufferedReader dis = new BufferedReader(new InputStreamReader(in));

            String line;
            while ((line = dis.readLine()) != null) {
                if (line.trim().startsWith("#")) {
                    String[] code = line.trim().split("\\=");
                    if ("#description.length".equals(code[0])) {
                        try {
                            this.descLength[type] = Integer.parseInt(code[1]);
                        } catch (NumberFormatException e) {
                            Debug.logWarning(e, module);
                        }
                    } else if ("#productId.length".equals(code[0])) {
                        try {
                            this.pridLength[type] = Integer.parseInt(code[1]);
                        } catch (NumberFormatException e) {
                            Debug.logWarning(e, module);
                        }
                    } else if ("#price.length".equals(code[0])) {
                        try {
                            this.priceLength[type] = Integer.parseInt(code[1]);
                        } catch (NumberFormatException e) {
                            Debug.logWarning(e, module);
                        }
                    } else if ("#quantity.length".equals(code[0])) {
                        try {
                            this.qtyLength[type] = Integer.parseInt(code[1]);
                        } catch (NumberFormatException e) {
                            Debug.logWarning(e, module);
                        }
                    } else if ("#infoString.length".equals(code[0])) {
                        try {
                            this.infoLength[type] = Integer.parseInt(code[1]);
                        } catch (NumberFormatException e) {
                            Debug.logWarning(e, module);
                        }
                    } else if ("#dateFormat".equals(code[0])) {
                        this.dateFmtStr[type] = code[1];
                    }
                } else if (line.trim().startsWith("[BEGIN ITEM LOOP]")) {
                    template[currentPart++] = buf.toString();
                    buf = new StringBuffer();
                    buf.append("[DLOOP]");
                } else if (line.trim().startsWith("[END ITEM LOOP]")) {
                    template[currentPart++] = buf.toString();
                    buf = new StringBuffer();
                } else if (line.trim().startsWith("[BEGIN PAY LOOP]")) {
                    template[currentPart++] = buf.toString();
                    buf = new StringBuffer();
                    buf.append("[PLOOP]");
                } else if (line.trim().startsWith("[END PAY LOOP]")) {
                    template[currentPart++] = buf.toString();
                    buf = new StringBuffer();
                } else if (line.trim().startsWith("[ORDER BARCODE]")) {
                    template[currentPart++] = buf.toString();
                    template[currentPart++] = "[ORDER_BARCODE]";
                    buf = new StringBuffer();
                } else {
                    if (UtilValidate.isEmpty(line)) {
                        line = " ";
                    }
                    buf.append(line + "\n");
                }
            }
            in.close();
        } catch (IOException e) {
            Debug.logError(e, "Unable to open receipt template", module);
        }

        template[currentPart] = buf.toString();
        return template;
    }

    private synchronized SimpleDateFormat getDateFormat(int type) {
        if (dateFormat == null) {
            dateFormat = new SimpleDateFormat[3];
        }
        if (dateFormat[type] == null) {
            dateFormat[type] = new SimpleDateFormat(this.dateFmtStr[type]);
        }
        return dateFormat[type];
    }

    private void printInfo(String template, Map context, PosTransaction trans, int type) {
        Map expandMap = this.makeCodeExpandMap(trans, type);
        if (context != null) {
            expandMap.putAll(context); // context overrides
        }
        this.printInfo(template, expandMap);
    }

    private void printInfo(String template, PosTransaction trans, int type) {
        this.printInfo(template, null, trans, type);
    }

    private void printInfo(String template, Map context) {
        String toPrint = FlexibleStringExpander.expandString(template, context);
        if (toPrint.indexOf("\n") > -1) {
            String[] lines = toPrint.split("\\n");
            for (int i = 0; i < lines.length; i++) {
                this.println(lines[i]);
            }
        } else {
            this.println(toPrint);
        }
    }

    private void printDetail(String loop, PosTransaction trans, int type) {
        String loopStr = loop.substring(7);
        int size = trans.size();
        for (int i = 0; i < size; i++) {
            Map expandMap = this.makeCodeExpandMap(trans, type);
            expandMap.putAll(trans.getItemInfo(i));
            // adjust the padding
            expandMap.put("description", UtilFormatOut.padString((String) expandMap.get("description"), descLength[type], true, ' '));
            expandMap.put("productId", UtilFormatOut.padString((String) expandMap.get("productId"), pridLength[type], true, ' '));
            expandMap.put("basePrice", UtilFormatOut.padString((String) expandMap.get("basePrice"), priceLength[type], false, ' '));
            expandMap.put("subtotal", UtilFormatOut.padString((String) expandMap.get("subtotal"), priceLength[type], false, ' '));
            expandMap.put("quantity", UtilFormatOut.padString((String) expandMap.get("quantity"), qtyLength[type], false, ' '));
            expandMap.put("adjustments", UtilFormatOut.padString((String) expandMap.get("adjustments"), priceLength[type], false, ' '));
            String toPrint = FlexibleStringExpander.expandString(loopStr, expandMap);
            if (toPrint.indexOf("\n") > -1) {
                String[] lines = toPrint.split("\\n");
                for (int x = 0; x < lines.length; x++) {
                    this.println(lines[x]);
                }
            } else {
                this.println(toPrint);
            }
        }
    }

    private void printPayInfo(String loop, PosTransaction trans, int type) {
        String loopStr = loop.substring(7);
        int size = trans.getNumberOfPayments();
        for (int i = 0; i < size; i++) {
            Map payInfoMap = trans.getPaymentInfo(i);
            this.printPayInfo(loopStr, trans, type, payInfoMap);
        }
    }

    private void printPayInfo(String template, PosTransaction trans, int type, Map payInfo) {
        Map expandMap = this.makeCodeExpandMap(trans, type);
        expandMap.putAll(payInfo);
        // adjust the padding
        expandMap.put("authInfoString", UtilFormatOut.padString((String) expandMap.get("authInfoString"), infoLength[type], false, ' '));
        expandMap.put("nameOnCard", UtilFormatOut.padString((String) expandMap.get("nameOnCard"), infoLength[type], false, ' '));
        expandMap.put("payInfo", UtilFormatOut.padString((String) expandMap.get("payInfo"), infoLength[type], false, ' '));
        expandMap.put("amount", UtilFormatOut.padString((String) expandMap.get("amount"), priceLength[type], false, ' '));
        String toPrint = FlexibleStringExpander.expandString(template, expandMap);
        if (toPrint.indexOf("\n") > -1) {
            String[] lines = toPrint.split("\\n");
            for (int x = 0; x < lines.length; x++) {
                this.println(lines[x]);
            }
        } else {
            this.println(toPrint);
        }
    }

    private Map makeCodeExpandMap(PosTransaction trans, int type) {
        Map expandMap = new HashMap();
        SimpleDateFormat fmt = this.getDateFormat(type);
        String dateString = fmt.format(new Date());

        expandMap.put("DOUBLE_HEIGHT", TEXT_DOUBLE_HEIGHT);
        expandMap.put("CENTER", ALIGN_CENTER);
        expandMap.put("BOLD", TEXT_BOLD);
        expandMap.put("UNDERLINE", TEXT_UNDERLINE);
        expandMap.put("LF", LF);
        expandMap.put("transactionId", trans.getTransactionId());
        expandMap.put("terminalId", trans.getTerminalId());
        expandMap.put("userId", trans.getUserId());
        expandMap.put("orderId", trans.getOrderId());
        expandMap.put("dateStamp", dateString);
        expandMap.put("drawerNo", Integer.toString(trans.getDrawerNumber()));
        expandMap.put("taxTotal", UtilFormatOut.padString(UtilFormatOut.formatPrice(trans.getTaxTotal()), priceLength[type], false, ' '));
        expandMap.put("grandTotal", UtilFormatOut.padString(UtilFormatOut.formatPrice(trans.getGrandTotal()), priceLength[type], false, ' '));
        expandMap.put("totalPayments", UtilFormatOut.padString(UtilFormatOut.formatPrice(trans.getPaymentTotal()), priceLength[type], false, ' '));
        expandMap.put("change", UtilFormatOut.padString((trans.getTotalDue() < 0 ?
                UtilFormatOut.formatPrice(trans.getTotalDue() * -1) : "0.00"), priceLength[type], false, ' '));

        return expandMap;
    }

    private boolean checkState(POSPrinter printer) throws JposException {
        if (printer.getCoverOpen() == true) {
            // printer is not ready
            PosScreen.currentScreen.showDialog("main/dialog/error/printernotready", this);
            return false;
        }

        return true;
    }

    public void receiveDialogCb(PosDialog dialog) {
        PosScreen.currentScreen.refresh();
        this.reprintReceipt();
    }
}

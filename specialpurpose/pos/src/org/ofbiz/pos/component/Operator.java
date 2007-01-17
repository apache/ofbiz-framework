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
package org.ofbiz.pos.component;

import java.awt.Color;
import java.awt.Font;
import java.awt.Component;
import java.text.SimpleDateFormat;
import java.util.Date;
import javax.swing.BorderFactory;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;
import java.util.Locale;

import net.xoetrope.xui.style.XStyle;
import net.xoetrope.xui.XProjectManager;
import net.xoetrope.swing.XEdit;
import net.xoetrope.swing.XPanel;

import org.ofbiz.pos.screen.PosScreen;
import org.ofbiz.pos.PosTransaction;
import org.ofbiz.base.util.UtilFormatOut;
import org.ofbiz.base.util.UtilProperties;

public class Operator {

    public static final String module = Operator.class.getName();
    public static final String style = "operTitle";

    public static final String[] OPER_TOTAL = { "oper_total", "TOTAL" };
    public static final String[] OPER_DATE = { "oper_date", "DATE" };
    public static final String[] OPER_EMPL = { "oper_empl", "EMPL" };
    public static final String[] OPER_TXID = { "oper_txid", "TXID" };
    public static final String[] OPER_DRWR = { "oper_drwr", "DRAWER" };    
    
    public static SimpleDateFormat sdf = new SimpleDateFormat(UtilProperties.getMessage("pos","DateFormat",Locale.getDefault()));
    protected Component[] operatorField = null;
    protected XStyle titleStyle = null;
    protected XPanel operPanel = null;
    
    private Locale defaultLocale = Locale.getDefault();
   

    public Operator(PosScreen page) {
        this.titleStyle = XProjectManager.getStyleManager().getStyle(style);
        this.operPanel = (XPanel) page.findComponent("oper_panel");
        this.operatorField = operPanel.getComponents();
        this.operPanel.setVisible(false);
        this.refresh();
    }

    public void setLock(boolean lock) {
        operPanel.setVisible(!lock);        
    }

    public void refresh() {
        for (int i = 0; i < operatorField.length; i++) {
            if (operatorField[i] instanceof XEdit) {
                this.setupField((XEdit) operatorField[i]);
                this.setFieldValue((XEdit) operatorField[i]);
            }
        }
    }

    protected void setupField(XEdit field) {
        Color titleColor = titleStyle.getStyleAsColor(XStyle.COLOR_FORE);
        String fontName = titleStyle.getStyleAsString(XStyle.FONT_FACE);
        int fontStyle = titleStyle.getStyleAsInt(XStyle.FONT_WEIGHT);
        int fontSize = titleStyle.getStyleAsInt(XStyle.FONT_SIZE);
        Font titleFont = new Font(fontName, fontStyle, fontSize);

        Border base = BorderFactory.createEtchedBorder();
        TitledBorder border = BorderFactory.createTitledBorder(base, this.getFieldTitle(field.getName()),
                TitledBorder.LEFT, TitledBorder.TOP, titleFont, titleColor);
        field.setBorder(border);
        field.setOpaque(true);
        field.setEditable(false);
    }

    protected void setFieldValue(XEdit field) {
        PosTransaction trans = null;
        if (operPanel.isVisible()) {
            trans = PosTransaction.getCurrentTx(PosScreen.currentScreen.getSession());
        }

        String fieldName = field.getName();
        if (OPER_TOTAL[0].equals(fieldName)) {
            String total = "0.00";
            if (trans != null) {
                total = UtilFormatOut.formatPrice(trans.getTotalDue());
            }
            field.setText(total);
        } else if (OPER_DATE[0].equals(fieldName)) {
            field.setText(sdf.format(new Date()));
        } else if (OPER_EMPL[0].equals(fieldName)) {
            String userId = "NA";
            if (trans != null) {
                userId = PosScreen.currentScreen.getSession().getUserId();
            }
            field.setText(userId);
        } else if (OPER_TXID[0].equals(fieldName)) {
            String txId = "NA";
            if (trans != null) {
                txId = trans.getTransactionId();
            }
            field.setText(txId);
        } else if (OPER_DRWR[0].equals(fieldName)) {
            String drawer = "0";
            if (trans != null) {
                drawer = "" + trans.getDrawerNumber();
            }
            field.setText(drawer);
        }
    }

    protected String getFieldTitle(String fieldName) {
        if (OPER_TOTAL[0].equals(fieldName)) {
            return UtilProperties.getMessage("pos","TOTAL",defaultLocale);            
        } else if (OPER_DATE[0].equals(fieldName)) {
        	return UtilProperties.getMessage("pos","DATE",defaultLocale);
        } else if (OPER_EMPL[0].equals(fieldName)) {
        	return UtilProperties.getMessage("pos","EMPL",defaultLocale);
        } else if (OPER_TXID[0].equals(fieldName)) {
        	return UtilProperties.getMessage("pos","TXID",defaultLocale);
        } else if (OPER_DRWR[0].equals(fieldName)) {
        	return UtilProperties.getMessage("pos","DRWR",defaultLocale);
        }
        return "";
    }

}

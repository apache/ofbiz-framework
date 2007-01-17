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

import java.util.Locale;

import net.xoetrope.swing.XTable;
import net.xoetrope.swing.XScrollPane;
import net.xoetrope.xui.data.XModel;

import org.ofbiz.pos.PosTransaction;
import org.ofbiz.pos.screen.PosScreen;
import org.ofbiz.base.util.UtilProperties;

public class Journal {

    public static final String module = Journal.class.getName();

    private static String[] field = { "sku", "desc", "qty", "price", "index" };
    private static String[] name = { "SKU", "ITEM", "QTY", "AMT", "" };
    private static int[] width = { 100, 170, 60, 80, 0};
    private Locale defaultLocale = Locale.getDefault();

    protected XScrollPane jpanel = null;
    protected XTable jtable = null;
    protected String style = null;

    public Journal(PosScreen page) {    	
        //The vertical bar is always visible to allow access to horizontal bar without shrink the journal panel
        this.jpanel = (XScrollPane) page.findComponent("journal_panel");
        this.jpanel.setVisible(false);
        
        this.jtable = (XTable) page.findComponent("jtable");
                        
        // set the table as selectable        
        jtable.setInteractiveTable(true);
        jtable.setFocusable(false);

        // set the styles
        jtable.setBorderStyle("journalBorder");
        jtable.setHeaderStyle("journalHeader");
        jtable.setStyle("journalData");
        jtable.setSelectedStyle("journalSelected");

        // initialize the journal table header
        XModel jmodel = createModel();
        if (jmodel != null) {
            this.appendEmpty(jmodel);
            jtable.setModel(jmodel);

            for (int i = 0; i < width.length; i++) {
                jtable.setColWidth(i, width[i]);
            }
        }
        jtable.setSelectedRow(0);
    }

    public String getSelectedSku() {
        XModel jmodel = (XModel) XModel.getInstance().get("journal/items");        
        XModel model = jmodel.get(jtable.getSelectedRow() + 1);
        return model.getValueAsString("sku");
    }

    public String getSelectedIdx() {
        XModel jmodel = (XModel) XModel.getInstance().get("journal/items");
        XModel model = jmodel.get(jtable.getSelectedRow() + 1);
        return model.getValueAsString("index");
    }

    public void selectNext() {
        jtable.next();
    }

    public void selectPrevious() {
        jtable.prev();
    }

    public void focus() {
        if (jtable.isEnabled()) {
            jtable.requestFocus();
        }
    }

    public void setLock(boolean lock) {
        jtable.setInteractiveTable(!lock);
        jtable.setFocusable(!lock);
        jtable.setVisible(!lock);
        jtable.setEnabled(!lock);
        if (!lock) {
            this.jpanel.setVisible(true);
        }
    }

    public void refresh(PosScreen pos) {
        if (!jtable.isEnabled()) {
            // no point in refreshing when we are locked;
            // we will auto-refresh when unlocked
            return;
        }

        PosTransaction tx = PosTransaction.getCurrentTx(pos.getSession());
        XModel jmodel = this.createModel();
        if (tx != null && !tx.isEmpty()) {
            tx.appendItemDataModel(jmodel);
            this.appendEmpty(jmodel);
            tx.appendTotalDataModel(jmodel);
            if (tx.selectedPayments() > 0) {
                this.appendEmpty(jmodel);
                tx.appendPaymentDataModel(jmodel);
            }
            if (pos.getInput().isFunctionSet("PAID")) {
                tx.appendChangeDataModel(jmodel);
            }
        } else {
            this.appendEmpty(jmodel);
        }

        // make sure we are at the last item in the journal
        jtable.setSelectedRow(0);

        try {
            jtable.repaint();
        } catch (ArrayIndexOutOfBoundsException e) {
            // bug in XUI causes this; ignore for now
            // it has been reported and will be fixed soon
        }
    }

    private XModel createModel() {
        XModel jmodel = (XModel) XModel.getInstance().get("journal/items");

        // clear the list
        jmodel.clear();

        if (field.length == 0) {
            return null;
        }

        // create the header
        XModel headerNode = appendNode(jmodel, "th", "", "");
        for (int i = 0 ; i < field.length; i++) {
            appendNode(headerNode, "td", field[i],UtilProperties.getMessage("pos",name[i],defaultLocale));
        }

        return jmodel;
    }

    private void appendEmpty(XModel jmodel) {
        XModel headerNode = appendNode(jmodel, "tr", "", "");
        for (int i = 0 ; i < field.length; i++) {
            appendNode(headerNode, "td", field[i], "");
        }
    }

    public static XModel appendNode(XModel node, String tag, String name, String value) {
        XModel newNode = (XModel) node.append(name);
        newNode.setTagName(tag);
        if (value != null) {
            newNode.set(value);
        }
        return newNode;
    }
}

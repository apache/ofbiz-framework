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
package org.apache.ofbiz.pos.component;

import java.awt.Color;
import java.util.Locale;

import javax.swing.ListSelectionModel;

import net.xoetrope.swing.XScrollPane;
import net.xoetrope.swing.XTable;
import net.xoetrope.xui.XProject;
import net.xoetrope.xui.XProjectManager;
import net.xoetrope.xui.data.XModel;
import net.xoetrope.xui.style.XStyle;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilProperties;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.pos.PosTransaction;
import org.apache.ofbiz.pos.screen.PosScreen;

public class Journal {

    public static final String module = Journal.class.getName();
    protected XProject currentProject = XProjectManager.getCurrentProject();

    private static String[] field = { "sku", "desc", "qty", "price" };
    private static String[] name = { "PosSku", "PosItem", "PosQty", "PosAmt" };
    private static int[] width = { 100, 170, 50, 90};
    private Locale defaultLocale = Locale.getDefault();

    protected XScrollPane jpanel = null;
    protected XTable jtable = null;
    protected String style = null;

    public Journal(PosScreen page) {
        jpanel = (XScrollPane) page.findComponent("journal_panel");
        jpanel.setVisible(false);

        this.jtable = (XTable) page.findComponent("jtable");

        // set the table as selectable
        jtable.setInteractiveTable(true);
        jtable.setFocusable(false);
        jtable.setDragEnabled(false);
        jtable.setColumnSelectionAllowed(false);
        jtable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);


        // set the styles
        jtable.setBorderStyle("journalBorder");
        jtable.setHeaderStyle("journalHeader");
        jtable.setStyle("journalData");
        jtable.setSelectedStyle("journalSelected");

        // some settings needed for XUI 3.2rc2b update
        jtable.setRowHeight(30); // Better to catch the line on a touch screen (minimal height I think)
        XStyle style = currentProject.getStyleManager().getStyle("journalBorder");
        Color borderColor = style.getStyleAsColor(XStyle.COLOR_FORE);
        jtable.setGridColor(borderColor); // jtable.setBorderStyle("journalBorder"); above is not working anymore
        style = currentProject.getStyleManager().getStyle("journalData");
        Color backgoundColor = style.getStyleAsColor(XStyle.COLOR_BACK);
        jtable.setBackground(backgoundColor); // TODO This line is not working
        jpanel.setBorder(jtable.getBorder()); // TODO there is a small shift between the vertical header grid lines and the other vertical grid lines. This line is not working

        // initialize the journal table header
        XModel jmodel = createModel();
        if (jmodel != null) {
            this.appendEmpty(jmodel);
            jtable.setModel(jmodel);

            for (int i = 0; i < width.length; i++) {
                if (defaultLocale.getLanguage().equals("ar")) {
                    jtable.setColWidth(width.length - i - 1, width[i]);
                } else {
                jtable.setColWidth(i, width[i]);
            }
        }
        }
        jtable.setSelectedRow(0);
    }

    public String getSelectedSku() {
        XModel jmodel = jtable.getXModel();
        XModel model = jmodel.get(jtable.getSelectedRow() + 1);
        return model.getValueAsString("sku");
    }

    public String getSelectedIdx() {
        XModel jmodel = jtable.getXModel();
        XModel model = jmodel.get(jtable.getSelectedRow() + 1);
        return model.getId();
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
        this.jpanel.setVisible(!lock);
    }

    public synchronized void refresh(PosScreen pos) {
        if (!jtable.isEnabled()) {
            // no point in refreshing when we are locked;
            // we will auto-refresh when unlocked
            return;
        }

        PosTransaction tx = PosTransaction.getCurrentTx(pos.getSession());
        XModel jmodel = this.createModel();
        if (UtilValidate.isNotEmpty(tx)) {
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
            jtable.update();
        } catch (ArrayIndexOutOfBoundsException e) {
            Debug.logError(e, "Unable to repaint the Journal", module);
        }
        //Debug.logInfo(getModelText(jmodel), module);
    }

    private XModel createModel() {
        XModel jmodel = (XModel)currentProject.getModel().get("table/items");
        // clear the list
        jmodel.clear();

        if (field.length == 0) {
            return null;
        }
        jmodel.setTagName("table");
        // create the header
        XModel headerNode = appendNode(new JournalLineParams(jmodel, "th", "header", ""));
        if (defaultLocale.getLanguage().equals("ar")) {
            for (int i = field.length - 1; i >= 0; i--) {
                appendNode(new JournalLineParams(headerNode, "td", field[i], UtilProperties.getMessage(PosTransaction.resource, name[i], defaultLocale)));
            }
        } else {
        for (int i = 0 ; i < field.length; i++) {
                appendNode(new JournalLineParams(headerNode, "td", field[i], UtilProperties.getMessage(PosTransaction.resource, name[i], defaultLocale)));
        }
        }

        return jmodel;
    }

    private void appendEmpty(XModel jmodel) {
        XModel headerNode = appendNode(new JournalLineParams(jmodel, "tr", "emptyrow", ""));
        for (int i = 0 ; i < field.length; i++) {
            appendNode(new JournalLineParams(headerNode, "td", field[i], ""));
        }
    }

    public static XModel appendNode(JournalLineParams journalLineParams) {
        XModel newNode = (XModel) journalLineParams.getNode().append(journalLineParams.getName());
        newNode.setTagName(journalLineParams.getTag());

        if (journalLineParams.getValue() != null) {
            newNode.set(journalLineParams.getValue());
        }
        return newNode;
    }
}

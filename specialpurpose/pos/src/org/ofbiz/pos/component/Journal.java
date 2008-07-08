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
import java.io.StringWriter;
import java.util.Locale;

import javax.swing.ListSelectionModel;

import net.xoetrope.data.XDataSource;
import net.xoetrope.swing.XScrollPane;
import net.xoetrope.swing.XTable;
import net.xoetrope.xui.XProject;
import net.xoetrope.xui.XProjectManager;
import net.xoetrope.xui.data.XModel;
import net.xoetrope.xui.style.XStyle;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilProperties;
import org.ofbiz.pos.PosTransaction;
import org.ofbiz.pos.screen.PosScreen;

public class Journal {

    public static final String module = Journal.class.getName();
    protected XProject currentProject = (XProject)XProjectManager.getCurrentProject();
    
    private static String[] field = { "sku", "desc", "qty", "price" };
    private static String[] name = { "SKU", "ITEM", "QTY", "AMT" };
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
        Color borderColor = style.getStyleAsColor(XStyle.COLOR_FORE );        
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
                jtable.setColWidth(i, width[i]);
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
        XModel headerNode = appendNode(jmodel, "th", "header", "");
        for (int i = 0 ; i < field.length; i++) {
            appendNode(headerNode, "td", field[i],UtilProperties.getMessage("PosUiLabels",name[i],defaultLocale));
        }

        return jmodel;
    }

    private void appendEmpty(XModel jmodel) {
        XModel headerNode = appendNode(jmodel, "tr", "emptyrow", "");
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
    
    private String getModelText(XModel model)
    {
        StringWriter sw = new StringWriter();
        XDataSource.outputModel( sw, model );
        return "<Datasets>" + sw.toString() + "</Datasets>";
    }
}
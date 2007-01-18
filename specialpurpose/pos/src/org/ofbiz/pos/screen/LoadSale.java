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
package org.ofbiz.pos.screen;

import java.util.Hashtable;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;

import javax.swing.DefaultListModel;
import javax.swing.ListSelectionModel;

import net.xoetrope.swing.XButton;
import net.xoetrope.swing.XDialog;
import net.xoetrope.swing.XList;
import net.xoetrope.xui.XPage;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilProperties;
import org.ofbiz.pos.PosTransaction;


public class LoadSale extends XPage {

    /**
     * To load a sale from a shopping list. 2 modes : add to or replace the current sale. Also a button to delete a sale (aka shopping list) 
     */
    public static final String module = LoadSale.class.getName();
    protected static PosScreen m_pos = null;
    protected XDialog m_dialog = null;
    static protected Hashtable m_saleMap = new Hashtable();
    protected XList m_salesList = null;
    protected XButton m_cancel = null;
    protected XButton m_add = null;
    protected XButton m_replace = null;
    protected XButton m_delete = null;
    protected XButton m_replaceAndDelete = null;
    protected DefaultListModel m_listModel = null; 
    protected static PosTransaction m_trans = null;

    //TODO : make getter and setter for members (ie m_*) if needed (extern calls). For that in Eclipse use Source/Generate Getters and setters

    public LoadSale(Hashtable saleMap, PosTransaction trans, PosScreen page) {
        m_saleMap.putAll(saleMap);
        m_trans = trans;
        m_pos = page;
    }

    public void openDlg() {
        m_dialog = (XDialog) pageMgr.loadPage(m_pos.getScreenLocation() + "/dialog/loadsale");
        m_dialog.setCaption(UtilProperties.getMessage("pos", "LoadASale", Locale.getDefault()));
        m_salesList = (XList) m_dialog.findComponent("salesList");
        addMouseHandler(m_salesList, "saleDoubleClick");

        m_cancel = (XButton) m_dialog.findComponent("BtnCancel");
        m_add = (XButton) m_dialog.findComponent("BtnAdd");
        m_replace = (XButton) m_dialog.findComponent("BtnReplace");
        m_delete = (XButton) m_dialog.findComponent("BtnDelete");
        m_replaceAndDelete = (XButton) m_dialog.findComponent("BtnReplaceAndDelete");
        addMouseHandler(m_cancel, "cancel");
        addMouseHandler(m_add, "addSale");
        addMouseHandler(m_replace, "replaceSale");
        addMouseHandler(m_delete, "deleteShoppingList");
        addMouseHandler(m_replaceAndDelete, "replaceSaleAndDeleteShoppingList");

        m_listModel = new DefaultListModel();
        for (Iterator i = m_saleMap.entrySet().iterator(); i.hasNext();) {
            Object o = i.next();
            Map.Entry entry = (Map.Entry)o;
            String val = entry.getValue().toString();
            m_listModel.addElement(val);
        }
        m_salesList.setModel(m_listModel);
        m_salesList.setVisibleRowCount(-1);
        m_salesList.ensureIndexIsVisible(m_salesList.getItemCount());     
        m_salesList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        m_salesList.setToolTipText(UtilProperties.getMessage("pos", "LoadSaleListDblClickTip", Locale.getDefault()));

        m_dialog.pack();
        m_salesList.requestFocusInWindow();
        m_dialog.showDialog(this);
    }

    public synchronized void saleDoubleClick() {
        if (wasMouseDoubleClicked()) {
            String sale = selectedSale();
            if (null != sale) {
                replaceSaleAndDeleteShoppingList_();
            }
        }
    }

    public synchronized void cancel() {
        if (wasMouseClicked()) {
            closeDlg();
        }
    }

    public synchronized void addSale() {
        if (wasMouseClicked()) {
            addSale_();
        }
    }

    private synchronized void addSale_() {
        String sale = selectedSale();
        if (null != sale) {
            addListToCart(sale, true);
        }
    }

    public synchronized void replaceSale() {
        if (wasMouseClicked()) {
            replaceSale_();
        }
    }

    private synchronized void replaceSale_() {
        String sale = selectedSale();
        if (null != sale) {
            addListToCart(sale, false);
        }
    }

    public synchronized void deleteShoppingList() {
        if (wasMouseClicked()) {
            deleteShoppingList_();
        }
    }

    private synchronized void deleteShoppingList_() {
        String sale= (String) m_salesList.getSelectedValue();
        if (null != sale) {
            String shoppingListId = selectedSale();
            final ClassLoader cl = this.getClassLoader(m_pos);
            Thread.currentThread().setContextClassLoader(cl);
            if (m_trans.clearList(shoppingListId, m_pos)) {
                int index = m_salesList.getSelectedIndex();
                m_saleMap.remove(shoppingListId);
                m_listModel = new DefaultListModel();
                for (Iterator i = m_saleMap.entrySet().iterator(); i.hasNext();) {
                    Object o = i.next();
                    Map.Entry entry = (Map.Entry)o;
                    String val = entry.getValue().toString();
                    m_listModel.addElement(val);
                }
                m_salesList.setModel(m_listModel);
                int size = m_listModel.getSize();
                if (size == 0) { //Nobody's left, nothing to do here
                    closeDlg();
                } else { //Select an index.
                    if (index == size) {
                        //removed item in last position
                        index--;
                    }
                }
                m_salesList.setSelectedIndex(index);
                m_salesList.ensureIndexIsVisible(index);
                m_salesList.repaint();
                repaint();
            }
        }
    }

    public synchronized void replaceSaleAndDeleteShoppingList() {
        if (wasMouseClicked()) {
            replaceSaleAndDeleteShoppingList_();
        }
    }

    public synchronized void replaceSaleAndDeleteShoppingList_() {
        replaceSale_();
        deleteShoppingList_();
    }

    private String selectedSale() {
        String saleSelected = null;
        if (null != m_salesList.getSelectedValue()) {
            String sale = (String) m_salesList.getSelectedValue();
            Iterator i = m_saleMap.entrySet().iterator();
            while(i.hasNext()) {
                Object o = i.next();
                Map.Entry entry = (Map.Entry)o;
                String val = entry.getValue().toString();
                if (val.equals(sale)) {
                    saleSelected = entry.getKey().toString();
                }
            }
        }
        return saleSelected;
    }

    private void addListToCart(String sale, boolean addToCart) {
        final ClassLoader cl = this.getClassLoader(m_pos);
        Thread.currentThread().setContextClassLoader(cl);
        if (!m_trans.addListToCart(sale, m_pos, addToCart)) {
            Debug.logError("Error while loading cart from shopping list : " + sale, module);
        } 
        else {
            m_trans.calcTax();
            m_pos.refresh();
        }   
        closeDlg();
    }

    private ClassLoader getClassLoader(PosScreen pos) {
        ClassLoader cl = pos.getClassLoader();
        if (cl == null) {
            try {
                cl = Thread.currentThread().getContextClassLoader();
            } catch (Throwable t) {
            }
            if (cl == null) {
                Debug.log("No context classloader available; using class classloader", module);
                try {
                    cl = this.getClass().getClassLoader();
                } catch (Throwable t) {
                    Debug.logError(t, module);
                }
            }
        }
        return cl;
    }    

    private void closeDlg() {
        m_dialog.closeDlg();
    }
}
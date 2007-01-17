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

import org.ofbiz.base.util.UtilProperties;
import org.ofbiz.pos.PosTransaction;


public class SelectProduct extends XPage {

    /**
     * To choose a product in a list of products whith the same bar code
     */
    public static final String module = SelectProduct.class.getName();
    protected static PosScreen m_pos = null;
    protected XDialog m_dialog = null;
    static protected Hashtable m_productsMap = new Hashtable();
    protected XList m_productsList = null;
    protected XButton m_cancel = null;
    protected XButton m_select = null;
    protected DefaultListModel m_listModel = null;
    protected static PosTransaction m_trans = null;
    protected static String m_productIdSelected = null;

    //TODO : make getter and setter for members (ie m_*) if needed (extern calls). For that in Eclipse use Source/Generate Getters and setters

    public SelectProduct(Hashtable saleMap, PosTransaction trans, PosScreen page) {
        m_productsMap.putAll(saleMap);
        m_trans = trans;
        m_pos = page;
    }

    public String openDlg() {
        XDialog dlg = (XDialog) pageMgr.loadPage(m_pos.getScreenLocation() + "/dialog/SelectProduct");
        m_dialog = dlg;
        dlg.setCaption(UtilProperties.getMessage("pos", "SelectAProduct", Locale.getDefault()));
        //dlg.setModal(true);
        m_productsList = (XList) dlg.findComponent("productsList");
        addMouseHandler(m_productsList, "DoubleClick");

        m_cancel = (XButton) dlg.findComponent("BtnCancel");
        m_select = (XButton) dlg.findComponent("BtnSelect");
        addMouseHandler(m_cancel, "cancel");
        addMouseHandler(m_select, "selectProduct");

        m_listModel = new DefaultListModel();
        for (Iterator i = m_productsMap.entrySet().iterator(); i.hasNext();) {
            Object o = i.next();
            Map.Entry entry = (Map.Entry)o;
            String val = entry.getValue().toString();
            m_listModel.addElement(val);
        }
        m_productsList.setModel(m_listModel);
        m_productsList.setVisibleRowCount(-1);
        m_productsList.ensureIndexIsVisible(m_productsList.getItemCount());
        m_productsList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        m_productsList.setToolTipText(UtilProperties.getMessage("pos", "SelectProductListDblClickTip", Locale.getDefault()));

        dlg.pack();
        dlg.showDialog(this);
        return m_productIdSelected;
    }

    public synchronized void DoubleClick() {
        if (wasMouseDoubleClicked()) {
            selectProductId();
        }
    }

    public synchronized void cancel() {
        if (wasMouseClicked()) {
            closeDlg();
        }
    }

    public synchronized void selectProduct() {
        if (wasMouseClicked()) {
            selectProductId();
        }
    }

    private void selectProductId() {
        if (null != m_productsList.getSelectedValue()) {
            String product = (String) m_productsList.getSelectedValue();
            Iterator i = m_productsMap.entrySet().iterator();
            while(i.hasNext()) {
                Object o = i.next();
                Map.Entry entry = (Map.Entry)o;
                String val = entry.getValue().toString();
                if (val.equals(product)) {
                    m_productIdSelected = entry.getKey().toString();
                }
            }
        }
        closeDlg();
    }

    private void closeDlg() {
        m_dialog.closeDlg();
    }
}

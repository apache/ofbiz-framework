/*
 * Copyright 2001-2006 The Apache Software Foundation
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package org.ofbiz.pos.screen;

import java.util.Hashtable;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.swing.ListSelectionModel;

import net.xoetrope.swing.XButton;
import net.xoetrope.swing.XDialog;
import net.xoetrope.swing.XList;
import net.xoetrope.xui.XPage;
import net.xoetrope.xui.XProjectManager;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilProperties;
import org.ofbiz.pos.PosTransaction;

/**
 * 
 * @author     <a href="mailto:jleroux@apache.org">Jacques Le Roux</a>
 * @version    $Rev$
 * @since      3.2
 */

public class Sales extends XPage {

	/**
	 * To load a sale from a shopping list. 2 modes : add to or replace the current sale.  
	 */
	public static final String module = Sales.class.getName();
	protected static PosScreen m_page = null;
	protected XDialog m_dialog = null;	
	protected Hashtable m_saleMap = null;
	protected XList m_salesList = null;
	protected XButton m_add = null;
	protected XButton m_replace = null;
	protected XButton m_cancel = null;
	protected static PosTransaction m_trans = null;

	//TODO : make getter and setter for members (ie m_*) if needed (extern calls)
	
	public Sales(Hashtable saleMap, PosTransaction trans, PosScreen page) {
		m_saleMap = saleMap;
		m_trans = trans;
		m_page = page;
	}

    public void openDlg() {
    	XDialog dlg = (XDialog) pageMgr.loadPage(m_page.getScreenLocation() + "/includes/sales");
    	m_dialog = dlg;
    	dlg.setCaption(UtilProperties.getMessage("pos", "LoadASale", Locale.getDefault()));
    	m_salesList = (XList) dlg.findComponent("salesList");
    	
    	m_add = (XButton) dlg.findComponent("BtnAdd");
    	m_replace = (XButton) dlg.findComponent("BtnReplace");
    	m_cancel = (XButton) dlg.findComponent("BtnCancel");
    	addMouseHandler(m_add, "add");
    	addMouseHandler(m_replace, "replace");
    	addMouseHandler(m_cancel, "cancel");
    	
    	m_salesList.setListData(m_saleMap.values().toArray());            
       	m_salesList.setVisibleRowCount(-1);
       	m_salesList.ensureIndexIsVisible(m_salesList.getItemCount());     
       	m_salesList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
       	
    	dlg.pack();
    	dlg.showDialog(this);
    }

    public synchronized void cancel()
    {
    	if (wasMouseClicked()) {
    		this.m_dialog.closeDlg();
    	}
    }

    public synchronized void add() {
    	if (wasMouseClicked()) {
    		String sale = selectedSale();
    		if (null != sale) {
    			addListToCart(sale, true);
    		}
    	}
    }

    public synchronized void replace() {
    	if (wasMouseClicked()) {
    		String sale = selectedSale();
    		if (null != sale) {
    			addListToCart(sale, false);
    		}
    	}
    }
    
    private String selectedSale() {
		String saleSelected = null;
		if (null != m_salesList.getSelectedValue()) {
    		String sale = (String) m_salesList.getSelectedValue();
    		Set sales = m_saleMap.entrySet();
    		Iterator i = sales.iterator();
		    while(i.hasNext()) {
		    	Object o = i.next();
		    	Map.Entry entry = (Map.Entry)o;
		    	String val = entry.getValue().toString();
		    	if (val == sale) {
		    		saleSelected = entry.getKey().toString();
		    	}
		    }
		}
		return saleSelected;
    }
    
    private void addListToCart(String sale, boolean addToCart) {
        final ClassLoader cl = this.getClassLoader(m_page);
        Thread.currentThread().setContextClassLoader(cl);    	
		if (!m_trans.addListToCart(sale, m_page, addToCart)) {
			Debug.logError("Error while loading cart from shopping list : " + sale, module);
		} 
		else {
			m_trans.calcTax();
	        m_page.refresh();			
    		this.m_dialog.closeDlg();
		}    	
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
}
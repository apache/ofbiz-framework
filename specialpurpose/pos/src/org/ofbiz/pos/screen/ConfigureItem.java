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
import java.util.List;
import java.util.Locale;
import java.util.ArrayList;
import java.util.EventObject;
import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.ListSelectionEvent;

import net.xoetrope.swing.XButton;
import net.xoetrope.swing.XDialog;
import net.xoetrope.swing.XList;
import net.xoetrope.swing.XScrollPane;
import net.xoetrope.xui.XPage;
import net.xoetrope.xui.events.XEventHelper;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilProperties;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.pos.PosTransaction;
import org.ofbiz.product.config.ProductConfigWrapper;
import org.ofbiz.product.config.ProductConfigWrapper.ConfigItem;
import org.ofbiz.product.config.ProductConfigWrapper.ConfigOption;


public class ConfigureItem extends XPage {

    /**
     * To create or configure a configurable item 
     */
    
    public static final String module = ConfigureItem.class.getName();
    protected PosScreen m_pos = null;
    protected ConfigureItem m_configureItem = null;
    protected XDialog m_dialog = null;
    protected XList m_configList = null;
    protected XList m_optionList = null;
    protected XButton m_ok = null;
    protected XButton m_reset = null;
    protected XScrollPane m_optionListPane = null;
    protected PosTransaction m_trans = null;
    protected static final String buttonArray[][] = {
        { "r1c1", "r1c2", "r1c3", "r1c4" },
        { "r2c1", "r2c2", "r2c3", "r2c4" },
        { "r3c1", "r3c2", "r3c3", "r3c4" },
        { "r4c1", "r4c2", "r4c3", "r4c4" }
    };
    protected ProductConfigWrapper m_pcw = null;
    protected ArrayList m_buttonList = null;
    protected Hashtable questionHashMap = null;

    public ConfigureItem(ProductConfigWrapper pcw, PosTransaction trans, PosScreen page) {
        m_pcw = pcw;
        m_trans = trans;
        m_pos = page;
        m_configureItem = this;
    }
        
    public ProductConfigWrapper openDlg() {
        // cache must be set to false because there's no method to remove actionhandlers
        m_dialog = (XDialog) pageMgr.loadPage(
                m_pos.getScreenLocation() + "/dialog/ConfigureItem", false);
        m_dialog.setCaption(UtilProperties.getMessage(PosTransaction.resource, "PosConfigureItem", Locale.getDefault()));

        m_optionListPane = (XScrollPane) m_dialog.findComponent("optionListPane");
        m_configList = (XList) m_dialog.findComponent("configList");
        m_optionList = (XList) m_dialog.findComponent("optionList");        
        m_ok = (XButton) m_dialog.findComponent("BtnOk");
        m_reset = (XButton) m_dialog.findComponent("BtnReset");

        XEventHelper.addMouseHandler(this, m_ok, "ok");
        XEventHelper.addMouseHandler(this, m_reset, "reset");

        getButtons();
        //debugQuestions();
        showItem();
        displayQuestions();
        m_dialog.pack();
        m_dialog.showDialog(this);

        return m_pcw;
    }
    
    public synchronized void ok() {
        if (wasMouseClicked()) {
            closeDlg();
        }
    }
    
    public synchronized void reset() {
        if (wasMouseClicked()) {
            m_pcw.setDefaultConfig();
            resetButtons();
            showItem();
            m_dialog.repaint();
        }
    }

    public synchronized void buttonPressed() {
        if (wasMouseClicked()) {
            EventObject eo = getCurrentEvent();
            XButton button = (XButton) eo.getSource();
            Question question = (Question)questionHashMap.get(button.getName());
            question.buttonClicked();
            showItem();
            m_dialog.repaint();
            return;
        }
    }

    public synchronized void listPressed() {
        EventObject eo = getCurrentEvent();
        showItem();
        m_dialog.repaint();
        return;
    }
    
    
    private void closeDlg() {
        m_dialog.closeDlg();
    }
    
    private void resetButtons() {
        Object[] questions = questionHashMap.values().toArray();
        for(Object question : questions) {
            ((Question)question).reset();
        }
        return;
    }
    
    private void showItem() {
        DefaultListModel listModel = null;         
        listModel = new DefaultListModel();
        
        GenericValue gv = m_pcw.getProduct();
        listModel.addElement(gv.get("description")); 
        
        List questions = m_pcw.getQuestions();
        if (questions==null) ; // no questions, we shouldn't be here
        else{
            Iterator iter = questions.iterator();
            while (iter.hasNext()) {
                ConfigItem question = (ConfigItem)iter.next();
                List options = question.getOptions();
                Iterator itero = options.iterator();
                while (itero.hasNext()) {
                    ConfigOption configoption = (ConfigOption)itero.next();
                    if (configoption.isSelected()) {
                        listModel.addElement("  "+configoption.getDescription());
                    }
                }
            }
        }                
        m_configList.setModel(listModel);
        return;
    }

    private void displayQuestions() {

        QuestionFactory qf = new QuestionFactory();
        questionHashMap = new Hashtable();
        
        List questions = m_pcw.getQuestions();
        if (questions==null) ; // no questions, we shouldn't be here
        else{
            Iterator iter = questions.iterator();
            Iterator buttons = m_buttonList.iterator();
            while (iter.hasNext()) {
                Question buttonQuestion = qf.get((ConfigItem)iter.next());
                XButton button = (XButton)buttons.next();
                questionHashMap.put(button.getName(), buttonQuestion );
                buttonQuestion.setupButton(button);
                if (buttonQuestion instanceof ListQuestion) {
                    ((ListQuestion)buttonQuestion).setupListPane(m_optionListPane);
                }
                XEventHelper.addMouseHandler(this, button, "buttonPressed");
            }
        }
        return;
    }
    
    private void getButtons() {
        ArrayList buttonList = new ArrayList();
        for(String[] buttonSingleArray : buttonArray ) {
            for(String buttonName : buttonSingleArray) {
                //Debug.logInfo("ButtonName: "+buttonName, module);
                XButton button = (XButton) m_dialog.findComponent(buttonName);
                buttonList.add(button);
            }
        }
        m_buttonList = buttonList;
    }
    
    private void debugQuestions() {
        //Debug.logInfo("debugQuestions",module);
        GenericValue gv = m_pcw.getProduct();

        //Debug.logInfo("Product: " +gv.get("description"), module);
        
        List questions = m_pcw.getQuestions();
        if (questions==null) return; // no questions, return
     
        Iterator iter = questions.iterator();
        while (iter.hasNext()) {
            ConfigItem question = (ConfigItem)iter.next();
            /*Debug.logInfo("Question: " + question.getQuestion(), module);
            Debug.logInfo("IsFirst: "+question.isFirst()+
                    ", IsMandatory: "+question.isMandatory()+
                    ", IsSelected: "+question.isSelected()+
                    ", IsSingleChoice: "+question.isSingleChoice()+
                    ", IsStandard: "+question.isStandard(), module);*/
                              
            List options = question.getOptions();
            Iterator itero = options.iterator();
            
            while (itero.hasNext()) {
                ConfigOption configoption = (ConfigOption)itero.next();
                /*Debug.logInfo("Found option " + configoption.getDescription(), module);
                Debug.logInfo("IsAvailable: "+configoption.isAvailable()+
                    ", IsSelected: "+configoption.isSelected(), module);*/
                //configoption.getComponents()
            }
        }
    }

    /*
     *  What are the Question types?
            Must choose one - isMandatory & isSingleChoice
            May choose one - !isMandatory & isSingleChoice
            May choose one or more - !isMandatory & !isSingleChoice
            Must choose one or more - isMandatory & !isSingleChoice
            For !isMandatory, include way to select none

     *  SingleChoice with one option can be a button.  (Can't be mandatory)
    Example: decaf
        SingleChoice with multiple options can be a group of buttons or a list.
                Example: temperature (extra hot, warm, cold)
        Not SingleChoice with multiple options can be a group of buttons or a list
                Example: flavor (vanilla and chocolate)
        Can I specify multiple of an item?  4 shots of espresso?
                Could do as a list?  Example:  Extra shots -> single, double, etc.

        What is isStandard? Maybe I can key off IsStandard for using buttons, others get a list.

        */
    
    protected class QuestionFactory{
        
        public Question get(ConfigItem question) {

            List options = question.getOptions();         
            if (question.isSingleChoice()) {
                if (options.size()>2) {
                   return new ListButtonQuestion(question);
                } else {
                    //TODO: this doesn't handle the case of 
                    // two options with none required to be selected
                   return new SingleButtonQuestion(question);
                }
            }
            else{
                return new ListButtonQuestion(question);
            }
        }
    }

    protected interface Question{        
        public void setupButton(XButton button);
        //public void setupList(XList list);
        public void buttonClicked();
        public void reset();
    }
    
    protected interface ListQuestion{
        public void setupListPane(XScrollPane m_optionListPane);
    }
    
    protected class SingleButtonQuestion implements Question{
        private XButton button = null;
        private ConfigItem question = null;
        private int showOption = 0;
        
        public SingleButtonQuestion(ConfigItem question) {
            this.question = question;
            return;
        }

        public void setupButton(XButton button) {
            this.button = button;
            List options = question.getOptions();
            if (question.isSelected()) {
                ConfigOption selectedOption = question.getSelected();
                showOption = options.indexOf(selectedOption);
            }
            ConfigOption configoption = (ConfigOption)options.get(showOption);         
            button.setText(configoption.getDescription());
            return;
        }

        /*public void setupListPane(XScrollPane m_optionListPane) {
            return;
        } */

        public void buttonClicked() {
            //only two choices, if the button is clicked, toggle
            List options = question.getOptions();
            ConfigOption unselectedoption = (ConfigOption)options.get(showOption);         
            unselectedoption.setSelected(false);
            showOption = (showOption+1)%2;
            ConfigOption selectedoption = (ConfigOption)options.get(showOption);         
            selectedoption.setSelected(true);
            button.setText(selectedoption.getDescription());
            return;
        }
               
        public void reset() {
            showOption = 0;
            List options = question.getOptions();
            if (question.isSelected()) {
                ConfigOption selectedOption = question.getSelected();
                showOption = options.indexOf(selectedOption);
            }
            ConfigOption configoption = (ConfigOption)options.get(showOption);         
            button.setText(configoption.getDescription());
            return;           
        }
    }

    protected class ListButtonQuestion implements Question, ListQuestion{
        private XButton button = null;
        private XScrollPane scrollpane = null;
        private ConfigItem question = null;
        
        public ListButtonQuestion(ConfigItem question) {
            this.question = question;
            return;
        }

        public void setupButton(XButton button) {
            this.button = button;
            int showOption = 0;
            button.setText(question.getQuestion());
            return;
        }   

        public void setupListPane(XScrollPane m_optionListPane) {
            scrollpane = m_optionListPane;
            return;
        }
        
        public void buttonClicked() {
            Iterator options = question.getOptions().iterator();
           
            DefaultListModel listModel = new DefaultListModel();
            
            while (options.hasNext()) {
                ConfigOption configoption = (ConfigOption)options.next();
                listModel.addElement(configoption.getDescription());
                //Debug.logInfo("Found option " + configoption.getDescription(), module);
                //Debug.logInfo("IsAvailable: "+configoption.isAvailable()+
                //    ", IsSelected: "+configoption.isSelected(), module);
            }
            
            //Create the list and put it in a scroll pane.
            JList list = new JList(listModel);
            list.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
            LBQSelectionHandler selectionHandler = new LBQSelectionHandler();
            selectionHandler.setQuestion(question);
            list.addListSelectionListener(selectionHandler);

            scrollpane.add(list);
            return;
        }
        
        public void reset() {
            return;
        }
    }    
    
    protected class LBQSelectionHandler implements ListSelectionListener {
        private ConfigItem question = null;
        
        public void setQuestion(ConfigItem question) {
            this.question = question;
        }
        
        public void valueChanged(ListSelectionEvent event) {
            try {
                JList jlist = (JList)event.getSource();
                boolean isAdjusting = event.getValueIsAdjusting();
                if (!isAdjusting) {
                    int[] selected = jlist.getSelectedIndices();
                    //for(int i: selected) {
                    //    Debug.logInfo(""+i, module);
                    //}
                    List<ConfigOption> options = (List<ConfigOption>)question.getOptions();
                    for (ConfigOption option: options) {
                        option.setSelected(false);
                    }
                    for(int i: selected) {
                        ConfigOption option = options.get(i);
                        option.setSelected(true);
                    }
                    m_configureItem.listPressed();
                }
            }
            catch (Exception ex) {
                Debug.logInfo(ex.getMessage(), module);
                ex.printStackTrace();                
            }
        }
    }
}

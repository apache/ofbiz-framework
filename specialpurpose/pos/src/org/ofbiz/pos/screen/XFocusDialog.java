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

//package org.ofbiz.pos.screen;
//
//import javax.swing.SwingUtilities;
//import java.awt.Component;
//import net.xoetrope.swing.XEdit;
//import net.xoetrope.swing.XDialog;
//
//import org.ofbiz.base.util.Debug;
//
//public class XFocusDialog extends XDialog {
//    
//    protected XEdit m_focused = null;
//
//    public XFocusDialog() {
//        super();
//    }
//    
//    public void pageActivated() {
//        super.pageActivated();
//        setFocus();
//    }
//    
//    public void setFocus(){
//        SwingUtilities.invokeLater( 
//            new Runnable() {
//                public void run(){
//                    Debug.logInfo( "isEditable in setFocus :" + m_focused.isEditable(), "======================================" );
//                    Debug.logInfo( "isEnabled in setFocus: " + m_focused.isEnabled(), "======================================" );
//                    Debug.logInfo( "isFocusable in setFocus :" + m_focused.isFocusable(), "======================================" );                    
//                    m_focused.requestFocusInWindow();
//                }
//            }      
//        );
//    }
//
//    /**
//     * @param m_focused the m_focused to set
//     */
//    public void setM_focused(XEdit focused) {
//        Debug.logInfo( "isEditable in setM_focused :" + focused.isEditable(), "======================================" );
//        Debug.logInfo( "isEnabled in setM_focused :" + focused.isEnabled(), "======================================" );
//        Debug.logInfo( "isFocusable in setM_focused :" + focused.isFocusable(), "======================================" );                            
//        this.m_focused = focused;
//    }
//}
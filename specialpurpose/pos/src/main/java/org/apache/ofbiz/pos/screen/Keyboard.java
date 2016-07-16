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
package org.apache.ofbiz.pos.screen;

import java.awt.Color;
import java.util.Locale;

import net.xoetrope.swing.XButton;
import net.xoetrope.swing.XDialog;
import net.xoetrope.swing.XEdit;
import net.xoetrope.xui.PageSupport;
import net.xoetrope.xui.XPage;
import net.xoetrope.xui.events.XEventHelper;

import org.apache.ofbiz.base.util.UtilProperties;
import org.apache.ofbiz.pos.PosTransaction;

@SuppressWarnings("serial")
public class Keyboard extends XPage {
    public static final String module = Keyboard.class.getName();

    protected XEdit m_edit = null;
    protected XDialog m_dialog = null;
    protected PosScreen m_pos = null;
    protected PageSupport m_pageSupport = null;

    private String originalText;
    private boolean m_shift = false;
    private boolean m_shiftLock = false;
    private static Locale locale = Locale.getDefault();


    public Keyboard(PosScreen pos) {
        m_pos = pos;
        if (locale.toString().contains("fr")) {
            m_pageSupport = pageMgr.loadPage(m_pos.getScreenLocation() + "/dialog/keyboard_fr");
        } else {
            m_pageSupport = pageMgr.loadPage(m_pos.getScreenLocation() + "/dialog/keyboard");
        }
        m_dialog = (XDialog) m_pageSupport;
        m_edit = (XEdit) m_pageSupport.findComponent("keyboard_input");
        m_edit.setText("");
        m_dialog.setCaption(UtilProperties.getMessage(PosTransaction.resource, "PosVirtualKeyboardTitle", Locale.getDefault()));
    }

    public String openDlg() {
        setupEvents();
        originalText = getText();
//      XuiUtilities.getMaxCoordinates(m_dialog);
//      Panel m_panel = m_dialog.PANEL;
//      pageHelper.componentFactory.setParentComponent(this);
//      contentPanel = (XPanel)pageHelper.componentFactory.addComponent(XPage.PANEL, 0, 0, 800, 600);
//      FIXME XUI dialog boxes are hardcoded to a 800*600 max ! https://issues.apache.org/jira/browse/OFBIZ-1606?focusedCommentId=12614469#action_12614469
//      actually maxi seem to be 808*628 certainly due to margins(?)
        m_dialog.pack();
        m_dialog.showDialog(this);
        return m_edit.getText();
    }

    // call before openDlg
    public void setText(String text) {
        clear();
        m_edit.setText(text);
    }

    public String getText() {
        return m_edit.getText();
    }

    private void reverseButtonColors(String button) {
        XButton xbutton = (XButton) m_dialog.findComponent(button);
        Color fore = xbutton.getForeground();
        Color back = xbutton.getBackground();
        xbutton.setForeground(back);
        xbutton.setBackground(fore);
        return;
    }

    private void setupEvents() {
        String[] keys = {"A" ,"B" ,"C" ,"D" ,"E" ,"F" ,"G" ,"H" ,"I" ,"J" ,"K" ,"L" ,"M" ,"N" ,"O" ,"P" ,"Q" ,"R" ,"S" ,"T" ,"U" ,"V" ,"W" ,"X" ,"Y" ,"Z",
                "1" ,"2" ,"3" ,"4" ,"5" ,"6" ,"7" ,"8" ,"9" ,"0" , "At",
                "Dot", "Dash", "Del", "Space", "Clear", "Enter", "Cancel", "Shift", "ShiftLock"};
        XButton button = null;
        for (String key : keys) {
            button = (XButton) m_dialog.findComponent("char" + key);
            XEventHelper.addMouseHandler(this, button, "trigger" + key);
        }
    }

    private void setButtonText(String buttonName, String newText) {
        XButton button = (XButton) m_dialog.findComponent(buttonName);
        button.setText(newText);
    }

    private void setUppercase() {
        String[] keys = {"A" ,"B" ,"C" ,"D" ,"E" ,"F" ,"G" ,"H" ,"I" ,"J" ,"K" ,"L" ,"M" ,"N" ,"O" ,"P" ,"Q" ,"R" ,"S" ,"T" ,"U" ,"V" ,"W" ,"X" ,"Y" ,"Z"};
        for (String key : keys) {
            setButtonText("char" + key, key);
        }
    }

    private void setLowercase() {

        String[] keys = {"a" ,"b" ,"c" ,"d" ,"e" ,"f" ,"g" ,"h" ,"i" ,"j" ,"k" ,"l" ,"m" ,"n" ,"o" ,"p" ,"q" ,"r" ,"s" ,"t" ,"u" ,"v" ,"w" ,"x" ,"y" ,"z"};
        for (String key : keys) {
            setButtonText("char" + key, key);
        }
    }

    public void triggerA() {
        keypress('a', 'A');
    }

    public void triggerB() {
        keypress('b', 'B');
    }

    public void triggerC() {
        keypress('c', 'C');
    }

    public void triggerD() {
        keypress('d', 'D');
    }

    public void triggerE() {
        keypress('e', 'E');
    }

    public void triggerF() {
        keypress('f', 'F');
    }

    public void triggerG() {
        keypress('g', 'G');
    }

    public void triggerH() {
        keypress('h', 'H');
    }

    public void triggerI() {
        keypress('i', 'I');
    }

    public void triggerJ() {
        keypress('j', 'J');
    }

    public void triggerK() {
        keypress('k', 'K');
    }

    public void triggerL() {
        keypress('l', 'L');
    }

    public void triggerM() {
        keypress('m', 'M');
    }

    public void triggerN() {
        keypress('n', 'N');
    }

    public void triggerO() {
        keypress('o', 'O');
    }

    public void triggerP() {
        keypress('p', 'P');
    }

    public void triggerQ() {
        keypress('q', 'Q');
    }

    public void triggerR() {
        keypress('r', 'R');
    }

    public void triggerS() {
        keypress('s', 'S');
    }

    public void triggerT() {
        keypress('t', 'T');
    }

    public void triggerU() {
        keypress('u', 'U');
    }

    public void triggerV() {
        keypress('v', 'V');
    }

    public void triggerW() {
        keypress('w', 'W');
    }

    public void triggerX() {
        keypress('x', 'X');
    }

    public void triggerY() {
        keypress('y', 'Y');
    }

    public void triggerZ() {
        keypress('z', 'Z');
    }

    public void trigger1() {
        triggerAndAppend("1");
    }

    public void trigger2() {
        triggerAndAppend("2");
    }

    public void trigger3() {
        triggerAndAppend("3");
    }

    public void trigger4() {
        triggerAndAppend("4");
    }

    public void trigger5() {
        triggerAndAppend("5");
    }

    public void trigger6() {
        triggerAndAppend("6");
    }

    public void trigger7() {
        triggerAndAppend("7");
    }

    public void trigger8() {
        triggerAndAppend("8");
    }

    public void trigger9() {
        triggerAndAppend("9");
    }

    public void trigger0() {
        triggerAndAppend("0");
    }

    public void triggerDot() {
        triggerAndAppend(".");
    }

    public void triggerDash() {
        triggerAndAppend("-");
    }

    public void triggerSpace() {
        triggerAndAppend(" ");
    }

    public void triggerAt() {
        triggerAndAppend("@");
    }

    public void triggerClear() {
        clear();
    }

    public void triggerDel() {
        delete();
    }

    public void triggerEnter() {
        close();
    }

    public void triggerCancel() {
        cancel();
    }

    public void triggerShift() {
        if (wasMouseClicked()) {
            shift();
        }
    }

    public void triggerShiftLock() {
        if (wasMouseClicked()) {
            shiftLock();
        }
    }

    public void triggerAndAppend(String s) {
        if (wasMouseClicked()) {
            append(s);
        }
    }

    private synchronized void keypress(char lower, char upper) {
        if (wasMouseClicked()) {
            if (m_shiftLock) {
                if (m_shift) {
                    append(lower);
                    shift();
                } else {
                    append(upper);
                }
            } else {
                if (m_shift) {
                    append(upper);
                    shift();
                } else {
                    append(lower);
                }
            }
        }
    }

    private synchronized void shiftLock() {
        if (m_shiftLock) {
            m_shiftLock = false;
            this.setLowercase();
        } else {
            m_shiftLock = true;
            this.setUppercase();
        }
        if (m_shift) { // turn off shift button
            m_shift = false;
            reverseButtonColors("menuShift");
        }

        reverseButtonColors("menuShiftLock");
        m_dialog.repaint();
        return;
    }

    private synchronized void shift() {
        if (m_shiftLock) {
            if (m_shift) {
                m_shift = false;
                this.setUppercase();
            } else {
                m_shift = true;
                this.setLowercase();
            }
        } else {
            if (m_shift) {
                m_shift = false;
                this.setLowercase();
            } else {
                m_shift = true;
                this.setUppercase();
            }
        }

        reverseButtonColors("menuShift");
        m_dialog.repaint();
        return;
    }

    private synchronized void close() {
        if (wasMouseClicked()) {
            m_dialog.closeDlg();
            return;
        }
    }

    private synchronized void clear() {
        if (wasMouseClicked()) {
            String text = "";
            m_edit.setText(text);
            m_dialog.repaint();
            return;
        }
    }

    private synchronized void cancel() {
        if (wasMouseClicked()) {
            this.setText(originalText);
            m_dialog.closeDlg();
            return;
        }
    }

    private synchronized void append(char c) {
        String text = "";
        try {
            text = m_edit.getText();
        } catch (NullPointerException e) {
            // getText throws exception if no text
            text = "";
        } finally {
            m_edit.setText(text + c);
        }
        m_dialog.repaint();
        return;
    }

    private synchronized void append(String c) {
        String text = "";
        try {
            text = m_edit.getText();
        } catch (NullPointerException e) {
            // getText throws exception if no text
            text = "";
        } finally {
            m_edit.setText(text + c);
        }
        m_dialog.repaint();
        return;
    }

    private synchronized void delete() {
        if (wasMouseClicked()) {
            String text = "";
            try {
                text = m_edit.getText();
            } catch (NullPointerException e) {
                // getText throws exception if no text
                text = "";
            } finally {
                if (text.length() > 1) {
                    m_edit.setText(text.substring(0, text.length() - 1));
                } else {
                    m_edit.setText("");
                }
            }
            m_dialog.repaint();
            return;
        }
    }
}

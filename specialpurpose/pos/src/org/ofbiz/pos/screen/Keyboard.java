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

import java.awt.Color;
import net.xoetrope.swing.XButton;
import net.xoetrope.swing.XDialog;
import net.xoetrope.swing.XEdit;
import net.xoetrope.xui.PageSupport;
import net.xoetrope.xui.XPage;
import net.xoetrope.xui.events.XEventHelper;

public class Keyboard extends XPage {
    public static final String module = Keyboard.class.getName();

    XEdit m_edit = null;
    XDialog m_dialog = null;
    PosScreen m_pos = null;
    PageSupport m_pageSupport = null;

    String originalText;
    boolean m_shift = false;
    boolean m_shiftLock = false;

    public Keyboard(PosScreen pos) {
        m_pos = pos;
        m_pageSupport = pageMgr.loadPage(m_pos.getScreenLocation() + "/dialog/keyboard");
        m_dialog = (XDialog) m_pageSupport;
        m_edit = (XEdit) m_pageSupport.findComponent("keyboard_input");
        m_edit.setText("");
    }

    public String openDlg() {
        setupEvents();
        originalText = getText();
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
        XButton button = (XButton) m_dialog.findComponent("charA");
        XEventHelper.addMouseHandler(this, button, "triggerA");
        button = (XButton) m_dialog.findComponent("charB");
        XEventHelper.addMouseHandler(this, button, "triggerB");
        button = (XButton) m_dialog.findComponent("charC");
        XEventHelper.addMouseHandler(this, button, "triggerC");
        button = (XButton) m_dialog.findComponent("charD");
        XEventHelper.addMouseHandler(this, button, "triggerD");
        button = (XButton) m_dialog.findComponent("charE");
        XEventHelper.addMouseHandler(this, button, "triggerE");
        button = (XButton) m_dialog.findComponent("charF");
        XEventHelper.addMouseHandler(this, button, "triggerF");
        button = (XButton) m_dialog.findComponent("charG");
        XEventHelper.addMouseHandler(this, button, "triggerG");
        button = (XButton) m_dialog.findComponent("charH");
        XEventHelper.addMouseHandler(this, button, "triggerH");
        button = (XButton) m_dialog.findComponent("charI");
        XEventHelper.addMouseHandler(this, button, "triggerI");
        button = (XButton) m_dialog.findComponent("charJ");
        XEventHelper.addMouseHandler(this, button, "triggerJ");
        button = (XButton) m_dialog.findComponent("charK");
        XEventHelper.addMouseHandler(this, button, "triggerK");
        button = (XButton) m_dialog.findComponent("charL");
        XEventHelper.addMouseHandler(this, button, "triggerL");
        button = (XButton) m_dialog.findComponent("charM");
        XEventHelper.addMouseHandler(this, button, "triggerM");
        button = (XButton) m_dialog.findComponent("charN");
        XEventHelper.addMouseHandler(this, button, "triggerN");
        button = (XButton) m_dialog.findComponent("charO");
        XEventHelper.addMouseHandler(this, button, "triggerO");
        button = (XButton) m_dialog.findComponent("charP");
        XEventHelper.addMouseHandler(this, button, "triggerP");
        button = (XButton) m_dialog.findComponent("charQ");
        XEventHelper.addMouseHandler(this, button, "triggerQ");
        button = (XButton) m_dialog.findComponent("charR");
        XEventHelper.addMouseHandler(this, button, "triggerR");
        button = (XButton) m_dialog.findComponent("charS");
        XEventHelper.addMouseHandler(this, button, "triggerS");
        button = (XButton) m_dialog.findComponent("charT");
        XEventHelper.addMouseHandler(this, button, "triggerT");
        button = (XButton) m_dialog.findComponent("charU");
        XEventHelper.addMouseHandler(this, button, "triggerU");
        button = (XButton) m_dialog.findComponent("charV");
        XEventHelper.addMouseHandler(this, button, "triggerV");
        button = (XButton) m_dialog.findComponent("charW");
        XEventHelper.addMouseHandler(this, button, "triggerW");
        button = (XButton) m_dialog.findComponent("charX");
        XEventHelper.addMouseHandler(this, button, "triggerX");
        button = (XButton) m_dialog.findComponent("charY");
        XEventHelper.addMouseHandler(this, button, "triggerY");
        button = (XButton) m_dialog.findComponent("charZ");
        XEventHelper.addMouseHandler(this, button, "triggerZ");
        button = (XButton) m_dialog.findComponent("charDel");
        XEventHelper.addMouseHandler(this, button, "triggerDel");
        button = (XButton) m_dialog.findComponent("charSpace");
        XEventHelper.addMouseHandler(this, button, "triggerSpace");
        button = (XButton) m_dialog.findComponent("menuClear");
        XEventHelper.addMouseHandler(this, button, "triggerClear");
        button = (XButton) m_dialog.findComponent("menuEnter");
        XEventHelper.addMouseHandler(this, button, "triggerEnter");
        button = (XButton) m_dialog.findComponent("menuCancel");
        XEventHelper.addMouseHandler(this, button, "triggerCancel");
        button = (XButton) m_dialog.findComponent("menuShift");
        XEventHelper.addMouseHandler(this, button, "triggerShift");
        button = (XButton) m_dialog.findComponent("menuShiftLock");
        XEventHelper.addMouseHandler(this, button, "triggerShiftLock");
    }

    private void setButtonText(String buttonName, String newText) {
        XButton button = (XButton) m_dialog.findComponent(buttonName);
        button.setText(newText);
    }

    private void setUppercase() {
        setButtonText("charA", "A");
        setButtonText("charB", "B");
        setButtonText("charC", "C");
        setButtonText("charD", "D");
        setButtonText("charE", "E");
        setButtonText("charF", "F");
        setButtonText("charG", "G");
        setButtonText("charH", "H");
        setButtonText("charI", "I");
        setButtonText("charJ", "J");
        setButtonText("charK", "K");
        setButtonText("charL", "L");
        setButtonText("charM", "M");
        setButtonText("charN", "N");
        setButtonText("charO", "O");
        setButtonText("charP", "P");
        setButtonText("charQ", "Q");
        setButtonText("charR", "R");
        setButtonText("charS", "S");
        setButtonText("charT", "T");
        setButtonText("charU", "U");
        setButtonText("charV", "V");
        setButtonText("charW", "W");
        setButtonText("charX", "X");
        setButtonText("charY", "Y");
        setButtonText("charZ", "Z");
    }

    private void setLowercase() {
        setButtonText("charA", "a");
        setButtonText("charB", "b");
        setButtonText("charC", "c");
        setButtonText("charD", "d");
        setButtonText("charE", "e");
        setButtonText("charF", "f");
        setButtonText("charG", "g");
        setButtonText("charH", "h");
        setButtonText("charI", "i");
        setButtonText("charJ", "j");
        setButtonText("charK", "k");
        setButtonText("charL", "l");
        setButtonText("charM", "m");
        setButtonText("charN", "n");
        setButtonText("charO", "o");
        setButtonText("charP", "p");
        setButtonText("charQ", "q");
        setButtonText("charR", "r");
        setButtonText("charS", "s");
        setButtonText("charT", "t");
        setButtonText("charU", "u");
        setButtonText("charV", "v");
        setButtonText("charW", "w");
        setButtonText("charX", "x");
        setButtonText("charY", "y");
        setButtonText("charZ", "z");
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

    public void triggerClear() {
        clear();
    }

    public void triggerSpace() {
        keypress(' ', ' ');
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

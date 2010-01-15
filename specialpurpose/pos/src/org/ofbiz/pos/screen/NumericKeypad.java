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

import java.util.Locale;

import org.ofbiz.base.util.UtilProperties;
import org.ofbiz.pos.PosTransaction;

import net.xoetrope.swing.XButton;
import net.xoetrope.swing.XDialog;
import net.xoetrope.swing.XEdit;
import net.xoetrope.xui.PageSupport;
import net.xoetrope.xui.XPage;
import net.xoetrope.xui.events.XEventHelper;

public class NumericKeypad extends XPage
{
    public static final String module = NumericKeypad.class.getName();

    XEdit m_edit = null;
    XDialog m_dialog = null;
    PosScreen m_pos = null;
    PageSupport m_pageSupport = null;

    boolean m_minus = false;
    boolean m_percent = false;
    String originalText;

    public NumericKeypad(PosScreen pos) {
        m_pos = pos;
        m_pageSupport = pageMgr.loadPage(m_pos.getScreenLocation() + "/dialog/numerickeypad");
        m_dialog = (XDialog) m_pageSupport;
        m_edit = (XEdit) m_pageSupport.findComponent("numeric_input");
        m_edit.setText("");
        m_dialog.setCaption(UtilProperties.getMessage(PosTransaction.resource, "PosVirtualNumPadTitle", Locale.getDefault()));

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

    //call before openDlg
    public void setMinus(boolean minus) {
        m_minus = minus;
    }

    public boolean getMinus() {
        return m_minus;
    }

    //call before openDlg
    public void setPercent(boolean percent) {
        if (percent) {
            disableButton("menuCancel");
        }
        m_percent = percent;
    }

    public boolean getPercent() {
        return m_percent;
    }

    private void disableButton(String button) {
        XButton xbutton = (XButton) m_dialog.findComponent(button);
        xbutton.setVisible(false);
    }

    private void enableButton(String button) {
        XButton xbutton = (XButton) m_dialog.findComponent(button);
        xbutton.setVisible(true);
    }

    private void setupEvents() {
        XButton button = (XButton) m_dialog.findComponent("numOne");
        XEventHelper.addMouseHandler(this, button, "triggerOne");
        button = (XButton) m_dialog.findComponent("numTwo");
        XEventHelper.addMouseHandler(this, button, "triggerTwo");
        button = (XButton) m_dialog.findComponent("numThree");
        XEventHelper.addMouseHandler(this, button, "triggerThree");
        button = (XButton) m_dialog.findComponent("numFour");
        XEventHelper.addMouseHandler(this, button, "triggerFour");
        button = (XButton) m_dialog.findComponent("numFive");
        XEventHelper.addMouseHandler(this, button, "triggerFive");
        button = (XButton) m_dialog.findComponent("numSix");
        XEventHelper.addMouseHandler(this, button, "triggerSix");
        button = (XButton) m_dialog.findComponent("numSeven");
        XEventHelper.addMouseHandler(this, button, "triggerSeven");
        button = (XButton) m_dialog.findComponent("numEight");
        XEventHelper.addMouseHandler(this, button, "triggerEight");
        button = (XButton) m_dialog.findComponent("numNine");
        XEventHelper.addMouseHandler(this, button, "triggerNine");
        button = (XButton) m_dialog.findComponent("numZero");
        XEventHelper.addMouseHandler(this, button, "triggerZero");
        button = (XButton) m_dialog.findComponent("numDZero");
        XEventHelper.addMouseHandler(this, button, "triggerDoubleZero");
        button = (XButton) m_dialog.findComponent("menuClear");
        XEventHelper.addMouseHandler(this, button, "triggerClear");
        button = (XButton) m_dialog.findComponent("menuEnter");
        XEventHelper.addMouseHandler(this, button, "triggerEnter");
        button = (XButton) m_dialog.findComponent("menuCancel");
        XEventHelper.addMouseHandler(this, button, "triggerCancel");

        if (getMinus()) {
            button = (XButton) m_dialog.findComponent("numMinus");
            XEventHelper.addMouseHandler(this, button, "triggerMinus");
        } else {
            disableButton("numMinus");
        }
        if (getPercent()) {
            button = (XButton) m_dialog.findComponent("numPercent");
            XEventHelper.addMouseHandler(this, button, "triggerMinus");
        } else {
            disableButton("numPercent");
        }

        return;
    }

    public void triggerOne()
    {
        append('1');
    }

    public void triggerTwo()
    {
        append('2');
    }

    public void triggerThree()
    {
        append('3');
    }

    public void triggerFour()
    {
        append('4');
    }

    public void triggerFive()
    {
        append('5');
    }

    public void triggerSix()
    {
        append('6');
    }

    public void triggerSeven()
    {
        append('7');
    }

    public void triggerEight()
    {
        append('8');
    }

    public void triggerNine()
    {
        append('9');
    }

    public void triggerZero()
    {
        append('0');
    }

    public void triggerDoubleZero()
    {
        append("00");
    }

    public void triggerClear()
    {
        clear();
    }

    public void triggerEnter()
    {
        close();
    }

    public void triggerCancel()
    {
        cancel();
    }

    public void triggerMinus()
    {
        prependUnique('-');
    }

    public void triggerPercent()
    {
        prependUnique('%');
    }

    private synchronized void prependUnique(char c) {
        if (wasMouseClicked()) {
            String text = "";
            try {
                text = m_edit.getText();
            } catch (NullPointerException e) {
                // getText throws exception if no text
                text = "";
            } finally {
                text=c+text;
            }
            if (countChars(text, c) > 1) {
                text = stripChars(text, c);
            }
            m_edit.setText(text);

            m_dialog.repaint();
            return;
        }
    }

    private int countChars(String string, char c) {
        int count = 0;
        for(int i=0; i<string.length(); i++) {
            if (string.charAt(i) == c) {
                count++;
            }
        }
        return count;
    }

    private String stripChars(String string, char c) {
        StringBuffer buf = new StringBuffer();
        for(int i=0; i<string.length(); i++) {
            char current = string.charAt(i);
            if (current != c) {
                buf.append(current);
            }
        }
        return buf.toString();
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

    private synchronized void append(char c) {
        if (wasMouseClicked()) {
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
            //update the screen?
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

    private synchronized void append(String c) {
        if (wasMouseClicked()) {
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
            //update the screen?
            return;
        }
    }
}
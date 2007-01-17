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
import java.awt.Component;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.EmptyStackException;
import java.util.Iterator;
import java.util.Stack;

import net.xoetrope.swing.XEdit;

import org.ofbiz.pos.adaptor.KeyboardAdaptor;
import org.ofbiz.pos.adaptor.KeyboardReceiver;
import org.ofbiz.pos.screen.PosScreen;

public class Input implements KeyboardReceiver, KeyListener {

    public static final String module = Input.class.getName();
    private static final String[] validFunc = { "LOGIN", "OPEN", "CLOSE", "UNLOCK", "MGRLOGIN", "PAID", "TOTAL",
                                                "CREDIT", "GIFTCARD", "MSRINFO", "CHECK", "CHECKINFO", "REFNUM",
                                                "QTY", "VOID", "SHIFT", "PAID_IN" , "PAID_OUT" };

    protected Stack functionStack = new Stack();
    protected Component[] pageComs = null;
    protected Color lastColor = null;
    protected XEdit input = null;
    protected boolean isLocked = false;

    public Input(PosScreen page) {
        this.input = (XEdit) page.findComponent("pos_input");
        this.input.setVisible(true);
        this.input.setFocusable(false);

        // initialize the KeyboardAdaptor
        KeyboardAdaptor.getInstance(this, KeyboardAdaptor.KEYBOARD_DATA);
    }

    public void focus() {
        this.input.requestFocus();
    }

    public void setLock(boolean lock) {
        // hide the input text
        if (lock) {
            //lastColor = this.input.getForeground();
            //input.setForeground(this.input.getBackground());
        } else {
            //input.setForeground(this.lastColor);
        }
        isLocked = lock;
    }

    public void setFunction(String function, String value) throws IllegalArgumentException {
        if (isValidFunction(function)) {
            this.functionStack.push( new String[] { function, value });
            input.setText("");
        } else {
            throw new IllegalArgumentException();
        }
    }

    public void setFunction(String function) throws IllegalArgumentException {
        setFunction(function, input.getText());
    }

    private boolean isValidFunction(String function) {
        for (int i = 0; i < validFunc.length; i++) {
            if (validFunc[i].equals(function)) {
                return true;
            }
        }
        return false;
    }

    public String[] getLastFunction() {
        String[] f = null;
        try {
            f = (String[]) this.functionStack.peek();
        } catch (EmptyStackException e) {
        }
        return f;
    }

    public String[] clearLastFunction() {
        String[] f = null;
        try {
            f = (String[]) this.functionStack.pop();
        } catch (EmptyStackException e) {
        }
        return f;
    }

    public String[] getFunction(String function) {
        Iterator i = functionStack.iterator();
        while (i.hasNext()) {
            String[] func = (String[]) i.next();
            if (func[0].equals(function)) {
                return func;
            }
        }
        return null;
    }

    public String[] clearFunction(String function) {
        Iterator i = functionStack.iterator();
        while (i.hasNext()) {
            String[] func = (String[]) i.next();
            if (func[0].equals(function)) {
                i.remove();
                return func;
            }
        }
        return null;
    }

    public boolean isFunctionSet(String function) {
        Iterator i = functionStack.iterator();
        while (i.hasNext()) {
            String func[] = (String[]) i.next();
            if (func[0].equals(function)) {
                return true;
            }
        }
        return false;
    }

    public void clearInput() {
        input.setText("");
    }

    public void clear() {
        input.setText("");
        functionStack.clear();
    }

    public String value() {
        return input.getText();
    }

    public void appendChar(char c) {
        input.setText(this.input.getText() + c);
    }

    public void appendString(String str) {
        input.setText(this.input.getText() + str);
    }

    public void stripLastChar() {
        if (this.value().length() > 0) {
            this.input.setText(this.value().substring(0, this.value().length() - 1));
        }
    }

    // KeyboardReceiver
    public synchronized void receiveData(int[] codes, char[] chars) {
        if (chars.length > 0 && checkChars(chars))
            this.appendString(new String(chars));
    }

    // KeyListener
    public void keyPressed(KeyEvent event) {
        // implements to handle backspacing only
        if (event.getKeyCode() == 8 && this.value().length() > 0) {
            this.input.setText(this.value().substring(0, this.value().length() - 1));
        } else if (event.getKeyCode() == 27 && this.value().length() > 0) {
            this.input.setText("");
        }
    }

    public void keyTyped(KeyEvent event) {
    }

    public void keyReleased(KeyEvent event) {
    }

    private boolean checkChars(char[] chars) {
        int[] idxToRemove = new int[chars.length];
        boolean process = false;
        int remIdx = 0;
        for (int i = 0; i < chars.length; i++) {
            if (((int) chars[i]) == 10 || ((int) chars[i]) == 8 || ((int) chars[i] == 27)) {
                idxToRemove[remIdx++] = i+1;
            } else {
                process = true;
            }
        }

        if (chars.length == 1) {
            return process;
        }

        int toRemove = 0;
        for (int i = 0; i < idxToRemove.length; i++) {
            if (idxToRemove[i] > 0) {
                toRemove++;
            }
        }

        if (toRemove > 0) {
            if (chars.length - toRemove < 1) {
                return false;
            }

            char[] newChars = new char[chars.length - toRemove];
            int currentIndex = 0;
            for (int i = 0; i < chars.length; i++) {
                boolean appendChar = true;
                for (int x = 0; x < idxToRemove.length; x++) {
                    if ((idxToRemove[x] - 1) == i) {
                        appendChar = false;
                        continue;
                    } else {
                    }
                }
                if (appendChar) {
                    newChars[currentIndex] = chars[i];
                    currentIndex++;
                }
            }
        }

        return process;
    }
}

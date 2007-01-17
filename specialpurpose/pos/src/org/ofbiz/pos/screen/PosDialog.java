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

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Point;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.awt.event.FocusListener;
import java.awt.event.FocusEvent;
import java.awt.event.ComponentListener;
import java.awt.event.ComponentEvent;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JRootPane;
import javax.swing.SwingUtilities;

import net.xoetrope.swing.XButton;
import net.xoetrope.swing.XTextArea;
import net.xoetrope.xui.XPage;
import net.xoetrope.xui.XProjectManager;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.cache.UtilCache;

public class PosDialog {

    public static final String module = PosDialog.class.getName();
    protected static UtilCache instances = new UtilCache("pos.Dialogs", 0, 0);

    protected final Frame clientFrame = XProjectManager.getCurrentProject().getAppFrame();
    protected final Window appWindow = XProjectManager.getCurrentProject().getAppWindow();

    protected DialogCallback cb = null;
    protected Component parent = null;

    protected JDialog dialog = null;
    protected XTextArea output = null;
    protected XButton closeBtn = null;
    protected XPage page = null;
    protected boolean modal = true;
    protected int padding = 0;
    protected boolean posDialogVisible = false;

    public static PosDialog getInstance(XPage page) {
        return getInstance(page, true, 0);
    }

    public static PosDialog getInstance(XPage page, boolean modal, int padding) {
        PosDialog dialog = (PosDialog) instances.get(page);
        if (dialog == null) {
            synchronized(PosDialog.class) {
                dialog = (PosDialog) instances.get(page);

                if (dialog == null) {
                    dialog = new PosDialog(page, modal, padding);
                    instances.put(page, dialog);
                }
            }
        }

        dialog.modal = modal;
        dialog.padding = padding;
        dialog.pack();
        return dialog;
    }

    protected PosDialog(XPage page, boolean modal, int padding) {
        this.page = page;
        this.modal = modal;
        this.padding = padding;
        this.configure();
    }

    protected void configure() {
        // create the new dialog box
        this.dialog = new JDialog(clientFrame, "Alert", modal);
        dialog.setUndecorated(true);
        dialog.setResizable(false);
        dialog.setSize(page.getSize());
        dialog.getRootPane().setWindowDecorationStyle(JRootPane.PLAIN_DIALOG);

        // find the output edit object
        this.output = (XTextArea) page.findComponent("dialog_output");
        if (this.output != null) {
            this.output.setWrapStyleWord(true);
            this.output.setLineWrap(true);
            this.output.setEditable(false);
        }

        // set the page pieces
        Component[] coms = page.getComponents();
        for (int i = 0; i < coms.length; i++) {
            dialog.getContentPane().add(coms[i]);
            coms[i].setVisible(true);
        }

        // set the close button
        this.setCloseBtn(dialog);

        // fix the layout and size
        this.pack();

        // adjust the dialog location
        Dimension wSize = dialog.getSize();
        dialog.setLocation(appWindow.getLocation().x + (appWindow.getSize().width / 2 - wSize.width / 2),
                appWindow.getLocation().y + (appWindow.getSize().height / 2 - wSize.height / 2));

        // set the component listener
        final PosDialog  thisPosDialog = this;
        dialog.addComponentListener(new ComponentListener() {

            public void componentResized(ComponentEvent event) {
                this.reset();
            }

            public void componentMoved(ComponentEvent event) {
                this.reset();
            }

            public void componentShown(ComponentEvent event) {
                this.reset();
            }

            public void componentHidden(ComponentEvent event) {
                this.reset();
            }

            public void reset() {
                if (dialog.isEnabled()) {                    
                    thisPosDialog.checkSize();
                    Dimension wSize = dialog.getSize();
                    dialog.setLocation(appWindow.getLocation().x + (appWindow.getSize().width / 2 - wSize.width / 2),
                    appWindow.getLocation().y + (appWindow.getSize().height / 2 - wSize.height / 2));
                    dialog.requestFocus();
                }
            }
        });

        // set the window listener
        dialog.addWindowListener(new WindowListener() {
            public void windowClosing(WindowEvent e) {
            }

            public void windowActivated(WindowEvent e) {
            }

            public void windowDeactivated(WindowEvent e) {
                this.reset();
            }

            public void windowClosed(WindowEvent e) {
            }

            public void windowOpened(WindowEvent e) {
            }

            public void windowDeiconified(WindowEvent e) {
            }

            public void windowIconified(WindowEvent e) {
            }

            public void reset() {
                // always keep focus if we are enabled
                if (dialog.isEnabled()) {
                    dialog.requestFocus();
                }
            }
        });

        // set the focus listener
        dialog.addFocusListener(new FocusListener() {

            public void focusGained(FocusEvent event) {
            }

            public void focusLost(FocusEvent event) {
                if (dialog.isEnabled()) {
                    Component focused = event.getOppositeComponent();
                    if (focused == null || !"closeBtn".equals(focused.getName())) {
                        dialog.requestFocus();
                    }
                }
            }
        });
    }

    public void showDialog(Container parent, DialogCallback cb, String text) {
        this.parent = parent;
        this.cb = cb;
        if (text != null) {
            this.setText(text);
        }

        // don't allow the main window to take focus
        appWindow.setFocusable(false);
        parent.setFocusable(false);

        dialog.setFocusable(true);
        dialog.setEnabled(true);
        dialog.requestFocus();
        dialog.repaint();
        dialog.pack();
        posDialogSetVisible(true);
    }

    public void setText(String text) {
        if (this.output != null) {
            this.output.setText(text);
        } else if (this.closeBtn != null) {
            this.closeBtn.setText("<html><center>" + text + "</center></html>");
        } else {
            Debug.log("PosDialog output edit box is NULL!", module);
        }
    }

    public String getName() {
        return page.getName();
    }

    protected void close() {
        // close down the dialog
        dialog.setEnabled(false);
        dialog.setVisible(false);
        dialog.setFocusable(false);

        // refocus the parent window
        appWindow.setFocusable(true);
        appWindow.requestFocus();
        parent.setFocusable(true);
        parent.requestFocus();

        // callback the parent
        if (cb != null) {
            cb.receiveDialogCb(this);
        }
    }

    private void setCloseBtn(Container con) {
        Component[] coms = con.getComponents();
        for (int i = 0; i < coms.length; i++) {
            if (coms[i].getName() != null && "closeBtn".equals(coms[i].getName())) {
                if (coms[i] instanceof XButton) {
                    this.closeBtn = (XButton) coms[i];
                    JButton b = (JButton) coms[i];
                    b.addActionListener(new ActionListener() {
                        public void actionPerformed(ActionEvent event) {
                            dialog.setEnabled(false);
                            close();
                        }
                    });
                } else {
                    Debug.logWarning("Found component with name 'closeBtn' but was not an instance of JButton", module);
                }
            } else if (coms[i] instanceof Container) {
                setCloseBtn((Container) coms[i]);
            } else {
                coms[i].requestFocus();
            }
        }
    }

    private void pack() {
        dialog.pack();

        Dimension pageSize = page.getSize();
        if (pageSize.getHeight() > 0 || pageSize.getWidth() > 0) {
            dialog.setSize(page.getSize());
        } else {
            Container contentPane = dialog.getContentPane();
            Point size = this.getMaxCoordinates(contentPane);
            this.setSize(size.x + 2 * padding + 2, size.y + 2 * padding + 4);
        }
    }

    private void checkSize() {
        Dimension wSize = dialog.getSize();

        Container contentPane = dialog.getContentPane();
        Point size = this.getMaxCoordinates(contentPane);
        size.x += 2 * padding + 2;
        size.y += 2 * padding + 4 + 2;
        if ( size.x != wSize.width || size.y != wSize.height ) {
            this.pack();
        }
    }

    private void setSize(int width, int height) {
        dialog.getContentPane().setBounds(padding, padding, width - (padding * 2), height - (padding * 2));
        dialog.setSize(width, height + 2);
    }

    private Point getMaxCoordinates(Container cont) {
        Point pt = cont.getLocation();

        int maxX = pt.x;
        int maxY = pt.y;
        int numChildren = cont.getComponentCount();

        for (int i = 0; i < numChildren; i++) {
            Component comp = cont.getComponent(i);
            Dimension size = comp.getSize();
            Point p = comp.getLocation();
            maxX = Math.max(pt.x + p.x + size.width, maxX);
            maxY = Math.max(pt.y + p.y + size.height, maxY);
            if (comp instanceof Container) {
                Point childDim = this.getMaxCoordinates((Container) comp);
                maxX = Math.max(childDim.x, maxX);
                maxY = Math.max(childDim.y, maxY);
            }
        }

        return new Point(maxX, maxY);
    }

      public void posDialogSetVisible(boolean visible){
      posDialogVisible = visible;
      SwingUtilities.invokeLater( 
          new Runnable() {
              public void run(){
                  dialog.setVisible(posDialogVisible);
              }
          }      
      );
    }
    
}

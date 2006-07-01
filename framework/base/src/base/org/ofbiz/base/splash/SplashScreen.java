/*
 * $Id: SplashScreen.java 5462 2005-08-05 18:35:48Z jonesde $
 *
 * Copyright (c) 2004 The Open For Business Project - www.ofbiz.org
 *
 * Permission is hereby granted, free of charge, to any person obtaining a
 * copy of this software and associated documentation files (the "Software"),
 * to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
 * OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY
 * CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT
 * OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR
 * THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 */
package org.ofbiz.base.splash;

import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.MediaTracker;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.Window;

public final class SplashScreen extends Frame {

    private final String fImageId;
    private MediaTracker fMediaTracker;
    private Window splashWindow;
    private Image fImage;

    public SplashScreen(String aImageId) {
        if (aImageId == null || aImageId.trim().length() == 0) {
            throw new IllegalArgumentException("Image Id does not have content.");
        }
        fImageId = aImageId;
    }

    public void splash() {
        initImageAndTracker();
        setSize(fImage.getWidth(null), fImage.getHeight(null));
        center();

        fMediaTracker.addImage(fImage, 0);
        try {
            fMediaTracker.waitForID(0);
        } catch (InterruptedException ie) {
            System.out.println("Cannot track image load.");
        }

        splashWindow = new SplashWindow(this, fImage);
    }

    public void close() {
        this.dispose();
        splashWindow.dispose();
        splashWindow = null;
    }

    private void initImageAndTracker() {
        fMediaTracker = new MediaTracker(this);
        fImage = Toolkit.getDefaultToolkit().getImage(fImageId);
    }
    
    private void center() {
        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        Rectangle frame = getBounds();
        setLocation((screen.width - frame.width) / 2, (screen.height - frame.height) / 2);
    }

    private class SplashWindow extends Window {

        private Image fImage;

        public SplashWindow(Frame aParent, Image aImage) {
            super(aParent);
            fImage = aImage;
            setSize(fImage.getWidth(null), fImage.getHeight(null));
            Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
            Rectangle window = getBounds();
            setLocation((screen.width - window.width) / 2, (screen.height - window.height) / 2);
            setVisible(true);
        }

        public void paint(Graphics graphics) {
            if (fImage != null) {
                graphics.drawImage(fImage, 0, 0, this);
            }
        }
    }
}

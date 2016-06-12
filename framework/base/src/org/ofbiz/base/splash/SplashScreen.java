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
package org.ofbiz.base.splash;

import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.MediaTracker;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.Window;

import org.ofbiz.base.util.UtilValidate;

@SuppressWarnings("serial")
public final class SplashScreen extends Frame {

    private final String fImageId;
    private MediaTracker fMediaTracker;
    private Window splashWindow;
    private Image fImage;

    public SplashScreen(String aImageId) {
        if (UtilValidate.isEmpty(aImageId)) {
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

        @Override
        public void paint(Graphics graphics) {
            if (fImage != null) {
                graphics.drawImage(fImage, 0, 0, this);
            }
        }
    }
}

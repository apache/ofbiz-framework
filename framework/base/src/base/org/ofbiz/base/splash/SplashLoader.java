/*
 * $Id: SplashLoader.java 5462 2005-08-05 18:35:48Z jonesde $
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

import java.awt.EventQueue;

import org.ofbiz.base.start.Start;
import org.ofbiz.base.start.StartupException;
import org.ofbiz.base.start.StartupLoader;

/**
 * 
 * @author     <a href="mailto:jaz@ofbiz.org">Andy Zeneski</a>
 * @version    $Rev$
 * @since      3.3
 */
public class SplashLoader implements StartupLoader, Runnable {

    public static final String module = SplashLoader.class.getName();
    private static SplashScreen screen = null;
    private Start.Config config = null;

    /**
     * Load a startup class
     *
     * @param config Startup config
     * @param args   Input arguments
     * @throws org.ofbiz.base.start.StartupException
     *
     */
    public void load(Start.Config config, String args[]) throws StartupException {
        this.config = config;

        Thread t = new Thread(this);
        t.setName(this.toString());
        t.setDaemon(false);
        t.run();
    }

    /**
     * Start the startup class
     *
     * @throws org.ofbiz.base.start.StartupException
     *
     */
    public void start() throws StartupException {
    }

    /**
     * Stop the container
     *
     * @throws org.ofbiz.base.start.StartupException
     *
     */
    public void unload() throws StartupException {
        SplashLoader.close();
    }

    public static SplashScreen getSplashScreen() {
        return screen;
    }

    public static void close() {
        if (screen != null) {
            EventQueue.invokeLater(new SplashScreenCloser());
        }
    }

    public void run() {
        if (config.splashLogo != null) {
            screen = new SplashScreen(config.splashLogo);
            screen.splash();
        }
    }

    private static final class SplashScreenCloser implements Runnable {
        public void run() {
            screen.close();
            screen = null;
        }
    }
}

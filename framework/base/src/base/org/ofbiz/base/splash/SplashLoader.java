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

import java.awt.EventQueue;

import org.ofbiz.base.start.Start;
import org.ofbiz.base.start.StartupException;
import org.ofbiz.base.start.StartupLoader;

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

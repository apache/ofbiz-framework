/*
 * Copyright 2007 Bruce Fancher
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/*
 * This file is a modified version of the original one.
 */

package org.ofbiz.base.container.groovy;

import groovy.lang.Binding;

import java.util.Map;

import org.apache.log4j.Logger;

public abstract class GroovyService {

    protected Logger log = Logger.getLogger(getClass());

    private Map<String, Object> bindings;
    private boolean launchAtStart;
    private Thread serverThread;

    public GroovyService() {
    }

    public GroovyService(Map<String, Object> bindings) {
        this.bindings = bindings;
    }

    public void launchInBackground() {
        serverThread = new Thread() {
            @Override
            public void run() {
                try {
                    launch();
                } catch (Exception e) {
                    log.error("Could not launch the Groovy service.", e);
                }
            }
        };

        serverThread.setDaemon(true);
        serverThread.start();
    }

    public abstract void launch();

    protected Binding createBinding() {
        return new Binding(bindings);
    }

    public void initialize() {
        if (launchAtStart) {
            launchInBackground();
        }
    }

    public void destroy() {}

    public void setBindings(final Map<String, Object> bindings) {
        this.bindings = bindings;
    }

    public boolean isLaunchAtStart() {
        return launchAtStart;
    }

    public void setLaunchAtStart(boolean launchAtStart) {
        this.launchAtStart = launchAtStart;
    }
}

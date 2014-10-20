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
package org.ofbiz.base.container;

import org.ofbiz.base.component.AlreadyLoadedException;
import org.ofbiz.base.component.ComponentException;
import org.ofbiz.base.util.Debug;

/**
 * A Container implementation to run the tests configured through this testtools stuff.
 */
public class JustLoadComponentsContainer implements Container {

    public static final String module = JustLoadComponentsContainer.class.getName();

    private String name;

    @Override
    public void init(String[] args, String name, String configFile) {
        this.name = name;
        try {
            ComponentContainer cc = new ComponentContainer();
            cc.loadComponents(null);
        } catch (AlreadyLoadedException e) {
            Debug.logError(e, module);
        } catch (ComponentException e) {
            Debug.logError(e, module);
            //throw UtilMisc.initCause(new ContainerException(e.getMessage()), e);
        }
    }

    public boolean start() throws ContainerException {
        return true;
    }

    public void stop() throws ContainerException {
    }

    public String getName() {
        return name;
    }
}

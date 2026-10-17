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
package org.apache.ofbiz.entity;

import java.util.List;

import org.apache.ofbiz.base.container.Container;
import org.apache.ofbiz.base.start.StartupCommand;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.entity.transaction.TransactionFactoryLoader;

/**
 * Closes the entity engine datasources when OFBiz shuts down.
 * <p>
 * An embedded database such as H2 must be closed for its files to be left in a consistent state: on the next
 * open after an unclean close, H2 can silently lose committed data. This container is loaded before the others
 * so that it is stopped last, after every container that may still use the datasources.
 */
public class DataSourceContainer implements Container {

    private static final String MODULE = DataSourceContainer.class.getName();

    private String name;

    @Override
    public void init(List<StartupCommand> ofbizCommands, String name, String configFile) {
        this.name = name;
    }

    @Override
    public boolean start() {
        return true;
    }

    @Override
    public void stop() {
        Debug.logInfo("Closing the datasources", MODULE);
        TransactionFactoryLoader.getInstance().shutdown();
    }

    @Override
    public String getName() {
        return name;
    }
}

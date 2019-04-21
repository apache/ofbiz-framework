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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;

import org.apache.ofbiz.base.concurrent.ExecutionPool;
import org.apache.ofbiz.base.container.Container;
import org.apache.ofbiz.base.container.ContainerConfig;
import org.apache.ofbiz.base.container.ContainerException;
import org.apache.ofbiz.base.start.StartupCommand;
import org.apache.ofbiz.base.util.StringUtil;
import org.apache.ofbiz.base.util.UtilValidate;

public class DelegatorContainer implements Container {
    private String name;
    private List<String> preloadedDelegatorNames;

    @Override
    public void init(List<StartupCommand> ofbizCommands, String name, String configFile) throws ContainerException {
        this.name = name;

        ContainerConfig.Configuration cc = ContainerConfig.getConfiguration(name, configFile);

        preloadedDelegatorNames = StringUtil.split(ContainerConfig.getPropertyValue(cc, "preloaded-delegators", "default"), ", ");
    }

    @Override
    public boolean start() {
        if (UtilValidate.isEmpty(preloadedDelegatorNames)) {
            return true;
        }
        List<Future<Delegator>> futures = new ArrayList<>();
        for (String preloadedDelegatorName: preloadedDelegatorNames) {
            futures.add(DelegatorFactory.getDelegatorFuture(preloadedDelegatorName));
        }
        ExecutionPool.getAllFutures(futures);
        return true;
    }

    @Override
    public void stop() {
    }

    @Override
    public String getName() {
        return name;
    }
}

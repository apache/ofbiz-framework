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
package org.apache.ofbiz.webtools.secret;

import java.util.List;

import org.apache.ofbiz.base.container.Container;
import org.apache.ofbiz.base.container.ContainerConfig;
import org.apache.ofbiz.base.container.ContainerException;
import org.apache.ofbiz.base.secret.SecretValueResolver;
import org.apache.ofbiz.base.start.StartupCommand;
import org.apache.ofbiz.base.util.Debug;

/**
 * OFBiz startup container that registers {@link SecretAuditQueue} as the active
 * {@link org.apache.ofbiz.base.secret.SecretAuditSink} with {@link SecretValueResolver}.
 *
 * <p>This bridges the module-layer gap: {@code SecretValueResolver} lives in
 * {@code framework/base} (no entity dependency), while {@link SecretAuditQueue}
 * lives in {@code framework/webtools} (has {@code DelegatorFactory} access).
 * The container wires them together after OFBiz's entity layer has started.</p>
 *
 * <p>Declared in {@code framework/webtools/ofbiz-component.xml} with
 * {@code loaders="main"} so it runs after {@code delegator-container}.</p>
 */
public final class SecretAuditContainer implements Container {

    private static final String MODULE = SecretAuditContainer.class.getName();

    private String name;
    private String delegatorName = "default";

    @Override
    public void init(List<StartupCommand> ofbizCommands, String name, String configFile) {
        this.name = name;
        try {
            ContainerConfig.Configuration cfg = ContainerConfig.getConfiguration(name);
            String configured = ContainerConfig.getPropertyValue(cfg, "delegator-name", "default");
            if (configured != null && !configured.isEmpty()) {
                this.delegatorName = configured;
            }
        } catch (ContainerException e) {
            // No container configuration registered for this name (common in unit tests and
            // in deployments where the property block is omitted). Use the "default" fallback.
            Debug.logInfo("SecretAuditContainer: no container configuration found for '"
                    + name + "'; using delegator 'default'", MODULE);
        }
    }

    @Override
    public boolean start() throws ContainerException {
        SecretAuditQueue.INSTANCE.setDelegatorName(delegatorName);
        SecretValueResolver.setAuditSink(SecretAuditQueue.INSTANCE);
        Debug.logInfo("SecretAuditContainer: SecretAuditQueue registered as audit sink"
                + " using delegator '" + delegatorName + "'"
                + " (FETCH events=" + SecretValueResolver.LOG_FETCH_EVENTS
                + ", CACHE_HIT events=" + SecretValueResolver.LOG_CACHE_HITS + ")", MODULE);
        return true;
    }

    @Override
    public void stop() throws ContainerException {
        SecretAuditQueue.INSTANCE.shutdown();
        SecretValueResolver.setAuditSink(null);
        Debug.logInfo("SecretAuditContainer: SecretAuditQueue shut down", MODULE);
    }

    @Override
    public String getName() {
        return name;
    }
}

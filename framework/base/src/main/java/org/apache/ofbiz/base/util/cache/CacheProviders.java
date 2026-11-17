/*
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
package org.apache.ofbiz.base.util.cache;

import java.util.ArrayList;
import java.util.List;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;
import java.util.stream.Collectors;

import org.apache.ofbiz.base.util.Debug;

/** Holds the {@link CacheProvider} used by every {@link UtilCache}. */
public final class CacheProviders {

    private static final String MODULE = CacheProviders.class.getName();
    private static volatile CacheProvider active;

    private CacheProviders() { }

    /**
     * Returns the provider in use, never {@code null}. The provider is selected on first use and assigned before
     * anything is logged, because logging may itself create a {@link UtilCache} and so re-enter this method.
     */
    public static CacheProvider getActiveProvider() {
        CacheProvider provider = active;
        if (provider != null) {
            return provider;
        }
        List<String> messages = new ArrayList<>();
        CacheProvider selected = select(load(messages), messages);
        boolean assigned = false;
        synchronized (CacheProviders.class) {
            if (active == null) {
                active = selected;
                assigned = true;
            }
            provider = active;
        }
        if (assigned) {
            messages.forEach(message -> Debug.logWarning(message, MODULE));
            Debug.logInfo("Using cache provider " + provider.getName(), MODULE);
        }
        return provider;
    }

    private static List<CacheProvider> load(List<String> messages) {
        List<CacheProvider> providers = new ArrayList<>();
        try {
            ServiceLoader.load(CacheProvider.class).forEach(providers::add);
        } catch (ServiceConfigurationError e) {
            providers.clear();
            messages.add("Could not load the cache providers (" + e.getMessage() + "), using the Caffeine provider");
        }
        return providers;
    }

    static CacheProvider select(List<CacheProvider> found) {
        List<String> messages = new ArrayList<>();
        CacheProvider chosen = select(found, messages);
        messages.forEach(message -> Debug.logWarning(message, MODULE));
        return chosen;
    }

    private static CacheProvider select(List<CacheProvider> providers, List<String> messages) {
        if (providers.isEmpty()) {
            return new CaffeineCacheProvider();
        }
        CacheProvider chosen = providers.get(0);
        if (providers.size() > 1) {
            messages.add("Several cache providers found " + providers.stream().map(CacheProvider::getName)
                    .collect(Collectors.toList()) + ", using " + chosen.getName());
        }
        return chosen;
    }
}

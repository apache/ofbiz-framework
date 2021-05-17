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
 */
package org.apache.ofbiz.webapp;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import org.apache.ofbiz.base.component.ComponentConfig;
import org.apache.ofbiz.base.component.ComponentConfig.WebappInfo;
import org.apache.ofbiz.base.util.UtilValidate;

/**
 * Cache for web applications information retrieved from
 * {@linkplain ComponentConfig component configurations}.
 * <p>
 * This improves performance by avoiding to retrieve web applications from
 * component configurations each time.
 * <p>
 * This is a cache which doesn't implement any invalidation mechanism.  Once a
 * web applications is defined, it is <b>memoized</b> because it is not meant
 * to change while OFBiz is running.
 *
 * @see <a href="https://en.wikipedia.org/wiki/Memoization">Memoization</a>
 */
public class WebAppCache {
    // Synchronized map storing web applications.
    // The LinkedHashMap is used to maintain insertion order (which client code depends on).
    // There is no concurrent implementation of LinkedHashMap, so we are using manual synchronization instead.
    private final LinkedHashMap<String, List<WebappInfo>> serverWebApps;
    // Source for retrieving components configurations.
    private final Supplier<Collection<ComponentConfig>> ccs;

    /**
     * Constructs an empty web application cache.
     * @param supplier the source from which components configurations are retrieved
     */
    public WebAppCache(Supplier<Collection<ComponentConfig>> supplier) {
        ccs = supplier;
        serverWebApps = new LinkedHashMap<>();
    }

    /**
     * Retrieves the web applications information that must be visible
     * in the context of the server {@code serverName}.
     * @param serverName the name of the server to match
     * @return the corresponding web applications information
     */
    public List<WebappInfo> getAppBarWebInfos(String serverName) {
        return getAppBarWebInfos(serverName, null);
    }

    /**
     * Retrieves the web applications information that must be visible inside
     * the menu {@code menuName} in the context of the server {@code serverName}.
     * <p>
     * When an empty string or {@code null} is used for {@code menuName},
     * all the web application information corresponding to {@code serverName} are matched.
     * @param serverName the name of server to match
     * @param menuName the name of the menu to match
     * @return the corresponding web applications information
     * @throws NullPointerException when {@code serverName} is {@code null}
     */
    public List<WebappInfo> getAppBarWebInfos(String serverName, String menuName) {
        String serverWebAppsKey = serverName + menuName;
        List<WebappInfo> webInfos = null;
        synchronized (serverWebApps) {
            webInfos = serverWebApps.get(serverWebAppsKey);
        }
        if (webInfos == null) {
            AtomicInteger emptyPosition = new AtomicInteger(999);
            TreeMap<Integer, WebappInfo> tm = ccs.get().stream()
                    .flatMap(cc -> cc.getWebappInfos().stream())
                    .filter(wInfo -> {
                        if (wInfo.getAppBarDisplay()) {
                            return serverName.equals(wInfo.getServer())
                                    && (UtilValidate.isEmpty(menuName) || menuName.equals(wInfo.getMenuName()));
                        } else {
                            return UtilValidate.isEmpty(menuName);
                        }
                    })
                    // Keep only one WebappInfo per title (the last appearing one).
                    .collect(TreeMap::new, (acc, wInfo) -> {
                        String stringKey = UtilValidate.isNotEmpty(wInfo.getPosition()) ? wInfo.getPosition() : wInfo.getTitle();
                        Integer key = null;
                        try {
                            key = Integer.valueOf(stringKey);
                            key = (key != null) ? key : emptyPosition.incrementAndGet();
                        } catch (NumberFormatException e) {
                            key = emptyPosition.incrementAndGet();
                        }
                        acc.put(key, wInfo);
                    },
                            TreeMap::putAll);
            // Create the list of WebappInfos ordered by their title/position.
            webInfos = Collections.unmodifiableList(new ArrayList<>(tm.values()));
            synchronized (serverWebApps) {
                // We are only preventing concurrent modification, we are not guaranteeing a singleton.
                serverWebApps.put(serverWebAppsKey, webInfos);
            }
        }
        return webInfos;
    }

    /**
     * Retrieves the first web application information which mount point correspond to
     * {@code webAppName} in the context of the server {@code serverName}.
     * @param serverName the name of the server to match
     * @param webAppName the name of the web application to match
     * @return the corresponding web application information
     * @throws NullPointerException when {@code serverName} is {@code null}
     */
    public Optional<WebappInfo> getWebappInfo(String serverName, String webAppName) {
        return getAppBarWebInfos(serverName).stream()
                .filter(app -> app.getMountPoint().replaceAll("[/*]", "").equals(webAppName))
                .findFirst();
    }

    // Instance of the cache shared by the loginWorker and Freemarker appbar rendering.
    // TODO: Find a way to share this cache without relying on a global variable.
    private static WebAppCache sharedCache = new WebAppCache(ComponentConfig::getAllComponents);

    /**
     * Provides access to a shared instance of the webapp cache.
     * @return the shared webapp cache.
     */
    public static WebAppCache getShared() {
        return sharedCache;
    }
}

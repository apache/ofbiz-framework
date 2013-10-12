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
package org.ofbiz.security;

import java.util.Iterator;
import java.util.ServiceLoader;

import org.ofbiz.base.util.Assert;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.cache.UtilCache;
import org.ofbiz.entity.Delegator;

/**
 * A <code>Security</code> factory.
 */
public final class SecurityFactory {

    public static final String module = SecurityFactory.class.getName();
    // The default implementation stores a Delegator reference, so we will cache by delegator name.
    // The goal is to remove Delegator references in the Security interface, then we can use a singleton
    // and eliminate the cache.
    private static final UtilCache<String, Security> authorizationCache = UtilCache.createUtilCache("security.AuthorizationCache");

    /**
     * Returns a <code>Security</code> instance. The method uses Java's
     * <a href="http://docs.oracle.com/javase/6/docs/api/java/util/ServiceLoader.html"><code>ServiceLoader</code></a>
     * to get a <code>Security</code> instance. If no instance is found, a default implementation is used.
     * The default implementation is based on/backed by the OFBiz entity engine.
     *
     * @param delegator The delegator
     * @return A <code>Security</code> instance
     */
    @SuppressWarnings("deprecation")
    public static Security getInstance(Delegator delegator) throws SecurityConfigurationException {
        Assert.notNull("delegator", delegator);
        Security security = authorizationCache.get(delegator.getDelegatorName());
        if (security == null) {
            Iterator<Security> iterator = ServiceLoader.load(Security.class).iterator();
            if (iterator.hasNext()) {
                security = iterator.next();
            } else {
                security = new OFBizSecurity();
            }
            security.setDelegator(delegator);
            security = authorizationCache.putIfAbsentAndGet(delegator.getDelegatorName(), security);
            if (Debug.verboseOn()) {
                Debug.logVerbose("Security implementation " + security.getClass().getName() + " created for delegator " + delegator.getDelegatorName(), module);
            }
        }
        return security;
    }

    private SecurityFactory() {}
}

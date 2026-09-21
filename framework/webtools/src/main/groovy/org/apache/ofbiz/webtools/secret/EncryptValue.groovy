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
package org.apache.ofbiz.webtools.secret

import org.apache.ofbiz.base.secret.SecretProviderFactory
import org.apache.ofbiz.base.secret.SecretValueResolver
import org.apache.ofbiz.base.util.UtilProperties

context.SecretValueMarker = SecretValueResolver.MARKER_NAME
context.activeSecretProvider = SecretProviderFactory.getProviderName()
Properties props = UtilProperties.getProperties('security')
// A List of [label, value] pairs, not a Map: FreeMarker's BeansWrapper exposes java.util.Map's
// own methods (getClass, keySet, replace, ...) as extra "keys" alongside real entries when a
// Map is iterated with ?keys/[k], so the template renders garbage for those bogus pseudo-keys.
// A List is wrapped as a plain sequence with no such method-name leakage.
context.activeSettings = [
    ['Lookup marker', props?.getProperty('secret.value.marker', 'LOOKUP')],
    ['Cache TTL (seconds)', props?.getProperty('secret.cache.ttl.seconds', '300')],
    ['Retry count', props?.getProperty('secret.provider.retry.count', '2')],
    ['Retry delay (ms)', props?.getProperty('secret.provider.retry.delay.ms', '500')],
    ['Master key env var', props?.getProperty('secret.master.key.env.var', 'OFBIZ_MASTER_KEY')],
    ['PBKDF2 iterations', props?.getProperty('secret.pbkdf2.iterations', '310000')]
]

// Provider health probe: call the active provider directly (bypasses SecretValueResolver so
// no cache touch and no audit event is written). A "not found" exception means the vault is
// reachable — the ping key simply doesn't exist, which is expected.
try {
    SecretProviderFactory.getInstance().getSecret('__ping__')
    context.providerHealthy = true
} catch (Exception e) {
    String msg = e.getMessage() ?: ''
    context.providerHealthy = (msg.contains('not found') || msg.contains('NotFound')
            || msg.contains('does not exist') || msg.contains('ResourceNotFoundException')
            || msg.contains('SecretNotFoundException'))
}

// Auto-load usage stats on every page load so the summary is always visible without
// requiring the operator to click Refresh first.
context.usageSummary = SecretValueResolver.getUsageSummary()
context.usageReportRows = SecretValueResolver.getUsageReport().collect { key, stats ->
    [key, stats.hits ?: 0L, stats.misses ?: 0L, stats.total ?: 0L]
}

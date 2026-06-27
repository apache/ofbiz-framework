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
package org.apache.ofbiz.base.secret;

import java.util.Iterator;
import java.util.Properties;
import java.util.ServiceLoader;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.ofbiz.base.lang.ThreadSafe;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilProperties;

/**
 * Factory that provides the active {@link SecretProvider} instance.
 *
 * <p>On first access the factory uses Java's {@link ServiceLoader} to discover
 * a custom {@link SecretProvider} registered in any plugin's
 * {@code META-INF/services/org.apache.ofbiz.base.secret.SecretProvider} file.
 * If none is found, {@link FileBasedSecretProvider} is used automatically,
 * preserving backward compatibility with {@code passwords.properties}.</p>
 *
 * <p>If a custom (remote) provider is found, it is wrapped in a
 * {@link FallbackSecretProvider} together with {@link FileBasedSecretProvider}.
 * When the remote provider's own {@code <prefix>.fallback.enabled} configuration
 * property is {@code true} (the default) and the remote provider fails (e.g. the
 * remote secret manager is unreachable), the secret is resolved from the local
 * {@code passwords.properties} file instead.</p>
 *
 * <p>This mirrors the pattern already used by
 * {@link org.apache.ofbiz.security.SecurityFactory}.</p>
 */
@ThreadSafe
public final class SecretProviderFactory {

    private static final String MODULE = SecretProviderFactory.class.getName();

    private static final Properties SECURITY_PROPERTIES = UtilProperties.getProperties("security");

    // Proactive rotation poll: disabled (0) by default. Set secret.rotation.poll.seconds in
    // security.properties to periodically flush both cache layers so a secret rotated in the
    // remote vault is picked up within a bounded window even if nobody clicks "Flush Secret Cache".
    private static final long ROTATION_POLL_SECONDS = readPollSeconds();

    private static volatile SecretProvider instance;
    private static volatile String providerName;

    static {
        loadAndSet();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                instance.close();
                Debug.logInfo("SecretProvider: provider closed on shutdown", MODULE);
            } catch (Exception e) {
                Debug.logWarning("SecretProvider: error closing provider on shutdown: " + e.getMessage(), MODULE);
            }
        }, "SecretProvider-Shutdown"));
        if (ROTATION_POLL_SECONDS > 0) {
            ScheduledExecutorService poll = Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "SecretProvider-RotationPoll");
                t.setDaemon(true);
                return t;
            });
            poll.scheduleAtFixedRate(() -> {
                try {
                    instance.invalidateCache();
                    SecretValueResolver.invalidateAll();
                    Debug.logInfo("[SECRET_AUDIT] action=ROTATION_POLL user=system provider=" + providerName
                            + " outcome=SUCCESS", MODULE);
                    if (SecretValueResolver.LOG_FETCH_EVENTS) {
                        SecretAuditSink s = SecretValueResolver.getAuditSink();
                        if (s != null) s.onRotationPoll("SUCCESS", null);
                    }
                } catch (Exception e) {
                    Debug.logWarning("SecretProvider: scheduled rotation poll failed: " + e.getMessage(), MODULE);
                    if (SecretValueResolver.LOG_FETCH_EVENTS) {
                        SecretAuditSink s = SecretValueResolver.getAuditSink();
                        if (s != null) s.onRotationPoll("FAILURE", "PROVIDER_ERROR");
                    }
                }
            }, ROTATION_POLL_SECONDS, ROTATION_POLL_SECONDS, TimeUnit.SECONDS);
            Debug.logInfo("SecretProvider: scheduled rotation poll enabled every "
                    + ROTATION_POLL_SECONDS + "s", MODULE);
        }
    }

    private static long readPollSeconds() {
        if (SECURITY_PROPERTIES == null) {
            return 0L;
        }
        try {
            return Long.parseLong(SECURITY_PROPERTIES.getProperty("secret.rotation.poll.seconds", "0").trim());
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    /** Discovers and instantiates the active provider via {@link ServiceLoader}, setting {@code instance}/{@code providerName}. */
    private static void loadAndSet() {
        SecretProvider provider;
        String name;
        try {
            Iterator<SecretProvider> it = ServiceLoader.load(SecretProvider.class).iterator();
            if (it.hasNext()) {
                SecretProvider custom = it.next();
                name = custom.getClass().getSimpleName();
                Debug.logInfo("SecretProvider: using custom implementation " + custom.getClass().getName(), MODULE);
                if (it.hasNext()) {
                    StringBuilder extras = new StringBuilder();
                    it.forEachRemaining(p -> extras.append(" ").append(p.getClass().getName()));
                    Debug.logWarning("SecretProvider: multiple SecretProvider implementations found on the classpath."
                            + " Only '" + custom.getClass().getName() + "' will be used; ignoring:"
                            + extras + ". Remove unused provider plugins to eliminate ambiguity.", MODULE);
                }
                provider = new FallbackSecretProvider(custom, new FileBasedSecretProvider());
            } else {
                Debug.logInfo("SecretProvider: no custom implementation found, using FileBasedSecretProvider"
                        + " (framework/base/config/passwords.properties)", MODULE);
                provider = new FileBasedSecretProvider();
                name = "FileBasedSecretProvider (no vault configured)";
            }
        } catch (Exception e) {
            // A plugin provider whose constructor throws (e.g. missing SDK dependency, bad config)
            // must not prevent OFBiz from starting. Fall back to FileBasedSecretProvider and log
            // a clear error so the operator knows the vault plugin was not loaded.
            Debug.logError("SecretProvider: failed to load custom provider (" + e.getClass().getName()
                    + ": " + e.getMessage() + "). Falling back to FileBasedSecretProvider.", MODULE);
            provider = new FileBasedSecretProvider();
            name = "FileBasedSecretProvider (no vault configured)";
        }
        instance = provider;
        providerName = name;
    }

    /** Returns the active {@link SecretProvider} instance. */
    public static SecretProvider getInstance() {
        return instance;
    }

    /**
     * Returns the simple class name of the primary provider.
     * When a vault plugin is loaded this is the plugin class (e.g. {@code AwsSecretsManagerProvider}),
     * not the wrapping {@link FallbackSecretProvider}. When no plugin is loaded this is
     * {@code FileBasedSecretProvider}.
     */
    public static String getProviderName() {
        return providerName;
    }

    /** Clears the active provider's in-memory secret cache. See {@link SecretProvider#invalidateCache()}. */
    public static void invalidateCache() {
        instance.invalidateCache();
    }

    /**
     * Re-runs {@link ServiceLoader} discovery and replaces the active provider, then closes the
     * previous one. Useful after deploying a new vault plugin or updating that plugin's own
     * connection credentials (e.g. a rotated AWS access key or HashiCorp AppRole secret_id) in its
     * {@code config/*.properties} file, without requiring a full OFBiz restart.
     *
     * <p>Callers must ensure the relevant plugin config resource's property cache has already been
     * cleared (e.g. via {@code UtilCache.clearCachesThatStartWith("properties.UtilProperties")}) so
     * the new provider picks up the updated values.</p>
     */
    public static synchronized void reload() {
        SecretProvider previous = instance;
        loadAndSet();
        if (previous != null) {
            try {
                previous.close();
            } catch (Exception e) {
                Debug.logWarning("SecretProvider: error closing previous provider during reload: "
                        + e.getMessage(), MODULE);
            }
        }
        Debug.logInfo("SecretProvider: provider reloaded -> " + providerName, MODULE);
    }

    private SecretProviderFactory() { }
}

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
package org.apache.ofbiz.catalina.container;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpSession;

import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.catalina.valves.ValveBase;
import org.apache.logging.log4j.ThreadContext;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.webapp.stats.VisitHandler;

/**
 * Populates Log4j2's {@code ThreadContext} (MDC) with per-request logging correlation fields -
 * {@code requestId}, {@code visitId}, {@code userLoginId} - so every log line produced while
 * handling one HTTP request or one user visit can be tied together (see
 * {@code framework/base/config/log4j2.xml}, the ECS JSON logging profile in particular).
 *
 * <p>Runs as an engine-level Valve, ahead of the servlet filter chain, so the fields are already
 * in place before {@code ControlFilter} and every downstream filter/servlet run.
 *
 * <p>Additional fields can be contributed by any component without changing this class - see
 * {@link CorrelationFieldProvider}.
 */
public class CorrelationValve extends ValveBase {

    private static final String MODULE = CorrelationValve.class.getName();
    private static final Set<String> CORE_FIELDS = Set.of("requestId", "visitId", "userLoginId");
    private static final List<CorrelationFieldProvider> PROVIDERS = loadProviders();

    CorrelationValve() {
        super();
    }

    private static List<CorrelationFieldProvider> loadProviders() {
        List<CorrelationFieldProvider> providers = new ArrayList<>();
        ServiceLoader.load(CorrelationFieldProvider.class).forEach(providers::add);
        return List.copyOf(providers);
    }

    @Override
    public void invoke(Request request, Response response) throws IOException, ServletException {
        Set<String> contextKeys = new LinkedHashSet<>();
        try {
            putField(contextKeys, "requestId", generateRequestId());

            HttpSession session = request.getSession(false);
            String visitId = session != null ? VisitHandler.getVisitId(session) : null;
            if (visitId != null) {
                putField(contextKeys, "visitId", visitId);
            }
            GenericValue userLogin = session != null ? (GenericValue) session.getAttribute("userLogin") : null;
            if (userLogin != null) {
                putField(contextKeys, "userLoginId", userLogin.getString("userLoginId"));
            }

            for (CorrelationFieldProvider provider : PROVIDERS) {
                applyProvider(provider, request, userLogin, contextKeys);
            }

            getNext().invoke(request, response);
        } finally {
            contextKeys.forEach(ThreadContext::remove);
        }
    }

    private void applyProvider(CorrelationFieldProvider provider, Request request, GenericValue userLogin, Set<String> contextKeys) {
        try {
            Map<String, String> fields = provider.getFields(request, userLogin);
            if (fields == null) {
                return;
            }
            for (Map.Entry<String, String> field : fields.entrySet()) {
                if (CORE_FIELDS.contains(field.getKey())) {
                    Debug.logWarning("CorrelationFieldProvider [" + provider.getClass().getName() + "] overwrote core "
                            + "correlation field '" + field.getKey() + "'", MODULE);
                }
                putField(contextKeys, field.getKey(), field.getValue());
            }
        } catch (Exception e) {
            Debug.logError(e, "CorrelationFieldProvider [" + provider.getClass().getName() + "] failed to provide "
                    + "correlation fields; skipping it for this request", MODULE);
        }
    }

    private void putField(Set<String> contextKeys, String key, String value) {
        ThreadContext.put(key, value);
        contextKeys.add(key);
    }

    /**
     * Generates a UUID-shaped log correlation id from {@link ThreadLocalRandom} instead of
     * {@link UUID#randomUUID()}. Request ids only need to be unique for log correlation, not
     * cryptographically unpredictable, and UUID.randomUUID() draws from a shared SecureRandom
     * that can become a contention point across threads under high concurrency.
     */
    private static String generateRequestId() {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        return new UUID(random.nextLong(), random.nextLong()).toString();
    }
}

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
package org.apache.ofbiz.base.util.template;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilProperties;

import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;

/**
 * A {@link TemplateHashModel} that wraps FreeMarker's built-in {@code StaticModels} and
 * enforces a whitelist of allowed Java classes and their static members.
 *
 * <p>When a FreeMarker template accesses the {@code Static} shared variable
 * (e.g. {@code Static["org.apache.ofbiz.base.util.UtilMisc"].toMap(...)}), this
 * class intercepts the class lookup and the subsequent member access, and throws
 * a {@link TemplateModelException} for anything not present in the whitelist.
 *
 * <p>The whitelist is loaded from a properties file via
 * {@link UtilProperties#getProperties(String)}. Each entry has the form:
 * <pre>
 *   fully.qualified.ClassName = member1,member2,FIELD_NAME
 * </pre>
 * An empty value means the class is in the whitelist but none of its members
 * are accessible. There is no wildcard support — every allowed member must be
 * listed explicitly.
 *
 * <p>If the configuration resource cannot be found, the whitelist is empty and
 * all {@code Static[...]} access is denied (fail-safe behaviour).
 *
 * @see FreeMarkerWorker#makeConfiguration(freemarker.ext.beans.BeansWrapper)
 */
public final class RestrictedStaticModels implements TemplateHashModel {

    private static final String MODULE = RestrictedStaticModels.class.getName();

    private final TemplateHashModel delegate;
    private final Map<String, Set<String>> whitelist;

    private RestrictedStaticModels(TemplateHashModel delegate, Map<String, Set<String>> whitelist) {
        this.delegate = delegate;
        this.whitelist = whitelist;
    }

    /**
     * Builds a {@code RestrictedStaticModels} by loading the whitelist from the named
     * properties resource (resolved via {@link UtilProperties#getProperties(String)}).
     *
     * <p>If the resource is not found, the returned instance denies all access and an
     * error is logged.
     *
     * @param delegate       the underlying {@code StaticModels} from the BeansWrapper
     * @param configResource the properties resource name (without the {@code .properties}
     *                       extension), e.g. {@code "freemarker-whitelist"}
     * @return an immutable {@code RestrictedStaticModels} instance
     */
    public static RestrictedStaticModels fromConfig(TemplateHashModel delegate, String configResource) {
        Properties props = UtilProperties.getProperties(configResource);
        if (props == null) {
            Debug.logError("FreeMarker static-member whitelist configuration not found: ["
                    + configResource + ".properties]. All Static[...] access will be denied.", MODULE);
            return new RestrictedStaticModels(delegate, Collections.emptyMap());
        }

        Map<String, Set<String>> whitelist = new LinkedHashMap<>();
        for (String rawClassName : props.stringPropertyNames()) {
            String className = rawClassName.trim();
            if (className.isEmpty()) {
                continue;
            }
            String rawValue = props.getProperty(rawClassName, "").trim();
            if (rawValue.isEmpty()) {
                whitelist.put(className, Collections.emptySet());
            } else {
                Set<String> members = new LinkedHashSet<>();
                for (String token : rawValue.split(",")) {
                    String member = token.trim();
                    if (!member.isEmpty()) {
                        members.add(member);
                    }
                }
                whitelist.put(className, Collections.unmodifiableSet(members));
            }
        }

        Debug.logInfo("FreeMarker static-member whitelist loaded from [" + configResource
                + ".properties]: " + whitelist.size() + " class(es) allowed.", MODULE);
        return new RestrictedStaticModels(delegate, Collections.unmodifiableMap(whitelist));
    }

    /**
     * Returns the {@link TemplateModel} for the requested Java class, provided it is
     * in the whitelist.
     *
     * <p>The model is always wrapped in a {@link RestrictedStaticModel} that enforces
     * member-level access control. An empty allowed-member set means the class is
     * whitelisted but every member lookup will be denied.
     *
     * @param className the fully-qualified Java class name
     * @return a (possibly wrapped) {@link TemplateModel} for the class
     * @throws TemplateModelException if the class is not in the whitelist
     */
    @Override
    public TemplateModel get(String className) throws TemplateModelException {
        if (!whitelist.containsKey(className)) {
            throw new TemplateModelException(
                    "FreeMarker static access denied: class [" + className + "] is not in the"
                    + " whitelist. To allow access, add an entry to freemarker-whitelist.properties.");
        }
        TemplateModel model = delegate.get(className);
        Set<String> allowedMembers = whitelist.get(className);
        return new RestrictedStaticModel((TemplateHashModel) model, allowedMembers, className);
    }

    @Override
    public boolean isEmpty() throws TemplateModelException {
        return whitelist.isEmpty();
    }

    /**
     * A {@link TemplateHashModel} wrapper around a single {@code StaticModel} that
     * restricts member access to a pre-approved set of names.
     */
    private static final class RestrictedStaticModel implements TemplateHashModel {

        private final TemplateHashModel delegate;
        private final Set<String> allowedMembers;
        private final String className;

        private RestrictedStaticModel(TemplateHashModel delegate, Set<String> allowedMembers,
                String className) {
            this.delegate = delegate;
            this.allowedMembers = allowedMembers;
            this.className = className;
        }

        @Override
        public TemplateModel get(String memberName) throws TemplateModelException {
            if (!allowedMembers.contains(memberName)) {
                throw new TemplateModelException(
                        "FreeMarker static access denied: member [" + className + "." + memberName
                        + "] is not in the whitelist. To allow access, add it to"
                        + " freemarker-whitelist.properties.");
            }
            return delegate.get(memberName);
        }

        @Override
        public boolean isEmpty() throws TemplateModelException {
            return allowedMembers.isEmpty();
        }
    }
}

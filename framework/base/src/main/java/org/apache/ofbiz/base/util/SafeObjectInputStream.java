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
package org.apache.ofbiz.base.util;

import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.joining;
import static org.apache.ofbiz.base.util.UtilProperties.getPropertyValue;

import java.io.IOException;
import java.io.InputStream;
import java.io.InvalidClassException;
import java.io.ObjectInputStream;
import java.io.ObjectStreamClass;
import java.util.Arrays;
import java.util.regex.Pattern;

/**
 * SafeObjectInputStream
 *
 * <p> An implementation of {@link java.io.ObjectInputStream} that ensure that
 * only authorized class can be read from it.
 */
public final class SafeObjectInputStream extends ObjectInputStream {
    private static final String[] DEFAULT_ALLOWLIST_PATTERN = {
            "byte\\[\\]", "foo", "SerializationInjector",
            "\\[Z", "\\[B", "\\[S", "\\[I", "\\[J", "\\[F", "\\[D", "\\[C",
            "java..*", "sun.util.calendar..*", "org.apache.ofbiz..*",
            "org.codehaus.groovy.runtime.GStringImpl", "groovy.lang.GString"};
    private static final String[] DEFAULT_DENYLIST = { "rmi", "<" };

    /** The regular expression used to match serialized types. */
    private final Pattern allowlistPattern;

    /**
     * Instantiates a safe object input stream.
     * @param in  the input stream to read
     * @throws IOException when reading is not possible.
     */
    public SafeObjectInputStream(InputStream in) throws IOException {
        super(in);
        String allowListProp = getPropertyValue("SafeObjectInputStream", "allowList", "");
        String[] allowList = allowListProp.isEmpty() ? DEFAULT_ALLOWLIST_PATTERN : allowListProp.split(",");
        allowlistPattern = Arrays.stream(allowList)
                .map(String::trim)
                .filter(str -> !str.isEmpty())
                .collect(collectingAndThen(joining("|", "(", ")"), Pattern::compile));
    }

    @Override
    protected Class<?> resolveClass(ObjectStreamClass classDesc) throws IOException, ClassNotFoundException {
        String className = classDesc.getName();
        // DenyList
        String rejectedObjectsProp = getPropertyValue("security", "denyList", "");
        String[] denyList = rejectedObjectsProp.isEmpty() ? DEFAULT_DENYLIST : rejectedObjectsProp.split(",");
        // For now DEFAULT_DENYLIST: don't allow RMI, prevent generics markup in string type names
        for (String deny : denyList) {
            if (className.contains(deny)) {
                throw new InvalidClassException(className, "Unauthorized deserialisation attempt");
            }
        }
        if (!allowlistPattern.matcher(className).find()) {
            Debug.logWarning("***Incompatible class***: " + className
                    + ". Please see OFBIZ-10837.  Report to dev ML if you use OFBiz without changes. "
                    + "Else follow https://s.apache.org/45war",
                    "SafeObjectInputStream");
            throw new ClassCastException("Incompatible class: " + classDesc.getName());
        }
        return ObjectType.loadClass(classDesc.getName(), Thread.currentThread().getContextClassLoader());
    }
}

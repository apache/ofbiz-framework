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
package org.apache.ofbiz.widget.model;

import java.nio.file.Paths;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilValidate;

/**
 * Central gatekeeper for every widget XML resource location (screen, form, grid, menu, tree) before
 * it is resolved and parsed. A widget location can be influenced by request context (for example a
 * decorator's {@code location} attribute is commonly a Flexible String Expression such as
 * {@code ${parameters.mainDecoratorLocation}}), so this class treats every location as untrusted and
 * only allows it through when it is either:
 * <ul>
 *   <li>a {@code component://} location with no {@code ..} traversal segment, or</li>
 *   <li>a bare filesystem path matched by the administrator-configured
 *       {@code security.allowFilePaths} allowlist (denied by default).</li>
 * </ul>
 * Any {@code file:} scheme location - single-slash or otherwise, in any letter case - is rejected
 * outright at the protocol layer: it is never a legitimate widget location, and on Linux it is the
 * carrier used to reach live process-descriptor aliases such as {@code file:/dev/fd/N} or
 * {@code file:/proc/thread-self/fd/N}.
 */
public final class WidgetSecureLocation {

    private static final String MODULE = WidgetSecureLocation.class.getName();
    private static final String COMPONENT_PROTOCOL = "component://";

    private WidgetSecureLocation() { }

    /**
     * Sanitizes a widget resource location.
     * @param location the raw, potentially untrusted location
     * @return the (possibly normalized) location if it is allowed, or {@code null} if it must be rejected
     */
    public static String sanitize(String location) {
        if (UtilValidate.isEmpty(location)) {
            Debug.logWarning("Unable to sanitize an empty widget location", MODULE);
            return null;
        }
        if (isFileScheme(location) || UtilValidate.isUrlInStringAndDoesNotStartByComponentProtocol(location)) {
            Debug.logWarning(String.format("Unable to sanitize location: [%s]", location), MODULE);
            return null;
        }
        if (location.startsWith(COMPONENT_PROTOCOL)) {
            String componentRelativePath = location.substring(COMPONENT_PROTOCOL.length());
            if (componentRelativePath.contains("..")) {
                Debug.logWarning(String.format("Traversal sequence '..' is not allowed in location: [%s]", location), MODULE);
                return null;
            }
            return COMPONENT_PROTOCOL + Paths.get(componentRelativePath).normalize();
        }
        if (UtilValidate.isAllowedPath(location)) {
            return location;
        }
        Debug.logWarning(String.format("Location isn't on the configured allowed file path: [%s]", location), MODULE);
        return null;
    }

    /**
     * Detects the {@code file:} URI scheme regardless of case or slash count, e.g. {@code file:/},
     * {@code File:/}, {@code FILE://}. Java's URL/{@code FlexibleLocation} scheme resolution is
     * case-insensitive, so this check must be too.
     */
    private static boolean isFileScheme(String location) {
        int colonIndex = location.indexOf(':');
        if (colonIndex < 0) {
            return false;
        }
        return "file".equalsIgnoreCase(location.substring(0, colonIndex));
    }
}

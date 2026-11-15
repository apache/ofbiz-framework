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

/**
 * How a cache behaves when several OFBiz servers share one database.
 * Without an invalidation bus every policy behaves as {@link #LOCAL}.
 */
public enum CachePolicy {
    /** Entries stay on this server; nothing is sent to or received from other servers. */
    LOCAL,
    /** A removal on one server is sent to the other servers, which drop the same entry. */
    INVALIDATE,
    /** Like {@link #INVALIDATE}, with a shared tier when the cache provider supports one. */
    NEAR;

    /**
     * Parses the {@code policy} attribute of a {@code <cache>} element.
     * @param value the attribute value, empty when the attribute is absent
     * @return the policy, or {@code null} when {@code value} is null or empty
     * @throws IllegalArgumentException when the value is not {@code local}, {@code invalidate} or {@code near}
     */
    public static CachePolicy fromAttribute(String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        return switch (value) {
        case "local" -> LOCAL;
        case "invalidate" -> INVALIDATE;
        case "near" -> NEAR;
        default -> throw new IllegalArgumentException("Unknown cache policy [" + value + "], expected local, invalidate or near");
        };
    }
}

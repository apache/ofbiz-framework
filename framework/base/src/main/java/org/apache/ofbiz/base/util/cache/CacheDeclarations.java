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

import java.util.Comparator;
import java.util.List;

/** Works out the settings of a cache from the {@link CacheDeclaration}s that match it. */
public final class CacheDeclarations {

    private CacheDeclarations() { }

    /**
     * Returns the code settings with every matching declaration applied, attribute by attribute: prefixes from the
     * shortest to the longest, then exact names, and declarations of the same name or prefix in component load order.
     * Each declaration only changes the attributes it sets, so for each attribute the most specific declaration that
     * sets it wins, and among declarations of the same name or prefix the one loaded last.
     * @param declarations the declarations in component load order
     * @param codeSettings the settings given by the code that creates the cache
     */
    public static CacheSettings resolve(List<CacheDeclaration> declarations, CacheSettings codeSettings) {
        CacheSettings result = codeSettings;
        for (CacheDeclaration declaration : declarations.stream()
                .filter(declaration -> declaration.matches(codeSettings.name()))
                .sorted(Comparator.comparingInt(CacheDeclaration::rank))
                .toList()) {
            result = declaration.applyTo(result);
        }
        return result;
    }

    /** Returns whether any declaration, by exact name or by name prefix, matches {@code cacheName}. */
    public static boolean isDeclared(List<CacheDeclaration> declarations, String cacheName) {
        return declarations.stream().anyMatch(declaration -> declaration.matches(cacheName));
    }
}

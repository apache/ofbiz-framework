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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

public class CacheDeclarationsTest {

    private static final CacheSettings CODE = new CacheSettings("widget.screen.locationResource", 0, 0, true,
            CachePolicy.INVALIDATE);

    private static CacheDeclaration exact(String name, Long expire, CachePolicy policy) {
        return new CacheDeclaration(name, false, null, expire, null, policy);
    }

    private static CacheDeclaration prefix(String prefix, Long maxSize) {
        return new CacheDeclaration(prefix, true, maxSize, null, null, null);
    }

    @Test
    public void noDeclarationKeepsCodeSettings() {
        assertEquals(CODE, CacheDeclarations.resolve(List.of(), CODE));
    }

    @Test
    public void exactNameAppliesOnlyDeclaredAttributes() {
        CacheSettings resolved = CacheDeclarations.resolve(
                List.of(exact("widget.screen.locationResource", 10000L, null)), CODE);
        assertEquals(CODE.withExpireTimeMillis(10000), resolved);
    }

    @Test
    public void prefixMatches() {
        CacheSettings resolved = CacheDeclarations.resolve(List.of(prefix("widget.", 500L)), CODE);
        assertEquals(CODE.withMaxSize(500), resolved);
    }

    @Test
    public void nonMatchingDeclarationIsIgnored() {
        assertEquals(CODE, CacheDeclarations.resolve(List.of(prefix("entitycache.", 500L)), CODE));
    }

    @Test
    public void exactNameIsMergedOverPrefix() {
        CacheSettings resolved = CacheDeclarations.resolve(List.of(
                exact("widget.screen.locationResource", 10000L, CachePolicy.LOCAL),
                prefix("widget.", 500L)), CODE);
        assertEquals(CODE.withMaxSize(500).withExpireTimeMillis(10000).withPolicy(CachePolicy.LOCAL), resolved);
    }

    @Test
    public void exactNameBeatsPrefixForTheSameAttribute() {
        CacheSettings resolved = CacheDeclarations.resolve(List.of(
                new CacheDeclaration("widget.screen.locationResource", false, 50L, null, null, null),
                prefix("widget.", 500L)), CODE);
        assertEquals(CODE.withMaxSize(50), resolved);
    }

    @Test
    public void longerPrefixBeatsShorter() {
        CacheSettings resolved = CacheDeclarations.resolve(List.of(
                prefix("widget.screen.", 20L), prefix("widget.", 500L)), CODE);
        assertEquals(CODE.withMaxSize(20), resolved);
    }

    @Test
    public void equalLengthPrefixesFollowLoadOrder() {
        CacheSettings resolved = CacheDeclarations.resolve(List.of(
                prefix("widget.screen.", 20L), prefix("widget.screen.", 30L)), CODE);
        assertEquals(CODE.withMaxSize(30), resolved);
    }

    @Test
    public void laterDeclarationIsMergedOverEarlier() {
        CacheSettings resolved = CacheDeclarations.resolve(List.of(
                exact("widget.screen.locationResource", 10000L, null),
                exact("widget.screen.locationResource", null, CachePolicy.LOCAL)), CODE);
        assertEquals(CODE.withExpireTimeMillis(10000).withPolicy(CachePolicy.LOCAL), resolved);
    }

    @Test
    public void laterDeclarationWinsForTheSameAttribute() {
        CacheSettings resolved = CacheDeclarations.resolve(List.of(
                exact("widget.screen.locationResource", 10000L, null),
                exact("widget.screen.locationResource", 0L, null)), CODE);
        assertEquals(CODE, resolved);
    }

    @Test
    public void softValuesCanBeTurnedOff() {
        CacheDeclaration declaration = new CacheDeclaration("widget.screen.locationResource", false, null, null,
                Boolean.FALSE, null);
        assertEquals(CODE.withSoftValues(false), CacheDeclarations.resolve(List.of(declaration), CODE));
    }

    @Test
    public void negativeValuesAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> new CacheDeclaration("widget.", true, -1L, null, null, null));
        assertThrows(IllegalArgumentException.class, () -> new CacheDeclaration("widget.", true, null, -1L, null, null));
    }

    @Test
    public void emptyNameIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> exact("", null, null));
    }

    @Test
    public void isDeclaredMatchesExactNameOrPrefix() {
        List<CacheDeclaration> declarations = List.of(exact("a.b", null, CachePolicy.LOCAL), prefix("widget.", null));
        assertTrue(CacheDeclarations.isDeclared(declarations, "a.b"));
        assertTrue(CacheDeclarations.isDeclared(declarations, "widget.screen.x"));
        assertFalse(CacheDeclarations.isDeclared(declarations, "a.bc"));
        assertFalse(CacheDeclarations.isDeclared(declarations, "other"));
        assertFalse(CacheDeclarations.isDeclared(List.of(), "a.b"));
    }
}

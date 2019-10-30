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

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.in;
import static org.hamcrest.Matchers.is;
import static org.junit.Assume.assumeNoException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;

import com.pholser.junit.quickcheck.Property;
import com.pholser.junit.quickcheck.runner.JUnitQuickcheck;

@RunWith(JUnitQuickcheck.class)
public class DiGraphTest {

    @Test
    public void testAcyclic() {
        checkTopologicalOrder(UtilMisc.toMap(
                "a", asList("b"),
                "b", asList("c", "d"),
                "c", emptyList(),
                "d", emptyList(),
                "z", emptyList(),
                "f", emptyList(),
                "e", asList("z", "b")));
    }

    @Test(expected = IllegalStateException.class)
    public void testWithCycle() {
        Map<String, Collection<String>> g = UtilMisc.toMap(
                "a", asList("b"),
                "b", asList("c"),
                "c", asList("a"));
        Digraph<String> dg = new Digraph<>(g);
        dg.sort();
    }

    @Test
    public void testMultipleParents() {
        checkTopologicalOrder(UtilMisc.toMap(
                "a", asList("b"),
                "b", emptyList(),
                "c", asList("b")));
    }

    @Property
    public <T> void topologicalOrderProperty(Map<T, Collection<T>> graphspec) {
        try {
            checkTopologicalOrder(graphspec);
        } catch (IllegalArgumentException e) {
            assumeNoException("Invalid Graph", e);
        } catch (IllegalStateException e) {
            assumeNoException("Not a directed acyclic graph", e);
        }
    }

    private static <T> void checkTopologicalOrder(Map<T, Collection<T>> graphspec) {
        Digraph<T> g = new Digraph<>(graphspec);
        List<T> seen = new ArrayList<>();
        for (T node : g.sort()) {
            for (T adjacent : graphspec.get(node)) {
                assertThat("child nodes are before their parents", adjacent, is(in(seen)));
            }
            seen.add(node);
        }
    }
}

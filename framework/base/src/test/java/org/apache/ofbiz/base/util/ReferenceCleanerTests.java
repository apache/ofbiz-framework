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

import static org.apache.ofbiz.base.test.GenericTestCaseBase.useAllMemory;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.TimeUnit;

import org.junit.Ignore;
import org.junit.Test;

public class ReferenceCleanerTests {

    // XXX: This test has been disabled since 2014 in revision
    // 1648403, so it is not evident if it is still relevant.  Maybe
    // we should simply remove it.
    @Ignore("Failing test")
    @Test
    public void testReferenceCleaner() throws Exception {
        final SynchronousQueue<String> queue = new SynchronousQueue<>();
        Object obj = new Object();
        ReferenceCleaner.Soft<Object> soft = new ReferenceCleaner.Soft<Object>(obj) {
            @Override
            public void remove() throws Exception {
                queue.put("soft");
                Thread.currentThread().interrupt();
            }
        };
        ReferenceCleaner.Weak<Object> weak = new ReferenceCleaner.Weak<Object>(obj) {
            @Override
            public void remove() throws Exception {
                queue.put("weak");
                throw new RuntimeException();
            }
        };
        new ReferenceCleaner.Phantom<Object>(obj) {
            @Override
            public void remove() throws Exception {
                queue.put("phantom");
            }
        };
        HashSet<String> foundEvents = new HashSet<>();
        useAllMemory();
        assertSame("still-soft", obj, soft.get());
        assertSame("still-weak", obj, weak.get());
        assertNull("no event", queue.poll(100, TimeUnit.MILLISECONDS));
        useAllMemory();
        foundEvents.add(queue.poll(100, TimeUnit.MILLISECONDS));
        foundEvents.add(queue.poll(100, TimeUnit.MILLISECONDS));
        foundEvents.add(queue.poll(100, TimeUnit.MILLISECONDS));
        useAllMemory();
        foundEvents.add(queue.poll(100, TimeUnit.MILLISECONDS));
        foundEvents.remove(null);
        assertFalse("no null", foundEvents.contains(null));
        assertNull("no-soft", soft.get());
        assertNull("no-weak", weak.get());
        assertTrue("soft event", foundEvents.contains("soft"));
        assertTrue("weak event", foundEvents.contains("weak"));
        assertTrue("phantom event", foundEvents.contains("phantom"));
    }
}

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
package org.apache.ofbiz.base.util.test;

import java.util.HashSet;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.TimeUnit;

import org.apache.ofbiz.base.lang.SourceMonitored;
import org.apache.ofbiz.base.test.GenericTestCaseBase;
import org.apache.ofbiz.base.util.ReferenceCleaner;

@SourceMonitored
public class ReferenceCleanerTests extends GenericTestCaseBase {
    public ReferenceCleanerTests(String name) {
        super(name);
    }

    public void testReferenceCleaner() throws Exception {
        assertStaticHelperClass(ReferenceCleaner.class);
        final SynchronousQueue<String> queue = new SynchronousQueue<String>();
        Object obj = new Object();
        ReferenceCleaner.Soft<Object> soft = new ReferenceCleaner.Soft<Object>(obj) {
            public void remove() throws Exception {
                queue.put("soft");
                Thread.currentThread().interrupt();
            }
        };
        ReferenceCleaner.Weak<Object> weak = new ReferenceCleaner.Weak<Object>(obj) {
            public void remove() throws Exception {
                queue.put("weak");
                throw new RuntimeException();
            }
        };
        new ReferenceCleaner.Phantom<Object>(obj) {
            public void remove() throws Exception {
                queue.put("phantom");
            }
        };
        HashSet<String> foundEvents = new HashSet<String>();
        useAllMemory();
        assertSame("still-soft", obj, soft.get());
        assertSame("still-weak", obj, weak.get());
        assertNull("no event", queue.poll(100, TimeUnit.MILLISECONDS));
        obj = null;
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

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
package org.ofbiz.base.concurrent.test;

import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import org.ofbiz.base.concurrent.TTLObject;
import org.ofbiz.base.lang.SourceMonitored;
import org.ofbiz.base.test.GenericTestCaseBase;

@SourceMonitored
public class AsyncTTLObjectTest extends TTLObjectTest {
    public AsyncTTLObjectTest(String name) {
        super(name, false);
    }

    public void testGet() throws Exception {
        loadData = "1";
        sleepTime = 1000;
        assertGetObject("Fetch data first time, blocking", loadData, 1, 950000000, 1200000000);
        loadData = "2";
        sleepTime = 100;
        assertGetObject("Not called all the time", "1", 1, 0, 100000000);
        Thread.sleep(300);
        assertGetObject("Stale data, starting regen", "1", 1, 0, 100000000);
        Thread.sleep(300);
        assertGetObject("Refreshed with old data", loadData, 2, 0, 100000000);
        loadData = "3";
        sleepTime = 1000;
        Thread.sleep(200);
        assertGetObject("Load called, serve stale data", "2", 2, 0, 100000000);
        Thread.sleep(200);
        assertGetObject("Load called, serve stale data", "2", 2, 0, 100000000);
        Thread.sleep(200);
        assertGetObject("Load called, serve stale data", "2", 2, 0, 100000000);
        Thread.sleep(800);
        assertGetObject("Serve new data", loadData, 3, 0, 100000000);
        object.set("a");
        assertGetObject("Serve set data(a)", "a", 3, 0, 100000000);
        Thread.sleep(500);
        object.set("b");
        assertGetObject("Serve set data(b)", "b", 3, 0, 100000000);
        Thread.sleep(300);
        loadData = "4";
        sleepTime = 200;
        Future<Void> future = schedule(new Callable<Void>() {
            public Void call() {
                object.refresh();
                return null;
            }
        }, 50);
        assertGetObject("Refreshed with old data", "b", 3, 0, 100000000);
        Thread.sleep(100);
        assertGetObject("Refreshed with old data", "b", 3, 0, 100000000);
        Thread.sleep(350);
        assertGetObject("Refreshed with old data", "4", 5, 0, 100000000);
        object.set("5");
        assertGetObject("set new data", "5", 5, 0, 100000000);
        TTLObject.pulseAll();
        sleepTime = 200;
        loadData = "c";
        object.set("5");
        object.refresh();
        assertGetObject("refresh after set", "5", 5, 0, 100000000);
        Thread.sleep(300);
        assertGetObject("refresh after set", "c", 6, 0, 100000000);
    }

    public void testSet() throws Exception {
        object.set("set");
        assertEquals("data after set", "set", object.getObject());
        assertEquals("no dones", 0, doneCount.get());
        loadData = "1";
        sleepTime = 100;
        Thread.sleep(200);
        assertGetObject("SET: stale, start load", "set", 0, 0, 100000000);
        Thread.sleep(200);
        loadData = "2";
        sleepTime = 500;
        assertGetObject("SET: valid, process load, schedule pulse 1", "1", 1, 0, 100000000);
        Thread.sleep(100);
        assertGetObject("SET: stale 1", "1", 1, 0, 100000000);
        Thread.sleep(100);
        assertGetObject("SET: stale 2", "1", 1, 0, 100000000);
        Thread.sleep(100);
        assertGetObject("SET: stale 3", "1", 1, 0, 100000000);
        Thread.sleep(600);
        assertGetObject("SET: valid, process load, schedule pulse 2", "2", 2, 0, 100000000);
    }

    public void testSetGetAbort() throws Exception {
        loadData = "1";
        sleepTime = 1000;
        Future<Void> future = setObjectDelayed(300, "override");
        assertGetObject("Fetch data first time, blocking/setting", "override", 1, 250000000, 400000000);
        assertFuture("delayed set", future, null, false, null, null);
    }

    public void testThrowException() throws Exception {
        loadData = "1";
        sleepTime = 100;
        throwException.set(new Thrower() {
            public void throwException() throws Exception {
                throw new Exception("exc1");
            }
        });
        TTLObject.ObjectException caught = null;
        try {
            assertGetObject("Fetch data first time, throw exception", "override", 1, 0, 200000000);
        } catch (TTLObject.ObjectException e) {
            caught = e;
        } finally {
            assertNotNull("exception thrown", caught);
            assertEquals("correct exception thrown", "exc1", caught.getCause().getMessage());
        }
        caught = null;
        try {
            object.getObject();
        } catch (TTLObject.ObjectException e) {
            caught = e;
        } finally {
            assertNotNull("exception thrown", caught);
            assertEquals("correct exception rethrown", "exc1", caught.getCause().getMessage());
        }
        Thread.sleep(300);
        loadData = "2";
        sleepTime = 100;
        assertGetObject("Fetch data after exception, blocked", loadData, 2, 50000000, 200000000);
        loadData = "3";
        sleepTime = 500;
        throwException.set(new Thrower() {
            public void throwException() throws Exception {
                throw new Exception("exc2");
            }
        });
        Thread.sleep(200);
        // the next call should not throw an exception; if it does, it will
        // leave this method, and be caught by junit, which will then fail
        // the test.
        assertGetObject("Fetch data second time, stale, process pulse", "2", 2, 0, 200000000);
        Thread.sleep(600);
        caught = null;
        try {
            assertGetObject("Fetch data second time, throw exception", "2", 2, 0, 200000000);
        } catch (TTLObject.ObjectException e) {
            caught = e;
        } finally {
            assertNotNull("exception thrown", caught);
            assertEquals("correct exception thrown", "exc2", caught.getCause().getMessage());
        }
    }
}

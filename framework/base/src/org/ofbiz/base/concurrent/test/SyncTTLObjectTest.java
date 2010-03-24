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
import org.ofbiz.base.lang.SourceMonitor;
import org.ofbiz.base.lang.ObjectWrapper;
import org.ofbiz.base.test.GenericTestCaseBase;

@SourceMonitor("Adam Heath")
public class SyncTTLObjectTest extends TTLObjectTest {
    public SyncTTLObjectTest(String name) {
        super(name, true);
    }

    private static class TTLStaticRegistryObject extends TTLObject<String> {
        protected String load(String old, int serial) {
            return old;
        }
    }

    public void testTTLStaticRegistries() throws Exception {
        ObjectWrapper.ConfigurationException caught = null;
        try {
            TTLObject.getTTLForClass(TTLStaticRegistryObject.class);
        } catch (ObjectWrapper.ConfigurationException e) {
            caught = e;
        } finally {
            assertNotNull("no ttl set", caught);
        }
        TTLObject.setDefaultTTLForClass(TTLStaticRegistryObject.class, 100);
        assertEquals("ttl default", 100, TTLObject.getTTLForClass(TTLStaticRegistryObject.class));
        TTLObject.setDefaultTTLForClass(TTLStaticRegistryObject.class, 200);
        assertEquals("can't override ttl default", 100, TTLObject.getTTLForClass(TTLStaticRegistryObject.class));
        TTLObject.setTTLForClass(TTLStaticRegistryObject.class, 200);
        assertEquals("change ttl", 200, TTLObject.getTTLForClass(TTLStaticRegistryObject.class));
        TTLObject.setTTLForClass(TTLStaticRegistryObject.class, 300);
        assertEquals("change ttl", 300, TTLObject.getTTLForClass(TTLStaticRegistryObject.class));
        caught = null;
        assertTrue("default foreground", TTLObject.getForegroundForClass(TTLStaticRegistryObject.class));
        TTLObject.setDefaultForegroundForClass(TTLStaticRegistryObject.class, false);
        assertFalse("set foreground", TTLObject.getForegroundForClass(TTLStaticRegistryObject.class));
        TTLObject.setDefaultForegroundForClass(TTLStaticRegistryObject.class, true);
        assertFalse("can't override foreground", TTLObject.getForegroundForClass(TTLStaticRegistryObject.class));
        TTLObject.setForegroundForClass(TTLStaticRegistryObject.class, true);
        assertTrue("set foreground true", TTLObject.getForegroundForClass(TTLStaticRegistryObject.class));
        TTLObject.setForegroundForClass(TTLStaticRegistryObject.class, false);
        assertFalse("set foreground false", TTLObject.getForegroundForClass(TTLStaticRegistryObject.class));
        // this is only to cause coverage to be 100%
        new TTLStaticRegistryObject().getObject();
    }

    public void testRefresh() throws Exception {
        assertEquals("state:invalid", TTLObject.State.INVALID, object.getState());
        assertEquals("no dones", 0, doneCount.get());
        object.refresh();
        assertEquals("state:generate", TTLObject.State.GENERATE, object.getState());
        assertEquals("no dones", 0, doneCount.get());
        object.refresh();
        assertEquals("state:generate", TTLObject.State.GENERATE, object.getState());
        assertEquals("no dones", 0, doneCount.get());
        object.getObject();
        assertEquals("state:valid", TTLObject.State.VALID, object.getState());
        assertEquals("one done", 1, doneCount.get());
        object.getObject();
        assertEquals("state:valid", TTLObject.State.VALID, object.getState());
        assertEquals("one done", 1, doneCount.get());
        object.getObject();
        assertEquals("state:valid", TTLObject.State.VALID, object.getState());
        assertEquals("one done", 1, doneCount.get());
        object.getObject();
        assertEquals("state:valid", TTLObject.State.VALID, object.getState());
        assertEquals("one done", 1, doneCount.get());
        object.refresh();
        assertEquals("state:generate", TTLObject.State.GENERATE, object.getState());
        assertEquals("one done", 1, doneCount.get());
        object.getObject();
        assertEquals("state:valid", TTLObject.State.VALID, object.getState());
        assertEquals("two dones", 2, doneCount.get());
        object.refresh();
        assertEquals("state:generate", TTLObject.State.GENERATE, object.getState());
        assertEquals("two dones", 2, doneCount.get());
        throwException.set(new Thrower() {
            public void throwException() throws Exception {
                throw new Exception("exc1");
            }
        });
        TTLObject.ObjectException caught = null;
        try {
            object.getObject();
        } catch (TTLObject.ObjectException e) {
            caught = e;
        } finally {
            assertNotNull("exception thrown", caught);
            assertEquals("correct exception thrown", "exc1", caught.getCause().getMessage());
        }
        assertEquals("two dones", 3, doneCount.get());
        caught = null;
        try {
            object.getObject();
        } catch (TTLObject.ObjectException e) {
            caught = e;
        } finally {
            assertNotNull("exception thrown", caught);
            assertEquals("correct exception rethrown", "exc1", caught.getCause().getMessage());
        }
        object.refresh();
        assertEquals("two dones", 3, doneCount.get());
        object.getObject();
        assertEquals("two dones", 4, doneCount.get());
        object.set("one");
        assertEquals("one", "one", object.getObject());
        assertEquals("two dones", 4, doneCount.get());
        object.set("two");
        object.refresh();
        assertEquals("two", (String) null, object.getObject());
        assertEquals("two dones", 5, doneCount.get());
    }

    public void testGetTTL() throws Exception {
        Exception caught = null;
        try {
            new TTLObject<Object>() {
                protected Object load(Object old, int serial) {
                    return old;
                }
            }.getObject();
        } catch (TTLObject.ConfigurationException e) {
            caught = e;
        } finally {
            assertNotNull("exception thrown", caught);
            assertTrue("is a ttl configuration exception", caught.getMessage().startsWith("No TTL defined for "));
        }
        new TTLObject<Object>() {
            protected long getTTL() {
                return 1000;
            }

            protected Object load(Object old, int serial) {
                return old;
            }
        }.getObject();
    }

    public void testGet() throws Exception {
        loadData = "1";
        sleepTime = 1000;
        assertGetObject("Fetch data first time, blocking", loadData, 1, 950000000, 1200000000);
        loadData = "2";
        sleepTime = 100;
        assertGetObject("Not called all the time", "1", 1, 0, 100000000);
        Thread.sleep(200);
        assertGetObject("Auto-refresh", "2", 2, 0, 100000000);
        loadData = "3";
        sleepTime = 200;
        object.refresh();
        assertGetObject("manual-refresh", "3", 3, 0, 100000000);
        assertGetObject("Not called all the time after manual refresh", "3", 3, 0, 100000000);
        Thread.sleep(200);
        loadData = "4";
        sleepTime = 200;
        Future<Void> future = schedule(new Callable<Void>() {
            public Void call() {
                object.refresh();
                return null;
            }
        }, 50);
        assertGetObject("Refreshed with old data", "4", 5, 0, 100000000);
        object.refresh();
        loadData = "5";
        assertGetObject("syncing after refresh", "5", 6, 0, 100000000);
        loadData = "6";
        assertGetObject("no load after refresh", "5", 6, 0, 100000000);
        TTLObject.pulseAll();
        assertGetObject("new data after pulse all", "6", 7, 0, 100000000);
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

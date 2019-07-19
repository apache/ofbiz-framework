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

import java.io.ByteArrayInputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Set;

import org.apache.ofbiz.base.lang.Factory;
import org.apache.ofbiz.base.lang.SourceMonitored;
import org.apache.ofbiz.base.test.GenericTestCaseBase;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.GroovyUtil;
import org.apache.ofbiz.base.util.UtilObject;

@SourceMonitored
public class UtilObjectTests extends GenericTestCaseBase {
    public UtilObjectTests(String name) {
        super(name);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void testStaticHelperClass() throws Exception {
        assertStaticHelperClass(UtilObject.class);
    }

    public static final class ErrorInjector extends FilterInputStream {
        private int after;
        private final boolean onClose;

        public ErrorInjector(InputStream in, boolean onClose) {
            this(in, -1, onClose);
        }

        public ErrorInjector(InputStream in, int after) {
            this(in, after, false);
        }

        public ErrorInjector(InputStream in, int after, boolean onClose) {
            super(in);
            this.after = after;
            this.onClose = onClose;
        }

        @Override
        public int read() throws IOException {
            if (after == 0) {
                throw new IOException();
            }
            if (after > 0) {
                after--;
            }
            return super.read();
        }

        @Override
        public int read(byte[] buf) throws IOException {
            return read(buf, 0, buf.length);
        }

        @Override
        public int read(byte[] buf, int offset, int length) throws IOException {
            if (after == 0) {
                throw new IOException();
            }
            if (after > 0) {
                if (length > after) {
                    length = after;
                }
                int r = super.read(buf, offset, length);
                after -= r;
                return r;
            } else {
                return super.read(buf, offset, length);
            }
        }

        @Override
        public void close() throws IOException {
            if (onClose) {
                throw new IOException();
            }
            super.close();
        }
    }

    public void testErrorInjector() throws Exception {
        byte[] source = new byte[] { 0, 1, 2, 3, 4, 5, 6 };
        InputStream in = new ErrorInjector(new ByteArrayInputStream(source), true);
        byte[] result = new byte[source.length];
        int r = in.read();
        assertEquals("onClose, read short length", 2, in.read(new byte[2]));
        assertNotSame("onClose, not read/eof", -1, r);
        assertEquals("onClose, read length", source.length - 3, in.read(result, 3, result.length - 3));
        Exception caught = null;
        try {
            in.close();
        } catch (IOException e) {
            caught = e;
        } finally {
            assertNotNull("onClose, exception", caught);
        }
        in = new ErrorInjector(new ByteArrayInputStream(source), 4);
        result = new byte[source.length];
        r = in.read();
        assertNotSame("after, not read/eof", -1, r);
        assertEquals("after, read short length", 2, in.read(result, 0, 2));
        assertEquals("after, read long length", 1, in.read(result, 3, result.length - 3));
        caught = null;
        try {
            in.read(result, 4, result.length - 4);
        } catch (IOException e) {
            caught = e;
        } finally {
            assertNotNull("read, buffer exception", caught);
        }
        caught = null;
        try {
            in.read();
        } catch (IOException e) {
            caught = e;
        } finally {
            assertNotNull("read, singleton exception", caught);
        }
        in.close();
    }

    public void testGetBytes_Stream() {
        boolean errorOn = Debug.isOn(Debug.ERROR);
        try {
            Debug.set(Debug.ERROR, false);
            byte[] source = new byte[] { 0, 1, 2, 3, 4, 5, 6 };
            byte[] result = UtilObject.getBytes(new ByteArrayInputStream(source));
            assertNotNull("initial result", result);
            assertEquals("initial equals", source, result);
            assertNull("error after read", UtilObject.getBytes(new ErrorInjector(new ByteArrayInputStream(source), 3)));
            byte[] closeResult = UtilObject.getBytes(new ErrorInjector(new ByteArrayInputStream(source), true));
            assertNotNull("error on close", closeResult);
            assertEquals("error on close equals", source, result);
            Exception caught = null;
            try {
                UtilObject.getBytes(null);
            } catch (NullPointerException e) {
                caught = e;
            } finally {
                assertNotNull("null stream exception", caught);
            }
        } finally {
            Debug.set(Debug.ERROR, errorOn);
        }
    }

    @SuppressWarnings("serial")
    public static class SerializationInjector implements Serializable {
        private boolean onRead;
        private boolean onWrite;

        public SerializationInjector(boolean onRead, boolean onWrite) {
            this.onRead = onRead;
            this.onWrite = onWrite;
        }

        private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
            onRead = stream.readBoolean();
            onWrite = stream.readBoolean();
            if (onRead) {
                throw new IOException();
            }
        }

        private void writeObject(ObjectOutputStream stream) throws IOException {
            if (onWrite) {
                throw new IOException();
            }
            stream.writeBoolean(onRead);
            stream.writeBoolean(onWrite);
        }
    }

    public void testGetBytes_Object() {
        assertNotNull("long", UtilObject.getBytes(0L));
        assertNotNull("injector good", UtilObject.getBytes(new SerializationInjector(false, false)));
        boolean errorOn = Debug.isOn(Debug.ERROR);
        try {
            Debug.set(Debug.ERROR, false);
            assertNull("injector bad", UtilObject.getBytes(new SerializationInjector(false, true)));
            assertNull("long", UtilObject.getBytes(this));
        } finally {
            Debug.set(Debug.ERROR, errorOn);
        }
    }

    public void testGetObject() {
        Long one = 1L;
        byte[] oneBytes = UtilObject.getBytes(one);
        assertNotNull("oneBytes", oneBytes);
        assertEquals("one getObject", one, UtilObject.getObject(oneBytes));
        boolean errorOn = Debug.isOn(Debug.ERROR);
        try {
            Debug.set(Debug.ERROR, false);
            assertNull("parse empty array", UtilObject.getObject(new byte[0]));

            // simulate a ClassNotFoundException
            Object groovySerializable = GroovyUtil.eval("class foo implements java.io.Serializable { }; return new foo()", new HashMap<String, Object>());
            byte[] groovySerializableBytes = UtilObject.getBytes(groovySerializable);
            assertNotNull("groovySerializableBytes", groovySerializableBytes);
            assertNull("groovyDeserializable", UtilObject.getObject(groovySerializableBytes));

            byte[] injectorBytes = UtilObject.getBytes(new SerializationInjector(false, false));
            assertNotNull("injectorBytes good", injectorBytes);
            assertNotNull("injector good", UtilObject.getObject(injectorBytes));
            injectorBytes = UtilObject.getBytes(new SerializationInjector(true, false));
            assertNotNull("injectorBytes bad", injectorBytes);
            assertNull("injector bad", UtilObject.getObject(injectorBytes));
        } finally {
            Debug.set(Debug.ERROR, errorOn);
        }
    }

    public void testGetByteCount() throws Exception {
        assertNotSame("long", 0, UtilObject.getByteCount(0L));
        Exception caught = null;
        try {
            UtilObject.getByteCount(this);
        } catch (IOException e) {
            caught = e;
        } finally {
            assertNotNull("exception thrown", caught);
        }
    }

    public void testCompareToHelper() {
        Long one = 1L;
        Long two = 2L;
        assertComparison("one <-> two", -1, UtilObject.compareToHelper(one, two));
        assertComparison("one <-> one", 0, UtilObject.compareToHelper(one, one));
        assertComparison("two <-> one", 1, UtilObject.compareToHelper(two, one));
        assertComparison("one <-> null", 1, UtilObject.compareToHelper(one, null));
        assertComparison("null <-> one", -1, UtilObject.compareToHelper(null, one));
    }

    public void testDoHashCode() throws Exception {
        UtilObject.doHashCode(this);
        UtilObject.doHashCode(null);
        UtilObject.doHashCode(0);
        UtilObject.doHashCode(new Object[] { this, Object.class });
        UtilObject.doHashCode(new Object[] { null, Object.class });
        UtilObject.doHashCode(new int[] { 1, 3 });
    }

    public interface TestFactoryIntf extends Factory<Object, Set<String>> {
    }

    public static class FirstTestFactory implements TestFactoryIntf {
        @Override
        public Object getInstance(Set<String> set) {
            if (!set.contains("first")) return null;
            if (set.contains("one")) return "ONE";
            if (set.contains("two")) return "TWO";
            if (set.contains("three")) return "THREE";
            return null;
        }
    }

    public static class SecondTestFactory implements TestFactoryIntf {
        @Override
        public Object getInstance(Set<String> set) {
            if (!set.contains("second")) return null;
            if (set.contains("ONE")) return "1";
            if (set.contains("TWO")) return "2";
            if (set.contains("THREE")) return "3";
            return null;
        }
    }

    public void testGetObjectFromFactory() throws Exception {
        assertEquals("first one", "ONE", UtilObject.getObjectFromFactory(TestFactoryIntf.class, set("first", "one")));
        assertEquals("first two", "TWO", UtilObject.getObjectFromFactory(TestFactoryIntf.class, set("first", "two")));
        assertEquals("first three", "THREE", UtilObject.getObjectFromFactory(TestFactoryIntf.class, set("first", "three")));
        assertEquals("first null", "1", UtilObject.getObjectFromFactory(TestFactoryIntf.class, set("first", "second", "ONE")));
        assertEquals("second one", "1", UtilObject.getObjectFromFactory(TestFactoryIntf.class, set("second", "ONE")));
        assertEquals("second two", "2", UtilObject.getObjectFromFactory(TestFactoryIntf.class, set("second", "TWO")));
        assertEquals("second three", "3", UtilObject.getObjectFromFactory(TestFactoryIntf.class, set("second", "THREE")));
        Exception caught = null;
        try {
            UtilObject.getObjectFromFactory(TestFactoryIntf.class, set("first"));
        } catch (ClassNotFoundException e) {
            caught = e;
        } finally {
            assertNotNull("nothing found first", caught);
        }
        caught = null;
        try {
            UtilObject.getObjectFromFactory(TestFactoryIntf.class, set("second"));
        } catch (ClassNotFoundException e) {
            caught = e;
        } finally {
            assertNotNull("nothing found second", caught);
        }
    }
}

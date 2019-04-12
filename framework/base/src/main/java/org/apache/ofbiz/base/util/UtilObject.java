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
package org.apache.ofbiz.base.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Array;
import java.util.Iterator;
import java.util.ServiceLoader;

import org.apache.ofbiz.base.lang.Factory;
import org.apache.ofbiz.base.lang.SourceMonitored;

/**
 * UtilObject
 *
 */
@SourceMonitored
public final class UtilObject {
    private UtilObject() {
    }

    public static final String module = UtilObject.class.getName();

    public static byte[] getBytes(InputStream is) {
        byte[] buffer = new byte[4 * 1024];
        byte[] data = null;
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream()){
            int numBytesRead;
            while ((numBytesRead = is.read(buffer)) != -1) {
                bos.write(buffer, 0, numBytesRead);
            }
            data = bos.toByteArray();
        } catch (IOException e) {
            Debug.logError(e, module);
        }
        return data;
    }

    /** Serialize an object to a byte array */
    public static byte[] getBytes(Object obj) {
        byte[] data = null;
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream(); ObjectOutputStream oos = new ObjectOutputStream(bos)) {
            oos.writeObject(obj);
            data = bos.toByteArray();
        } catch (IOException e) {
            Debug.logError(e, module);
        }
        return data;
    }

    /** Returns the size of a serializable object. Non-serializable objects
     * will throw an <code>IOException</code>.<p>It is important to note
     * that the returned value is length of the byte stream after the object has
     * been serialized. The returned value does not represent the amount of memory
     * the object uses. There is no accurate way to determine the size of an
     * object in memory.</p>
     * @param obj
     * @return the number of bytes in the serialized object
     * @throws IOException
     */
    public static long getByteCount(Object obj) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(bos);
        oos.writeObject(obj);
        oos.flush();
        long size = bos.size();
        bos.close();
        return size;
    }

    /** Deserialize a byte array back to an object; if there is an error, it is logged, and null is returned. */
    public static Object getObject(byte[] bytes) {
        Object obj = null;
        try {
            obj = getObjectException(bytes);
        } catch (ClassNotFoundException | IOException e) {
            Debug.logError(e, module);
        }
        return obj;
    }

    /** Deserialize a byte array back to an object */
    public static Object getObjectException(byte[] bytes) throws ClassNotFoundException, IOException {
        try (ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
                SafeObjectInputStream wois = new SafeObjectInputStream(bis,
                        Thread.currentThread().getContextClassLoader(),
                        java.util.Arrays.asList("byte\\[\\]", "foo", "SerializationInjector",
                                "\\[Z","\\[B","\\[S","\\[I","\\[J","\\[F","\\[D","\\[C",
                                "java..*", "sun.util.calendar..*", "org.apache.ofbiz..*"));) {
                        // "foo" and, "SerializationInjector" are used in UtilObjectTests::testGetObject
            return wois.readObject();
        }
    }

    public static boolean equalsHelper(Object o1, Object o2) {
        if (o1 == o2) {
            // handles same-reference, or null
            return true;
        } else if (o1 == null || o2 == null) {
            // either o1 or o2 is null, but not both
            return false;
        } else {
            return o1.equals(o2);
        }
    }

    public static <T> int compareToHelper(Comparable<T> o1, T o2) {
        if (o1 == o2) {
            // handles same-reference, or null
            return 0;
        } else if (o1 == null) {
            return -1;
        } else if (o2 == null) {
            // either o1 or o2 is null, but not both
            return 1;
        } else {
            return o1.compareTo(o2);
        }
    }

    public static int doHashCode(Object o1) {
        if (o1 == null) {
            return 0;
        }
        if (o1.getClass().isArray()) {
            int length = Array.getLength(o1);
            int result = 0;
            for (int i = 0; i < length; i++) {
                result += doHashCode(Array.get(o1, i));
            }
            return result;
        }
        return o1.hashCode();
    }

    public static <A, R> R getObjectFromFactory(Class<? extends Factory<R, A>> factoryInterface, A obj) throws ClassNotFoundException {
        Iterator<? extends Factory<R, A>> it = ServiceLoader.load(factoryInterface).iterator();
        while (it.hasNext()) {
            Factory<R, A> factory = it.next();
            R instance = factory.getInstance(obj);
            if (instance != null) {
                return instance;
            }
        }
        throw new ClassNotFoundException(factoryInterface.getClass().getName());
    }
}

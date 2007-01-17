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
package org.ofbiz.base.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.InputStream;

/**
 * UtilObject
 *
 */
public class UtilObject {

    public static final String module = UtilObject.class.getName();

    public static byte[] getBytes(InputStream is) {
        byte[] buffer = new byte[4 * 1024];
        ByteArrayOutputStream bos = null;
        byte[] data = null;
        try {
            bos = new ByteArrayOutputStream();
            
            int numBytesRead;
            while ((numBytesRead = is.read(buffer)) != -1) {
                bos.write(buffer, 0, numBytesRead);
            }
            data = bos.toByteArray();
        } catch (IOException e) {
            Debug.logError(e, module);
        } finally {
            try {
                if (is != null) {
                    is.close();
                }
                if (bos != null) {
                    bos.close();
                }
            } catch (IOException e) {
                Debug.logError(e, module);
            }
        }

        return data;
    }

    /** Serialize an object to a byte array */
    public static byte[] getBytes(Object obj) {
        ByteArrayOutputStream bos = null;
        ObjectOutputStream oos = null;
        byte[] data = null;
        try {
            bos = new ByteArrayOutputStream();
            oos = new ObjectOutputStream(bos);
            oos.writeObject(obj);
            data = bos.toByteArray();
        } catch (IOException e) {
            Debug.logError(e, module);
        } finally {
            try {
                if (oos != null) {
                    oos.flush();
                    oos.close();
                }
                if (bos != null) {
                    bos.close();
                }
            } catch (IOException e) {
                Debug.logError(e, module);
            }
        }

        return data;
    }

    public static long getByteCount(Object obj) {
        OutputStreamByteCount osbc = null;
        ObjectOutputStream oos = null;
        try {
            osbc = new OutputStreamByteCount();
            oos = new ObjectOutputStream(osbc);
            oos.writeObject(obj);
        } catch (IOException e) {
            Debug.logError(e, module);
        } finally {
            try {
                if (oos != null) {
                    oos.flush();
                    oos.close();
                }
                if (osbc != null) {
                    osbc.close();
                }
            } catch (IOException e) {
                Debug.logError(e, module);
            }
        }
        if (osbc != null) {
            return osbc.getByteCount();
        } else {
            return 0;
        }
    }

    /** Deserialize a byte array back to an object */
    public static Object getObject(byte[] bytes) {
        ByteArrayInputStream bis = null;
        ObjectInputStream ois = null;
        Object obj = null;

        try {
            bis = new ByteArrayInputStream(bytes);
            ois = new ObjectInputStream(bis, Thread.currentThread().getContextClassLoader());
            obj = ois.readObject();
        } catch (ClassNotFoundException e) {
            Debug.logError(e, module);
        } catch (IOException e) {
            Debug.logError(e, module);
        } finally {
            try {
                if (ois != null) {
                    ois.close();
                }
                if (bis != null) {
                    bis.close();
                }
            } catch (IOException e) {
                Debug.logError(e, module);
            }
        }

        return obj;
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

    public static int compareToHelper(Comparable o1, Object o2) {
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
        if (o1 == null) return 0;
        return o1.hashCode();
    }
}

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
package org.ofbiz.entity.util;

import java.io.Serializable;

/**
 * A very simple class to wrap a byte array for persistence.
 */
public class ByteWrapper implements Comparable<ByteWrapper>, Serializable {
    protected byte[] bytes;

    protected ByteWrapper() {}

    public ByteWrapper(byte[] bytes) {
        this.bytes = bytes;
    }

    public byte[] getBytes() {
        return bytes;
    }

    public byte getByte(int pos) {
        return bytes[pos];
    }

    public int getLength() {
        return bytes.length;
    }

    public int compareTo(ByteWrapper other) {
        int r = bytes.length - other.bytes.length;
        if (r != 0) return r;
        int i = 0;
        for (i = 0; i < bytes.length; i++) {
            r = bytes[i] - other.bytes[i];
            if (r != 0) return r;
        }
        return 0;
    }

    public boolean equals(Object obj) {
        if (obj instanceof ByteWrapper) return compareTo((ByteWrapper) obj) == 0;
        return false;
    }

    public int hashCode() {
        int hashCode = 0;
        for (byte b: bytes) {
            hashCode ^= b;
        }
        return hashCode;
    }
}

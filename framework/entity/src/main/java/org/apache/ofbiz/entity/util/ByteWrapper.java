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
package org.apache.ofbiz.entity.util;

import java.io.Serializable;

/**
 * @deprecated
 * NOTE DEJ20071022: deprecating this because we want to save the byte[] directly instead of inside a serialized
 * object, which makes it hard for other apps to use the data, and causes problems if this object is ever updated
 *
 * A very simple class to wrap a byte array for persistence.
 */
@SuppressWarnings("serial")
@Deprecated
public class ByteWrapper implements Serializable {
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
}

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
package org.ofbiz.base.util.cache;

import java.io.IOException;

import jdbm.RecordManager;
import jdbm.helper.ISerializationHandler;
import jdbm.helper.Serializer;

import org.ofbiz.base.util.UtilObject;

/**
 * JDBC Serializer which uses OFBiz internal serialization
 * (needed do to the fact that we do dynamic class loading)
 *
 */
@SuppressWarnings("serial")
public class JdbmSerializer implements Serializer, ISerializationHandler {

    public byte[] serialize(Object o) throws IOException {
        return UtilObject.getBytes(o);
    }

    public byte[] serialize(RecordManager recman, long recid, Object o) throws IOException {
        return UtilObject.getBytes(o);
    }

    public Object deserialize(byte[] bytes) throws IOException {
        return UtilObject.getObject(bytes);
    }

    public Object deserialize(RecordManager recman, long recid, byte[] bytes) throws IOException {
        return UtilObject.getObject(bytes);
    }
}

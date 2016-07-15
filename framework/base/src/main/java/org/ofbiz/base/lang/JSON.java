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
package org.ofbiz.base.lang;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;

import org.apache.commons.io.IOUtils;
import org.ofbiz.base.util.Assert;

import com.fasterxml.jackson.databind.ObjectMapper;

/** A JSON object. */
@ThreadSafe
public final class JSON {

    // TODO: Find a generic way to modify mapper options
    private static final ObjectMapper mapper = new ObjectMapper();

    /**
     * Creates a <code>JSON</code> instance from an <code>InputStream</code>.
     * The method assumes the character set is UTF-8.
     * 
     * @param inStream
     * @return a <code>JSON</code> instance
     * @throws IOException
     */
    public static JSON from(InputStream inStream) throws IOException {
        Assert.notNull("inStream", inStream);
        String jsonString = IOUtils.toString(inStream, "UTF-8");
        return from(jsonString);
    }

    /**
     * Creates a <code>JSON</code> instance from an unknown data type.
     * 
     * @param object
     * @return a <code>JSON</code> instance
     * @throws IOException
     */
    public static JSON from(Object object) throws IOException {
        Assert.notNull("object", object);
        try {
            return from(mapper.writeValueAsString(object));
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    /**
     * Creates a <code>JSON</code> instance from a <code>Reader</code>.
     * 
     * @param reader
     * @return a <code>JSON</code> instance
     * @throws IOException
     */
    public static JSON from(Reader reader) throws IOException {
        Assert.notNull("reader", reader);
        String jsonString = IOUtils.toString(reader);
        return from(jsonString);
    }

    /**
     * Creates a <code>JSON</code> instance from a <code>String</code>.
     * 
     * @param jsonString
     * @return a <code>JSON</code> instance
     */
    public static JSON from(String jsonString) {
        Assert.notNull("jsonString", jsonString);
        // TODO: Validate String
        return new JSON(jsonString);
    }

    private final String jsonString;

    private JSON(String jsonString) {
        this.jsonString = jsonString;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof JSON) {
            return jsonString.equals(((JSON)obj).jsonString);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return jsonString.hashCode();
    }

    /**
     * Converts this <code>JSON</code> object to the specified type.
     * 
     * @param targetClass
     * @return an object of the specified type
     * @throws IOException
     */
    public <T> T toObject(Class<T> targetClass) throws IOException {
        Assert.notNull("targetClass", targetClass);
        try {
            return mapper.readValue(jsonString, targetClass);
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    @Override
    public String toString() {
        return jsonString;
    }
}

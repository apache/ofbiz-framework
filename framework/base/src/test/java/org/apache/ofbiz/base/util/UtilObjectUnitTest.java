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
package org.apache.ofbiz.base.util;

import static org.apache.ofbiz.base.util.UtilObject.getObjectException;
import static org.hamcrest.Matchers.contains;
import static org.junit.Assert.assertThat;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

public class UtilObjectUnitTest {
    // Test reading a basic list of string object.
    @Test
    public void testGetObjectExceptionSafe() throws IOException, ClassNotFoundException {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
                ObjectOutputStream oos = new ObjectOutputStream(bos)) {
            List<String> allowedObject = Arrays.asList("foo", "bar", "baz");
            oos.writeObject(allowedObject);
            List<String> readObject = UtilGenerics.cast(getObjectException(bos.toByteArray()));
            assertThat(readObject, contains("foo", "bar", "baz"));
        }
    }

    // Test reading a valid customized list of string object.
    @Test
    public void testGetObjectExceptionCustomized() throws IOException, ClassNotFoundException {
        UtilProperties.setPropertyValueInMemory("SafeObjectInputStream", "ListOfSafeObjectsForInputStream",
                "java.util.Arrays.ArrayList,java.lang.String");
        testGetObjectExceptionSafe();

        // With extra whitespace
        UtilProperties.setPropertyValueInMemory("SafeObjectInputStream", "ListOfSafeObjectsForInputStream",
                "java.util.Arrays.ArrayList, java.lang.String");
        testGetObjectExceptionSafe();
    }

    // Test reading a basic list of string object after forbidding such kind of objects.
    @Test(expected = ClassCastException.class)
    public void testGetObjectExceptionUnsafe() throws IOException, ClassNotFoundException {
        // Only allow object of type where the package prefix is 'org.apache.ofbiz'
        UtilProperties.setPropertyValueInMemory("SafeObjectInputStream", "ListOfSafeObjectsForInputStream",
                "org.apache.ofbiz..*");
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
                ObjectOutputStream oos = new ObjectOutputStream(bos)) {
            List<String> forbiddenObject = Arrays.asList("foo", "bar", "baz");
            oos.writeObject(forbiddenObject);
            getObjectException(bos.toByteArray());
        }
    }
}

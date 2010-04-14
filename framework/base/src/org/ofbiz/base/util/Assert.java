/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.ofbiz.base.util;

/** Basic assertions.
 * 
 */
public class Assert {

    /**
     * Tests if an argument is not null and can be cast to a specified class.
     * <p><code>Assert.argumentCanBeCastTo("foo", foo, Map.class);</code></p>
     * 
     * @param argumentName
     * @param argumentObject
     * @param targetClass
     * @throws IllegalArgumentException
     */
    public static void argumentCanBeCastTo(String argumentName, Object argumentObject, Class<?> targetClass) {
        argumentNotNull(argumentName, argumentObject);
        if (!targetClass.isAssignableFrom(argumentObject.getClass())) {
            throw new IllegalArgumentException(argumentName + " is not a " + targetClass.getName());
        }
    }

    /**
     * Tests if an argument is not null and is equal to an object.
     * <p><code>Assert.argumentEqualsObject("foo", foo, new Foo());</code></p>
     * 
     * @param argumentName
     * @param argumentObject
     * @param targetObject
     * @throws IllegalArgumentException
     */
    public static void argumentEqualsObject(String argumentName, Object argumentObject, Object targetObject) {
        argumentNotNull(argumentName, argumentObject);
        if (!argumentObject.equals(targetObject)) {
            throw new IllegalArgumentException(argumentName + " is not equal to " + targetObject);
        }
    }

    /**
     * Tests if an argument is not null and is an instance of a class.
     * <p><code>Assert.argumentIsClass("foo", foo, HashMap.class);</code></p>
     * 
     * @param argumentName
     * @param argumentObject
     * @param targetClass
     * @throws IllegalArgumentException
     */
    public static void argumentIsClass(String argumentName, Object argumentObject, Class<?> targetClass) {
        argumentNotNull(argumentName, argumentObject);
        if (argumentObject.getClass() != targetClass) {
            throw new IllegalArgumentException(argumentName + " is not a " + targetClass.getName());
        }
    }

    /**
     * Tests an argument for <code>null</code>.
     * <p><code>Assert.argumentNotNull("foo", foo);</code></p>
     * 
     * @param argumentName
     * @param objectToTest
     * @throws IllegalArgumentException
     */
    public static void argumentNotNull(String argumentName, Object objectToTest) {
        if (objectToTest == null) {
            throw new IllegalArgumentException(argumentName + " cannot be null");
        }
    }

    /**
     * Tests a list of arguments for <code>null</code>.
     * <p><code>Assert.argumentsNotNull("foo", foo, "bar", bar, ...);</code></p>
     * 
     * @param arguments
     * @throws IllegalArgumentException
     */
    public static void argumentsNotNull(Object... arguments) {
        for (int i = 0; i < arguments.length;) {
            argumentNotNull((String) arguments[i++], arguments[i++]);
        }
    }

    private Assert() {}
}

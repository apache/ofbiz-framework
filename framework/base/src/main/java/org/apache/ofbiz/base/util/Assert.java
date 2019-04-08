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

package org.apache.ofbiz.base.util;

import java.util.Collection;
import java.util.Map;

import org.apache.ofbiz.base.lang.ThreadSafe;

/** Basic assertions. The static methods in this class provide a convenient way
 * to test method arguments. All of the methods in this class throw <code>
 * IllegalArgumentException</code> if the method arguments are invalid.
 * 
 */
@ThreadSafe
public class Assert {

    /**
     * Tests if an argument is not null and can be cast to a specified class.
     * <p><code>Assert.isAssignableTo("foo", foo, Foo.class);</code></p>
     * 
     * @param argumentName
     * @param argumentObject
     * @param targetClass
     * @throws IllegalArgumentException
     */
    public static void isAssignableTo(String argumentName, Object argumentObject, Class<?> targetClass) {
        notNull(argumentName, argumentObject);
        if (!targetClass.isAssignableFrom(argumentObject.getClass())) {
            throw new IllegalArgumentException(argumentName + " cannot be assigned to " + targetClass.getName());
        }
    }

    /**
     * Tests if an argument is not null and is an instance of a specified class.
     * <p><code>Assert.isInstanceOf("foo", foo, Foo.class);</code></p>
     * 
     * @param argumentName
     * @param argumentObject
     * @param targetClass
     * @throws IllegalArgumentException
     */
    public static void isInstanceOf(String argumentName, Object argumentObject, Class<?> targetClass) {
        notNull(argumentName, argumentObject);
        if (!targetClass.isInstance(argumentObject)) {
            throw new IllegalArgumentException(argumentName + " is not an instance of " + targetClass.getName());
        }
    }

    /**
     * Tests if an argument is not null and is an instance of one of the specified classes.
     * <p><code>Assert.isInstanceOf("foo", foo, Foo.class, Bar.class, ...);</code></p>
     * 
     * @param argumentName
     * @param argumentObject
     * @param targetClasses
     * @throws IllegalArgumentException
     */
    public static void isInstanceOf(String argumentName, Object argumentObject, Class<?>... targetClasses) {
        notNull(argumentName, argumentObject);
        for (int i = 0; i < targetClasses.length;) {
            if (targetClasses[i++].isInstance(argumentObject)) {
                return;
            }
        }
        StringBuilder sb = new StringBuilder(argumentName);
        sb.append(" must be an instance of");
        for (int i = 0; i < targetClasses.length;) {
            if (i != 0) {
                sb.append(",");
            }
            sb.append(" ").append(targetClasses[i++].getName());
        }
        throw new IllegalArgumentException(sb.toString());
    }

    /**
     * Tests if an argument is not null and is not an instance of a specified class.
     * <p><code>Assert.isNotInstanceOf("foo", foo, Foo.class);</code></p>
     * 
     * @param argumentName
     * @param argumentObject
     * @param targetClass
     * @throws IllegalArgumentException
     */
    public static void isNotInstanceOf(String argumentName, Object argumentObject, Class<?> targetClass) {
        notNull(argumentName, argumentObject);
        if (targetClass.isInstance(argumentObject)) {
            throw new IllegalArgumentException(argumentName + " cannot be an instance of " + targetClass.getName());
        }
    }

    /**
     * Tests if an argument is not null and is not an instance of one of the specified classes.
     * <p><code>Assert.isNotInstanceOf("foo", foo, Foo.class, Bar.class, ...);</code></p>
     * 
     * @param argumentName
     * @param argumentObject
     * @param targetClasses
     * @throws IllegalArgumentException
     */
    public static void isNotInstanceOf(String argumentName, Object argumentObject, Class<?>... targetClasses) {
        notNull(argumentName, argumentObject);
        for (int i = 0; i < targetClasses.length;) {
            Class<?> targetClass = targetClasses[i++];
            if (targetClass.isInstance(argumentObject)) {
                throw new IllegalArgumentException(argumentName + " cannot be an instance of " + targetClass.getName());
            }
        }
    }

    /**
     * Tests if an argument is not null and is not empty.
     * <p><code>Assert.notEmpty("foo", foo);</code></p>
     * 
     * @param argumentName
     * @param argumentObject
     * @throws IllegalArgumentException
     */
    public static void notEmpty(String argumentName, String argumentObject) {
        notNull(argumentName, argumentObject);
        if (argumentObject.length() == 0) {
            throw new IllegalArgumentException(argumentName + " cannot be empty");
        }
    }

    /**
     * Tests if an argument is not null and is not empty.
     * <p><code>Assert.notEmpty("foo", foo);</code></p>
     * 
     * @param argumentName
     * @param argumentObject
     * @throws IllegalArgumentException
     */
    public static <T extends Map<?, ?>> void notEmpty(String argumentName, T argumentObject) {
        notNull(argumentName, argumentObject);
        if (argumentObject.size() == 0) {
            throw new IllegalArgumentException(argumentName + " cannot be empty");
        }
    }

    /**
     * Tests if an argument is not null and is not empty.
     * <p><code>Assert.notEmpty("foo", foo);</code></p>
     * 
     * @param argumentName
     * @param argumentObject
     * @throws IllegalArgumentException
     */
    public static <T extends Collection<?>> void notEmpty(String argumentName, T argumentObject) {
        notNull(argumentName, argumentObject);
        if (argumentObject.size() == 0) {
            throw new IllegalArgumentException(argumentName + " cannot be empty");
        }
    }

    /**
     * Tests if an argument is not null and is not empty.
     * <p><code>Assert.notEmpty("foo", foo);</code></p>
     * 
     * @param argumentName
     * @param argumentObject
     * @throws IllegalArgumentException
     */
    public static <T> void notEmpty(String argumentName, T[] argumentObject) {
        notNull(argumentName, argumentObject);
        if (argumentObject.length == 0) {
            throw new IllegalArgumentException(argumentName + " cannot be empty");
        }
    }

    /**
     * Tests a list of arguments for <code>null</code>.
     * <p><code>Assert.notNull("foo", foo, "bar", bar, ...);</code></p>
     * 
     * @param arguments
     * @throws IllegalArgumentException
     */
    public static void notNull(Object... arguments) {
        for (int i = 0; i < arguments.length;) {
            notNull((String) arguments[i++], arguments[i++]);
        }
    }

    /**
     * Tests an argument for <code>null</code>.
     * <p><code>Assert.notNull("foo", foo);</code></p>
     * 
     * @param argumentName
     * @param objectToTest
     * @throws IllegalArgumentException
     */
    public static void notNull(String argumentName, Object objectToTest) {
        if (objectToTest == null) {
            throw new IllegalArgumentException(argumentName + " cannot be null");
        }
    }

    private Assert() {}
}

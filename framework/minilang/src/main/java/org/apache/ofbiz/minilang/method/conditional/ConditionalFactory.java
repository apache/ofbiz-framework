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
package org.apache.ofbiz.minilang.method.conditional;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.ServiceLoader;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilGenerics;
import org.apache.ofbiz.minilang.MiniLangException;
import org.apache.ofbiz.minilang.SimpleMethod;
import org.w3c.dom.Element;

/**
 * An abstract factory class for creating &lt;if&gt; element sub-element implementations.
 * <p>Mini-language can be extended to support additional condition elements
 * by extending this class to provide custom conditional element implementations.
 * </p>
 */
public abstract class ConditionalFactory<C extends Conditional> {

    private static final String MODULE = ConditionalFactory.class.getName();
    private static final Map<String, ConditionalFactory<?>> CONDITIONAL_FACTORIES;

    static {
        Map<String, ConditionalFactory<?>> factories = new HashMap<>();
        Iterator<ConditionalFactory<?>> it = UtilGenerics.cast(ServiceLoader.load(ConditionalFactory.class,
                ConditionalFactory.class.getClassLoader()).iterator());
        while (it.hasNext()) {
            ConditionalFactory<?> factory = it.next();
            factories.put(factory.getName(), factory);
        }
        CONDITIONAL_FACTORIES = Collections.unmodifiableMap(factories);
    }

    public static Conditional makeConditional(Element element, SimpleMethod simpleMethod) throws MiniLangException {
        String tagName = element.getTagName();
        ConditionalFactory<?> factory = CONDITIONAL_FACTORIES.get(tagName);
        if (factory != null) {
            return factory.createCondition(element, simpleMethod);
        } else {
            Debug.logWarning("Found an unknown if condition: " + tagName, MODULE);
            return null;
        }
    }

    public abstract C createCondition(Element element, SimpleMethod simpleMethod) throws MiniLangException;

    public abstract String getName();
}

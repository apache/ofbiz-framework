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
package org.ofbiz.minilang.method.conditional;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.imageio.spi.ServiceRegistry;

import org.w3c.dom.*;
import org.ofbiz.base.util.*;
import org.ofbiz.minilang.*;

/**
 * Creates Conditional objects according to the element that is passed.
 */
public abstract class ConditionalFactory<C extends Conditional> {
    private static final Map<String, ConditionalFactory<?>> conditionalFactories;
    static {
        Map<String, ConditionalFactory<?>> factories = new HashMap<String, ConditionalFactory<?>>();
        Iterator<ConditionalFactory<?>> it = UtilGenerics.cast(ServiceRegistry.lookupProviders(ConditionalFactory.class, ConditionalFactory.class.getClassLoader()));
        while (it.hasNext()) {
            ConditionalFactory<?> factory = it.next();
            factories.put(factory.getName(), factory);
        }
        conditionalFactories = Collections.unmodifiableMap(factories);
    }

    public static final String module = ConditionalFactory.class.getName();

    public static Conditional makeConditional(Element element, SimpleMethod simpleMethod) {
        String tagName = element.getTagName();

        ConditionalFactory<?> factory = conditionalFactories.get(tagName);
        if (factory != null) {
            return factory.createCondition(element, simpleMethod);
        } else {
            Debug.logWarning("Found an unknown if condition: " + tagName, module);
            return null;
        }
    }

    public abstract C createCondition(Element element, SimpleMethod simpleMethod);
    public abstract String getName();
}

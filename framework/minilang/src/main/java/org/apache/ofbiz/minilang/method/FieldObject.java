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
package org.apache.ofbiz.minilang.method;

import org.apache.ofbiz.base.util.ObjectType;
import org.apache.ofbiz.base.util.UtilGenerics;
import org.apache.ofbiz.base.util.collections.FlexibleMapAccessor;
import org.apache.ofbiz.minilang.SimpleMethod;
import org.w3c.dom.Element;

/**
 * Implements the &lt;field&gt; element.
 * 
 * @see <a href="https://cwiki.apache.org/confluence/display/OFBIZ/Mini+Language+-+minilang+-+simple-method+-+Reference">Mini-language Reference</a>
 */
public final class FieldObject<T> extends MethodObject<T> {

    private final FlexibleMapAccessor<Object> fieldFma;
    private final String type;

    public FieldObject(Element element, SimpleMethod simpleMethod) {
        super(element, simpleMethod);
        this.fieldFma = FlexibleMapAccessor.getInstance(element.getAttribute("field"));
        String typeAttribute = element.getAttribute("type");
        if (typeAttribute.isEmpty()) {
            this.type = "java.lang.String";
        } else {
            this.type = typeAttribute;
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public T getObject(MethodContext methodContext) {
        return (T) this.fieldFma.get(methodContext.getEnvMap());
    }

    @Override
    public Class<T> getTypeClass(MethodContext methodContext) throws ClassNotFoundException {
        return UtilGenerics.cast(ObjectType.loadClass(this.type, methodContext.getLoader()));
    }

    @Override
    public String getTypeName() {
        return this.type;
    }
}

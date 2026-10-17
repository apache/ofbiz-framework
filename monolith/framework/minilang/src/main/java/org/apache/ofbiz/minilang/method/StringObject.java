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

import org.apache.ofbiz.base.util.UtilXml;
import org.apache.ofbiz.base.util.string.FlexibleStringExpander;
import org.apache.ofbiz.minilang.SimpleMethod;
import org.w3c.dom.Element;

/**
 * Specifies a <code>java.lang.String</code> to be passed as an argument to a method call.
 */
public final class StringObject extends MethodObject<String> {

    private final FlexibleStringExpander cdataValueFse;
    private final FlexibleStringExpander valueFse;

    public StringObject(Element element, SimpleMethod simpleMethod) {
        super(element, simpleMethod);
        this.cdataValueFse = FlexibleStringExpander.getInstance(UtilXml.elementValue(element));
        this.valueFse = FlexibleStringExpander.getInstance(element.getAttribute("value"));
    }

    @Override
    public String getObject(MethodContext methodContext) {
        String value = this.valueFse.expandString(methodContext.getEnvMap());
        String cdataValue = this.cdataValueFse.expandString(methodContext.getEnvMap());
        return value.concat(cdataValue);
    }

    @Override
    public Class<String> getTypeClass(MethodContext methodContext) throws ClassNotFoundException {
        return java.lang.String.class;
    }

    /** Get the name for the type of the object */
    @Override
    public String getTypeName() {
        return "java.lang.String";
    }
}

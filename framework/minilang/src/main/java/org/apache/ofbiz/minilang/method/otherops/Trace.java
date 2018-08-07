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
package org.apache.ofbiz.minilang.method.otherops;

import java.util.Collections;
import java.util.List;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.minilang.MiniLangException;
import org.apache.ofbiz.minilang.MiniLangValidate;
import org.apache.ofbiz.minilang.SimpleMethod;
import org.apache.ofbiz.minilang.method.MethodContext;
import org.apache.ofbiz.minilang.method.MethodOperation;
import org.w3c.dom.Element;

/**
 * Implements the &lt;trace&gt; element.
 * 
 * @see <a href="https://cwiki.apache.org/confluence/display/OFBIZ/Mini+Language+-+minilang+-+simple-method+-+Reference">Mini-language Reference</a>
 */
public final class Trace extends MethodOperation {

    public static final String module = Trace.class.getName();

    private final int level;
    private final List<MethodOperation> methodOperations;

    public Trace(Element element, SimpleMethod simpleMethod) throws MiniLangException {
        super(element, simpleMethod);
        if (MiniLangValidate.validationOn()) {
            MiniLangValidate.attributeNames(simpleMethod, element, "level");
            MiniLangValidate.constantAttributes(simpleMethod, element, "level");
        }
        String levelAttribute = MiniLangValidate.checkAttribute(element.getAttribute("level"), "info");
        Integer levelInt = Debug.getLevelFromString(levelAttribute);
        if (levelInt == null) {
            MiniLangValidate.handleError("Invalid level attribute", simpleMethod, element);
            this.level = Debug.INFO;
        } else {
            this.level = levelInt;
        }
        methodOperations = Collections.unmodifiableList(SimpleMethod.readOperations(element, simpleMethod));
    }

    @Override
    public boolean exec(MethodContext methodContext) throws MiniLangException {
        methodContext.setTraceOn(this.level);
        outputTraceMessage(methodContext, "Trace on.");
        try {
            return SimpleMethod.runSubOps(methodOperations, methodContext);
        } finally {
            methodContext.setTraceOff();
            outputTraceMessage(methodContext, "Trace off.");
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("<trace ");
        sb.append("level=\"").append(Log.LEVEL_ARRAY[this.level]).append("\" >");
        return sb.toString();
    }

    /**
     * A factory for the &lt;trace&gt; element.
     */
    public static final class TraceFactory implements Factory<Trace> {
        @Override
        public Trace createMethodOperation(Element element, SimpleMethod simpleMethod) throws MiniLangException {
            return new Trace(element, simpleMethod);
        }

        @Override
        public String getName() {
            return "trace";
        }
    }
}

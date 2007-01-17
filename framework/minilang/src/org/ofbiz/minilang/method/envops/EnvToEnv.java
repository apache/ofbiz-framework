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
package org.ofbiz.minilang.method.envops;

import org.ofbiz.minilang.SimpleMethod;
import org.ofbiz.minilang.method.ContextAccessor;
import org.ofbiz.minilang.method.MethodContext;
import org.ofbiz.minilang.method.MethodOperation;
import org.w3c.dom.Element;

/**
 * Copies an environment field to a map field
 */
public class EnvToEnv extends MethodOperation {
    
    public static final String module = EnvToEnv.class.getName();

    protected ContextAccessor envAcsr;
    protected ContextAccessor toEnvAcsr;

    public EnvToEnv(Element element, SimpleMethod simpleMethod) {
        super(element, simpleMethod);
        envAcsr = new ContextAccessor(element.getAttribute("env-name"));
        toEnvAcsr = new ContextAccessor(element.getAttribute("to-env-name"));
    }

    public boolean exec(MethodContext methodContext) {
        toEnvAcsr.put(methodContext, envAcsr.get(methodContext));
        return true;
    }

    public String rawString() {
        return "<env-to-env env-name=\"" + this.envAcsr + "\" to-env-name=\"" + this.toEnvAcsr + "\"/>";
    }
    public String expandedString(MethodContext methodContext) {
        // TODO: something more than a stub/dummy
        return this.rawString();
    }
}

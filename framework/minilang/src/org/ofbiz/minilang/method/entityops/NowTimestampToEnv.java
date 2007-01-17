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
package org.ofbiz.minilang.method.entityops;

import org.w3c.dom.*;

import org.ofbiz.minilang.*;
import org.ofbiz.minilang.method.*;

/**
 * Creates a java.sql.Timestamp with the current date/time in it and puts it in the env
 */
public class NowTimestampToEnv extends MethodOperation {
    
    ContextAccessor envAcsr;

    public NowTimestampToEnv(Element element, SimpleMethod simpleMethod) {
        super(element, simpleMethod);
        envAcsr = new ContextAccessor(element.getAttribute("env-name"));
    }

    public boolean exec(MethodContext methodContext) {
        envAcsr.put(methodContext, new java.sql.Timestamp(System.currentTimeMillis()));
        return true;
    }

    public String rawString() {
        // TODO: something more than the empty tag
        return "<now-timestamp-to-env/>";
    }
    public String expandedString(MethodContext methodContext) {
        // TODO: something more than a stub/dummy
        return this.rawString();
    }
}

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
package org.ofbiz.minilang;

import org.ofbiz.minilang.method.MethodOperation;

/**
 * Thrown to indicate a Mini-language run-time error. 
 */
@SuppressWarnings("serial")
public class MiniLangRuntimeException extends MiniLangException {

    private final MethodOperation operation;

    public MiniLangRuntimeException(String str, MethodOperation operation) {
        super(str);
        this.operation = operation;
    }

    @Override
    public String getMessage() {
        StringBuilder sb = new StringBuilder(super.getMessage());
        if (operation != null) {
            SimpleMethod method = operation.getSimpleMethod();
            sb.append(" Method = ").append(method.methodName).append(", File = ").append(method.getFromLocation());
            sb.append(", Element = <").append(operation.getTagName()).append(">");
            sb.append(", Line ").append(operation.getLineNumber());
        }
        return sb.toString();
    }
}

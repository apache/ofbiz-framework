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
package org.apache.ofbiz.minilang;

/**
 * Thrown to indicate a Mini-language run-time error.
 */
@SuppressWarnings("serial")
public class MiniLangRuntimeException extends MiniLangException {

    private final MiniLangElement element;

    public MiniLangRuntimeException(String str, MiniLangElement element) {
        super(str);
        this.element = element;
    }

    public MiniLangRuntimeException(Throwable nested, MiniLangElement element) {
        super(nested);
        this.element = element;
    }

    @Override
    public String getMessage() {
        StringBuilder sb = new StringBuilder(super.getMessage());
        if (this.element != null) {
            SimpleMethod method = this.element.getSimpleMethod();
            sb.append(" Method = ").append(method.getMethodName()).append(", File = ").append(method.getFromLocation());
            sb.append(", Element = <").append(this.element.getTagName()).append(">");
            sb.append(", Line ").append(this.element.getLineNumber());
        }
        return sb.toString();
    }
}

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

import org.w3c.dom.Element;

/**
 * Thrown to indicate that a Mini-language element is invalid. 
 */
@SuppressWarnings("serial")
public class ValidationException extends MiniLangException {

    private final SimpleMethod method;
    private final Element element;

    public ValidationException(String str, SimpleMethod method, Element element) {
        super(str);
        this.method = method;
        this.element = element;
    }

    @Override
    public String getMessage() {
        StringBuilder sb = new StringBuilder(super.getMessage());
        if (method != null) {
            sb.append(" Method = ").append(method.getMethodName()).append(", File = ").append(method.getFromLocation());
        }
        if (element != null) {
            sb.append(", Element = <").append(element.getTagName()).append(">");
            Object lineNumber = element.getUserData("startLine");
            if (lineNumber != null) {
                sb.append(", Line ").append(lineNumber);
            }
        }
        return sb.toString();
    }
}

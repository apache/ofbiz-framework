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
package org.apache.ofbiz.minilang.method.envops;

import org.apache.ofbiz.minilang.MiniLangException;
import org.apache.ofbiz.minilang.SimpleMethod;
import org.apache.ofbiz.minilang.method.MethodContext;
import org.apache.ofbiz.minilang.method.MethodOperation;
import org.w3c.dom.Element;

/**
 * Implements the &lt;continue&gt; element.
 * 
 * @see <a href="https://cwiki.apache.org/confluence/display/OFBIZ/Mini+Language+-+minilang+-+simple-method+-+Reference">Mini-language Referenc</a>
 */
public class Continue extends MethodOperation {

    public Continue(Element element, SimpleMethod simpleMethod) throws MiniLangException {
        super(element, simpleMethod);
    }

    @Override
    public boolean exec(MethodContext methodContext) throws MiniLangException {
        throw new ContinueElementException();
    }

    @Override
    public String toString() {
        return "<continue/>";
    }

    @SuppressWarnings("serial")
    public class ContinueElementException extends MiniLangException {

        public ContinueElementException() {
            super("<continue> element encountered without enclosing loop");
        }

        @Override
        public String getMessage() {
            StringBuilder sb = new StringBuilder(super.getMessage());
            SimpleMethod method = getSimpleMethod();
            sb.append(" Method = ").append(method.getMethodName()).append(", File = ").append(method.getFromLocation());
            sb.append(", Element = <continue>, Line ").append(getLineNumber());
            return sb.toString();
        }
    }

    /**
     * A factory for the &lt;continue&gt; element.
     */
    public static final class ContinueFactory implements Factory<Continue> {
        @Override
        public Continue createMethodOperation(Element element, SimpleMethod simpleMethod) throws MiniLangException {
            return new Continue(element, simpleMethod);
        }

        @Override
        public String getName() {
            return "continue";
        }
    }
}

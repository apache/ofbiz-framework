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
package org.ofbiz.minilang.method.callops;

import java.io.*;
import java.util.*;

import javolution.util.FastList;

import org.w3c.dom.*;
import org.ofbiz.base.util.*;
import org.ofbiz.minilang.*;
import org.ofbiz.minilang.method.*;

import bsh.*;

/**
 * Simple class to wrap messages that come either from a straight string or a properties file
 */
public class CallBsh extends MethodOperation {
    public static final class CallBshFactory implements Factory<CallBsh> {
        public CallBsh createMethodOperation(Element element, SimpleMethod simpleMethod) {
            return new CallBsh(element, simpleMethod);
        }

        public String getName() {
            return "call-bsh";
        }
    }

    public static final String module = CallBsh.class.getName();

    public static final int bufferLength = 4096;

    String inline = null;
    String resource = null;
    ContextAccessor<List<Object>> errorListAcsr;

    public CallBsh(Element element, SimpleMethod simpleMethod) {
        super(element, simpleMethod);
        inline = UtilXml.elementValue(element);
        resource = element.getAttribute("resource");
        errorListAcsr = new ContextAccessor<List<Object>>(element.getAttribute("error-list-name"), "error_list");

        if (UtilValidate.isNotEmpty(inline)) {// pre-parse/compile inlined bsh, only accessed here
        }
    }

    @Override
    public boolean exec(MethodContext methodContext) {
        List<Object> messages = errorListAcsr.get(methodContext);

        if (messages == null) {
            messages = FastList.newInstance();
            errorListAcsr.put(methodContext, messages);
        }

        Interpreter bsh = new Interpreter();
        bsh.setClassLoader(methodContext.getLoader());

        try {
            // setup environment
            for (Map.Entry<String, Object> entry: methodContext) {
                bsh.set(entry.getKey(), entry.getValue());
            }

            // run external, from resource, first if resource specified
            if (UtilValidate.isNotEmpty(resource)) {
                String resource = methodContext.expandString(this.resource);
                InputStream is = methodContext.getLoader().getResourceAsStream(resource);

                if (is == null) {
                    messages.add("Could not find bsh resource: " + resource);
                } else {
                    try {
                        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                        StringBuilder outSb = new StringBuilder();

                        String tempStr = null;

                        while ((tempStr = reader.readLine()) != null) {
                            outSb.append(tempStr);
                            outSb.append('\n');
                        }

                        Object resourceResult = bsh.eval(outSb.toString());

                        // if map is returned, copy values into env
                        if ((resourceResult != null) && (resourceResult instanceof Map<?, ?>)) {
                            methodContext.putAllEnv(UtilGenerics.<String, Object>checkMap(resourceResult));
                        }
                    } catch (IOException e) {
                        messages.add("IO error loading bsh resource: " + e.getMessage());
                    }
                }
            }

            if (Debug.verboseOn()) Debug.logVerbose("Running inline BSH script: " + inline, module);
            // run inlined second to it can override the one from the property
            Object inlineResult = bsh.eval(inline);
            if (Debug.verboseOn()) Debug.logVerbose("Result of inline BSH script: " + inlineResult, module);

            // if map is returned, copy values into env
            if ((inlineResult != null) && (inlineResult instanceof Map<?, ?>)) {
                methodContext.putAllEnv(UtilGenerics.<String, Object>checkMap(inlineResult));
            }
        } catch (EvalError e) {
            Debug.logError(e, "BeanShell execution caused an error", module);
            messages.add("BeanShell execution caused an error: " + e.getMessage());
        }

        // always return true, error messages just go on the error list
        return true;
    }

    @Override
    public String rawString() {
        // TODO: something more than the empty tag
        return "<call-bsh/>";
    }
    @Override
    public String expandedString(MethodContext methodContext) {
        // TODO: something more than a stub/dummy
        return this.rawString();
    }
}

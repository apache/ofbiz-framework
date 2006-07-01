/*
 * $Id: CallBsh.java 5462 2005-08-05 18:35:48Z jonesde $
 *
 *  Copyright (c) 2001-2005 The Open For Business Project - www.ofbiz.org
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a
 *  copy of this software and associated documentation files (the "Software"),
 *  to deal in the Software without restriction, including without limitation
 *  the rights to use, copy, modify, merge, publish, distribute, sublicense,
 *  and/or sell copies of the Software, and to permit persons to whom the
 *  Software is furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included
 *  in all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
 *  OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 *  MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 *  IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY
 *  CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT
 *  OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR
 *  THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package org.ofbiz.minilang.method.callops;

import java.io.*;
import java.util.*;

import org.w3c.dom.*;
import org.ofbiz.base.util.*;
import org.ofbiz.minilang.*;
import org.ofbiz.minilang.method.*;

import bsh.*;

/**
 * Simple class to wrap messages that come either from a straight string or a properties file
 *
 * @author     <a href="mailto:jonesde@ofbiz.org">David E. Jones</a>
 * @version    $Rev$
 * @since      2.0
 */
public class CallBsh extends MethodOperation {
    
    public static final String module = CallBsh.class.getName();
    
    public static final int bufferLength = 4096;

    String inline = null;
    String resource = null;
    ContextAccessor errorListAcsr;

    public CallBsh(Element element, SimpleMethod simpleMethod) {
        super(element, simpleMethod);
        inline = UtilXml.elementValue(element);
        resource = element.getAttribute("resource");
        errorListAcsr = new ContextAccessor(element.getAttribute("error-list-name"), "error_list");

        if (inline != null && inline.length() > 0) {// pre-parse/compile inlined bsh, only accessed here
        }
    }

    public boolean exec(MethodContext methodContext) {
        List messages = (List) errorListAcsr.get(methodContext);

        if (messages == null) {
            messages = new LinkedList();
            errorListAcsr.put(methodContext, messages);
        }

        Interpreter bsh = new Interpreter();
        bsh.setClassLoader(methodContext.getLoader());

        try {
            // setup environment
            Iterator envEntries = methodContext.getEnvEntryIterator();

            while (envEntries.hasNext()) {
                Map.Entry entry = (Map.Entry) envEntries.next();
                bsh.set((String) entry.getKey(), entry.getValue());
            }

            // run external, from resource, first if resource specified
            if (resource != null && resource.length() > 0) {
                String resource = methodContext.expandString(this.resource);
                InputStream is = methodContext.getLoader().getResourceAsStream(resource);

                if (is == null) {
                    messages.add("Could not find bsh resource: " + resource);
                } else {
                    try {
                        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                        StringBuffer outSb = new StringBuffer();

                        String tempStr = null;

                        while ((tempStr = reader.readLine()) != null) {
                            outSb.append(tempStr);
                            outSb.append('\n');
                        }

                        Object resourceResult = bsh.eval(outSb.toString());

                        // if map is returned, copy values into env
                        if ((resourceResult != null) && (resourceResult instanceof Map)) {
                            methodContext.putAllEnv((Map) resourceResult);
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
            if ((inlineResult != null) && (inlineResult instanceof Map)) {
                methodContext.putAllEnv((Map) inlineResult);
            }
        } catch (EvalError e) {
            Debug.logError(e, "BeanShell execution caused an error", module);
            messages.add("BeanShell execution caused an error: " + e.getMessage());
        }

        // always return true, error messages just go on the error list
        return true;
    }

    public String rawString() {
        // TODO: something more than the empty tag
        return "<call-bsh/>";
    }
    public String expandedString(MethodContext methodContext) {
        // TODO: something more than a stub/dummy
        return this.rawString();
    }
}

/*
 * Copyright 2001-2006 The Apache Software Foundation
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package org.ofbiz.minilang.operation;

import java.util.*;

import org.apache.oro.text.regex.*;
import org.w3c.dom.*;

import org.ofbiz.base.util.*;

/**
 * Validates the current field using a regular expression
 *
 * @author     <a href="mailto:jonesde@ofbiz.org">David E. Jones</a>
 * @version    $Rev$
 * @since      2.0
 */
public class Regexp extends SimpleMapOperation {
    
    public static final String module = Regexp.class.getName();
    
    static PatternMatcher matcher = new Perl5Matcher();
    static PatternCompiler compiler = new Perl5Compiler();
    Pattern pattern = null;
    String expr;

    public Regexp(Element element, SimpleMapProcess simpleMapProcess) {
        super(element, simpleMapProcess);
        expr = element.getAttribute("expr");
        try {
            pattern = compiler.compile(expr);
        } catch (MalformedPatternException e) {
            Debug.logError(e, module);
        }
    }

    public void exec(Map inMap, Map results, List messages, Locale locale, ClassLoader loader) {
        Object obj = inMap.get(fieldName);

        String fieldValue = null;

        try {
            fieldValue = (String) ObjectType.simpleTypeConvert(obj, "String", null, locale);
        } catch (GeneralException e) {
            messages.add("Could not convert field value for comparison: " + e.getMessage());
            return;
        }

        if (pattern == null) {
            messages.add("Could not compile regular expression \"" + expr + "\" for validation");
            return;
        }

        if (!matcher.matches(fieldValue, pattern)) {
            addMessage(messages, loader, locale);
        }
    }
}

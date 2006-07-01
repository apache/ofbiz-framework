/*
 * $Id: Regexp.java 5462 2005-08-05 18:35:48Z jonesde $
 *
 *  Copyright (c) 2001, 2002 The Open For Business Project - www.ofbiz.org
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

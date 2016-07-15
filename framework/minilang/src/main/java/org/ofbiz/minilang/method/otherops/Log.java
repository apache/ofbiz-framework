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
package org.ofbiz.minilang.method.otherops;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilXml;
import org.ofbiz.base.util.string.FlexibleStringExpander;
import org.ofbiz.minilang.MiniLangException;
import org.ofbiz.minilang.MiniLangValidate;
import org.ofbiz.minilang.SimpleMethod;
import org.ofbiz.minilang.method.MethodContext;
import org.ofbiz.minilang.method.MethodOperation;
import org.w3c.dom.Element;

/**
 * Implements the &lt;log&gt; element.
 * 
 * @see <a href="https://cwiki.apache.org/confluence/display/OFBADMIN/Mini-language+Reference#Mini-languageReference-{{%3Clog%3E}}">Mini-language Reference</a>
 */
public final class Log extends MethodOperation {

    public static final String module = Log.class.getName();
    public static final String[] LEVEL_ARRAY = {"always", "verbose", "timing", "info", "important", "warning", "error", "fatal", "notify"};

    private final int level;
    private final FlexibleStringExpander messageFse;

    public Log(Element element, SimpleMethod simpleMethod) throws MiniLangException {
        super(element, simpleMethod);
        if (MiniLangValidate.validationOn()) {
            MiniLangValidate.attributeNames(simpleMethod, element, "level", "message");
            MiniLangValidate.requiredAttributes(simpleMethod, element, "level", "message");
            MiniLangValidate.constantAttributes(simpleMethod, element, "level");
            MiniLangValidate.constantPlusExpressionAttributes(simpleMethod, element, "message");
            MiniLangValidate.noChildElements(simpleMethod, element);
        }
        this.messageFse = FlexibleStringExpander.getInstance(element.getAttribute("message"));
        String levelAttribute = UtilXml.getAttributeValueIgnorePrefix(element, "level");
        if (levelAttribute.length() == 0) {
            levelAttribute = "info";
        }
        Integer levelInt = Debug.getLevelFromString(levelAttribute);
        if (levelInt == null) {
            MiniLangValidate.handleError("Invalid level attribute", simpleMethod, element);
            this.level = Debug.INFO;
        } else {
            this.level = levelInt.intValue();
        }
    }

    @Override
    public boolean exec(MethodContext methodContext) throws MiniLangException {
        if (Debug.isOn(level)) {
            String message = this.messageFse.expandString(methodContext.getEnvMap());
            StringBuilder buf = new StringBuilder("[");
            String methodLocation = this.simpleMethod.getFromLocation();
            int pos = methodLocation.lastIndexOf("/");
            if (pos != -1) {
                methodLocation = methodLocation.substring(pos + 1);
            }
            buf.append(methodLocation);
            buf.append("#");
            buf.append(this.simpleMethod.getMethodName());
            buf.append(" line ");
            buf.append(getLineNumber());
            buf.append("] ");
            buf.append(message);
            Debug.log(this.level, null, buf.toString(), module);
        }
        return true;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("<log ");
        sb.append("level=\"").append(LEVEL_ARRAY[this.level]).append("\" ");
        sb.append("message=\"").append(this.messageFse).append("\" ");
        sb.append("/>");
        return sb.toString();
    }

    /**
     * A factory for the &lt;log&gt; element.
     */
    public static final class LogFactory implements Factory<Log> {
        @Override
        public Log createMethodOperation(Element element, SimpleMethod simpleMethod) throws MiniLangException {
            return new Log(element, simpleMethod);
        }

        @Override
        public String getName() {
            return "log";
        }
    }
}

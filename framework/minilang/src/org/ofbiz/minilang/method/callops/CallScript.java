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

import org.ofbiz.base.util.ScriptUtil;
import org.ofbiz.base.util.Scriptlet;
import org.ofbiz.base.util.StringUtil;
import org.ofbiz.base.util.UtilXml;
import org.ofbiz.minilang.MiniLangException;
import org.ofbiz.minilang.MiniLangRuntimeException;
import org.ofbiz.minilang.MiniLangUtil;
import org.ofbiz.minilang.MiniLangValidate;
import org.ofbiz.minilang.SimpleMethod;
import org.ofbiz.minilang.method.MethodContext;
import org.ofbiz.minilang.method.MethodOperation;
import org.w3c.dom.Element;

/**
 * Implements the &lt;script&gt; element.
 * 
 * @see <a href="https://cwiki.apache.org/confluence/display/OFBADMIN/Mini-language+Reference#Mini-languageReference-{{%3Cscript%3E}}">Mini-language Reference</a>
 */
public final class CallScript extends MethodOperation {

    public static final String module = CallScript.class.getName();

    // This method is needed only during the v1 to v2 transition
    private static boolean autoCorrect(Element element) {
        String errorListAttr = element.getAttribute("error-list-name");
        if (errorListAttr.length() > 0) {
            element.removeAttribute("error-list-name");
            return true;
        }
        return false;
    }
    
    /*
     * Developers - the location attribute is a constant for security reasons.
     * Script invocations should always be hard-coded.
     */
    private final String location;
    private final String method;
    private final Scriptlet scriptlet;

    public CallScript(Element element, SimpleMethod simpleMethod) throws MiniLangException {
        super(element, simpleMethod);
        if (MiniLangValidate.validationOn()) {
            MiniLangValidate.attributeNames(simpleMethod, element, "location", "script");
            MiniLangValidate.requireAnyAttribute(simpleMethod, element, "location", "script");
            MiniLangValidate.constantAttributes(simpleMethod, element, "location");
            MiniLangValidate.scriptAttributes(simpleMethod, element, "script");
            MiniLangValidate.noChildElements(simpleMethod, element);
        }
        boolean elementModified = autoCorrect(element);
        if (elementModified && MiniLangUtil.autoCorrectOn()) {
            MiniLangUtil.flagDocumentAsCorrected(element);
        }
        String scriptLocation = element.getAttribute("location");
        if (scriptLocation.isEmpty()) {
            this.location = null;
            this.method = null;
        } else {
            int pos = scriptLocation.lastIndexOf("#");
            if (pos == -1) {
                this.location = scriptLocation;
                this.method = null;
            } else {
                this.location = scriptLocation.substring(0, pos);
                this.method = scriptLocation.substring(pos + 1);
            }
        }
        String inlineScript = element.getAttribute("script");
        if (inlineScript.isEmpty()) {
            inlineScript = UtilXml.elementValue(element);
        }
        if (inlineScript != null && MiniLangUtil.containsScript(inlineScript)) {
            this.scriptlet = new Scriptlet(StringUtil.convertOperatorSubstitutions(inlineScript));
        } else {
            this.scriptlet = null;
        }
    }

    @Override
    public boolean exec(MethodContext methodContext) throws MiniLangException {
        if (this.location != null) {
            if (location.endsWith(".xml")) {
                SimpleMethod.runSimpleMethod(location, method, methodContext);
            } else {
                ScriptUtil.executeScript(this.location, this.method, methodContext.getEnvMap());
            }
        }
        if (this.scriptlet != null) {
            try {
                this.scriptlet.executeScript(methodContext.getEnvMap());
            } catch (Exception e) {
                throw new MiniLangRuntimeException(e.getMessage(), this);
            }
        }
        return true;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("<script ");
        if (this.location != null) {
            sb.append("location=\"").append(this.location);
            if (this.method != null) {
                sb.append("#").append(this.method);
            }
            sb.append("\" ");
        }
        if (this.scriptlet != null) {
            sb.append("script=\"").append(this.scriptlet).append("\" ");
        }
        sb.append("/>");
        return sb.toString();
    }

    /**
     * A factory for the &lt;script&gt; element.
     */
    public static final class CallScriptFactory implements Factory<CallScript> {
        @Override
        public CallScript createMethodOperation(Element element, SimpleMethod simpleMethod) throws MiniLangException {
            return new CallScript(element, simpleMethod);
        }

        @Override
        public String getName() {
            return "script";
        }
    }
}

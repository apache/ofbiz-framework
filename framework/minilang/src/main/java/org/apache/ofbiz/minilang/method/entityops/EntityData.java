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
package org.apache.ofbiz.minilang.method.entityops;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;

import org.apache.ofbiz.base.location.FlexibleLocation;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.collections.FlexibleMapAccessor;
import org.apache.ofbiz.base.util.string.FlexibleStringExpander;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.util.EntityDataAssert;
import org.apache.ofbiz.entity.util.EntitySaxReader;
import org.apache.ofbiz.minilang.MiniLangException;
import org.apache.ofbiz.minilang.MiniLangValidate;
import org.apache.ofbiz.minilang.SimpleMethod;
import org.apache.ofbiz.minilang.method.MethodContext;
import org.w3c.dom.Element;

/**
 * Implements the &lt;entity-data&gt; element.
 * @see <a href="https://cwiki.apache.org/confluence/display/OFBIZ/Mini+Language+-+minilang+-+simple-method+-+Reference">Mini-language Reference</a>
 */
public final class EntityData extends EntityOperation {

    private static final String MODULE = EntityData.class.getName();

    private final FlexibleMapAccessor<List<Object>> errorListFma;
    private final FlexibleStringExpander locationFse;
    private final String mode;
    private final int timeout;

    public EntityData(Element element, SimpleMethod simpleMethod) throws MiniLangException {
        super(element, simpleMethod);
        if (MiniLangValidate.validationOn()) {
            MiniLangValidate.attributeNames(simpleMethod, element, "location", "timeout", "delegator-name", "error-list-name", "mode");
            MiniLangValidate.requiredAttributes(simpleMethod, element, "location");
            MiniLangValidate.expressionAttributes(simpleMethod, element, "delegator-name");
            MiniLangValidate.constantAttributes(simpleMethod, element, "timeout", "mode");
            MiniLangValidate.noChildElements(simpleMethod, element);
        }
        locationFse = FlexibleStringExpander.getInstance(element.getAttribute("location"));
        mode = MiniLangValidate.checkAttribute(element.getAttribute("mode"), "load");
        String timeoutAttribute = element.getAttribute("timeout");
        if (!"load".equals(mode) && !timeoutAttribute.isEmpty()) {
            MiniLangValidate.handleError("timeout attribute is valid only when mode=\"load\".", simpleMethod, element);
        }
        int timeout = -1;
        if (!timeoutAttribute.isEmpty()) {
            try {
                timeout = Integer.parseInt(timeoutAttribute);
            } catch (NumberFormatException e) {
                MiniLangValidate.handleError("Exception thrown while parsing timeout attribute: " + e.getMessage(), simpleMethod, element);
            }
        }
        this.timeout = timeout;
        errorListFma = FlexibleMapAccessor.getInstance(MiniLangValidate.checkAttribute(element.getAttribute("error-list-name"), "error_list"));
    }

    @Override
    public boolean exec(MethodContext methodContext) throws MiniLangException {
        List<Object> messages = errorListFma.get(methodContext.getEnvMap());
        if (messages == null) {
            messages = new LinkedList<>();
            errorListFma.put(methodContext.getEnvMap(), messages);
        }
        String location = this.locationFse.expandString(methodContext.getEnvMap());
        Delegator delegator = getDelegator(methodContext);
        URL dataUrl = null;
        try {
            dataUrl = FlexibleLocation.resolveLocation(location, methodContext.getLoader());
        } catch (MalformedURLException e) {
            messages.add("Could not find Entity Data document in resource: " + location + "; error was: " + e.toString());
        }
        if (dataUrl == null) {
            messages.add("Could not find Entity Data document in resource: " + location);
        }
        if ("assert".equals(mode)) {
            try {
                EntityDataAssert.assertData(dataUrl, delegator, messages);
            } catch (Exception e) {
                String xmlError = "Error checking/asserting XML Resource \"" + dataUrl.toExternalForm() + "\"; Error was: " + e.getMessage();
                messages.add(xmlError);
                Debug.logWarning(e, xmlError, MODULE);
            }
        } else {
            try {
                EntitySaxReader reader = null;
                if (timeout > 0) {
                    reader = new EntitySaxReader(delegator, timeout);
                } else {
                    reader = new EntitySaxReader(delegator);
                }
                reader.parse(dataUrl);
            } catch (Exception e) {
                String xmlError = "Error loading XML Resource \"" + dataUrl.toExternalForm() + "\"; Error was: " + e.getMessage();
                messages.add(xmlError);
                Debug.logWarning(e, xmlError, MODULE);
            }
        }
        return true;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("<entity-data ");
        sb.append("location=\"").append(this.locationFse).append("\" ");
        sb.append("mode=\"").append(this.mode).append("\" ");
        sb.append("timeout=\"").append(this.timeout).append("\" ");
        sb.append("error-list-name=\"").append(this.errorListFma).append("\" ");
        sb.append("/>");
        return sb.toString();
    }

    /**
     * A factory for the &lt;entity-data&gt; element.
     */
    public static final class EntityDataFactory implements Factory<EntityData> {
        @Override
        public EntityData createMethodOperation(Element element, SimpleMethod simpleMethod) throws MiniLangException {
            return new EntityData(element, simpleMethod);
        }

        @Override
        public String getName() {
            return "entity-data";
        }
    }
}

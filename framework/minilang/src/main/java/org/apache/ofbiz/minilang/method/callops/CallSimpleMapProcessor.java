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
package org.apache.ofbiz.minilang.method.callops;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.ofbiz.base.util.UtilXml;
import org.apache.ofbiz.base.util.collections.FlexibleMapAccessor;
import org.apache.ofbiz.minilang.MiniLangException;
import org.apache.ofbiz.minilang.MiniLangValidate;
import org.apache.ofbiz.minilang.SimpleMapProcessor;
import org.apache.ofbiz.minilang.SimpleMethod;
import org.apache.ofbiz.minilang.method.MethodContext;
import org.apache.ofbiz.minilang.method.MethodOperation;
import org.apache.ofbiz.minilang.operation.MapProcessor;
import org.w3c.dom.Element;

/**
 * Implements the &lt;call-map-processor&gt; element.
 * @see <a href="https://cwiki.apache.org/confluence/display/OFBIZ/Mini+Language+-+minilang+-+simple-method+-+Reference">Mini-language Reference</a>
 */
public final class CallSimpleMapProcessor extends MethodOperation {

    private final FlexibleMapAccessor<List<Object>> errorListFma;
    private final MapProcessor inlineMapProcessor;
    private final FlexibleMapAccessor<Map<String, Object>> inMapFma;
    private final FlexibleMapAccessor<Map<String, Object>> outMapFma;
    private final String processorName;
    private final String xmlResource;

    public CallSimpleMapProcessor(Element element, SimpleMethod simpleMethod) throws MiniLangException {
        super(element, simpleMethod);
        if (MiniLangValidate.validationOn()) {
            MiniLangValidate.attributeNames(simpleMethod, element, "processor-name", "xml-resource", "in-map-name",
                    "out-map-name", "error-list-name");
            MiniLangValidate.constantAttributes(simpleMethod, element, "processor-name", "xml-resource", "error-list-name");
            MiniLangValidate.expressionAttributes(simpleMethod, element, "in-map-name", "out-map-name");
            MiniLangValidate.requiredAttributes(simpleMethod, element, "in-map-name", "out-map-name");
            MiniLangValidate.childElements(simpleMethod, element, "simple-map-processor");
        }
        processorName = element.getAttribute("processor-name");
        xmlResource = element.getAttribute("xml-resource");
        errorListFma = FlexibleMapAccessor.getInstance(MiniLangValidate.checkAttribute(element.getAttribute("error-list-name"), "error_list"));
        inMapFma = FlexibleMapAccessor.getInstance(element.getAttribute("in-map-name"));
        outMapFma = FlexibleMapAccessor.getInstance(element.getAttribute("out-map-name"));
        Element simpleMapProcessorElement = UtilXml.firstChildElement(element, "simple-map-processor");
        if (simpleMapProcessorElement != null) {
            inlineMapProcessor = new MapProcessor(simpleMapProcessorElement);
        } else {
            inlineMapProcessor = null;
        }
    }

    @Override
    public boolean exec(MethodContext methodContext) throws MiniLangException {
        List<Object> messages = errorListFma.get(methodContext.getEnvMap());
        if (messages == null) {
            messages = new LinkedList<>();
            errorListFma.put(methodContext.getEnvMap(), messages);
        }
        Map<String, Object> inMap = inMapFma.get(methodContext.getEnvMap());
        if (inMap == null) {
            inMap = new HashMap<>();
        }
        Map<String, Object> outMap = outMapFma.get(methodContext.getEnvMap());
        if (outMap == null) {
            outMap = new HashMap<>();
            outMapFma.put(methodContext.getEnvMap(), outMap);
        }
        // run external map processor first
        if (!this.xmlResource.isEmpty() && !this.processorName.isEmpty()) {
            try {
                SimpleMapProcessor.runSimpleMapProcessor(xmlResource, processorName, inMap, outMap, messages, methodContext.getLocale(),
                        methodContext.getLoader());
            } catch (MiniLangException e) {
                messages.add("Error running SimpleMapProcessor in XML file \"" + xmlResource + "\": " + e.toString());
            }
        }
        // run inline map processor last so it can override the external map processor
        if (inlineMapProcessor != null) {
            inlineMapProcessor.exec(inMap, outMap, messages, methodContext.getLocale(), methodContext.getLoader());
        }
        return true;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("<call-map-processor ");
        if (!this.processorName.isEmpty()) {
            sb.append("processor-name=\"").append(this.processorName).append("\" ");
        }
        if (!this.xmlResource.isEmpty()) {
            sb.append("xml-resource=\"").append(this.xmlResource).append("\" ");
        }
        if (!this.inMapFma.isEmpty()) {
            sb.append("in-map-name=\"").append(this.inMapFma).append("\" ");
        }
        if (!this.outMapFma.isEmpty()) {
            sb.append("out-map-name=\"").append(this.outMapFma).append("\" ");
        }
        if (!"error_list".equals(errorListFma.toString())) {
            sb.append("error-list-name=\"").append(errorListFma).append("\" ");
        }
        sb.append("/>");
        return sb.toString();
    }

    /**
     * A factory for the &lt;call-map-processor&gt; element.
     */
    public static final class CallSimpleMapProcessorFactory implements Factory<CallSimpleMapProcessor> {
        @Override
        public CallSimpleMapProcessor createMethodOperation(Element element, SimpleMethod simpleMethod) throws MiniLangException {
            return new CallSimpleMapProcessor(element, simpleMethod);
        }

        @Override
        public String getName() {
            return "call-map-processor";
        }
    }
}

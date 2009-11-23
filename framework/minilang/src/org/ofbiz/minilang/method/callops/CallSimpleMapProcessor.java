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

import java.util.*;

import javolution.util.FastList;
import javolution.util.FastMap;

import org.w3c.dom.*;
import org.ofbiz.base.util.*;
import org.ofbiz.minilang.*;
import org.ofbiz.minilang.method.*;
import org.ofbiz.minilang.operation.*;

/**
 * An event operation that calls a simple map processor inlined or from a separate file
 */
public class CallSimpleMapProcessor extends MethodOperation {
    public static final class CallSimpleMapProcessorFactory implements Factory<CallSimpleMapProcessor> {
        public CallSimpleMapProcessor createMethodOperation(Element element, SimpleMethod simpleMethod) {
            return new CallSimpleMapProcessor(element, simpleMethod);
        }

        public String getName() {
            return "call-map-processor";
        }
    }

    String xmlResource;
    String processorName;
    ContextAccessor<Map<String, Object>> inMapAcsr;
    ContextAccessor<Map<String, Object>> outMapAcsr;
    ContextAccessor<List<Object>> errorListAcsr;

    MapProcessor inlineMapProcessor = null;

    public CallSimpleMapProcessor(Element element, SimpleMethod simpleMethod) {
        super(element, simpleMethod);
        xmlResource = element.getAttribute("xml-resource");
        processorName = element.getAttribute("processor-name");
        inMapAcsr = new ContextAccessor<Map<String, Object>>(element.getAttribute("in-map-name"));
        outMapAcsr = new ContextAccessor<Map<String, Object>>(element.getAttribute("out-map-name"));
        errorListAcsr = new ContextAccessor<List<Object>>(element.getAttribute("error-list-name"), "error_list");

        Element simpleMapProcessorElement = UtilXml.firstChildElement(element, "simple-map-processor");
        if (simpleMapProcessorElement != null) {
            inlineMapProcessor = new MapProcessor(simpleMapProcessorElement);
        }
    }

    @Override
    public boolean exec(MethodContext methodContext) {
        List<Object> messages = errorListAcsr.get(methodContext);
        if (messages == null) {
            messages = FastList.newInstance();
            errorListAcsr.put(methodContext, messages);
        }

        Map<String, Object> inMap = inMapAcsr.get(methodContext);
        if (inMap == null) {
            inMap = FastMap.newInstance();
            inMapAcsr.put(methodContext, inMap);
        }

        Map<String, Object> outMap = outMapAcsr.get(methodContext);
        if (outMap == null) {
            outMap = FastMap.newInstance();
            outMapAcsr.put(methodContext, outMap);
        }

        // run external map processor first
        if (UtilValidate.isNotEmpty(this.xmlResource) && UtilValidate.isNotEmpty(this.processorName)) {
            String xmlResource = methodContext.expandString(this.xmlResource);
            String processorName = methodContext.expandString(this.processorName);
            try {
                org.ofbiz.minilang.SimpleMapProcessor.runSimpleMapProcessor(
                    xmlResource, processorName, inMap, outMap, messages,
                    methodContext.getLocale(), methodContext.getLoader());
            } catch (MiniLangException e) {
                messages.add("Error running SimpleMapProcessor in XML file \"" + xmlResource + "\": " + e.toString());
            }
        }

        // run inlined map processor last so it can override the external map processor
        if (inlineMapProcessor != null) {
            inlineMapProcessor.exec(inMap, outMap, messages,
                (methodContext.getRequest() != null ? methodContext.getRequest().getLocale() : null),
                methodContext.getLoader());
        }

        return true;
    }

    @Override
    public String rawString() {
        // TODO: something more than the empty tag
        return "<call-simple-map-processor/>";
    }
    @Override
    public String expandedString(MethodContext methodContext) {
        // TODO: something more than a stub/dummy
        return this.rawString();
    }
}

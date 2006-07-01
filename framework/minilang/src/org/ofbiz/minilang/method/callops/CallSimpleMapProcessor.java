/*
 * $Id: CallSimpleMapProcessor.java 5462 2005-08-05 18:35:48Z jonesde $
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

import java.util.*;

import org.w3c.dom.*;
import org.ofbiz.base.util.*;
import org.ofbiz.minilang.*;
import org.ofbiz.minilang.method.*;
import org.ofbiz.minilang.operation.*;

/**
 * An event operation that calls a simple map processor inlined or from a separate file
 *
 * @author     <a href="mailto:jonesde@ofbiz.org">David E. Jones</a>
 * @version    $Rev$
 * @since      2.0
 */
public class CallSimpleMapProcessor extends MethodOperation {
    
    String xmlResource;
    String processorName;
    ContextAccessor inMapAcsr;
    ContextAccessor outMapAcsr;
    ContextAccessor errorListAcsr;

    MapProcessor inlineMapProcessor = null;

    public CallSimpleMapProcessor(Element element, SimpleMethod simpleMethod) {
        super(element, simpleMethod);
        xmlResource = element.getAttribute("xml-resource");
        processorName = element.getAttribute("processor-name");
        inMapAcsr = new ContextAccessor(element.getAttribute("in-map-name"));
        outMapAcsr = new ContextAccessor(element.getAttribute("out-map-name"));
        errorListAcsr = new ContextAccessor(element.getAttribute("error-list-name"), "error_list");

        Element simpleMapProcessorElement = UtilXml.firstChildElement(element, "simple-map-processor");
        if (simpleMapProcessorElement != null) {
            inlineMapProcessor = new MapProcessor(simpleMapProcessorElement);
        }
    }

    public boolean exec(MethodContext methodContext) {
        List messages = (List) errorListAcsr.get(methodContext);
        if (messages == null) {
            messages = new LinkedList();
            errorListAcsr.put(methodContext, messages);
        }

        Map inMap = (Map) inMapAcsr.get(methodContext);
        if (inMap == null) {
            inMap = new HashMap();
            inMapAcsr.put(methodContext, inMap);
        }

        Map outMap = (Map) outMapAcsr.get(methodContext);
        if (outMap == null) {
            outMap = new HashMap();
            outMapAcsr.put(methodContext, outMap);
        }

        // run external map processor first
        if (this.xmlResource != null && this.xmlResource.length() > 0 &&
                this.processorName != null && this.processorName.length() > 0) {
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

    public String rawString() {
        // TODO: something more than the empty tag
        return "<call-simple-map-processor/>";
    }
    public String expandedString(MethodContext methodContext) {
        // TODO: something more than a stub/dummy
        return this.rawString();
    }
}

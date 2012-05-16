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
package org.ofbiz.minilang.method.envops;

import java.util.Map;

import javolution.util.FastMap;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.GeneralException;
import org.ofbiz.base.util.ObjectType;
import org.ofbiz.base.util.StringUtil;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.minilang.MiniLangException;
import org.ofbiz.minilang.MiniLangUtil;
import org.ofbiz.minilang.SimpleMethod;
import org.ofbiz.minilang.method.ContextAccessor;
import org.ofbiz.minilang.method.MethodContext;
import org.ofbiz.minilang.method.MethodOperation;
import org.w3c.dom.Element;

/**
 * Converts the specified field to a String, using toString()
 */
public class ToString extends MethodOperation {

    public static final String module = ToString.class.getName();

    ContextAccessor<Object> fieldAcsr;
    String format;
    ContextAccessor<Map<String, Object>> mapAcsr;
    Integer numericPadding;

    public ToString(Element element, SimpleMethod simpleMethod) throws MiniLangException {
        super(element, simpleMethod);
        // the schema for this element now just has the "field" attribute, though the old "field-name" and "map-name" pair is still supported
        fieldAcsr = new ContextAccessor<Object>(element.getAttribute("field"), element.getAttribute("field-name"));
        mapAcsr = new ContextAccessor<Map<String, Object>>(element.getAttribute("map-name"));
        format = element.getAttribute("format");
        String npStr = element.getAttribute("numeric-padding");
        if (UtilValidate.isNotEmpty(npStr)) {
            try {
                this.numericPadding = Integer.valueOf(npStr);
            } catch (Exception e) {
                Debug.logError(e, "Error parsing numeric-padding attribute value on the to-string element", module);
            }
        }
    }

    public String doToString(Object obj, MethodContext methodContext) {
        String outStr = null;
        try {
            if (UtilValidate.isNotEmpty(format)) {
                outStr = (String) MiniLangUtil.convertType(obj, String.class, methodContext.getLocale(), methodContext.getTimeZone(), format);
            } else {
                outStr = obj.toString();
            }
        } catch (Exception e) {
            Debug.logError(e, "", module);
            outStr = obj.toString();
        }
        if (this.numericPadding != null) {
            outStr = StringUtil.padNumberString(outStr, this.numericPadding.intValue());
        }
        return outStr;
    }

    @Override
    public boolean exec(MethodContext methodContext) throws MiniLangException {
        if (!mapAcsr.isEmpty()) {
            Map<String, Object> toMap = mapAcsr.get(methodContext);
            if (toMap == null) {
                // it seems silly to create a new map, but necessary since whenever
                // an env field like a Map or List is referenced it should be created, even if empty
                if (Debug.verboseOn())
                    Debug.logVerbose("Map not found with name " + mapAcsr + ", creating new map", module);
                toMap = FastMap.newInstance();
                mapAcsr.put(methodContext, toMap);
            }
            Object obj = fieldAcsr.get(toMap, methodContext);
            if (obj != null) {
                fieldAcsr.put(toMap, doToString(obj, methodContext), methodContext);
            }
        } else {
            Object obj = fieldAcsr.get(methodContext);
            if (obj != null) {
                fieldAcsr.put(methodContext, doToString(obj, methodContext));
            }
        }
        return true;
    }

    @Override
    public String expandedString(MethodContext methodContext) {
        // TODO: something more than a stub/dummy
        return this.rawString();
    }

    @Override
    public String rawString() {
        // TODO: something more than the empty tag
        return "<to-string field-name=\"" + this.fieldAcsr + "\" map-name=\"" + this.mapAcsr + "\"/>";
    }

    public static final class ToStringFactory implements Factory<ToString> {
        public ToString createMethodOperation(Element element, SimpleMethod simpleMethod) throws MiniLangException {
            return new ToString(element, simpleMethod);
        }

        public String getName() {
            return "to-string";
        }
    }
}

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
package org.ofbiz.minilang.method.entityops;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.minilang.SimpleMethod;
import org.ofbiz.minilang.method.ContextAccessor;
import org.ofbiz.minilang.method.MethodContext;
import org.ofbiz.minilang.method.MethodOperation;
import org.w3c.dom.Element;

/**
 * Look at existing values for a sub-entity with a sequenced secondary ID, and get the highest plus 1
 */
public class MakeNextSeqId extends MethodOperation {

    public static final String module = MakeNextSeqId.class.getName();

    String seqFieldName;
    ContextAccessor valueAcsr;
    String numericPaddingStr;
    String incrementByStr;

    public MakeNextSeqId(Element element, SimpleMethod simpleMethod) {
        super(element, simpleMethod);
        seqFieldName = element.getAttribute("seq-field-name");
        valueAcsr = new ContextAccessor(element.getAttribute("value-name"));

        numericPaddingStr = element.getAttribute("numeric-padding");
        incrementByStr = element.getAttribute("increment-by");
    }

    public boolean exec(MethodContext methodContext) {
        String seqFieldName = methodContext.expandString(this.seqFieldName);
        String numericPaddingStr = methodContext.expandString(this.numericPaddingStr);
        String incrementByStr = methodContext.expandString(this.incrementByStr);
        int numericPadding = 5;
        int incrementBy = 1;
        try {
            if (UtilValidate.isNotEmpty(numericPaddingStr)) {
                numericPadding = Integer.parseInt(numericPaddingStr);
            }
        } catch (Exception e) {
            Debug.logError(e, "numeric-padding format invalid for [" + numericPaddingStr + "]", module);
        }
        try {
            if (UtilValidate.isNotEmpty(incrementByStr)) {
                incrementBy = Integer.parseInt(incrementByStr);
            }
        } catch (Exception e) {
            Debug.logError(e, "increment-by format invalid for [" + incrementByStr + "]", module);
        }

        GenericValue value = (GenericValue) valueAcsr.get(methodContext);
        methodContext.getDelegator().setNextSubSeqId(value, seqFieldName, numericPadding, incrementBy);
        
        return true;
    }

    public String rawString() {
        // TODO: something more than the empty tag
        return "<make-next-seq-id/>";
    }
    public String expandedString(MethodContext methodContext) {
        // TODO: something more than a stub/dummy
        return this.rawString();
    }
}

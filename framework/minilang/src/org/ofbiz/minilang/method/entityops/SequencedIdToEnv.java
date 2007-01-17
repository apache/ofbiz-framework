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

import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.minilang.SimpleMethod;
import org.ofbiz.minilang.method.ContextAccessor;
import org.ofbiz.minilang.method.MethodContext;
import org.ofbiz.minilang.method.MethodOperation;
import org.w3c.dom.Element;

/**
 * Gets a sequenced ID from the delegator and puts it in the env
 */
public class SequencedIdToEnv extends MethodOperation {
    
    String seqName;
    ContextAccessor envAcsr;
    boolean getLongOnly;
    long staggerMax = 1;

    public SequencedIdToEnv(Element element, SimpleMethod simpleMethod) {
        super(element, simpleMethod);
        seqName = element.getAttribute("sequence-name");
        envAcsr = new ContextAccessor(element.getAttribute("env-name"));
        // default false, anything but true is false
        getLongOnly = "true".equals(element.getAttribute("get-long-only"));
        String staggerMaxStr = element.getAttribute("stagger-max");
        if (UtilValidate.isNotEmpty(staggerMaxStr)) {
            try {
                this.staggerMax = Long.parseLong(staggerMaxStr);
                if (this.staggerMax < 1) {
                    this.staggerMax = 1;
                }
            } catch (NumberFormatException e) {
                this.staggerMax = 1;
            }
        }
    }

    public boolean exec(MethodContext methodContext) {
        String seqName = methodContext.expandString(this.seqName);
        if (getLongOnly) {
            envAcsr.put(methodContext, methodContext.getDelegator().getNextSeqIdLong(seqName, staggerMax));
        } else {
            envAcsr.put(methodContext, methodContext.getDelegator().getNextSeqId(seqName, staggerMax));
        }
        return true;
    }

    public String rawString() {
        // TODO: something more than the empty tag
        return "<sequenced-id-to-env/>";
    }
    public String expandedString(MethodContext methodContext) {
        // TODO: something more than a stub/dummy
        return this.rawString();
    }
}

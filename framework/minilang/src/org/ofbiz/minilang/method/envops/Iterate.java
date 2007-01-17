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

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.ofbiz.base.util.Debug;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.util.EntityListIterator;
import org.ofbiz.minilang.SimpleMethod;
import org.ofbiz.minilang.method.ContextAccessor;
import org.ofbiz.minilang.method.MethodContext;
import org.ofbiz.minilang.method.MethodOperation;
import org.w3c.dom.Element;

/**
 * Process sub-operations for each entry in the list
 */
public class Iterate extends MethodOperation {
    
    public static final String module = Iterate.class.getName();

    List subOps = new LinkedList();

    ContextAccessor entryAcsr;
    ContextAccessor listAcsr;

    public Iterate(Element element, SimpleMethod simpleMethod) {
        super(element, simpleMethod);
        this.entryAcsr = new ContextAccessor(element.getAttribute("entry-name"));
        this.listAcsr = new ContextAccessor(element.getAttribute("list-name"));

        SimpleMethod.readOperations(element, subOps, simpleMethod);
    }

    public boolean exec(MethodContext methodContext) {

        if (listAcsr.isEmpty()) {
            Debug.logWarning("No list-name specified in iterate tag, doing nothing: " + rawString(), module);
            return true;
        }

        Object oldEntryValue = entryAcsr.get(methodContext);
        Object objList = listAcsr.get(methodContext);
        if (objList instanceof EntityListIterator) {
            EntityListIterator eli = (EntityListIterator) objList;

            GenericValue theEntry;
            while ((theEntry = (GenericValue) eli.next()) != null) {
                entryAcsr.put(methodContext, theEntry);

                if (!SimpleMethod.runSubOps(subOps, methodContext)) {
                    // only return here if it returns false, otherwise just carry on
                    return false;
                }
            }

            // close the iterator
            try {
                eli.close();
            } catch (GenericEntityException e) {
                Debug.logError(e, module);
                String errMsg = "ERROR: Error closing entityListIterator in " + simpleMethod.getShortDescription() + " [" + e.getMessage() + "]: " + rawString();
                if (methodContext.getMethodType() == MethodContext.EVENT) {
                    methodContext.putEnv(simpleMethod.getEventErrorMessageName(), errMsg);
                    methodContext.putEnv(simpleMethod.getEventResponseCodeName(), simpleMethod.getDefaultErrorCode());
                } else if (methodContext.getMethodType() == MethodContext.SERVICE) {
                    methodContext.putEnv(simpleMethod.getServiceErrorMessageName(), errMsg);
                    methodContext.putEnv(simpleMethod.getServiceResponseMessageName(), simpleMethod.getDefaultErrorCode());
                }
                return false;
            }
        } else {
            Collection theList = (Collection) objList;

            if (theList == null) {
                if (Debug.infoOn()) Debug.logInfo("List not found with name " + listAcsr + ", doing nothing: " + rawString(), module);
                return true;
            }
            if (theList.size() == 0) {
                if (Debug.verboseOn()) Debug.logVerbose("List with name " + listAcsr + " has zero entries, doing nothing: " + rawString(), module);
                return true;
            }

            Iterator theIterator = theList.iterator();

            while (theIterator.hasNext()) {
                Object theEntry = theIterator.next();
                entryAcsr.put(methodContext, theEntry);

                if (!SimpleMethod.runSubOps(subOps, methodContext)) {
                    // only return here if it returns false, otherwise just carry on
                    return false;
                }
            }
        }
        entryAcsr.put(methodContext, oldEntryValue);
        return true;
    }

    public String rawString() {
        // TODO: something more than the empty tag
        return "<iterate list-name=\"" + this.listAcsr + "\" entry-name=\"" + this.entryAcsr + "\"/>";
    }
    public String expandedString(MethodContext methodContext) {
        // TODO: something more than a stub/dummy
        return this.rawString();
    }
}

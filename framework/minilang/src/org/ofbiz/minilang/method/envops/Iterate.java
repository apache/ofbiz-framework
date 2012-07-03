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
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilGenerics;
import org.ofbiz.base.util.collections.FlexibleMapAccessor;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.util.EntityListIterator;
import org.ofbiz.minilang.MiniLangException;
import org.ofbiz.minilang.MiniLangRuntimeException;
import org.ofbiz.minilang.MiniLangValidate;
import org.ofbiz.minilang.SimpleMethod;
import org.ofbiz.minilang.artifact.ArtifactInfoContext;
import org.ofbiz.minilang.method.MethodContext;
import org.ofbiz.minilang.method.MethodOperation;
import org.ofbiz.minilang.method.envops.Break.BreakElementException;
import org.ofbiz.minilang.method.envops.Continue.ContinueElementException;
import org.w3c.dom.Element;

/**
 * Implements the &lt;iterate&gt; element.
 * 
 * @see <a href="https://cwiki.apache.org/OFBADMIN/mini-language-reference.html#Mini-languageReference-{{%3Citerate%3E}}">Mini-language Reference</a>
 */
public final class Iterate extends MethodOperation {

    public static final String module = Iterate.class.getName();

    private final FlexibleMapAccessor<Object> entryFma;
    private final FlexibleMapAccessor<Object> listFma;
    private final List<MethodOperation> subOps;

    public Iterate(Element element, SimpleMethod simpleMethod) throws MiniLangException {
        super(element, simpleMethod);
        if (MiniLangValidate.validationOn()) {
            MiniLangValidate.attributeNames(simpleMethod, element, "entry", "list");
            MiniLangValidate.expressionAttributes(simpleMethod, element, "entry", "list");
            MiniLangValidate.requiredAttributes(simpleMethod, element, "entry", "list");
        }
        this.entryFma = FlexibleMapAccessor.getInstance(element.getAttribute("entry"));
        this.listFma = FlexibleMapAccessor.getInstance(element.getAttribute("list"));
        this.subOps = Collections.unmodifiableList(SimpleMethod.readOperations(element, simpleMethod));
    }

    @Override
    public boolean exec(MethodContext methodContext) throws MiniLangException {
        if (listFma.isEmpty()) {
            if (Debug.verboseOn())
                Debug.logVerbose("Collection not found, doing nothing: " + this, module);
            return true;
        }
        Object oldEntryValue = entryFma.get(methodContext.getEnvMap());
        Object objList = listFma.get(methodContext.getEnvMap());
        if (objList instanceof EntityListIterator) {
            EntityListIterator eli = (EntityListIterator) objList;
            GenericValue theEntry;
            try {
                while ((theEntry = eli.next()) != null) {
                    entryFma.put(methodContext.getEnvMap(), theEntry);
                    try {
                        for (MethodOperation methodOperation : subOps) {
                            if (!methodOperation.exec(methodContext)) {
                                return false;
                            }
                        }
                    } catch (MiniLangException e) {
                        if (e instanceof BreakElementException) {
                            break;
                        }
                        if (e instanceof ContinueElementException) {
                            continue;
                        }
                        throw e;
                    }
                }
            } finally {
                try {
                    eli.close();
                } catch (GenericEntityException e) {
                    throw new MiniLangRuntimeException("Error closing entityListIterator: " + e.getMessage(), this);
                }
            }
        } else if (objList instanceof Collection<?>) {
            Collection<Object> theCollection = UtilGenerics.checkCollection(objList);
            if (theCollection.size() == 0) {
                if (Debug.verboseOn())
                    Debug.logVerbose("Collection has zero entries, doing nothing: " + this, module);
                return true;
            }
            for (Object theEntry : theCollection) {
                entryFma.put(methodContext.getEnvMap(), theEntry);
                try {
                    for (MethodOperation methodOperation : subOps) {
                        if (!methodOperation.exec(methodContext)) {
                            return false;
                        }
                    }
                } catch (MiniLangException e) {
                    if (e instanceof BreakElementException) {
                        break;
                    }
                    if (e instanceof ContinueElementException) {
                        continue;
                    }
                    throw e;
                }
            }
        } else if (objList instanceof Iterator<?>) {
            Iterator<Object> theIterator = UtilGenerics.cast(objList);
            if (!theIterator.hasNext()) {
                if (Debug.verboseOn())
                    Debug.logVerbose("Iterator has zero entries, doing nothing: " + this, module);
                return true;
            }
            while (theIterator.hasNext()) {
                Object theEntry = theIterator.next();
                entryFma.put(methodContext.getEnvMap(), theEntry);
                try {
                    for (MethodOperation methodOperation : subOps) {
                        if (!methodOperation.exec(methodContext)) {
                            return false;
                        }
                    }
                } catch (MiniLangException e) {
                    if (e instanceof BreakElementException) {
                        break;
                    }
                    if (e instanceof ContinueElementException) {
                        continue;
                    }
                    throw e;
                }
            }
        } else {
            if (Debug.verboseOn())
                Debug.logVerbose("Cannot iterate over a " + objList.getClass().getName() + ", doing nothing: " + this, module);
            return true;
        }
        entryFma.put(methodContext.getEnvMap(), oldEntryValue);
        return true;
    }

    @Override
    public void gatherArtifactInfo(ArtifactInfoContext aic) {
        for (MethodOperation method : this.subOps) {
            method.gatherArtifactInfo(aic);
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("<iterate ");
        if (!this.entryFma.isEmpty()) {
            sb.append("entry=\"").append(this.entryFma).append("\" ");
        }
        if (!this.listFma.isEmpty()) {
            sb.append("list=\"").append(this.listFma).append("\" ");
        }
        sb.append("/>");
        return sb.toString();
    }

    /**
     * A factory for the &lt;iterate&gt; element.
     */
    public static final class IterateFactory implements Factory<Iterate> {
        @Override
        public Iterate createMethodOperation(Element element, SimpleMethod simpleMethod) throws MiniLangException {
            return new Iterate(element, simpleMethod);
        }

        @Override
        public String getName() {
            return "iterate";
        }
    }
}

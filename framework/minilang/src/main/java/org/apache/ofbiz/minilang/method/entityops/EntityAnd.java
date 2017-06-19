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

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.GeneralException;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.finder.ByAndFinder;
import org.apache.ofbiz.minilang.MiniLangException;
import org.apache.ofbiz.minilang.MiniLangValidate;
import org.apache.ofbiz.minilang.SimpleMethod;
import org.apache.ofbiz.minilang.artifact.ArtifactInfoContext;
import org.apache.ofbiz.minilang.method.MethodContext;
import org.w3c.dom.Element;

/**
 * Implements the &lt;entity-and&gt; element.
 * 
 * @see <a href="https://cwiki.apache.org/confluence/display/OFBIZ/Mini+Language+-+minilang+-+simple-method+-+Reference">Mini-language Reference</a>
 */
public final class EntityAnd extends EntityOperation {

    public static final String module = EntityAnd.class.getName();

    private final ByAndFinder finder;

    public EntityAnd(Element element, SimpleMethod simpleMethod) throws MiniLangException {
        super(element, simpleMethod);
        if (MiniLangValidate.validationOn()) {
            MiniLangValidate.attributeNames(simpleMethod, element, "entity-name", "use-cache", "filter-by-date", "list", "distinct", "delegator-name");
            MiniLangValidate.requiredAttributes(simpleMethod, element, "entity-name", "list");
            MiniLangValidate.expressionAttributes(simpleMethod, element, "list");
            MiniLangValidate.childElements(simpleMethod, element, "field-map", "order-by", "limit-range", "limit-view", "use-iterator");
            MiniLangValidate.requiredChildElements(simpleMethod, element, "field-map");
        }
        this.finder = new ByAndFinder(element);
    }

    @Override
    public boolean exec(MethodContext methodContext) throws MiniLangException {
        try {
            Delegator delegator = getDelegator(methodContext);
            this.finder.runFind(methodContext.getEnvMap(), delegator);
        } catch (GeneralException e) {
            String errMsg = "Exception thrown while performing entity find: " + e.getMessage();
            Debug.logWarning(e, errMsg, module);
            simpleMethod.addErrorMessage(methodContext, errMsg);
            return false;
        }
        return true;
    }

    @Override
    public void gatherArtifactInfo(ArtifactInfoContext aic) {
        aic.addEntityName(this.finder.getEntityName());
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("<entity-and ");
        sb.append("entity-name=\"").append(this.finder.getEntityName()).append("\" />");
        return sb.toString();
    }

    /**
     * A factory for the &lt;entity-and&gt; element.
     */
    public static final class EntityAndFactory implements Factory<EntityAnd> {
        @Override
        public EntityAnd createMethodOperation(Element element, SimpleMethod simpleMethod) throws MiniLangException {
            return new EntityAnd(element, simpleMethod);
        }

        @Override
        public String getName() {
            return "entity-and";
        }
    }
}

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

import java.util.Map;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.collections.FlexibleMapAccessor;
import org.ofbiz.base.util.string.FlexibleStringExpander;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.minilang.MiniLangException;
import org.ofbiz.minilang.MiniLangValidate;
import org.ofbiz.minilang.SimpleMethod;
import org.ofbiz.minilang.artifact.ArtifactInfoContext;
import org.ofbiz.minilang.method.MethodContext;
import org.ofbiz.minilang.method.MethodOperation;
import org.w3c.dom.Element;

/**
 * Implements the &lt;remove-by-and&gt; element.
 * 
 * @see <a href="https://cwiki.apache.org/OFBADMIN/mini-language-reference.html#Mini-languageReference-{{%3Cremovebyand%3E}}">Mini-language Reference</a>
 */
public final class RemoveByAnd extends MethodOperation {

    public static final String module = RemoveByAnd.class.getName();

    private final FlexibleStringExpander doCacheClearFse;
    private final FlexibleStringExpander entityNameFse;
    private final FlexibleMapAccessor<Map<String, ? extends Object>> mapFma;

    public RemoveByAnd(Element element, SimpleMethod simpleMethod) throws MiniLangException {
        super(element, simpleMethod);
        if (MiniLangValidate.validationOn()) {
            MiniLangValidate.attributeNames(simpleMethod, element, "entity-name", "map", "do-cache-clear");
            MiniLangValidate.requiredAttributes(simpleMethod, element, "entity-name", "map");
            MiniLangValidate.expressionAttributes(simpleMethod, element, "map");
            MiniLangValidate.noChildElements(simpleMethod, element);
        }
        entityNameFse = FlexibleStringExpander.getInstance(element.getAttribute("entity-name"));
        mapFma = FlexibleMapAccessor.getInstance(element.getAttribute("map"));
        doCacheClearFse = FlexibleStringExpander.getInstance(element.getAttribute("do-cache-clear"));
    }

    @Override
    public boolean exec(MethodContext methodContext) throws MiniLangException {
        boolean doCacheClear = !"false".equals(doCacheClearFse.expandString(methodContext.getEnvMap()));
        String entityName = entityNameFse.expandString(methodContext.getEnvMap());
        try {
            methodContext.getDelegator().removeByAnd(entityName, mapFma.get(methodContext.getEnvMap()), doCacheClear);
        } catch (GenericEntityException e) {
            String errMsg = "Exception thrown while removing entities: " + e.getMessage();
            Debug.logWarning(e, errMsg, module);
            simpleMethod.addErrorMessage(methodContext, errMsg);
            return false;
        }
        return true;
    }

    @Override
    public void gatherArtifactInfo(ArtifactInfoContext aic) {
        aic.addEntityName(entityNameFse.toString());
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("<remove-by-and ");
        sb.append("entity-name=\"").append(this.entityNameFse).append("\" ");
        sb.append("map=\"").append(this.mapFma).append("\" ");
        if (!doCacheClearFse.isEmpty()) {
            sb.append("do-cache-clear=\"").append(this.doCacheClearFse).append("\" ");
        }
        sb.append("/>");
        return sb.toString();
    }

    /**
     * A factory for the &lt;remove-by-and&gt; element.
     */
    public static final class RemoveByAndFactory implements Factory<RemoveByAnd> {
        @Override
        public RemoveByAnd createMethodOperation(Element element, SimpleMethod simpleMethod) throws MiniLangException {
            return new RemoveByAnd(element, simpleMethod);
        }

        @Override
        public String getName() {
            return "remove-by-and";
        }
    }
}

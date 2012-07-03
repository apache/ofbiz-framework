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

import org.ofbiz.base.util.collections.FlexibleMapAccessor;
import org.ofbiz.base.util.string.FlexibleStringExpander;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.minilang.MiniLangException;
import org.ofbiz.minilang.MiniLangRuntimeException;
import org.ofbiz.minilang.MiniLangValidate;
import org.ofbiz.minilang.SimpleMethod;
import org.ofbiz.minilang.artifact.ArtifactInfoContext;
import org.ofbiz.minilang.method.MethodContext;
import org.ofbiz.minilang.method.MethodOperation;
import org.w3c.dom.Element;

/**
 * Implements the &lt;make-value&gt; element.
 * 
 * @see <a href="https://cwiki.apache.org/OFBADMIN/mini-language-reference.html#Mini-languageReference-{{%3Cmakevalue%3E}}">Mini-language Reference</a>
 */
public final class MakeValue extends MethodOperation {

    private final FlexibleStringExpander entityNameFse;
    private final FlexibleMapAccessor<Map<String, ? extends Object>> mapFma;
    private final FlexibleMapAccessor<GenericValue> valueFma;

    public MakeValue(Element element, SimpleMethod simpleMethod) throws MiniLangException {
        super(element, simpleMethod);
        if (MiniLangValidate.validationOn()) {
            MiniLangValidate.attributeNames(simpleMethod, element, "value-field", "entity-name", "map");
            MiniLangValidate.requiredAttributes(simpleMethod, element, "value-field", "entity-name");
            MiniLangValidate.expressionAttributes(simpleMethod, element, "value-field", "map");
            MiniLangValidate.noChildElements(simpleMethod, element);
        }
        valueFma = FlexibleMapAccessor.getInstance(element.getAttribute("value-field"));
        entityNameFse = FlexibleStringExpander.getInstance(element.getAttribute("entity-name"));
        mapFma = FlexibleMapAccessor.getInstance(element.getAttribute("map"));
    }

    @Override
    public boolean exec(MethodContext methodContext) throws MiniLangException {
        String entityName = entityNameFse.expandString(methodContext.getEnvMap());
        if (entityName.isEmpty()) {
            throw new MiniLangRuntimeException("Entity name not found.", this);
        }
        valueFma.put(methodContext.getEnvMap(), methodContext.getDelegator().makeValidValue(entityName, mapFma.get(methodContext.getEnvMap())));
        return true;
    }

    @Override
    public void gatherArtifactInfo(ArtifactInfoContext aic) {
        aic.addEntityName(entityNameFse.toString());
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("<make-value ");
        sb.append("entity-name=\"").append(this.entityNameFse).append("\" ");
        sb.append("value-field=\"").append(this.valueFma).append("\" ");
        if (!mapFma.isEmpty()) {
            sb.append("map=\"").append(this.mapFma).append("\" ");
        }
        sb.append("/>");
        return sb.toString();
    }

    /**
     * A factory for the &lt;make-value&gt; element.
     */
    public static final class MakeValueFactory implements Factory<MakeValue> {
        @Override
        public MakeValue createMethodOperation(Element element, SimpleMethod simpleMethod) throws MiniLangException {
            return new MakeValue(element, simpleMethod);
        }

        @Override
        public String getName() {
            return "make-value";
        }
    }
}

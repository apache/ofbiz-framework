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
package org.apache.ofbiz.widget.model;

import java.util.Map;

import org.apache.ofbiz.base.util.UtilGenerics;
import org.apache.ofbiz.base.util.string.FlexibleStringExpander;
import org.apache.ofbiz.widget.model.AbstractModelCondition.DefaultConditionFactory;
import org.w3c.dom.Element;

/**
 * Models the &lt;condition&gt; element.
 *
 * @see <code>widget-screen.xsd</code>
 */
@SuppressWarnings("serial")
public final class ModelScreenCondition {

    /*
     * ----------------------------------------------------------------------- *
     *                     DEVELOPERS PLEASE READ
     * ----------------------------------------------------------------------- *
     *
     * This model is intended to be a read-only data structure that represents
     * an XML element. Outside of object construction, the class should not
     * have any behaviors.
     *
     * Instances of this class will be shared by multiple threads - therefore
     * it is immutable. DO NOT CHANGE THE OBJECT'S STATE AT RUN TIME!
     *
     */

    public static final String module = ModelScreenCondition.class.getName();
    public static final ModelConditionFactory SCREEN_CONDITION_FACTORY = new ScreenConditionFactory();

    public static class IfEmptySection extends AbstractModelCondition {
        private final FlexibleStringExpander sectionExdr;

        private IfEmptySection(ModelConditionFactory factory, ModelWidget modelWidget, Element condElement) {
            super (factory, modelWidget, condElement);
            this.sectionExdr = FlexibleStringExpander.getInstance(condElement.getAttribute("section-name"));
        }

        @Override
        public void accept(ModelConditionVisitor visitor) throws Exception {
            visitor.visit(this);
        }

        @Override
        public boolean eval(Map<String, Object> context) {
            Map<String, Object> sectionsMap = UtilGenerics.toMap(context.get("sections"));
            return !sectionsMap.containsKey(this.sectionExdr.expandString(context));
        }

        public FlexibleStringExpander getSectionExdr() {
            return sectionExdr;
        }
    }

    private static class ScreenConditionFactory extends DefaultConditionFactory {

        @Override
        public ModelCondition newInstance(ModelWidget modelWidget, Element conditionElement) {
            if (conditionElement == null) {
                return DefaultConditionFactory.TRUE;
            }
            if ("if-empty-section".equals(conditionElement.getNodeName())) {
                return new IfEmptySection(this, modelWidget, conditionElement);
            }
            return super.newInstance(this, modelWidget,conditionElement);
        }
    }
}

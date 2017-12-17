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

import org.apache.ofbiz.base.util.string.FlexibleStringExpander;
import org.w3c.dom.Element;

/**
 * Models the &lt;condition&gt; element.
 *
 * @see <code>widget-menu.xsd</code>
 */
public final class ModelMenuCondition {

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

    public static final String module = ModelMenuCondition.class.getName();

    private final FlexibleStringExpander passStyleExdr;
    private final FlexibleStringExpander failStyleExdr;
    private final ModelCondition condition;

    public ModelMenuCondition(ModelMenuItem modelMenuItem, Element conditionElement) {
        this.passStyleExdr = FlexibleStringExpander.getInstance(conditionElement.getAttribute("pass-style"));
        this.failStyleExdr = FlexibleStringExpander.getInstance(conditionElement.getAttribute("disabled-style"));
        this.condition = AbstractModelCondition.DEFAULT_CONDITION_FACTORY.newInstance(modelMenuItem, conditionElement);
    }

    public ModelCondition getCondition() {
        return condition;
    }

    public FlexibleStringExpander getFailStyleExdr() {
        return failStyleExdr;
    }

    public FlexibleStringExpander getPassStyleExdr() {
        return passStyleExdr;
    }
}

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
package org.ofbiz.widget.tree;

import org.ofbiz.widget.ModelWidgetCondition;
import org.w3c.dom.Element;

/**
 * Models the &lt;condition&gt; element.
 * 
 * @see <code>widget-tree.xsd</code>
 */
@SuppressWarnings("serial")
public class ModelTreeCondition extends ModelWidgetCondition {
    public static final String module = ModelTreeCondition.class.getName();

    public ModelTreeCondition(ModelTree modelTree, Element conditionElement) {
        super(ModelWidgetCondition.DEFAULT_CONDITION_FACTORY, modelTree, conditionElement);
    }
}

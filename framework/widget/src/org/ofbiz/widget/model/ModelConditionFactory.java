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
package org.ofbiz.widget.model;

import org.w3c.dom.Element;

/**
 * A factory for <code>Condition</code> instances.
 *
 */
public interface ModelConditionFactory {
    /**
     * Returns a new <code>ModelCondition</code> instance built from <code>conditionElement</code>.
     * 
     * @param modelWidget The <code>ModelWidget</code> that contains the <code>Condition</code> instance.
     * @param conditionElement The XML element used to build the <code>Condition</code> instance.
     * @return A new <code>ModelCondition</code> instance built from <code>conditionElement</code>.
     * @throws IllegalArgumentException if no model was found for the XML element
     */
    ModelCondition newInstance(ModelWidget modelWidget, Element conditionElement);
}

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
package org.ofbiz.widget;

import org.ofbiz.widget.form.ModelFormAction;
import org.ofbiz.widget.menu.ModelMenuAction;
import org.ofbiz.widget.tree.ModelTreeAction;

/**
 *  A <code>ModelWidgetAction</code> visitor.
 */
public interface ModelActionVisitor {

    void visit(ModelFormAction.CallParentActions callParentActions);

    void visit(ModelWidgetAction.EntityAnd entityAnd);

    void visit(ModelWidgetAction.EntityCondition entityCondition);

    void visit(ModelWidgetAction.EntityOne entityOne);

    void visit(ModelWidgetAction.GetRelated getRelated);

    void visit(ModelWidgetAction.GetRelatedOne getRelatedOne);

    void visit(ModelWidgetAction.PropertyMap propertyMap);

    void visit(ModelWidgetAction.PropertyToField propertyToField);

    void visit(ModelWidgetAction.Script script);

    void visit(ModelWidgetAction.Service service);

    void visit(ModelWidgetAction.SetField setField);

    void visit(ModelFormAction.Service service);

    void visit(ModelMenuAction.SetField setField);

    void visit(ModelTreeAction.Script script);

    void visit(ModelTreeAction.Service service);

    void visit(ModelTreeAction.EntityAnd entityAnd);

    void visit(ModelTreeAction.EntityCondition entityCondition);
}

/*******************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License") throws Exception ; you may not use this file except in compliance
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

/**
 *  A <code>ModelAction</code> visitor.
 */
public interface ModelActionVisitor {

    void visit(ModelFormAction.CallParentActions callParentActions) throws Exception;

    void visit(AbstractModelAction.EntityAnd entityAnd) throws Exception;

    void visit(AbstractModelAction.EntityCondition entityCondition) throws Exception;

    void visit(AbstractModelAction.EntityOne entityOne) throws Exception;

    void visit(AbstractModelAction.GetRelated getRelated) throws Exception;

    void visit(AbstractModelAction.GetRelatedOne getRelatedOne) throws Exception;

    void visit(AbstractModelAction.PropertyMap propertyMap) throws Exception;

    void visit(AbstractModelAction.PropertyToField propertyToField) throws Exception;

    void visit(AbstractModelAction.Script script) throws Exception;

    void visit(AbstractModelAction.Service service) throws Exception;

    void visit(AbstractModelAction.SetField setField) throws Exception;

    void visit(ModelFormAction.Service service) throws Exception;

    void visit(ModelMenuAction.SetField setField) throws Exception;

    void visit(ModelTreeAction.Script script) throws Exception;

    void visit(ModelTreeAction.Service service) throws Exception;

    void visit(ModelTreeAction.EntityAnd entityAnd) throws Exception;

    void visit(ModelTreeAction.EntityCondition entityCondition) throws Exception;
}

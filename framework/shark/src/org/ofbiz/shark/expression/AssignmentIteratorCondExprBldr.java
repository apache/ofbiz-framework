/*
 * $Id: AssignmentIteratorCondExprBldr.java 7426 2006-04-26 23:35:58Z jonesde $
 *
 * Copyright 2004-2006 The Apache Software Foundation
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package org.ofbiz.shark.expression;

import org.enhydra.shark.api.common.AssignmentIteratorExpressionBuilder;

/**
 *
 * @author     <a href="mailto:jaz@ofbiz.org">Andy Zeneski</a>
 * @since      3.3
 */
public class AssignmentIteratorCondExprBldr extends BaseEntityCondExprBldr implements AssignmentIteratorExpressionBuilder {

    public AssignmentIteratorCondExprBldr() {
        this.addEntity("WFAS", "WfAssignment");
        this.addAllFields("WFAS");
    }

    public AssignmentIteratorExpressionBuilder and() {
        this.setOr(false);
        return this;
    }

    public AssignmentIteratorExpressionBuilder or() {
        this.setOr(true);
        return this;
    }

    public AssignmentIteratorExpressionBuilder not() {
        this.setNot(true);
        return this;
    }

    public AssignmentIteratorExpressionBuilder addUsernameEquals(String s) {
        return null;  // TODO: Implement Me!
    }

    public AssignmentIteratorExpressionBuilder addProcessIdEquals(String s) {
        return null;  // TODO: Implement Me!
    }

    public AssignmentIteratorExpressionBuilder addIsAccepted() {
        return null;  // TODO: Implement Me!
    }

    public AssignmentIteratorExpressionBuilder addPackageIdEquals(String s) {
        return null;  // TODO: Implement Me!
    }

    public AssignmentIteratorExpressionBuilder addPackageVersionEquals(String s) {
        return null;  // TODO: Implement Me!
    }

    public AssignmentIteratorExpressionBuilder addProcessDefEquals(String s) {
        return null;  // TODO: Implement Me!
    }

    public AssignmentIteratorExpressionBuilder addActivitySetDefEquals(String s) {
        return null;  // TODO: Implement Me!
    }

    public AssignmentIteratorExpressionBuilder addActivityDefEquals(String s) {
        return null;  // TODO: Implement Me!
    }
}

/*
 * $Id: EntityDateFilterCondition.java 5831 2005-09-26 06:52:24Z jonesde $
 *
 * Copyright (c) 2001-2005 The Open For Business Project - www.ofbiz.org
 *
 * Permission is hereby granted, free of charge, to any person obtaining a
 * copy of this software and associated documentation files (the "Software"),
 * to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
 * OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY
 * CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT
 * OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR
 * THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 */
package org.ofbiz.entity.condition;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;

import org.ofbiz.base.util.UtilDateTime;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericModelException;
import org.ofbiz.entity.model.ModelEntity;

public class EntityDateFilterCondition extends EntityCondition {

    protected String fromDateName;
    protected String thruDateName;

    public EntityDateFilterCondition(String fromDateName, String thruDateName) {
        this.fromDateName = fromDateName;
        this.thruDateName = thruDateName;
    }

    public String makeWhereString(ModelEntity modelEntity, List entityConditionParams) {
        EntityCondition condition = makeCondition();
        return condition.makeWhereString(modelEntity, entityConditionParams);
    }

    public void checkCondition(ModelEntity modelEntity) throws GenericModelException {
        EntityCondition condition = makeCondition();
        condition.checkCondition(modelEntity);
    }

    public boolean mapMatches(GenericDelegator delegator, Map map) {    
        EntityCondition condition = makeCondition();
        return condition.mapMatches(delegator, map);
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof EntityDateFilterCondition)) return false;
        EntityDateFilterCondition other = (EntityDateFilterCondition) obj;
        return equals(fromDateName, other.fromDateName) && equals(thruDateName, other.thruDateName);
    }

    public int hashCode() {
        return hashCode(fromDateName) ^ hashCode(thruDateName);
    }

    public void visit(EntityConditionVisitor visitor) {
        visitor.acceptEntityDateFilterCondition(this);
    }

    public void accept(EntityConditionVisitor visitor) {
        visitor.acceptEntityDateFilterCondition(this);
    }

    public EntityCondition freeze() {
        return this;
    }

    public void encryptConditionFields(ModelEntity modelEntity, GenericDelegator delegator) {
        // nothing to do here...
    }

    protected EntityCondition makeCondition() {
        return makeCondition(UtilDateTime.nowTimestamp(), fromDateName, thruDateName);
    }

    public static EntityExpr makeCondition(Timestamp moment, String fromDateName, String thruDateName) {
        return new EntityExpr(
            new EntityExpr(
                new EntityExpr( thruDateName, EntityOperator.EQUALS, null ),
                EntityOperator.OR,
                new EntityExpr( thruDateName, EntityOperator.GREATER_THAN, moment )
            ),
            EntityOperator.AND,
            new EntityExpr(
                new EntityExpr( fromDateName, EntityOperator.EQUALS, null ),
                EntityOperator.OR,
                new EntityExpr( fromDateName, EntityOperator.LESS_THAN_EQUAL_TO, moment )
            )
       );
    }
}

/*
 * $Id: EntityConditionListBase.java 5831 2005-09-26 06:52:24Z jonesde $
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

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.ofbiz.base.util.Debug;
import org.ofbiz.entity.EntityCryptoException;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericModelException;
import org.ofbiz.entity.model.ModelEntity;
import org.ofbiz.entity.model.ModelField;

/**
 * Encapsulates a list of EntityConditions to be used as a single EntityCondition combined as specified
 *
 * @author     <a href="mailto:jonesde@ofbiz.org">David E. Jones</a>
 * @version    $Rev$
 * @since      2.0
 */
public abstract class EntityConditionListBase extends EntityCondition {
    public static final String module = EntityConditionListBase.class.getName();

    protected List conditionList;
    protected EntityJoinOperator operator;

    protected EntityConditionListBase() {}

    public EntityConditionListBase(List conditionList, EntityJoinOperator operator) {
        this.conditionList = conditionList;
        this.operator = operator;
    }

    public EntityOperator getOperator() {
        return this.operator;
    }

    public EntityCondition getCondition(int index) {
        return (EntityCondition) this.conditionList.get(index);
    }
    
    protected int getConditionListSize() {
        return this.conditionList.size();
    }
    
    protected Iterator getConditionIterator() {
        return this.conditionList.iterator();
    }
    
    public void visit(EntityConditionVisitor visitor) {
        visitor.acceptEntityJoinOperator(operator, conditionList);
    }

    public String makeWhereString(ModelEntity modelEntity, List entityConditionParams) {
        // if (Debug.verboseOn()) Debug.logVerbose("makeWhereString for entity " + modelEntity.getEntityName(), module);
        StringBuffer sql = new StringBuffer();
        operator.addSqlValue(sql, modelEntity, entityConditionParams, conditionList);
        return sql.toString();
    }

    public void checkCondition(ModelEntity modelEntity) throws GenericModelException {
        // if (Debug.verboseOn()) Debug.logVerbose("checkCondition for entity " + modelEntity.getEntityName(), module);
        operator.validateSql(modelEntity, conditionList);
    }

    public boolean mapMatches(GenericDelegator delegator, Map map) {
        return operator.mapMatches(delegator, map, conditionList);
    }

    public EntityCondition freeze() {
        return operator.freeze(conditionList);
    }

    public void encryptConditionFields(ModelEntity modelEntity, GenericDelegator delegator) {
        Iterator conditionIter = this.conditionList.iterator();
        while (conditionIter.hasNext()) {
            EntityCondition cond = (EntityCondition) conditionIter.next();
            cond.encryptConditionFields(modelEntity, delegator);
        }
    }
    
    public boolean equals(Object obj) {
        if (!(obj instanceof EntityConditionListBase)) return false;
        EntityConditionListBase other = (EntityConditionListBase) obj;
        
        boolean isEqual = conditionList.equals(other.conditionList) && operator.equals(other.operator);
        //if (!isEqual) {
        //    Debug.logWarning("EntityConditionListBase.equals is false:\n this.operator=" + this.operator + "; other.operator=" + other.operator + 
        //            "\nthis.conditionList=" + this.conditionList +
        //            "\nother.conditionList=" + other.conditionList, module);
        //}
        return isEqual;
    }

    public int hashCode() {
        return conditionList.hashCode() + operator.hashCode();
    }
}

/*
 * $Id: EntityCondition.java 5831 2005-09-26 06:52:24Z jonesde $
 *
 * <p>Copyright (c) 2001 The Open For Business Project - www.ofbiz.org
 *
 * <p>Permission is hereby granted, free of charge, to any person obtaining a
 *  copy of this software and associated documentation files (the "Software"),
 *  to deal in the Software without restriction, including without limitation
 *  the rights to use, copy, modify, merge, publish, distribute, sublicense,
 *  and/or sell copies of the Software, and to permit persons to whom the
 *  Software is furnished to do so, subject to the following conditions:
 *
 * <p>The above copyright notice and this permission notice shall be included
 *  in all copies or substantial portions of the Software.
 *
 * <p>THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
 *  OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 *  MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 *  IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY
 *  CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT
 *  OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR
 *  THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package org.ofbiz.entity.condition;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericModelException;
import org.ofbiz.entity.GenericEntity;
import org.ofbiz.entity.model.ModelEntity;

/**
 * Represents the conditions to be used to constrain a query
 * <br/>An EntityCondition can represent various type of constraints, including:
 * <ul>
 *  <li>EntityConditionList: a list of EntityConditions, combined with the operator specified
 *  <li>EntityExpr: for simple expressions or expressions that combine EntityConditions
 *  <li>EntityFieldMap: a map of fields where the field (key) equals the value, combined with the operator specified
 * </ul>
 * These can be used in various combinations using the EntityConditionList and EntityExpr objects.
 *
 * @author     <a href="mailto:jonesde@ofbiz.org">David E. Jones</a>
 * @version    $Rev$
 * @since      2.0
 */
public abstract class EntityCondition extends EntityConditionBase {

    public String toString() {
        return makeWhereString(null, new ArrayList());
    }

    public void accept(EntityConditionVisitor visitor) {
        throw new IllegalArgumentException(getClass().getName() + ".accept not implemented");
    }

    abstract public String makeWhereString(ModelEntity modelEntity, List entityConditionParams);

    abstract public void checkCondition(ModelEntity modelEntity) throws GenericModelException;

    public boolean entityMatches(GenericEntity entity) {
        return mapMatches(entity.getDelegator(), entity);
    }    

    public Object eval(GenericEntity entity) {
        return eval(entity.getDelegator(), entity);
    }

    public Object eval(GenericDelegator delegator, Map map) {
        return mapMatches(delegator, map) ? Boolean.TRUE : Boolean.FALSE;
    }

    abstract public boolean mapMatches(GenericDelegator delegator, Map map);

    abstract public EntityCondition freeze();

    abstract public void encryptConditionFields(ModelEntity modelEntity, GenericDelegator delegator);
    
    public void visit(EntityConditionVisitor visitor) {
        throw new IllegalArgumentException(getClass().getName() + ".visit not implemented");
    }
}

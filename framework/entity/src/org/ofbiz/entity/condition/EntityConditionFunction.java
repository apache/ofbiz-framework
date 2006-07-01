/*
 * $Id: EntityConditionFunction.java 5831 2005-09-26 06:52:24Z jonesde $
 *
 *  Copyright (c) 2002 The Open For Business Project - www.ofbiz.org
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a
 *  copy of this software and associated documentation files (the "Software"),
 *  to deal in the Software without restriction, including without limitation
 *  the rights to use, copy, modify, merge, publish, distribute, sublicense,
 *  and/or sell copies of the Software, and to permit persons to whom the
 *  Software is furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included
 *  in all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
 *  OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 *  MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 *  IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY
 *  CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT
 *  OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR
 *  THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package org.ofbiz.entity.condition;

import java.util.List;
import java.util.Map;

import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericModelException;
import org.ofbiz.entity.model.ModelEntity;

/**
 * Encapsulates operations between entities and entity fields. This is a immutable class.
 *
 *@author     <a href='mailto:chris_maurer@altavista.com'>Chris Maurer</a>
 *@author     <a href="mailto:jonesde@ofbiz.org">David E. Jones</a>
 *@author     <a href="mailto:jaz@jflow.net">Andy Zeneski</a>
 *@created    Nov 5, 2001
 *@version    1.0
 */
public abstract class EntityConditionFunction extends EntityCondition {

    public static final int ID_NOT = 1;

    public static class NOT extends EntityConditionFunction {
        public NOT(EntityCondition nested) { super(ID_NOT, "NOT", nested); }
        public boolean mapMatches(GenericDelegator delegator, Map map) {
            return !condition.mapMatches(delegator, map);
        }
        public EntityCondition freeze() {
            return new NOT(condition.freeze());
        }
        public void encryptConditionFields(ModelEntity modelEntity, GenericDelegator delegator) {
            // nothing to do here...
        }
    };

    protected int idInt;
    protected String codeString;
    protected EntityCondition condition;

    protected EntityConditionFunction(int id, String code, EntityCondition condition) {
        idInt = id;
        codeString = code;
        this.condition = condition;
    }

    public String getCode() {
        if (codeString == null)
            return "null";
        else
            return codeString;
    }

    public int getId() {
        return idInt;
    }

    public void visit(EntityConditionVisitor visitor) {
        visitor.acceptEntityConditionFunction(this, condition);
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof EntityConditionFunction)) return false;
        EntityConditionFunction otherFunc = (EntityConditionFunction) obj;
        return
            this.idInt == otherFunc.idInt
            && ( this.condition != null ? condition.equals( otherFunc.condition ) : otherFunc.condition != null );
    }

    public int hashCode() {
        return idInt ^ condition.hashCode();
    }

    public String makeWhereString(ModelEntity modelEntity, List entityConditionParams) {
        StringBuffer sb = new StringBuffer();
        sb.append(codeString).append('(');
        sb.append(condition.makeWhereString(modelEntity, entityConditionParams));
        sb.append(')');
        return sb.toString();
    }

    public void checkCondition(ModelEntity modelEntity) throws GenericModelException {
        condition.checkCondition(modelEntity);
    }
}

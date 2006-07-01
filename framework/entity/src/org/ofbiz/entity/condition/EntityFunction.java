/*
 * $Id: EntityFunction.java 5462 2005-08-05 18:35:48Z jonesde $
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
import org.ofbiz.entity.config.DatasourceInfo;
import org.ofbiz.entity.model.ModelEntity;
import org.ofbiz.entity.model.ModelField;

/**
 * Encapsulates operations between entities and entity fields. This is a immutable class.
 *
 *@author     <a href='mailto:chris_maurer@altavista.com'>Chris Maurer</a>
 *@author     <a href="mailto:jonesde@ofbiz.org">David E. Jones</a>
 *@author     <a href="mailto:jaz@ofbiz.org">Andy Zeneski</a>
 *@since      1.0
 *@version    $Rev$
 */
public abstract class EntityFunction extends EntityConditionValue {

    public static interface Fetcher {
        Object getValue(Object value);
    }

    public static final int ID_LENGTH = 1;
    public static final int ID_TRIM = 2;
    public static final int ID_UPPER = 3;
    public static final int ID_LOWER = 4;

    public static class LENGTH extends EntityFunction {
        public static Fetcher FETCHER = new Fetcher() {
            public Object getValue(Object value) { return new Integer(value.toString().length()); }
        };
        public LENGTH(EntityConditionValue nested) { super(FETCHER, ID_LENGTH, "LENGTH", nested); }
        public LENGTH(Object value) { super(FETCHER, ID_LENGTH, "LENGTH", value); }
    };

    public static class TRIM extends EntityFunction {
        public static Fetcher FETCHER = new Fetcher() {
            public Object getValue(Object value) { return value.toString().trim(); }
        };
        public TRIM(EntityConditionValue nested) { super(FETCHER, ID_TRIM, "TRIM", nested); }
        public TRIM(Object value) { super(FETCHER, ID_TRIM, "TRIM", value); }
    };

    public static class UPPER extends EntityFunction {
        public static Fetcher FETCHER = new Fetcher() {
            public Object getValue(Object value) { return value.toString().toUpperCase(); }
        };
        public UPPER(EntityConditionValue nested) { super(FETCHER, ID_UPPER, "UPPER", nested); }
        public UPPER(Object value) { super(FETCHER, ID_UPPER, "UPPER", value); }
    };

    public static class LOWER extends EntityFunction {
        public static Fetcher FETCHER = new Fetcher() {
            public Object getValue(Object value) { return value.toString().toLowerCase(); }
        };
        public LOWER(EntityConditionValue nested) { super(FETCHER, ID_LOWER, "LOWER", nested); }
        public LOWER(Object value) { super(FETCHER, ID_LOWER, "LOWER", value); }
    };

    protected int idInt;
    protected String codeString;
    protected EntityConditionValue nested;
    protected Object value;
    protected Fetcher fetcher;

    protected EntityFunction(Fetcher fetcher, int id, String code, EntityConditionValue nested) {
        this.fetcher = fetcher;
        idInt = id;
        codeString = code;
        this.nested = nested;
    }

    protected EntityFunction(Fetcher fetcher, int id, String code, Object value) {
        this.fetcher = fetcher;
        idInt = id;
        codeString = code;
        if (value instanceof EntityConditionValue) {
            this.nested = (EntityConditionValue) value;
        } else {
            this.value = value;
        }
    }

    public EntityConditionValue freeze() {
        if (nested != null) {
            return new EntityFunction(fetcher, idInt, codeString, nested.freeze()) {};
        } else {
            return new EntityFunction(fetcher, idInt, codeString, value) {};
        }
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

    public int hashCode() {
        return codeString.hashCode();
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof EntityFunction)) return false;
        EntityFunction otherFunc = (EntityFunction) obj;
        return (this.idInt == otherFunc.idInt &&
            (this.nested != null ? nested.equals(otherFunc.nested) : otherFunc.nested == null) &&
            (this.value != null ? value.equals(otherFunc.value) : otherFunc.value == null));
    }

    public void addSqlValue(StringBuffer sql, Map tableAliases, ModelEntity modelEntity, List entityConditionParams, boolean includeTableNamePrefix, DatasourceInfo datasourceinfo) {
        sql.append(codeString).append('(');
        if (nested != null) {
            nested.addSqlValue(sql, tableAliases, modelEntity, entityConditionParams, includeTableNamePrefix, datasourceinfo);
        } else {
            addValue(sql, null, value, entityConditionParams);
        }
        sql.append(')');
    }

    public void visit(EntityConditionVisitor visitor) {
        if (nested != null) {
            visitor.acceptEntityConditionValue(nested);
        } else {
            visitor.acceptObject(value);
        }
    }

    public void accept(EntityConditionVisitor visitor) {
        visitor.acceptEntityFunction(this);
    }

    public ModelField getModelField(ModelEntity modelEntity) {
        if (nested != null) {
            return nested.getModelField(modelEntity);
        }
        return null;
    }

    public void validateSql(ModelEntity modelEntity) throws GenericModelException {
        if (nested != null) {
            nested.validateSql(modelEntity);
        }
    }

    public Object getValue(GenericDelegator delegator, Map map) {
        Object value = nested != null ? nested.getValue(delegator, map) : this.value;
        return value != null ? fetcher.getValue(value) : null;
    }
}

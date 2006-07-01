/*
 * $Id: EntityFieldValue.java 5687 2005-09-10 01:10:15Z jonesde $
 *
 *  Copyright (c) 2002-2004 The Open For Business Project - www.ofbiz.org
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
import org.ofbiz.entity.GenericEntity;
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
public class EntityFieldValue extends EntityConditionValue {

    protected String fieldName;

    public EntityFieldValue(String fieldName) {
        this.fieldName = fieldName;
    }

    public String getFieldName() {
        return fieldName;
    }

    public int hashCode() {
        return fieldName.hashCode();
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof EntityFieldValue)) return false;
        EntityFieldValue otherValue = (EntityFieldValue) obj;
        return fieldName.equals(otherValue.fieldName);
    }

    public ModelField getModelField(ModelEntity modelEntity) {
        return getField(modelEntity, fieldName);
    }

    public void addSqlValue(StringBuffer sql, Map tableAliases, ModelEntity modelEntity, List entityConditionParams, boolean includeTableNamePrefix, DatasourceInfo datasourceInfo) {
        sql.append(getColName(tableAliases, modelEntity, fieldName, includeTableNamePrefix, datasourceInfo));
    }

    public void validateSql(ModelEntity modelEntity) throws GenericModelException {
        ModelField field = getModelField(modelEntity);
        if (field == null) {
            throw new GenericModelException("Field with name " + fieldName + " not found in the " + modelEntity.getEntityName() + " Entity");
        }
    }

    public Object getValue(GenericDelegator delegator, Map map) {
        if (map == null) {
            return null;
        }
        if (map instanceof GenericEntity.NULL) {
            return null;
        } else {
            return map.get(fieldName);
        }
    }

    public void visit(EntityConditionVisitor visitor) {
        visitor.acceptEntityFieldValue(this);
    }

    public void accept(EntityConditionVisitor visitor) {
        visitor.acceptEntityFieldValue(this);
    }

    public EntityConditionValue freeze() {
        return this;
    }
}

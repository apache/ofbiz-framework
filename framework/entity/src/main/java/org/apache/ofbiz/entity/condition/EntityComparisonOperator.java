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

package org.apache.ofbiz.entity.condition;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.oro.text.perl.Perl5Util;
import org.apache.oro.text.regex.MalformedPatternException;
import org.apache.oro.text.regex.Pattern;
import org.apache.oro.text.regex.PatternMatcher;
import org.apache.oro.text.regex.Perl5Matcher;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.PatternFactory;
import org.apache.ofbiz.base.util.UtilGenerics;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericModelException;
import org.apache.ofbiz.entity.config.model.Datasource;
import org.apache.ofbiz.entity.model.ModelEntity;
import org.apache.ofbiz.entity.model.ModelField;

/**
 * Base class for comparisons.
 */
@SuppressWarnings("serial")
public abstract class EntityComparisonOperator<L, R> extends EntityOperator<L, R, Boolean> {

    public static final String module = EntityComparisonOperator.class.getName();

    public static Pattern makeOroPattern(String sqlLike) {
        Perl5Util perl5Util = new Perl5Util();
        try {
            sqlLike = perl5Util.substitute("s/([$^.+*?])/\\\\$1/g", sqlLike);
            sqlLike = perl5Util.substitute("s/%/.*/g", sqlLike);
            sqlLike = perl5Util.substitute("s/_/./g", sqlLike);
        } catch (Throwable t) {
            String errMsg = "Error in ORO pattern substitution for SQL like clause [" + sqlLike + "]: " + t.toString();
            Debug.logError(t, errMsg, module);
            throw new IllegalArgumentException(errMsg);
        }
        try {
            return PatternFactory.createOrGetPerl5CompiledPattern(sqlLike, true);
        } catch (MalformedPatternException e) {
            Debug.logError(e, module);
        }
        return null;
    }

    @Override
    public void validateSql(ModelEntity entity, L lhs, R rhs) throws GenericModelException {
        if (lhs instanceof EntityConditionValue) {
            EntityConditionValue ecv = (EntityConditionValue) lhs;
            ecv.validateSql(entity);
        }
        if (rhs instanceof EntityConditionValue) {
            EntityConditionValue ecv = (EntityConditionValue) rhs;
            ecv.validateSql(entity);
        }
    }

    @Override
    public void visit(EntityConditionVisitor visitor, L lhs, R rhs) {
        visitor.accept(lhs);
        visitor.accept(rhs);
    }

    @Override
    public void addSqlValue(StringBuilder sql, ModelEntity entity, List<EntityConditionParam> entityConditionParams, boolean compat, L lhs, R rhs, Datasource datasourceInfo) {
        //Debug.logInfo("EntityComparisonOperator.addSqlValue field=" + lhs + ", value=" + rhs + ", value type=" + (rhs == null ? "null object" : rhs.getClass().getName()), module);

        // if this is an IN operator and the rhs Object isEmpty, add "1=0" instead of the normal SQL.  Note that "FALSE" does not work with all databases.
        if (this.idInt == EntityOperator.ID_IN && UtilValidate.isEmpty(rhs)) {
            sql.append("1=0");
            return;
        }

        ModelField field;
        if (lhs instanceof EntityConditionValue) {
            EntityConditionValue ecv = (EntityConditionValue) lhs;
            ecv.addSqlValue(sql, entity, entityConditionParams, false, datasourceInfo);
            field = ecv.getModelField(entity);
        } else if (compat && lhs instanceof String) {
            field = getField(entity, (String) lhs);
            if (field == null) {
                sql.append(lhs);
            } else {
                sql.append(field.getColName());
            }
        } else {
            addValue(sql, null, lhs, entityConditionParams);
            field = null;
        }

        makeRHSWhereString(entity, entityConditionParams, sql, field, rhs, datasourceInfo);
    }

    @Override
    public boolean isEmpty(L lhs, R rhs) {
        return false;
    }

    protected void makeRHSWhereString(ModelEntity entity, List<EntityConditionParam> entityConditionParams, StringBuilder sql, ModelField field, R rhs, Datasource datasourceInfo) {
        sql.append(' ').append(getCode()).append(' ');
        makeRHSWhereStringValue(entity, entityConditionParams, sql, field, rhs, datasourceInfo);
    }

    protected void makeRHSWhereStringValue(ModelEntity entity, List<EntityConditionParam> entityConditionParams, StringBuilder sql, ModelField field, R rhs, Datasource datasourceInfo) {
        if (rhs instanceof EntityConditionValue) {
            EntityConditionValue ecv = (EntityConditionValue) rhs;
            if (ecv.getModelField(entity) == null) {
                ecv.setModelField(field);
            }
            ecv.addSqlValue(sql, entity, entityConditionParams, false, datasourceInfo);
        } else {
            addValue(sql, field, rhs, entityConditionParams);
        }
    }

    public abstract boolean compare(L lhs, R rhs);

    public Boolean eval(Delegator delegator, Map<String, ? extends Object> map, L lhs, R rhs) {
        return Boolean.valueOf(mapMatches(delegator, map, lhs, rhs));
    }

    @Override
     public boolean mapMatches(Delegator delegator, Map<String, ? extends Object> map, L lhs, R rhs) {
        Object leftValue;
        if (lhs instanceof EntityConditionValue) {
            EntityConditionValue ecv = (EntityConditionValue) lhs;
            leftValue = ecv.getValue(delegator, map);
        } else if (lhs instanceof String) {
            leftValue = map.get(lhs);
        } else {
            leftValue = lhs;
        }
        Object rightValue;
        if (rhs instanceof EntityConditionValue) {
            EntityConditionValue ecv = (EntityConditionValue) rhs;
            rightValue = ecv.getValue(delegator, map);
        } else {
            rightValue = rhs;
        }

        if (leftValue == WILDCARD || rightValue == WILDCARD) return true;
        return compare(UtilGenerics.<L>cast(leftValue), UtilGenerics.<R>cast(rightValue));
    }

    @Override
    public EntityCondition freeze(L lhs, R rhs) {
        return EntityCondition.makeCondition(freeze(lhs), this, freeze(rhs));
    }

    protected Object freeze(Object item) {
        if (item instanceof EntityConditionValue) {
            EntityConditionValue ecv = (EntityConditionValue) item;
            return ecv.freeze();
        } else {
            return item;
        }
    }

    public EntityComparisonOperator(int id, String code) {
        super(id, code);
    }

    public static final <T> boolean compareEqual(Comparable<T> lhs, T rhs) {
        if (lhs == null) {
            if (rhs != null) {
                return false;
            }
        } else if (!lhs.equals(rhs)) {
            return false;
        }
        return true;
    }

    public static final <T> boolean compareNotEqual(Comparable<T> lhs, T rhs) {
        if (lhs == null) {
            if (rhs == null) {
                return false;
            }
        } else if (lhs.equals(rhs)) {
            return false;
        }
        return true;
    }

    public static final <T> boolean compareGreaterThan(Comparable<T> lhs, T rhs) {
        if (lhs == null) {
            if (rhs != null) {
                return false;
            }
        } else if (lhs.compareTo(rhs) <= 0) {
            return false;
        }
        return true;
    }

    public static final <T> boolean compareGreaterThanEqualTo(Comparable<T> lhs, T rhs) {
        if (lhs == null) {
            if (rhs != null) {
                return false;
            }
        } else if (lhs.compareTo(rhs) < 0) {
            return false;
        }
        return true;
    }

    public static final <T> boolean compareLessThan(Comparable<T> lhs, T rhs) {
        if (lhs == null) {
            if (rhs != null) {
                return false;
            }
        } else if (lhs.compareTo(rhs) >= 0) {
            return false;
        }
        return true;
    }

    public static final <T> boolean compareLessThanEqualTo(Comparable<T> lhs, T rhs) {
        if (lhs == null) {
            if (rhs != null) {
                return false;
            }
        } else if (lhs.compareTo(rhs) > 0) {
            return false;
        }
        return true;
    }

    public static final <L,R> boolean compareIn(L lhs, R rhs) {
        if (lhs == null) {
            if (rhs != null) {
                return false;
            } else {
                return true;
            }
        } else if (rhs instanceof Collection<?>) {
            if (((Collection<?>) rhs).contains(lhs)) {
                return true;
            } else {
                return false;
            }
        } else if (lhs.equals(rhs)) {
            return true;
        } else {
            return false;
        }
    }

    public static final <L,R> boolean compareLike(L lhs, R rhs) {
        PatternMatcher matcher = new Perl5Matcher();
        if (lhs == null) {
            if (rhs != null) {
                return false;
            }
        } else if (lhs instanceof String && rhs instanceof String) {
            //see if the lhs value is like the rhs value, rhs will have the pattern characters in it...
            return matcher.matches((String) lhs, makeOroPattern((String) rhs));
        }
        return true;
    }
}

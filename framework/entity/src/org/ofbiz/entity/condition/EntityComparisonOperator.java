/*
 * $Id: EntityComparisonOperator.java 6155 2005-11-21 07:30:52Z jonesde $
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

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.oro.text.perl.Perl5Util;
import org.apache.oro.text.regex.MalformedPatternException;
import org.apache.oro.text.regex.Pattern;
import org.apache.oro.text.regex.PatternCompiler;
import org.apache.oro.text.regex.PatternMatcher;
import org.apache.oro.text.regex.Perl5Compiler;
import org.apache.oro.text.regex.Perl5Matcher;
import org.ofbiz.base.util.Debug;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericModelException;
import org.ofbiz.entity.model.ModelEntity;
import org.ofbiz.entity.model.ModelField;

/**
 * Encapsulates operations between entities and entity fields. This is a immutable class.
 *
 * @author     <a href="mailto:adam@doogie.org">Adam Heath</a>
 * @author     <a href="mailto:jonesde@ofbiz.org">David E. Jones</a>
 * @version    $Rev$
 * @since      3.0
 */
public class EntityComparisonOperator extends EntityOperator {
    
    public static final String module = EntityComparisonOperator.class.getName();

    protected static PatternMatcher matcher = new Perl5Matcher();
    protected static Perl5Util perl5Util = new Perl5Util();
    protected static PatternCompiler compiler = new Perl5Compiler();

    public static Pattern makeOroPattern(String sqlLike) {
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
            return compiler.compile(sqlLike);
        } catch (MalformedPatternException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void validateSql(ModelEntity entity, Object lhs, Object rhs) throws GenericModelException {
        if (lhs instanceof EntityConditionValue) {
            EntityConditionValue ecv = (EntityConditionValue) lhs;
            ecv.validateSql(entity);
        }
        if (rhs instanceof EntityConditionValue) {
            EntityConditionValue ecv = (EntityConditionValue) rhs;
            ecv.validateSql(entity);
        }
    }

    public void visit(EntityConditionVisitor visitor, Object lhs, Object rhs) {
        visitor.accept(lhs);
        visitor.accept(rhs);
    }

    public void addSqlValue(StringBuffer sql, ModelEntity entity, List entityConditionParams, boolean compat, Object lhs, Object rhs) {
        //Debug.logInfo("EntityComparisonOperator.addSqlValue field=" + lhs + ", value=" + rhs + ", value type=" + (rhs == null ? "null object" : rhs.getClass().getName()), module);
        ModelField field;
        if (lhs instanceof EntityConditionValue) {
            EntityConditionValue ecv = (EntityConditionValue) lhs;
            ecv.addSqlValue(sql, entity, entityConditionParams, false, null);
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

        makeRHSWhereString(entity, entityConditionParams, sql, field, rhs);
    }

    protected void makeRHSWhereString(ModelEntity entity, List entityConditionParams, StringBuffer sql, ModelField field, Object rhs) {
        sql.append(' ').append(getCode()).append(' ');
        makeRHSWhereStringValue(entity, entityConditionParams, sql, field, rhs);
    }

    protected void makeRHSWhereStringValue(ModelEntity entity, List entityConditionParams, StringBuffer sql, ModelField field, Object rhs) {
        if (rhs instanceof EntityConditionValue) {
            EntityConditionValue ecv = (EntityConditionValue) rhs;
            ecv.addSqlValue(sql, entity, entityConditionParams, false, null);
        } else {
            addValue(sql, field, rhs, entityConditionParams);
        }
    }
            
    public boolean compare(Object lhs, Object rhs) {
        throw new UnsupportedOperationException(codeString);
    }

    public Object eval(GenericDelegator delegator, Map map, Object lhs, Object rhs) {
        return mapMatches(delegator, map, lhs, rhs) ? Boolean.TRUE : Boolean.FALSE;
    }

    public boolean mapMatches(GenericDelegator delegator, Map map, Object lhs, Object rhs) {
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
        return compare(leftValue, rightValue);
    }

    public EntityCondition freeze(Object lhs, Object rhs) {
        return new EntityExpr(freeze(lhs), this, freeze(rhs));
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

    public static final boolean compareEqual(Object lhs, Object rhs) {
        if (lhs == null) {
            if (rhs != null) {
                return false;
            }
        } else if (!lhs.equals(rhs)) {
            return false;
        }
        return true;
    }

    public static final boolean compareNotEqual(Object lhs, Object rhs) {
        if (lhs == null) {
            if (rhs == null) {
                return false;
            }
        } else if (lhs.equals(rhs)) {
            return false;
        }
        return true;
    }

    public static final boolean compareGreaterThan(Object lhs, Object rhs) {
        if (lhs == null) {
            if (rhs != null) {
                return false;
            }
        } else if (((Comparable) lhs).compareTo(rhs) <= 0) {
            return false;
        }
        return true;
    }

    public static final boolean compareGreaterThanEqualTo(Object lhs, Object rhs) {
        if (lhs == null) {
            if (rhs != null) {
                return false;
            }
        } else if (((Comparable) lhs).compareTo(rhs) < 0) {
            return false;
        }
        return true;
    }

    public static final boolean compareLessThan(Object lhs, Object rhs) {
        if (lhs == null) {
            if (rhs != null) {
                return false;
            }
        } else if (((Comparable) lhs).compareTo(rhs) >= 0) {
            return false;
        }
        return true;
    }

    public static final boolean compareLessThanEqualTo(Object lhs, Object rhs) {
        if (lhs == null) {
            if (rhs != null) {
                return false;
            }
        } else if (((Comparable) lhs).compareTo(rhs) > 0) {
            return false;
        }
        return true;
    }

    public static final boolean compareIn(Object lhs, Object rhs) {
        if (lhs == null) {
            if (rhs != null) {
                return false;
            } else {
                return true;
            }
        } else if (rhs instanceof Collection) {
            if (((Collection) rhs).contains(lhs)) {
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

    public static final boolean compareLike(Object lhs, Object rhs) {
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

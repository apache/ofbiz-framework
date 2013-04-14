/*
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
 */
package org.ofbiz.entity.sql;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javolution.util.FastList;

import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.model.DynamicViewEntity;
import org.ofbiz.entity.model.ModelKeyMap;
import org.ofbiz.entity.model.ModelViewEntity.ComplexAlias;
import org.ofbiz.entity.model.ModelViewEntity.ComplexAliasField;
import org.ofbiz.entity.model.ModelViewEntity.ComplexAliasMember;
import org.ofbiz.sql.ConstantValue;
import org.ofbiz.sql.FieldAll;
import org.ofbiz.sql.FieldDef;
import org.ofbiz.sql.FieldValue;
import org.ofbiz.sql.FunctionCall;
import org.ofbiz.sql.Joined;
import org.ofbiz.sql.KeyMap;
import org.ofbiz.sql.MathValue;
import org.ofbiz.sql.NumberValue;
import org.ofbiz.sql.OrderByItem;
import org.ofbiz.sql.Planner;
import org.ofbiz.sql.Relation;
import org.ofbiz.sql.SQLDelete;
import org.ofbiz.sql.SQLInsert;
import org.ofbiz.sql.SQLSelect;
import org.ofbiz.sql.SQLUpdate;
import org.ofbiz.sql.SQLView;
import org.ofbiz.sql.SelectGroup;
import org.ofbiz.sql.StaticValue;
import org.ofbiz.sql.StringValue;
import org.ofbiz.sql.Table;
import org.ofbiz.sql.TableName;
import org.ofbiz.sql.Unioned;
import org.ofbiz.sql.Value;

public class EntityPlanner extends Planner<EntityPlanner, EntityCondition, EntityDeletePlan, EntityInsertPlan, EntitySelectPlan, EntityUpdatePlan, EntityViewPlan> {
    public EntityPlanner() {
        super(new EntityConditionPlanner());
    }

    @Override
    public EntityDeletePlan planDelete(SQLDelete deleteStatement) {
        return null;
    }

    @Override
    public EntityInsertPlan planInsert(SQLInsert insertStatement) {
        return null;
    }

    @Override
    public EntitySelectPlan planSelect(SQLSelect selectStatement) {
        DynamicViewEntity dve = new DynamicViewEntity();
        Unioned unioned = selectStatement.getUnioned();
        if (unioned != null) {
            throw new IllegalArgumentException("union views not yet supported");
        }
        SelectGroup selectGroup = unioned.getGroup();
        Table table = selectGroup.getTable();
        addMember(dve, table.getTableName());
        addJoined(dve, table.getTableName().getAlias(), table.getJoined());
        if (selectGroup.getFieldAlls() != null) {
            for (FieldAll fieldAll: selectGroup.getFieldAlls()) {
                List<String> excludes = new LinkedList<String>();
                for (String exclude: fieldAll) {
                    excludes.add(exclude);
                }
                if (excludes.isEmpty()) {
                    excludes = null;
                }
                dve.addAliasAll(fieldAll.getAlias(), null, excludes);
            }
        }
        if (selectStatement.getRelations() != null) {
            for (Relation relation: selectStatement.getRelations().values()) {
                dve.addRelation(relation.getType(), relation.getTitle(), relation.getEntityName(), buildKeyMaps(relation));
            }
        }
        List<String> groupBy = selectGroup.getGroupBy();
        if (groupBy == null) {
            groupBy = Collections.emptyList();
        }
        if (selectGroup.getFieldDefs() != null) {
            for (FieldDef fieldDef: selectGroup.getFieldDefs()) {
                addFieldDef(dve, groupBy, fieldDef.getAlias(), fieldDef);
            }
        }
        List<String> orderBy;
        if (selectStatement.getOrderBy() == null) {
            orderBy = null;
        } else {
            orderBy = new LinkedList<String>();
            for (OrderByItem orderByItem: selectStatement.getOrderBy()) {
                orderBy.add(orderByItem.toString());
            }
        }
        return new EntitySelectPlan(dve, plan(selectGroup.getWhereCondition()), plan(selectGroup.getHavingCondition()), orderBy);
    }

    @Override
    public EntityUpdatePlan planUpdate(SQLUpdate updateStatement) {
        return null;
    }

    @Override
    public EntityViewPlan planView(SQLView viewStatement) {
        return null;
    }

    private static void addFieldDef(DynamicViewEntity dve, List<String> groupBy, String alias, FieldDef fieldDef) {
        StaticValue value = fieldDef.getValue();
        if (value instanceof FieldValue) {
            addFieldDef(dve, groupBy, fieldDef.getAlias(), (FieldValue) value, null);
        } else if (value instanceof ConstantValue) {
            addFieldDef(dve, groupBy, fieldDef.getAlias(), value);
        } else {
            throw new UnsupportedOperationException(alias + "[" + fieldDef + "]:" + fieldDef.getClass());
        }
    }

    private static void addFieldDef(DynamicViewEntity dve, List<String> groupBy, String alias, FieldValue fieldValue, String function) {
        dve.addAlias(fieldValue.getTableName(), alias, fieldValue.getFieldName(), null, null, groupBy.contains(alias), function);
    }

    private static void addFieldDef(DynamicViewEntity dve, List<String> groupBy, String alias, ComplexAliasMember member) {
        dve.addAlias(null, alias, null, null, null, groupBy.contains(alias), null, member);
    }

    private static void addFieldDef(DynamicViewEntity dve, List<String> groupBy, String alias, ConstantValue value) {
        if (value instanceof FieldValue) {
            addFieldDef(dve, groupBy, alias, (FieldValue) value, null);
            return;
        } else if (value instanceof FunctionCall) {
            FunctionCall fc = (FunctionCall) value;
            String name = fc.getName().toLowerCase();
            Iterator<Value> it = fc.iterator();
            if (it.hasNext()) {
                Value firstValue = it.next();
                if (!it.hasNext()) {
                    if (firstValue instanceof FieldValue) {
                        addFieldDef(dve, groupBy, alias, (FieldValue) firstValue, name);
                        return;
                    }
                }
            }
        }
        addFieldDef(dve, groupBy, alias, buildComplexMember(value));
    }

    private static ComplexAliasMember buildComplexMember(Value value) {
        if (value instanceof FieldValue) {
            FieldValue fv = (FieldValue) value;
            return new ComplexAliasField(fv.getTableName(), fv.getFieldName(), null, null);
        } else if (value instanceof FunctionCall) {
            FunctionCall fc = (FunctionCall) value;
            String name = fc.getName().toLowerCase();
            if (fc.getArgCount() == 1) {
                Value firstValue = fc.iterator().next();
                if (firstValue instanceof FieldValue) {
                    FieldValue fv = (FieldValue) firstValue;
                    return new ComplexAliasField(fv.getTableName(), fv.getFieldName(), null, name);
                } else if (firstValue instanceof FunctionCall) {
                    FunctionCall fc2 = (FunctionCall) firstValue;
                    if (fc2.getName().equalsIgnoreCase("coalesce") && fc2.getArgCount() == 2) {
                        Iterator<Value> it = fc2.iterator();
                        Value f1 = it.next(), f2 = it.next();
                        if (f1 instanceof FieldValue) {
                            FieldValue fv = (FieldValue) f1;
                            if (f2 instanceof NumberValue<?>) {
                                return new ComplexAliasField(fv.getTableName(), fv.getFieldName(), ((NumberValue<?>) f2).getNumber().toString(), name);
                            } else if (f2 instanceof StringValue) {
                                return new ComplexAliasField(fv.getTableName(), fv.getFieldName(), "'" + ((StringValue) f2).getString() + "'", name);
                           }
                        }
                    }
                }
            } else if (fc.getName().equalsIgnoreCase("coalesce") && fc.getArgCount() == 2) {
                Iterator<Value> it = fc.iterator();
                Value f1 = it.next(), f2 = it.next();
                if (f1 instanceof FieldValue) {
                    FieldValue fv = (FieldValue) f1;
                    if (f2 instanceof NumberValue<?>) {
                        return new ComplexAliasField(fv.getTableName(), fv.getFieldName(), ((NumberValue<?>) f2).getNumber().toString(), null);
                    } else if (f2 instanceof StringValue) {
                        return new ComplexAliasField(fv.getTableName(), fv.getFieldName(), "'" + ((StringValue) f2).getString() + "'", null);
                   }
                }
            }
        } else if (value instanceof MathValue) {
            MathValue mv = (MathValue) value;
            ComplexAlias complexAlias = new ComplexAlias(mv.getOp());
            for (ConstantValue constantValue: mv) {
                complexAlias.addComplexAliasMember(buildComplexMember(constantValue));
            }
            return complexAlias;
        }
        throw new UnsupportedOperationException(value + ":" + value.getClass());
    }

    private static void addMember(DynamicViewEntity dve, TableName member) {
        dve.addMemberEntity(member.getAlias(), member.getTableName());
    }

    private static void addJoined(DynamicViewEntity dve, String leftAlias, Joined joined) {
        if (joined == null) return;
        addMember(dve, joined.getTableName());
        dve.addViewLink(leftAlias, joined.getTableName().getAlias(), joined.isOptional(), buildKeyMaps(joined));
        addJoined(dve, joined.getTableName().getAlias(), joined.getJoined());
    }

    private static List<ModelKeyMap> buildKeyMaps(Iterable<KeyMap> keyMaps) {
        List<ModelKeyMap> entityKeyMaps = FastList.newInstance();
        for (KeyMap keyMap: keyMaps) {
            entityKeyMaps.add(new ModelKeyMap(keyMap.getLeftFieldName(), keyMap.getRightFieldName()));
        }
        return entityKeyMaps;
    }
}

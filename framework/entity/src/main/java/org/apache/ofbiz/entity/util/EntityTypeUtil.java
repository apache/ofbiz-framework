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
package org.apache.ofbiz.entity.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;

/**
 * Makes it easier to deal with entities that follow the
 * extensibility pattern and that can be of various types as identified in the database.
 */
public final class EntityTypeUtil {

    public static final String module = EntityTypeUtil.class.getName();

    private EntityTypeUtil() {}

    public static boolean isType(Collection<GenericValue> thisCollection, String typeRelation, GenericValue targetType) {
        for (GenericValue value: thisCollection) {
            try {
                GenericValue related = value.getRelatedOne(typeRelation, false);
                if (isType(related, targetType)) {
                    return true;
                } // else keep looking
            } catch (GenericEntityException e) {
                continue;
            }
        }
        return false;
    }

    /* public static boolean isType(Collection thisTypeCollection, GenericValue targetType) {
     Iterator iter = thisTypeCollection.iterator();
     while (iter.hasNext()) {
     if (isType((GenericValue) iter.next(), targetType)) {
     return true;
     }//else keep looking
     }
     return false;
     }*/

    /* private static Object getTypeID(GenericValue typeValue) {
     Collection keys = typeValue.getAllKeys();
     if (keys.size() == 1) {
     return keys.iterator().next();
     } else {
     throw new IllegalArgumentException("getTypeID expecting value with single key");
     }
     }*/

    private static GenericValue getParentType(GenericValue typeValue) {
        // assumes Parent relation is "Parent<entityName>"
        try {
            return typeValue.getRelatedOne("Parent" + typeValue.getEntityName(), true);
        } catch (GenericEntityException e) {
            Debug.logWarning(e, module);
            return null;
        }
    }

    public static List<GenericValue> getDescendantTypes(GenericValue typeValue) {
        // assumes Child relation is "Child<entityName>"
        List<GenericValue> descendantTypes = new ArrayList<GenericValue>();

        // first get all childrenTypes ...
        List<GenericValue> childrenTypes = null;
        try {
            childrenTypes = typeValue.getRelated("Child" + typeValue.getEntityName(), null, null, true);
        } catch (GenericEntityException e) {
            Debug.logWarning(e, module);
            return null;
        }
        if (childrenTypes == null)
            return null;

        // ... and add them as direct descendants
        descendantTypes.addAll(childrenTypes);

        // then add all descendants of the children
        for (GenericValue childType: childrenTypes) {
            List<GenericValue> childTypeDescendants = getDescendantTypes(childType);
            if (childTypeDescendants != null) {
                descendantTypes.addAll(childTypeDescendants);
            }
        }

        return descendantTypes;
    }

    public static boolean isType(GenericValue thisType, GenericValue targetType) {
        if (thisType == null) {
            return false;
        } else if (targetType.equals(thisType)) {
            return true;
        } else {
            return isType(getParentType(thisType), targetType);
        }
    }

    /**
     * A generic method to be used on Type enities, e.g. ProductType.  Recurse to the root level in the type hierarchy
     * and checks if the specified type childType has parentType as its parent somewhere in the hierarchy.
     *
     * @param delegator       The Delegator object.
     * @param entityName      Name of the Type entity on which check is performed.
     * @param primaryKey      Primary Key field of the Type entity.
     * @param childType       Type value for which the check is performed.
     * @param parentTypeField Field in Type entity which stores the parent type.
     * @param parentType      Value of the parent type against which check is performed.
     * @return boolean value based on the check results.
     */
    public static boolean hasParentType(Delegator delegator, String entityName, String primaryKey, String childType, String parentTypeField, String parentType) {
        GenericValue childTypeValue = null;
        try {
            childTypeValue = EntityQuery.use(delegator).from(entityName).where(primaryKey, childType).cache(true).queryOne();
        } catch (GenericEntityException e) {
            Debug.logError("Error finding "+entityName+" record for type "+childType, module);
        }
        if (childTypeValue != null) {
            if (parentType.equals(childTypeValue.getString(primaryKey))) return true;

            if (childTypeValue.getString(parentTypeField) != null) {
                if (parentType.equals(childTypeValue.getString(parentTypeField))) {
                    return true;
                } else {
                    return hasParentType(delegator, entityName, primaryKey, childTypeValue.getString(parentTypeField), parentTypeField, parentType);
                }
            }
        }

        return false;
    }
}

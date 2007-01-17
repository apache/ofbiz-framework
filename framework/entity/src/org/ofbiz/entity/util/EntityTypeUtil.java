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
package org.ofbiz.entity.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.ofbiz.base.util.Debug;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;

/**
 * Makes it easier to deal with entities that follow the
 * extensibility pattern and that can be of various types as identified in the database.
 */
public class EntityTypeUtil {
    
    public static final String module = EntityTypeUtil.class.getName();

    public static boolean isType(Collection thisCollection, String typeRelation, GenericValue targetType) {
        Iterator iter = thisCollection.iterator();

        while (iter.hasNext()) {
            try {
                GenericValue related = ((GenericValue) iter.next()).getRelatedOne(typeRelation);

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
            return typeValue.getRelatedOneCache("Parent" + typeValue.getEntityName());
        } catch (GenericEntityException e) {
            Debug.logWarning(e, module);
            return null;
        }
    }

    public static List getDescendantTypes(GenericValue typeValue) {
        // assumes Child relation is "Child<entityName>"
        List descendantTypes = new ArrayList();

        // first get all childrenTypes ...
        List childrenTypes = null;
        try {
            childrenTypes = typeValue.getRelatedCache("Child" + typeValue.getEntityName());
        } catch (GenericEntityException e) {
            Debug.logWarning(e, module);
            return null;
        }
        if (childrenTypes == null)
            return null;

        // ... and add them as direct descendants
        descendantTypes.addAll(childrenTypes);

        // then add all descendants of the children
        Iterator childrenTypeIter = childrenTypes.iterator();
        while (childrenTypeIter.hasNext()) {
            GenericValue childType = (GenericValue) childrenTypeIter.next();
            List childTypeDescendants = getDescendantTypes(childType);
            if (childTypeDescendants != null) {
                descendantTypes.addAll(childTypeDescendants);
            }
        }

        return descendantTypes;
    }

    /**
     *  Description of the Method
     *
     *@param  catName                       Description of Parameter
     *@exception  java.rmi.RemoteException  Description of Exception
     */
    public static boolean isType(GenericValue thisType, GenericValue targetType) {
        if (thisType == null) {
            return false;
        } else if (targetType.equals(thisType)) {
            return true;
        } else {
            return isType(getParentType(thisType), targetType);
        }
    }
}

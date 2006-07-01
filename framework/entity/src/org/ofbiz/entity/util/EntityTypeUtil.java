/*
 * $Id: EntityTypeUtil.java 5462 2005-08-05 18:35:48Z jonesde $
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
 *
 * @author     <a href="mailto:jonesde@ofbiz.org">David E. Jones</a>
 * @version    $Rev$
 * @since      2.0
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

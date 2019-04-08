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
package org.apache.ofbiz.entity;

import java.util.Map;

import org.apache.ofbiz.entity.model.ModelEntity;

/**
 * Generic Entity Primary Key Object
 *
 */
@SuppressWarnings("serial")
public class GenericPK extends GenericEntity {

    protected GenericPK() { }

    /** Creates new GenericPK */
    public static GenericPK create(ModelEntity modelEntity) {
        GenericPK newPK = new GenericPK();
        newPK.init(modelEntity);
        return newPK;
    }

    /** Creates new GenericPK from existing Map */
    public static GenericPK create(Delegator delegator, ModelEntity modelEntity, Map<String, ? extends Object> fields) {
        GenericPK newPK = new GenericPK();
        newPK.init(delegator, modelEntity, fields);
        return newPK;
    }

    /** Creates new GenericPK from existing Map */
    public static GenericPK create(Delegator delegator, ModelEntity modelEntity, Object singlePkValue) {
        GenericPK newPK = new GenericPK();
        newPK.init(delegator, modelEntity, singlePkValue);
        return newPK;
    }

    /** Creates new GenericPK from existing GenericPK */
    public static GenericPK create(GenericPK value) {
        GenericPK newPK = new GenericPK();
        newPK.init(value);
        return newPK;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof GenericPK) {
            return super.equals(obj);
        }
        return false;
    }

    /** Clones this GenericPK, this is a shallow clone & uses the default shallow HashMap clone
     *@return Object that is a clone of this GenericPK
     */
    @Override
    public Object clone() {
        return GenericPK.create(this);
    }
}

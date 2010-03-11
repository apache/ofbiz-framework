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
package org.ofbiz.entity;

import java.util.Map;

import javolution.context.ObjectFactory;

import org.ofbiz.entity.model.ModelEntity;

/**
 * Generic Entity Primary Key Object
 *
 */
public class GenericPK extends GenericEntity {

    protected static final ObjectFactory<GenericPK> genericPKFactory = new ObjectFactory<GenericPK>() {
        @Override
        protected GenericPK create() {
            return new GenericPK();
        }
    };

    protected GenericPK() { }

    /** Creates new GenericPK */
    public static GenericPK create(ModelEntity modelEntity) {
        GenericPK newPK = genericPKFactory.object();
        newPK.init(modelEntity);
        return newPK;
    }

    /** Creates new GenericPK from existing Map */
    public static GenericPK create(Delegator delegator, ModelEntity modelEntity, Map<String, ? extends Object> fields) {
        GenericPK newPK = genericPKFactory.object();
        newPK.init(delegator, modelEntity, fields);
        return newPK;
    }

    /** Creates new GenericPK from existing Map */
    public static GenericPK create(Delegator delegator, ModelEntity modelEntity, Object singlePkValue) {
        GenericPK newPK = genericPKFactory.object();
        newPK.init(delegator, modelEntity, singlePkValue);
        return newPK;
    }

    /** Creates new GenericPK from existing GenericPK */
    public static GenericPK create(GenericPK value) {
        GenericPK newPK = genericPKFactory.object();
        newPK.init(value);
        return newPK;
    }

    /** Clones this GenericPK, this is a shallow clone & uses the default shallow HashMap clone
     *@return Object that is a clone of this GenericPK
     */
    @Override
    public Object clone() {
        return GenericPK.create(this);
    }
}

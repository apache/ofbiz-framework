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

/**
 * Contains a number of variables used to select certain advanced options for storing GenericEntities.
 */
@SuppressWarnings("serial")
public class EntityStoreOptions implements java.io.Serializable {

    /** Option for creating missing referenced values as dummy (pk-only) entries */
    private boolean createDummyFks = false;

    /**
     * Default constructor. Defaults are as follows: createDummyFks = false
     */
    public EntityStoreOptions() {
    }

    /**
     * Optional constructor with options to specify.
     * @param createDummyFks
     */
    public EntityStoreOptions(boolean createDummyFks) {
        this.createDummyFks = createDummyFks;
    }

    /**
     * If true, missing entries in FK referenced entities will be created while storing the given GenericValues.
     * @return boolean
     */
    public boolean isCreateDummyFks() {
        return createDummyFks;
    }

    /**
     * If true, missing entries in FK referenced entities will be created while storing the given GenericValues.
     * @param createDummyFks
     */
    public void setCreateDummyFks(boolean createDummyFks) {
        this.createDummyFks = createDummyFks;
    }
}

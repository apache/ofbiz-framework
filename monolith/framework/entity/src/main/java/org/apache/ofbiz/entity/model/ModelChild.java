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
package org.apache.ofbiz.entity.model;

import java.io.Serializable;

/**
 * Abstract entity model class.
 *
 */
@SuppressWarnings("serial")
public abstract class ModelChild implements Serializable {

    private final ModelEntity modelEntity;
    /** The description for documentation purposes */
    private final String description;

    // TODO: Eliminate the need for this.
    protected ModelChild() {
        this.modelEntity = null;
        this.description = "";
    }

    protected ModelChild(ModelEntity modelEntity, String description) {
        this.modelEntity = modelEntity;
        this.description = description;
    }

    /**
     * Gets model entity.
     * @return the model entity
     */
    public ModelEntity getModelEntity() {
        return this.modelEntity;
    }

    /** The description for documentation purposes */
    public String getDescription() {
        return this.description;
    }
}

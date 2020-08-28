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
package org.apache.ofbiz.service;

import java.sql.Timestamp;

import org.apache.ofbiz.base.util.UtilDateTime;

public class RunningService {

    private ModelService model;
    private String name;
    private int mode;

    private Timestamp startStamp;
    private Timestamp endStamp;

    private RunningService() {
        this.startStamp = UtilDateTime.nowTimestamp();
        this.endStamp = null;
    }

    public RunningService(String localName, ModelService model, int mode) {
        this();
        this.name = localName;
        this.model = model;
        this.mode = mode;
    }

    /**
     * Gets model service.
     * @return the model service
     */
    public ModelService getModelService() {
        return this.model;
    }

    /**
     * Gets local name.
     * @return the local name
     */
    public String getLocalName() {
        return this.name;
    }

    /**
     * Gets mode.
     * @return the mode
     */
    public int getMode() {
        return mode;
    }

    /**
     * Gets start stamp.
     * @return the start stamp
     */
    public Timestamp getStartStamp() {
        return (Timestamp) this.startStamp.clone();
    }

    /**
     * Gets end stamp.
     * @return the end stamp
     */
    public Timestamp getEndStamp() {
        return (Timestamp) this.endStamp.clone();
    }

    /**
     * Sets end stamp.
     */
    public void setEndStamp() {
        this.endStamp = UtilDateTime.nowTimestamp();
    }

    @Override
    public boolean equals(Object o) {
        if (o != null && o instanceof RunningService) {
            RunningService x = (RunningService) o;
            if (this.model.equals(x.getModelService()) && this.mode == x.getMode() && this.startStamp.equals(x.getStartStamp())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public int hashCode() {
        return System.identityHashCode(this);
    }
}

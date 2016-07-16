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

/**
 * ModelServiceIface
 */
public class ModelServiceIface {

    protected String service;
    protected boolean optional;

    public ModelServiceIface(String service, boolean optional) {
        this.service = service;
        this.optional = optional;
    }

    public String getService() {
        return this.service;
    }

    public void setService(String service) {
        this.service = service;
    }

    public boolean isOptional() {
        return this.optional;
    }

    public void isOptional(boolean optional) {
        this.optional = optional;
    }

    @Override
    public String toString() {
        return "[" + service + ":" + optional + "]";
    }
}

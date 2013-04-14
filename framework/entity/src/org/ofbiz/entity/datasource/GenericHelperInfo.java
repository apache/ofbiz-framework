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

package org.ofbiz.entity.datasource;

import org.ofbiz.base.util.UtilValidate;




/**
 * Generic Entity Helper Info Class
 *
 */
public class GenericHelperInfo {
    protected String entityGroupName;
    protected String helperBaseName;
    protected String tenantId = null;
    protected String overrideJdbcUri = null;
    protected String overrideUsername = null;
    protected String overridePassword = null;
    
    public GenericHelperInfo(String entityGroupName, String helperBaseName) {
        this.entityGroupName = entityGroupName;
        this.helperBaseName = helperBaseName;
    }

    public String getHelperFullName() {
        if (UtilValidate.isNotEmpty(tenantId)) {
            return helperBaseName.concat("#").concat(tenantId);
        } else {
            return helperBaseName;
        }
    }

    public String getEntityGroupName() {
        return entityGroupName;
    }

    public String getHelperBaseName() {
        return helperBaseName;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getOverrideJdbcUri() {
        return overrideJdbcUri;
    }

    public void setOverrideJdbcUri(String overrideJdbcUri) {
        this.overrideJdbcUri = overrideJdbcUri;
    }

    public String getOverrideUsername() {
        return overrideUsername;
    }

    public void setOverrideUsername(String overrideUsername) {
        this.overrideUsername = overrideUsername;
    }

    public String getOverridePassword() {
        return overridePassword;
    }

    public void setOverridePassword(String overridePassword) {
        this.overridePassword = overridePassword;
    }
}

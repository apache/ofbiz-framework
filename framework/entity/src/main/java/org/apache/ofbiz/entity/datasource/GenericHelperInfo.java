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

package org.apache.ofbiz.entity.datasource;

/**
 * A container for datasource connection information.
 * <p><b>Note that this class is not synchronized.</b>
 * If multiple threads access a <code>GenericHelperInfo</code> concurrently it must be synchronized externally.
 * </p> 
 *
 */
public final class GenericHelperInfo {
    private final String entityGroupName;
    private final String helperBaseName;
    private String tenantId = "";
    private String overrideJdbcUri = "";
    private String overrideUsername = "";
    private String overridePassword = "";
    private String helperFullName = "";

    public GenericHelperInfo(String entityGroupName, String helperBaseName) {
        this.entityGroupName = entityGroupName == null ? "" : entityGroupName;
        this.helperBaseName = helperBaseName == null ? "" : helperBaseName;
        this.helperFullName = this.helperBaseName;
    }

    public String getHelperFullName() {
        return helperFullName;
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
        if (tenantId != null) {
            this.tenantId = tenantId;
            helperFullName = helperBaseName.concat("#").concat(tenantId);
        }
    }

    public String getOverrideJdbcUri() {
        return overrideJdbcUri;
    }

    public String getOverrideJdbcUri(String defaultValue) {
        return overrideJdbcUri.isEmpty() ? defaultValue : overrideJdbcUri;
    }

    public void setOverrideJdbcUri(String overrideJdbcUri) {
        if (overrideJdbcUri != null) {
            this.overrideJdbcUri = overrideJdbcUri;
        }
    }

    public String getOverrideUsername() {
        return overrideUsername;
    }

    public String getOverrideUsername(String defaultValue) {
        return overrideUsername.isEmpty() ? defaultValue : overrideUsername;
    }

    public void setOverrideUsername(String overrideUsername) {
        if (overrideUsername != null) {
            this.overrideUsername = overrideUsername;
        }
    }

    public String getOverridePassword() {
        return overridePassword;
    }

    public String getOverridePassword(String defaultValue) {
        return overridePassword.isEmpty() ? defaultValue : overridePassword;
    }

    public void setOverridePassword(String overridePassword) {
        if (overridePassword != null) {
            this.overridePassword = overridePassword;
        }
    }
}

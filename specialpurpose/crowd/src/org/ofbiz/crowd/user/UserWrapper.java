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

package org.ofbiz.crowd.user;

import com.atlassian.crowd.integration.soap.SOAPPrincipal;

/**
 * UserWrapper
 */
public class UserWrapper {

    protected UserAttributeMapper attMapper;
    protected UserGroupMapper grpMapper;
    protected SOAPPrincipal user;

    public UserWrapper(SOAPPrincipal user, String[] groups) {
        this.user = user;
        this.attMapper = new UserAttributeMapper(user.getAttributes());
        this.grpMapper = new UserGroupMapper(groups);
    }

    public SOAPPrincipal getSOAPPrincipal() {
        return user;
    }

    public UserAttributeMapper getUserAttributeMapper() {
        return attMapper;
    }

    public UserGroupMapper getUserGroupMapper() {
        return grpMapper;
    }

    public String getName() {
        return user.getName();
    }

    public String getDescription() {
        return user.getDescription();
    }
}

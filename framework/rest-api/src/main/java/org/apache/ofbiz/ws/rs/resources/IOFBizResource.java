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
package org.apache.ofbiz.ws.rs.resources;

import javax.ws.rs.ext.Provider;

import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.webapp.WebAppUtil;
import org.apache.ofbiz.ws.rs.listener.ApiContextListener;

/**
 * Resource Interface
 */
@Provider
public interface IOFBizResource {

    default Delegator getDelegator() {
        Delegator delegator = WebAppUtil.getDelegator(ApiContextListener.getApplicationCntx());
        return delegator;
    }

    default LocalDispatcher getDispatcher() {
        LocalDispatcher dispatcher = WebAppUtil.getDispatcher(ApiContextListener.getApplicationCntx());
        return dispatcher;
    }

}

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
package org.ofbiz.webapp.control;

import java.io.IOException;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.DelegatorFactory;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.util.EntityUtil;


/**
 * TenantServlet.java - Tenant servlet for the web application.
 */
@SuppressWarnings("serial")
public class TenantServlet extends HttpServlet {

    public static String module = TenantServlet.class.getName();
    
    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        // get default delegator
        Delegator delegator = DelegatorFactory.getDelegator("default");
        try {
            // if a domain name was specified for tenant, redirect to initial path
            List<GenericValue> tenants = delegator.findList("Tenant", EntityCondition.makeCondition("domainName", request.getServerName()), null, UtilMisc.toList("-createdStamp"), null, false);
            if (UtilValidate.isNotEmpty(tenants)) {
                GenericValue tenant = EntityUtil.getFirst(tenants);
                String initialPath = tenant.getString("initialPath");
                response.sendRedirect(initialPath);
            }
        } catch (GenericEntityException e) {
            String errMsg = "Error getting tenant by domain name: " + request.getServerName();
            Debug.logError(e, errMsg, module);
            throw new ServletException(errMsg, e);
        }
    }
}

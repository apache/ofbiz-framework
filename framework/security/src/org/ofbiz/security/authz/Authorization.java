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
package org.ofbiz.security.authz;

import java.util.Map;

import javax.servlet.http.HttpSession;

import org.ofbiz.entity.GenericDelegator;

public interface Authorization {

	/**
	 * Test to see if the specified user has permission
	 * 
	 * @param userId the user's userId
	 * @param permission the raw permission string
	 * @param context name/value pairs used for permission lookup
	 * @param expanded true if the permission string is already expanded, false if it will contain ${} context values
	 * @return true if the user has permission
	 */
	public boolean hasPermission(String userId, String permission, Map<String, ? extends Object> context, boolean expanded);
	
	/**
     * Test to see if the specified user has permission
     * 
     * @param session HttpSession used to obtain the userId
     * @param permission the raw permission string
     * @param context name/value pairs used for permission lookup
     * @param expanded true if the permission string is already expanded, false if it will contain ${} context values
     * @return true if the user has permission
     */
    public boolean hasPermission(HttpSession session, String permission, Map<String, ? extends Object> context, boolean expanded);
	
    /**
     * Method for injecting the delegator object
     * 
     * @param delegator the GenericDelegator object to use for the Authorization implementation
     */
    public void setDelegator(GenericDelegator delegator);
}

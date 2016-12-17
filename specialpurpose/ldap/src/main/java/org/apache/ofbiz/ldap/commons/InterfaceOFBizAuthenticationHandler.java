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

package org.apache.ofbiz.ldap.commons;

import javax.naming.NamingException;
import javax.naming.directory.SearchResult;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.w3c.dom.Element;

/**
 * The OFBiz LDAP Authentication Handler interface.<p>
 *
 */
public interface InterfaceOFBizAuthenticationHandler {

    /**
     * Login a user.
     *
     * @param request HttpServletRequest
     * @param response HttpServletResponse
     * @param rootElement Element root element of ldap config file
     *
     * @return success if the user can login successfully; otherwise, error.
     * @throws Exception
     */
    String login(HttpServletRequest request, HttpServletResponse response, Element rootElement) throws Exception;

    /**
     * Get the security group of a user.
     *
     * @param rootElement Element root element of ldap config file
     * @param result SearchResult
     *
     * @return the SecurityGroup object.
     */
    Object getSecurityGroup(Element rootElement, SearchResult result);

    /**
     * Get the party id of a user.
     *
     * @param rootElement Element root element of ldap config file
     * @param result SearchResult
     *
     * @return the PartyId object.
     */
    Object getPartyId(Element rootElement, SearchResult result);

    /**
     * Logout a user.
     *
     * @param request HttpServletRequest
     * @param response HttpServletResponse
     * @param rootElement Element root element of ldap config file
     *
     * @return success if the user can login successfully; otherwise, error.
     */
    String logout(HttpServletRequest request, HttpServletResponse response, Element rootElement);

    /**
     * Get LDAP search result from a username, password and configuration.
     *
     * @param username String
     * @param password String
     * @param rootElement Element root element of ldap config file
     * @param bindRequired boolean if true, bind; false, just search the user in LDAP
     *
     * @return result SearchResult if ldap search successfully; otherwise, null.
     * @throws NamingException
     */
    SearchResult getLdapSearchResult(String username, String password, Element rootElement, boolean bindRequired) throws NamingException;

    /**
     * An HTTP WebEvent handler that checks to see is a userLogin is logged out in ldap.
     * If yes, the user is forwarded to the login page.
     * Currently, this function is only used in CAS authentication handler.
     *
     * @param request The HTTP request object for the current JSP or Servlet request.
     * @param response The HTTP response object for the current JSP or Servlet request.
     * @param rootElement Element root element of ldap config file
     * @return true if the user has logged out from ldap; otherwise, false.
     */
    boolean hasLdapLoggedOut(HttpServletRequest request, HttpServletResponse response, Element rootElement);
}

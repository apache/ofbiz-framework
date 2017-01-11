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

package org.apache.ofbiz.ldap.activedirectory;

import java.util.Hashtable;

import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;

import org.jasig.cas.util.LdapUtils;
import org.apache.ofbiz.base.util.UtilXml;
import org.apache.ofbiz.ldap.commons.AbstractOFBizAuthenticationHandler;
import org.w3c.dom.Element;


/**
 * The OFBiz ActiveDirectory Authentication Handler.<p>
 *
 * The ACL of a user is still controlled by OFBiz.
 *
 */
public final class OFBizActiveDirectoryAuthenticationHandler extends AbstractOFBizAuthenticationHandler {

    /**
     * Public constructor, initializes some required member variables.<p>
     */
    public OFBizActiveDirectoryAuthenticationHandler() {

    }

    @Override
    public SearchResult getLdapSearchResult(String username, String password,
            Element rootElement, boolean bindRequired) throws NamingException {
        DirContext ctx = null;
        SearchResult result = null;
        String ldapURL = UtilXml.childElementValue(rootElement, "URL", "ldap://localhost:389");
        String authenType = UtilXml.childElementValue(rootElement, "AuthenType", "simple");
        String searchType = UtilXml.childElementValue(rootElement, "SearchType", "");
        String baseDN = UtilXml.childElementValue(rootElement, "BaseDN");
        Hashtable<String, String> env = new Hashtable<String, String>();
        env.put(Context.INITIAL_CONTEXT_FACTORY,"com.sun.jndi.ldap.LdapCtxFactory");
        env.put(Context.PROVIDER_URL, ldapURL);
        if (searchType == null || searchType.trim().equals("")) {
            env.put(Context.SECURITY_AUTHENTICATION, "none");
        } else if (searchType.trim().equals("login")) {
            env.put(Context.SECURITY_AUTHENTICATION, authenType);
            // specify the username for search
            String userDNForSearch = UtilXml.childElementValue(rootElement, "UserDNForSearch");
            env.put(Context.SECURITY_PRINCIPAL, userDNForSearch);
            // specify the password for search
            String passwordForSearch = UtilXml.childElementValue(rootElement, "PasswordForSearch");
            env.put(Context.SECURITY_CREDENTIALS, passwordForSearch);
        }
        try {
            ctx = new InitialDirContext(env);
            SearchControls controls = new SearchControls();
            // ldap search timeout
            controls.setTimeLimit(1000);
            // ldap search count
            controls.setCountLimit(2);
            // ldap search scope
            String sub = UtilXml.childElementValue(rootElement, "Scope", "sub").toLowerCase().trim();
            if (sub.equals("sub")) {
                controls.setSearchScope(SearchControls.SUBTREE_SCOPE);
            } else if (sub.equals("one")) {
                controls.setSearchScope(SearchControls.ONELEVEL_SCOPE);
            } else {
                controls.setSearchScope(SearchControls.OBJECT_SCOPE);
            }
            String filter = UtilXml.childElementValue(rootElement, "Filter", "(objectclass=*)");
            String attribute = UtilXml.childElementValue(rootElement, "Attribute", "uid=%u");
            attribute = LdapUtils.getFilterWithValues(attribute, username);
            NamingEnumeration<SearchResult> answer = ctx.search(baseDN,
                    // Filter expression
                    "(&(" + filter + ") (" + attribute +"))",
                    controls);
            if (answer.hasMoreElements()) {
                result = answer.next();
                if (bindRequired) {
                    env.put(Context.SECURITY_AUTHENTICATION, authenType);
                    // specify the username
                    String userDN = result.getName() + "," + baseDN;
                    env.put(Context.SECURITY_PRINCIPAL, userDN);
                    // specify the password
                    env.put(Context.SECURITY_CREDENTIALS, password);
                    ctx = new InitialDirContext(env);
                }
            }
        } catch (NamingException e) {
            // No ldap service found, or cannot login.
            throw new NamingException(e.getLocalizedMessage());
        }

        return result;
    }

}

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

package org.apache.ofbiz.common.login;

import java.util.Map;
import java.util.Properties;

import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.transaction.Transaction;

import org.apache.ofbiz.base.crypto.HashCrypt;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilProperties;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.transaction.GenericTransactionException;
import org.apache.ofbiz.entity.transaction.TransactionUtil;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.entity.util.EntityUtilProperties;
import org.apache.ofbiz.service.DispatchContext;

/** LDAP Authentication Services.
 */
public class LdapAuthenticationServices {

    public static final String module = LdapAuthenticationServices.class.getName();

    public static boolean userLogin(DispatchContext ctx, Map<String, ?> context) {
        Debug.logVerbose("Starting LDAP authentication", module);
        Properties env = UtilProperties.getProperties("jndiLdap");
        String username = (String) context.get("login.username");
        if (username == null) {
            username = (String) context.get("username");
        }
        String password = (String) context.get("login.password");
        if (password == null) {
            password = (String) context.get("password");
        }
        String dn = null;
        Delegator delegator = ctx.getDelegator();
        boolean isServiceAuth = context.get("isServiceAuth") != null && ((Boolean) context.get("isServiceAuth")).booleanValue();
        GenericValue userLogin = null;
        try {
            userLogin = EntityQuery.use(delegator).from("UserLogin").where("userLoginId", username).cache(isServiceAuth).queryOne();
        } catch (GenericEntityException e) {
            Debug.logWarning(e, "", module);
        }
        if (userLogin != null) {
            dn = userLogin.getString("userLdapDn");
        }
        if (UtilValidate.isEmpty(dn)) {
            String dnTemplate = (String) env.get("ldap.dn.template");
            if (dnTemplate != null) {
                dn = dnTemplate.replace("%u", username);
            }
            Debug.logVerbose("Using DN template: " + dn, module);
        } else {
            Debug.logVerbose("Using UserLogin.userLdapDn: " + dn, module);
        }
        env.put(Context.SECURITY_PRINCIPAL, dn);
        env.put(Context.SECURITY_CREDENTIALS, password);
        try {
            // Create initial context
            DirContext ldapCtx = new InitialDirContext(env);
            ldapCtx.close();
        } catch (NamingException e) {
            Debug.logVerbose("LDAP authentication failed: " + e.getMessage(), module);
            return false;
        }
        Debug.logVerbose("LDAP authentication succeeded", module);
        if (!"true".equals(env.get("ldap.synchronize.passwords"))) {
            return true;
        }
        // Synchronize user's OFBiz password with user's LDAP password
        if (userLogin != null) {
            boolean useEncryption = "true".equals(EntityUtilProperties.getPropertyValue("security", "password.encrypt", delegator));
            String currentPassword = userLogin.getString("currentPassword");
            boolean samePassword;
            if (useEncryption) {
                samePassword = HashCrypt.comparePassword(currentPassword, LoginServices.getHashType(), password);
            } else {
                samePassword = currentPassword.equals(password);
            }
            if (!samePassword) {
                Debug.logVerbose("Starting password synchronization", module);
                userLogin.set("currentPassword", useEncryption ? HashCrypt.cryptUTF8(LoginServices.getHashType(), null, password) : password, false);
                Transaction parentTx = null;
                boolean beganTransaction = false;
                try {
                    try {
                        parentTx = TransactionUtil.suspend();
                    } catch (GenericTransactionException e) {
                        Debug.logError(e, "Could not suspend transaction: " + e.getMessage(), module);
                    }
                    try {
                        beganTransaction = TransactionUtil.begin();
                        userLogin.store();
                    } catch (GenericEntityException e) {
                        Debug.logError(e, "Error saving UserLogin", module);
                        try {
                            TransactionUtil.rollback(beganTransaction, "Error saving UserLogin", e);
                        } catch (GenericTransactionException e2) {
                            Debug.logError(e2, "Could not rollback nested transaction: " + e2.getMessage(), module);
                        }
                    } finally {
                        try {
                            TransactionUtil.commit(beganTransaction);
                            Debug.logVerbose("Password synchronized", module);
                        } catch (GenericTransactionException e) {
                            Debug.logError(e, "Could not commit nested transaction: " + e.getMessage(), module);
                        }
                    }
                } finally {
                    if (parentTx != null) {
                        try {
                            TransactionUtil.resume(parentTx);
                            Debug.logVerbose("Resumed the parent transaction.", module);
                        } catch (GenericTransactionException e) {
                            Debug.logError(e, "Could not resume parent nested transaction: " + e.getMessage(), module);
                        }
                    }
                }
            }
        }
        return true;
    }
}

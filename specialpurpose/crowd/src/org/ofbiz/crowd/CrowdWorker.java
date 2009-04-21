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

package org.ofbiz.crowd;

import java.rmi.RemoteException;
import javax.xml.rpc.ServiceException;

import org.ofbiz.crowd.security.SecurityServerHttpBindingStub;
import org.ofbiz.crowd.security.SecurityServerLocator;
import org.ofbiz.crowd.user.UserWrapper;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilProperties;
import com.atlassian.crowd.integration.authentication.AuthenticatedToken;
import com.atlassian.crowd.integration.authentication.ApplicationAuthenticationContext;
import com.atlassian.crowd.integration.authentication.PasswordCredential;
import com.atlassian.crowd.integration.authentication.ValidationFactor;
import com.atlassian.crowd.integration.soap.SOAPPrincipal;
import com.atlassian.crowd.integration.exception.InvalidAuthorizationTokenException;
import com.atlassian.crowd.integration.exception.ObjectNotFoundException;
import com.atlassian.crowd.integration.exception.InvalidAuthenticationException;
import com.atlassian.crowd.integration.exception.InactiveAccountException;
import com.atlassian.crowd.integration.exception.ApplicationAccessDeniedException;

/**
 * CrowdWorker
 */
public abstract class CrowdWorker {

    private static final String module = CrowdWorker.class.getName();

    protected String callAuthenticate(String user, String password) throws RemoteException {
        SecurityServerHttpBindingStub stub = getStub();
        AuthenticatedToken token = getToken(stub);

        // auth the user
        String userToken;
        try {
            userToken = stub.authenticatePrincipalSimple(token, user, password);
        } catch (InvalidAuthenticationException e) {
            return null;
        } catch (InvalidAuthorizationTokenException e) {
            Debug.logError(e, module);
            throw e;
        } catch (ApplicationAccessDeniedException e) {
            Debug.logError(e, module);
            throw e;
        } catch (InactiveAccountException e) {
            return null;
        } catch (RemoteException e) {
            Debug.logError(e, module);
            throw e;
        }

        return userToken;
    }

    protected UserWrapper callGetUser(String user) throws RemoteException {
        SecurityServerHttpBindingStub stub = getStub();
        AuthenticatedToken token = getToken(stub);

        SOAPPrincipal principal;
        try {
            principal = stub.findPrincipalByName(token, user);
        } catch (InvalidAuthorizationTokenException e) {
            Debug.logError(e, module);
            throw e;
        } catch (ObjectNotFoundException e) {
            Debug.logError(e, module);
            throw e;
        } catch (RemoteException e) {
            Debug.logError(e, module);
            throw e;
        }

        String[] groups;
        try {
            groups = stub.findGroupMemberships(token, user);
        } catch (InvalidAuthorizationTokenException e) {
            Debug.logError(e, module);
            throw e;
        } catch (ObjectNotFoundException e) {
            groups = new String[0];
        } catch (RemoteException e) {
            Debug.logError(e, module);
            throw e;
        }

        return new UserWrapper(principal, groups);
    }

    protected void callUpdatePassword(String user, String password) throws RemoteException {
        PasswordCredential credential = new PasswordCredential();
        credential.setCredential(password);
        credential.setEncryptedCredential(false);

        SecurityServerHttpBindingStub stub = getStub();
        AuthenticatedToken token = getToken(stub);

        try {
            stub.updatePrincipalCredential(token, user, credential);
        } catch (RemoteException e) {
            Debug.logError(e, module);
            throw e;
        }
    }

    private AuthenticatedToken getToken(SecurityServerHttpBindingStub stub) {
        String appName = UtilProperties.getPropertyValue("crowd.properties", "crowd.application.name");
        String appPass = UtilProperties.getPropertyValue("crowd.properties", "crowd.application.pass");

        // authenticate the integrated crowd application
        if (stub == null) {
            stub = getStub();
        }
        if (stub != null) {
            try {
                return stub.authenticateApplication(new ApplicationAuthenticationContext(
                        new PasswordCredential(appPass, Boolean.FALSE), appName, new ValidationFactor[0]));
            } catch (RemoteException e) {
                Debug.logError(e, module);
                return null;
            }
        } else {
            return null;
        }
    }

    private SecurityServerHttpBindingStub getStub() {
        try {
            SecurityServerLocator secServer = new SecurityServerLocator();
            secServer.setSecurityServerHttpPortEndpointAddress(secServer.getSecurityServerHttpPortAddress());
            return (SecurityServerHttpBindingStub) secServer.getSecurityServerHttpPort();
        } catch (ServiceException e) {
            Debug.logError(e, module);
            return null;
        }
    }
}

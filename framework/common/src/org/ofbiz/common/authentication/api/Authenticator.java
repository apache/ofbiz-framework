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

package org.ofbiz.common.authentication.api;

import org.ofbiz.service.LocalDispatcher;

/**
 * Authenticator
 *
 * Classes which implement this interface (directly) will be auto-discovered and loaded as Authenticators
 * as long as isEnabled() returns true. All implementations MUST be in the org.ofbiz top level package in
 * order to be discovered.
 */
public interface Authenticator {

    /**
     * Method called when authenticator is first initialized (the delegator
     * object can be obtained from the LocalDispatcher)
     * @param dispatcher The LocalDispatcher to use for this Authenticator
     */
    public void initialize(LocalDispatcher dispatcher);

    /**
     * Method to authenticate a user
     * @param username User's username
     * @param password User's password
     * @param isServiceAuth true if authentication is for a service call
     * @return true if the user is authenticated
     * @throws AuthenticatorException when a fatal error occurs during authentication
     */
    public boolean authenticate(String username, String password, boolean isServiceAuth) throws AuthenticatorException;

    /**
     * Logs a user out
     * @param username User's username
     * @throws AuthenticatorException when logout fails
     */
    public void logout(String username) throws AuthenticatorException;

    /**
     * Reads user information and syncs it to OFBiz (i.e. UserLogin, Person, etc)
     * Note: when creating a UserLogin object, be sure to set 'externalAuthId'
     * @param username User's username
     * @throws AuthenticatorException user synchronization fails
     */
    public void syncUser(String username) throws AuthenticatorException;

    /**
     * Updates a user's password
     * @param username User's username
     * @param password User's current password
     * @param newPassword User's new password
     * @throws AuthenticatorException when update password fails
     */
    public void updatePassword(String username, String password, String newPassword) throws AuthenticatorException;

    /**
     * Weight of this authenticator (lower weights are run first)
     * @return the weight of this Authenticator
     */
    public float getWeight();

    /**
     * Is the user synchronized back to OFBiz
     * @return true if the user record is copied to the OFB database
     */
    public boolean isUserSynchronized();

    /**
     * Is this expected to be the only authenticator, if so errors will be thrown when users cannot be found
     * @return true if this is expected to be the only Authenticator
     */
    public boolean isSingleAuthenticator();

    /**
     * Flag to test if this Authenticator is enabled
     * @return true if the Authenticator is enabled
     */
    public boolean isEnabled();
}

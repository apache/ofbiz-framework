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
package org.apache.ofbiz.catalina.container;

import org.apache.catalina.CredentialHandler;
import org.apache.ofbiz.common.login.LoginServices;


public class SimpleCredentialHandler implements CredentialHandler {
    @Override
    public boolean matches(String inputCredentials, String storedCredentials) {
        return LoginServices.checkPassword(storedCredentials, false, inputCredentials);
    }

    @Override
    public String mutate(String inputCredentials) {
        // when password.encrypt=false, password is stored as clear text in the database.
        // no need to encrypt this input password.
        return null;
    }
}

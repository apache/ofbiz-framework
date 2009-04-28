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

package org.ofbiz.common.authentication;

import java.util.ArrayList;
import java.util.List;

import org.ofbiz.base.util.AbstractResolver;
import org.ofbiz.common.authentication.api.Authenticator;

/**
 * AuthInterfaceResolver
 *
 * Discovers implementations of Authenticator on the class path (implementations must be in org.ofbiz.* package)
 */
public class AuthInterfaceResolver extends AbstractResolver {

    @SuppressWarnings("unchecked")
    protected List<Class> authenticators = new ArrayList<Class>();
    

    public AuthInterfaceResolver() {
        super();
    }

    @SuppressWarnings("unchecked")
    public List<Class> getImplementations() {
        find("org.ofbiz");
        return authenticators;
    }

    @SuppressWarnings("unchecked")
    public void resolveClass(Class clazz) {
        Class[] ifaces = clazz.getInterfaces();
        for (Class iface : ifaces) {
            if (Authenticator.class.equals(iface)) {
                authenticators.add(clazz);
            }
        }
    }
}


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
package org.apache.ofbiz.ws.rs.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.ws.rs.NameBinding;


/**
 * JAX-RS name binding annotation that binds {@link org.apache.ofbiz.ws.rs.security.auth.HttpBasicAuthFilter}
 * to the resource classes or methods it annotates.
 *
 * <p>When applied to a JAX-RS resource class or method, the JAX-RS runtime
 * will execute {@link org.apache.ofbiz.ws.rs.security.auth.HttpBasicAuthFilter} before the request is processed,
 * validating the HTTP Basic Auth credentials in the {@code Authorization}
 * header against OFBiz user logins.</p>
 *
 * <p>This annotation is used on the token issuance endpoint
 * ({@code POST /auth/token}), which requires Basic Auth to authenticate
 * the user before issuing a JWT access token.</p>
 *
 * @see org.apache.ofbiz.ws.rs.security.auth.HttpBasicAuthFilter
 */
@NameBinding
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface AuthToken {

}

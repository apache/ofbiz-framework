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

package org.apache.ofbiz.base.html;

import org.owasp.html.HtmlPolicyBuilder;
import org.owasp.html.PolicyFactory;

/**
 * This interface is used to build a custom sanitizer policy which then can be
 * used instead of the default permissive policy. The custom policy will the be
 * used in
 * {@link org.apache.ofbiz.base.util.UtilCodec.HtmlEncoder#sanitize(String, String)}
 */
public interface SanitizerCustomPolicy {

    PolicyFactory POLICY_DEFINITION = new HtmlPolicyBuilder().toFactory();

    /**
     * Used for getting the policy from the custom class which implements this
     * interface
     * @return the policy specified in the class will be returned
     */
    PolicyFactory getSanitizerPolicy();
}

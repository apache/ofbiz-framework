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
package org.apache.ofbiz.ws.rs.openapi;

import java.util.HashSet;
import java.util.Set;

import org.apache.ofbiz.base.util.UtilValidate;

import io.swagger.v3.jaxrs2.integration.JaxrsApplicationAndResourcePackagesAnnotationScanner;

public class OFBizResourceScanner extends JaxrsApplicationAndResourcePackagesAnnotationScanner {

    private static final Set<String> IGNORED = new HashSet<>();

    static {
        IGNORED.add("org.apache.ofbiz.ws.rs.resources.OFBizServiceResource");
    }

    public OFBizResourceScanner() {
        onlyConsiderResourcePackages = true;
    }

    /**
     */
    public Set<Class<?>> classes() {
        Set<Class<?>> classes = super.classes();
        Set<Class<?>> outputClasses = new HashSet<>();
        for (Class<?> clz : classes) {
            if (!isIgnored(clz.getName())) {
                outputClasses.add(clz);
            }
        }
        return outputClasses;
    }

    /**
     */
    protected boolean isIgnored(String classOrPackageName) {
        if (UtilValidate.isEmpty(classOrPackageName)) {
            return true;
        }
        boolean isIgnored = IGNORED.stream().anyMatch(classOrPackageName::startsWith);
        return isIgnored;
    }

}

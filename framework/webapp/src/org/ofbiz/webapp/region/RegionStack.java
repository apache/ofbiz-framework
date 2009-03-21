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
package org.ofbiz.webapp.region;

import java.util.Stack;
import javax.servlet.ServletRequest;

import static org.ofbiz.base.util.UtilGenerics.checkStack;

public class RegionStack {
    private RegionStack() {} // no instantiations

    public static Stack<Region> getStack(ServletRequest request) {
        Stack<Region> s = checkStack(request.getAttribute("region-stack"), Region.class);

        if (s == null) {
            s = new Stack<Region>();
            request.setAttribute("region-stack", s);
        }
        return s;
    }

    public static Region peek(ServletRequest request) {
        return getStack(request).peek();
    }

    public static void push(ServletRequest request, Region region) {
        getStack(request).push(region);
    }

    public static Region pop(ServletRequest request) {
        return getStack(request).pop();
    }
}

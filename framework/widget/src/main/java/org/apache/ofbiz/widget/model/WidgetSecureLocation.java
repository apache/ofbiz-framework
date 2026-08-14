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
package org.apache.ofbiz.widget.model;

import java.nio.file.Paths;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilValidate;

public final class WidgetSecureLocation {

    private static final String MODULE = WidgetSecureLocation.class.getName();
    private static final String COMPO_TYPE = "component://";

    public static String sanitize(String location) {
        if (UtilValidate.isEmpty(location)
                || UtilValidate.isUrlInStringAndDoesNotStartByComponentProtocol(location)
                || location.startsWith("file:/")) {
            Debug.logWarning(String.format("Unable to sanitize location: [%s]", location), MODULE);
            return null;
        }
        if (location.startsWith(COMPO_TYPE) && location.length() > 12) {
            if (location.indexOf("..") > 0) {
                Debug.logWarning(String.format("For security reason traversal sequence '..' is not allowed: [%s]", location), MODULE);
                return null;
            }
            return COMPO_TYPE + Paths.get(location.substring(12)).normalize();
        }

        return location.startsWith(COMPO_TYPE) ? location : null;
    }
}

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
package org.ofbiz.widget.portal;

import org.ofbiz.base.util.Debug;

/**
 * PortalPageWorker Class
 */
public final class WidgetPortalPageWorker {
    public static final String module = WidgetPortalPageWorker.class.getName();
    private static PortalPageWorkerInterface portalPageWorker = null;

    private WidgetPortalPageWorker() {}

    static {
        try {
            ClassLoader loader = Thread.currentThread().getContextClassLoader();
            // note: loadClass is necessary for these since this class doesn't know anything about them at compile time
            portalPageWorker = (PortalPageWorkerInterface) loader.loadClass("org.ofbiz.widget.portal.PortalPageWorker").newInstance();
        } catch (ClassNotFoundException e) {
            Debug.logError(e, "Could not pre-initialize dynamically loaded class: ", module);
        } catch (IllegalAccessException e) {
            Debug.logError(e, "Could not pre-initialize dynamically loaded class: ", module);
        } catch (InstantiationException e) {
            Debug.logError(e, "Could not pre-initialize dynamically loaded class: ", module);
        }
    }
}

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
package org.apache.ofbiz.widget.content;

import org.apache.ofbiz.base.util.Debug;

/**
 * WidgetContentWorker Class
 */
public final class WidgetDataResourceWorker {
    private static final String MODULE = WidgetDataResourceWorker.class.getName();
    private WidgetDataResourceWorker() { }
    private static DataResourceWorkerInterface dataresourceWorker = null;
    static {
        try {
            ClassLoader loader = Thread.currentThread().getContextClassLoader();
            // note: loadClass is necessary for these since this class doesn't know anything about them at compile time
            dataresourceWorker = (DataResourceWorkerInterface) loader.loadClass("org.apache.ofbiz.content.data.DataResourceWorker").getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException e) {
            Debug.logError(e, "Could not pre-initialize dynamically loaded class: ", MODULE);
        }
    }
    public static DataResourceWorkerInterface getDataresourceWorker() {
       return dataresourceWorker;
    }
}

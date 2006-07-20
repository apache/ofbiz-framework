/*
 * Copyright 2001-2006 The Apache Software Foundation
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package org.ofbiz.widget;

import org.ofbiz.base.util.Debug;

/**
 * WidgetContentWorker Class
 * 
 * @author <a href="mailto:jonesde@ofbiz.org">David E. Jones</a>
 */
public class WidgetContentWorker {
    public static final String module = WidgetContentWorker.class.getName();
    public static ContentWorkerInterface contentWorker = null;
    static {
        try {
            ClassLoader loader = Thread.currentThread().getContextClassLoader();
            // note: loadClass is necessary for these since this class doesn't know anything about them at compile time
            contentWorker = (ContentWorkerInterface) loader.loadClass("org.ofbiz.content.content.ContentWorker").newInstance();
        } catch (ClassNotFoundException e) {
            Debug.logError(e, "Could not pre-initialize dynamically loaded class: ", module);
        } catch (IllegalAccessException e) {
            Debug.logError(e, "Could not pre-initialize dynamically loaded class: ", module);
        } catch (InstantiationException e) {
            Debug.logError(e, "Could not pre-initialize dynamically loaded class: ", module);
        }
    }
}

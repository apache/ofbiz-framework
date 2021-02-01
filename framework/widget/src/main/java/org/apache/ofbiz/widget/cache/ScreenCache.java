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
package org.apache.ofbiz.widget.cache;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.cache.UtilCache;

public class ScreenCache extends AbstractCache {
    private static final String MODULE = ScreenCache.class.getName();

    public ScreenCache() {
        super("screen");
    }

    /**
     * Get generic widget output.
     * @param screenName the screen name
     * @param wcck the wcck
     * @return the generic widget output
     */
    public GenericWidgetOutput get(String screenName, WidgetContextCacheKey wcck) {
        UtilCache<WidgetContextCacheKey, GenericWidgetOutput> screenCache = getCache(screenName);
        if (screenCache == null) {
            return null;
        }
        return screenCache.get(wcck);
    }

    /**
     * Put generic widget output.
     * @param screenName the screen name
     * @param wcck the wcck
     * @param output the output
     * @return the generic widget output
     */
    public GenericWidgetOutput put(String screenName, WidgetContextCacheKey wcck, GenericWidgetOutput output) {
        UtilCache<WidgetContextCacheKey, GenericWidgetOutput> screenCache = getOrCreateCache(screenName);
        return screenCache.put(wcck, output);
    }

    /**
     * Remove generic widget output.
     * @param screenName the screen name
     * @param wcck the wcck
     * @return the generic widget output
     */
    public GenericWidgetOutput remove(String screenName, WidgetContextCacheKey wcck) {
        UtilCache<WidgetContextCacheKey, GenericWidgetOutput> screenCache = getCache(screenName);
        if (Debug.verboseOn()) {
            Debug.logVerbose("Removing from ScreenCache with key [" + wcck + "], will remove from this cache: "
                    + (screenCache == null ? "[No cache found to remove from]" : screenCache.getName()), MODULE);
        }
        if (screenCache == null) {
            return null;
        }
        GenericWidgetOutput retVal = screenCache.remove(wcck);
        if (Debug.verboseOn()) {
            Debug.logVerbose("Removing from ScreenCache with key [" + wcck + "], found this in the cache: " + retVal, MODULE);
        }
        return retVal;
    }
}

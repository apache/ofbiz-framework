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
package org.apache.ofbiz.base.util.collections;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.Set;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilProperties;
import org.apache.ofbiz.base.util.string.FlexibleStringExpander;
import org.apache.ofbiz.service.calendar.TemporalExpressions.Frequency;

/**
 * Generic ResourceBundle Map Wrapper, given ResourceBundle allows it to be used as a Map
 *
 */
@SuppressWarnings("serial")
public class ResourceBundleMapWrapper implements Map<String, Object>, Serializable {

    private static final String MODULE = Frequency.class.getName();
    private MapStack<String> rbmwStack;
    private ResourceBundle initialResourceBundle;
    private Map<String, Object> context;

    protected ResourceBundleMapWrapper() {
        rbmwStack = MapStack.create();
    }

    /**
     * When creating new from a InternalRbmWrapper the one passed to the constructor should be the most specific or local InternalRbmWrapper,
     * with more common ones pushed onto the stack progressively.
     */
    public ResourceBundleMapWrapper(InternalRbmWrapper initialInternalRbmWrapper) {
        this.initialResourceBundle = initialInternalRbmWrapper.getResourceBundle();
        this.rbmwStack = MapStack.create(initialInternalRbmWrapper);
    }

    /** When creating new from a ResourceBundle the one passed to the constructor should be the most specific or local ResourceBundle,
     * with more common ones pushed onto the stack progressively.
     */
    public ResourceBundleMapWrapper(ResourceBundle initialResourceBundle) {
        if (initialResourceBundle == null) {
            throw new IllegalArgumentException("Cannot create ResourceBundleMapWrapper with a null initial ResourceBundle.");
        }
        this.initialResourceBundle = initialResourceBundle;
        this.rbmwStack = MapStack.create(new InternalRbmWrapper(initialResourceBundle));
    }

    /** When creating new from a ResourceBundle the one passed to the constructor should be the most specific or local ResourceBundle,
     * with more common ones pushed onto the stack progressively.
     */
    public ResourceBundleMapWrapper(ResourceBundle initialResourceBundle, Map<String, Object> context) {
        if (initialResourceBundle == null) {
            throw new IllegalArgumentException("Cannot create ResourceBundleMapWrapper with a null initial ResourceBundle.");
        }
        this.initialResourceBundle = initialResourceBundle;
        this.rbmwStack = MapStack.create(new InternalRbmWrapper(initialResourceBundle));
        this.context = context;
    }

    /** Puts ResourceBundle on the BOTTOM of the stack (bottom meaning will be overriden by higher layers on the stack,
     * ie everything else already there) */
    public void addBottomResourceBundle(ResourceBundle topResourceBundle) {
        this.rbmwStack.addToBottom(new InternalRbmWrapper(topResourceBundle));
    }

    /** Puts InternalRbmWrapper on the BOTTOM of the stack (bottom meaning will be overriden by higher layers on the stack,
     * ie everything else already there) */
    public void addBottomResourceBundle(InternalRbmWrapper topInternalRbmWrapper) {
        this.rbmwStack.addToBottom(topInternalRbmWrapper);
    }

    /** Don't pass the locale to make sure it has the same locale as the base */
    public void addBottomResourceBundle(String resource) {
        if (this.initialResourceBundle == null) {
            throw new IllegalArgumentException("Cannot add bottom resource bundle, this wrapper was not properly initialized"
                    + "(there is no base/initial ResourceBundle).");
        }
        this.addBottomResourceBundle(new InternalRbmWrapper(UtilProperties.getResourceBundle(resource, this.initialResourceBundle.getLocale())));
    }

    /** In general we don't want to use this, better to start with the more specific ResourceBundle and add layers of common ones...
     * Puts ResourceBundle on the top of the stack (top meaning will override lower layers on the stack)
     */
    public void pushResourceBundle(ResourceBundle topResourceBundle) {
        this.rbmwStack.push(new InternalRbmWrapper(topResourceBundle));
    }

    /**
     * Gets initial resource bundle.
     * @return the initial resource bundle
     */
    public ResourceBundle getInitialResourceBundle() {
        return this.initialResourceBundle;
    }

    @Override
    public void clear() {
        this.rbmwStack.clear();
    }
    @Override
    public boolean containsKey(Object arg0) {
        return this.rbmwStack.containsKey(arg0);
    }
    @Override
    public boolean containsValue(Object arg0) {
        return this.rbmwStack.containsValue(arg0);
    }
    @Override
    public Set<Map.Entry<String, Object>> entrySet() {
        return this.rbmwStack.entrySet();
    }
    @Override
    public Object get(Object arg0) {
        Object value = this.rbmwStack.get(arg0);
        if (value == null) {
            value = arg0;
        } else if (context != null) {
            try {
                String str = (String) value;
                return FlexibleStringExpander.expandString(str, context);
            } catch (ClassCastException e) {
                Debug.logInfo(e.getMessage(), MODULE);
            }
        }
        return value;
    }
    @Override
    public boolean isEmpty() {
        return this.rbmwStack.isEmpty();
    }
    @Override
    public Set<String> keySet() {
        return this.rbmwStack.keySet();
    }
    @Override
    public Object put(String key, Object value) {
        return this.rbmwStack.put(key, value);
    }
    @Override
    public void putAll(Map<? extends String, ? extends Object> arg0) {
        this.rbmwStack.putAll(arg0);
    }
    @Override
    public Object remove(Object arg0) {
        return this.rbmwStack.remove(arg0);
    }
    @Override
    public int size() {
        return this.rbmwStack.size();
    }
    @Override
    public Collection<Object> values() {
        return this.rbmwStack.values();
    }

    public static class InternalRbmWrapper implements Map<String, Object>, Serializable {
        private ResourceBundle resourceBundle;
        private Map<String, Object> topLevelMap;
        private boolean isMapInitialized = false;

        public InternalRbmWrapper(ResourceBundle resourceBundle) {
            if (resourceBundle == null) {
                throw new IllegalArgumentException("Cannot create InternalRbmWrapper with a null ResourceBundle.");
            }
            this.resourceBundle = resourceBundle;
        }

        /**
         * Creates the topLevelMap only when it is required
         */
        private void createMapWhenNeeded() {
            if (isMapInitialized) {
                return;
            }
            // NOTE: this does NOT return all keys, ie keys from parent
            // ResourceBundles, so we keep the resourceBundle object to look at
            // when the main Map doesn't have a certain value
            if (resourceBundle != null) {
                Set<String> set = resourceBundle.keySet();
                topLevelMap = new HashMap<>(set.size());
                for (String key : set) {
                    Object value = resourceBundle.getObject(key);
                    topLevelMap.put(key, value);
                }
            } else {
                topLevelMap = new HashMap<>(1);
            }
            topLevelMap.put("_RESOURCE_BUNDLE_", resourceBundle);
            isMapInitialized = true;
        }

        @Override
        public int size() {
            if (isMapInitialized) {
                // this is an approximate size, won't include elements from parent bundles
                return topLevelMap.size() - 1;
            }
            return resourceBundle.keySet().size();
        }

        @Override
        public boolean isEmpty() {
            if (isMapInitialized) {
                return topLevelMap.isEmpty();
            }
            return resourceBundle.keySet().isEmpty();
        }

        @Override
        public boolean containsKey(Object arg0) {
            if (isMapInitialized) {
                if (topLevelMap.containsKey(arg0)) {
                    return true;
                }
            } else {
                try {
                    //the following will just be executed to check if arg0 is null,
                    //if so the thrown exception will be caught, if not true will be returned
                    this.resourceBundle.getObject((String) arg0);
                    return true;
                } catch (NullPointerException e) {
                    // happens when arg0 is null
                } catch (MissingResourceException e) {
                    // nope, not found... nothing, will automatically return
                    // false below
                }
            }
            return false;
        }

        @Override
        public boolean containsValue(Object arg0) {
            throw new RuntimeException("Not implemented for ResourceBundleMapWrapper");
        }

        @Override
        public Object get(Object arg0) {
            Object value = null;
            if (isMapInitialized) {
                value = this.topLevelMap.get(arg0);
            }

            if (resourceBundle != null) {
                if (value == null) {
                    try {
                        value = this.resourceBundle.getObject((String) arg0);
                    } catch (MissingResourceException mre) {
                        // do nothing, this will be handled by recognition that the value is still null
                        Debug.logError(mre, MODULE);
                    }
                }
            }
            return value;
        }

        @Override
        public Object put(String arg0, Object arg1) {
            throw new RuntimeException("Not implemented/allowed for ResourceBundleMapWrapper");
        }

        @Override
        public Object remove(Object arg0) {
            throw new RuntimeException("Not implemented for ResourceBundleMapWrapper");
        }

        @Override
        public void putAll(Map<? extends String, ? extends Object> arg0) {
            throw new RuntimeException("Not implemented for ResourceBundleMapWrapper");
        }

        @Override
        public void clear() {
            throw new RuntimeException("Not implemented for ResourceBundleMapWrapper");
        }

        @Override
        public Set<String> keySet() {
            createMapWhenNeeded();
            return this.topLevelMap.keySet();
        }

        @Override
        public Collection<Object> values() {
            createMapWhenNeeded();
            return this.topLevelMap.values();
        }

        @Override
        public Set<Map.Entry<String, Object>> entrySet() {
            createMapWhenNeeded();
            return this.topLevelMap.entrySet();
        }

        /**
         * Gets resource bundle.
         * @return the resource bundle
         */
        public ResourceBundle getResourceBundle() {
            return this.resourceBundle;
        }

    }
}

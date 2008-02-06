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
package org.ofbiz.base.util.collections;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import org.ofbiz.base.util.UtilProperties;

/** ResourceBundle MapStack class. Resource bundles are wrapped with a
 * <code>InternalRbmWrapper</code> object and kept in a MapStack -
 * which allows multiple bundles to be queried with a single method call. The class
 * instance is constructed with the most specific resource bundle first, then additional
 * less specific resource bundles are added to the bottom of the stack.
 */
@SuppressWarnings("serial")
public class ResourceBundleMapWrapper extends MapStack<String> {
    
    protected ResourceBundle initialResourceBundle;

    protected ResourceBundleMapWrapper() {}

    /** When creating new from a InternalRbmWrapper the one passed to the constructor
     * should be the most specific or local InternalRbmWrapper, with more common ones
     * pushed onto the stack progressively.
     */
    public ResourceBundleMapWrapper(InternalRbmWrapper initialInternalRbmWrapper) {
        this.initialResourceBundle = initialInternalRbmWrapper.getResourceBundle();
        push(initialInternalRbmWrapper);
    }
    
    /** When creating new from a ResourceBundle the one passed to the constructor
     * should be the most specific or local ResourceBundle, with more common ones
     * pushed onto the stack progressively.
     */
    public ResourceBundleMapWrapper(ResourceBundle initialResourceBundle) {
        if (initialResourceBundle == null) {
            throw new IllegalArgumentException("Cannot create ResourceBundleMapWrapper with a null initial ResourceBundle.");
        }
        this.initialResourceBundle = initialResourceBundle;
        push(new InternalRbmWrapper(initialResourceBundle));
    }
    
    public void addToBottom(Map<String, Object> existingMap) {
        if (!stackList.contains(existingMap)) {
            super.addToBottom(existingMap);
        }
    }
    
    public void push(Map<String, Object> existingMap) {
        if (!stackList.contains(existingMap)) {
            super.push(existingMap);
        }
    }
    
    /** Puts ResourceBundle on the BOTTOM of the stack - meaning the bundle will
     * be overriden by higher layers on the stack.
     */
    public void addBottomResourceBundle(ResourceBundle topResourceBundle) {
        addToBottom(new InternalRbmWrapper(topResourceBundle));
    }

    /** Puts InternalRbmWrapper on the BOTTOM of the stack - meaning the InternalRbmWrapper
     * will be overriden by higher layers on the stack.
     */
    public void addBottomResourceBundle(InternalRbmWrapper topInternalRbmWrapper) {
        addToBottom(topInternalRbmWrapper);
    }

    /** Puts the specified ResourceBundle on the BOTTOM of the stack - meaning the
     * ResourceBundle will be overriden by higher layers on the stack. Th method
     * will throw an exception if the specified ResourceBundle isn't found.
     */
    public void addBottomResourceBundle(String resource) {
        if (this.initialResourceBundle == null) {
            throw new IllegalArgumentException("Cannot add bottom resource bundle, this wrapper was not properly initialized (there is no base/initial ResourceBundle).");
        }
        this.addBottomResourceBundle(UtilProperties.getResourceBundle(resource, this.initialResourceBundle.getLocale()));
    }

    /** Puts a ResourceBundle on the TOP of the stack - meaning the ResourceBundle will
     * override lower layers on the stack. This is the reverse of how resource bundles
     * are normally added. 
     */
    public void pushResourceBundle(ResourceBundle topResourceBundle) {
        push(new InternalRbmWrapper(topResourceBundle));
    }

    /** Returns the ResourceBundle that was passed in the class constructor.
     */
    public ResourceBundle getInitialResourceBundle() {
        return this.initialResourceBundle;
    }

    /** Retrieves the specified object from the MapStack. If no matching object is found,
     * the <code>arg0</code> object is returned.
     */
    public Object get(Object arg0) {
        Object value = super.get(arg0);
        if (value == null) {
            value = arg0;
        }
        return value;
    }
    
    /** Encapsulates a ResourceBundle in a HashMap. This is an incomplete implementation
     * of the Map interface - its intended use is to retrieve ResourceBundle elements
     * in a Map-like way. Map interface methods that remove elements will throw
     * an exception.
     */
    @SuppressWarnings("serial")
    public static class InternalRbmWrapper extends HashMap<String, Object> {
        protected ResourceBundle resourceBundle;
        
        public InternalRbmWrapper(ResourceBundle resourceBundle) {
            if (resourceBundle == null) {
                throw new IllegalArgumentException("Cannot create InternalRbmWrapper with a null ResourceBundle.");
            }
            this.resourceBundle = resourceBundle;
            // NOTE: this does NOT return all keys, ie keys from parent
            // ResourceBundles, so we keep the resourceBundle object to look
            // at when the main Map doesn't have a certain value
            Enumeration<String> keyNum = resourceBundle.getKeys();
            while (keyNum.hasMoreElements()) {
                String key = keyNum.nextElement();
                Object value = resourceBundle.getObject(key);
                put(key, value);
            }
            put("_RESOURCE_BUNDLE_", resourceBundle); // Is this being used anywhere?
        }
        
        public boolean equals(Object obj) {
            return resourceBundle.equals(obj);
        }

        public int hashCode() {
            return resourceBundle.hashCode();
        }

        /*
         * (non-Javadoc)
         * 
         * @see java.util.Map#containsKey(java.lang.Object)
         */
        public boolean containsKey(Object arg0) {
            if (super.containsKey(arg0)) {
                return true;
            } else {
                try {
                    if (this.resourceBundle.getObject((String) arg0) != null) {
                        return true;
                    }
                } catch (Exception e) {
                    // Do nothing
                }
            }
            return false;
        }
    
        /* (non-Javadoc)
         * @see java.util.Map#get(java.lang.Object)
         */
        public Object get(Object arg0) {
            Object value = super.get(arg0);
            if (value == null) {
                try {
                    value = this.resourceBundle.getObject((String) arg0);
                } catch (MissingResourceException mre) {
                    // Do nothing
                }
            }
            return value;
        }
    
        /* (non-Javadoc)
         * @see java.util.Map#remove(java.lang.Object)
         */
        public Object remove(Object arg0) {
            throw new RuntimeException("Not implemented for ResourceBundleMapWrapper");
        }
    
        /* (non-Javadoc)
         * @see java.util.Map#clear()
         */
        public void clear() {
            throw new RuntimeException("Not implemented for ResourceBundleMapWrapper");
        }
    
        public ResourceBundle getResourceBundle() {
            return this.resourceBundle;
        }
    }
}

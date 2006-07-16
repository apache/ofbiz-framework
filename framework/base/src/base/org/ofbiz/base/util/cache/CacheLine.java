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
package org.ofbiz.base.util.cache;

import java.io.Serializable;

import org.ofbiz.base.util.UtilObject;

/**
 * 
 * @author     <a href="mailto:jaz@ofbiz.org">Andy Zeneski</a>
 * @version    $Rev$
 * @since      3.2
 */
public class CacheLine implements Serializable {

    public static final String module = CacheLine.class.getName();

    public Object valueRef = null;
    public long loadTime = 0;
    public long expireTime = 0;
    public boolean useSoftReference = false;

    public CacheLine(Object value, boolean useSoftReference, long expireTime) {
        if (useSoftReference) {
            this.valueRef = new CacheSoftReference(value);
        } else {
            this.valueRef = value;
        }
        this.useSoftReference = useSoftReference;
        this.expireTime = expireTime;
    }

    public CacheLine(Object value, boolean useSoftReference, long loadTime, long expireTime) {
        this(value, useSoftReference, expireTime);
        this.loadTime = loadTime;
    }

    public Object getValue() {
        if (valueRef == null) return null;
        if (useSoftReference) {
            return ((CacheSoftReference) valueRef).get();
        } else {
            return valueRef;
        }
    }
    
    public boolean softReferenceCleared() {
        if (!this.useSoftReference || valueRef == null) {
            return false;
        } else {
            if (((CacheSoftReference) valueRef).get() == null) {
                return true;
            } else {
                return false;
            }
        }
    }

    public void setUseSoftReference(boolean useSoftReference) {
        if (this.useSoftReference != useSoftReference) {
            synchronized (this) {
                this.useSoftReference = useSoftReference;
                if (useSoftReference) {
                    this.valueRef = new CacheSoftReference(this.valueRef);
                } else {
                    this.valueRef = ((CacheSoftReference) this.valueRef).get();
                }
            }
        }
    }

    public long getExpireTime() {
        return this.expireTime;
    }

    public long getSizeInBytes() {
        return UtilObject.getByteCount(this);
    }
}


/*
 * $Id: CacheLine.java 5462 2005-08-05 18:35:48Z jonesde $
 *
 * Copyright (c) 2004 The Open For Business Project - www.ofbiz.org
 *
 * Permission is hereby granted, free of charge, to any person obtaining a
 * copy of this software and associated documentation files (the "Software"),
 * to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
 * OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY
 * CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT
 * OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR
 * THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
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


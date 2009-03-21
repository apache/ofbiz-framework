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
package org.ofbiz.base.util.cache;

import java.io.Serializable;

import org.ofbiz.base.util.UtilObject;

public abstract class CacheLine<V> implements Serializable {
    public long loadTime;
    public final long expireTime;

    protected CacheLine(long expireTime) {
        this(0, expireTime);
    }

    protected CacheLine(long loadTime, long expireTime) {
        this.expireTime = expireTime;
        this.loadTime = loadTime;
    }

    public abstract V getValue();
    public abstract boolean isInvalid();

    public long getExpireTime() {
        return this.expireTime;
    }

    public long getSizeInBytes() {
        try {
            return UtilObject.getByteCount(this);
        } catch (Exception e) {
            return 0;
        }
    }

    public boolean hasExpired() {
        // check this BEFORE checking to see if expireTime <= 0, ie if time expiration is enabled
        // check to see if we are using softReference first, slight performance increase
        if (isInvalid()) return true;

        // check if expireTime <= 0, ie if time expiration is not enabled
        if (expireTime <= 0) return false;

        // check if the time was saved for this; if the time was not saved, but expire time is > 0, then we don't know when it was saved so expire it to be safe
        if (loadTime <= 0) return true;

        return (loadTime + expireTime) < System.currentTimeMillis();
    }
}


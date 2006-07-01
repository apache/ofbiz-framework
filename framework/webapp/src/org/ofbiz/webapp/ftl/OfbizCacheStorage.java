/*
 * $Id: OfbizCacheStorage.java 5462 2005-08-05 18:35:48Z jonesde $
 *
 * Copyright (c) 2003 The Open For Business Project - www.ofbiz.org
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
 */
package org.ofbiz.webapp.ftl;

import freemarker.cache.CacheStorage;

import org.ofbiz.base.util.cache.UtilCache;

/**
 * A custom cache wrapper for caching FreeMarker templates
 *
 * @author     <a href="mailto:jonesde@ofbiz.org">David E. Jones</a>
 * @version    $Rev$
 * @since      2.1
 */
public class OfbizCacheStorage implements CacheStorage {
    //can't have global cache because names/keys are relative to the webapp
    protected final UtilCache localCache;
    
    public OfbizCacheStorage(String id) {
        this.localCache = new UtilCache("webapp.FreeMarkerCache." + id, 0, 0, false);
    }
    
    public Object get(Object key) {
        return localCache.get(key);
    }
    
    public void put(Object key, Object value) {
        localCache.put(key, value);
    }
    
    public void remove(Object key) {
        localCache.remove(key);
    }
    
    public void clear() {
        localCache.clear();
    }
}

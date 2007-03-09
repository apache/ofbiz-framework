/*
 Licensed to the Apache Software Foundation (ASF) under one
 or more contributor license agreements.  See the NOTICE file
 distributed with this work for additional information
 regarding copyright ownership.  The ASF licenses this file
 to you under the Apache License, Version 2.0 (the
 "License"); you may not use this file except in compliance
 with the License.  You may obtain a copy of the License at

 http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing,
 software distributed under the License is distributed on an
 "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 KIND, either express or implied.  See the License for the
 specific language governing permissions and limitations
 under the License.
 */

package org.ofbiz.content.content;

import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.util.EntityUtil;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.GeneralException;
import org.ofbiz.content.data.DataResourceWorker;

import java.util.*;
import java.io.IOException;

import javolution.util.FastList;
import javolution.util.FastMap;

/**
 * ContentMapFacade
 */
public class ContentMapFacade implements Map {

    public static final String module = ContentMapFacade.class.getName();

    protected final LocalDispatcher dispatcher;
    protected final GenericDelegator delegator;
    protected final String contentId;
    protected final GenericValue value;
    protected final Map context;
    protected final Locale locale;
    protected final String mimeType;
    protected final boolean cache;
    protected boolean isTop = false;

    // internal objects
    private DataResource dataResource;
    private SubContent subContent;
    private MetaData metaData;
    private Content content;

    public ContentMapFacade(LocalDispatcher dispatcher, GenericValue content, Map context, Locale locale, String mimeTypeId, boolean cache) {
        this.dispatcher = dispatcher;
        this.value = content;
        this.context = context;
        this.locale = locale;
        this.mimeType = mimeTypeId;
        this.cache = cache;
        this.contentId = content.getString("contentId");
        this.delegator = content.getDelegator();
        this.isTop = true;
        init();
    }

    private ContentMapFacade(LocalDispatcher dispatcher, GenericDelegator delegator, String contentId, Map context, Locale locale, String mimeTypeId, boolean cache) {
        this.dispatcher = dispatcher;
        this.delegator = delegator;
        this.contentId = contentId;
        this.context = context;
        this.locale = locale;
        this.mimeType = mimeTypeId;
        this.cache = cache;
        try {
            this.value = delegator.findByPrimaryKeyCache("Content", UtilMisc.toMap("contentId", contentId));
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
            throw new RuntimeException(e.getMessage());
        }
        init();
    }

    private void init() {
        this.dataResource = new DataResource();
        this.subContent = new SubContent();
        this.metaData = new MetaData();
        this.content = new Content();
    }

    // interface methods
    public int size() {
        return 0;
    }

    public boolean isEmpty() {
        return false;
    }

    public boolean containsKey(Object object) {
        return false;
    }

    public boolean containsValue(Object object) {
        return false;
    }

    public Object put(Object name, Object value) {
        Debug.logWarning("This [put()] method is not implemented in ContentMapFacade", module);
        return null;
    }

    public Object remove(Object object) {
        Debug.logWarning("This [remove()] method is not implemented in ContentMapFacade", module);
        return null;
    }

    public void putAll(Map map) {
        Debug.logWarning("This method [putAll()] is not implemented in ContentMapFacade", module);
    }

    public void clear() {
        Debug.logWarning("This method [clear()] is not implemented in ContentMapFacade", module);
    }

    public Set keySet() {
        Debug.logWarning("This method [keySet()] is not implemented in ContentMapFacade", module);
        return null;
    }

    public Collection values() {
        Debug.logWarning("This method [values()] is not implemented in ContentMapFacade", module);
        return null;
    }

    public Set entrySet() {
        Debug.logWarning("This method [entrySet()] is not implemented in ContentMapFacade", module);
        return null;
    }

    // implemented get method
    public Object get(Object obj) {
        if (!(obj instanceof String)) {
            Debug.logWarning("Key parameters must be a string", module);
            return null;
        }
        String name = (String) obj;

        // fields key, returns value object
        if ("fields".equals(name)) {
            GenericValue value = null;
            try {
                value = delegator.findByPrimaryKeyCache("Content", UtilMisc.toMap("contentId", contentId));
            } catch (GenericEntityException e) {
                Debug.logError(e, module);
            }
            return value;

        }

        // data (resource) object
        if ("data".equals(name)) {
            return dataResource;   
        }

        // subcontent list of ordered subcontent
        if ("subcontent_all".equals(name)) {
            List subContent = FastList.newInstance();
            List subs = null;
            try {
                subs = delegator.findByAnd("ContentAssoc", UtilMisc.toMap("contentId", contentId), UtilMisc.toList("-fromDate"));
            } catch (GenericEntityException e) {
                Debug.logError(e, module);
            }
            if (subs != null) {
                subs = EntityUtil.filterByDate(subs);

                Iterator i = subs.iterator();
                while (i.hasNext()) {
                    GenericValue v = (GenericValue) i.next();
                    subContent.add(new ContentMapFacade(dispatcher, delegator, v.getString("contentIdTo"), context, locale, mimeType, cache));
                }
            }
            return subContent;
        }

        // return the subcontent object
        if ("subcontent".equals(name)) {
            return this.subContent;
        }

        // return list of metaData by predicate ID
        if ("metadata".equals(name)) {
            return this.metaData;
        }

        // content; returns object from contentId
        if ("content".equals(name)) {            
            return content;
        }
        
        // render this content
        if ("render".equals(name)) {
            Map renderCtx = FastMap.newInstance();
            renderCtx.putAll(context);
            if (isTop) {
                Debug.logWarning("Cannot render content being rendered! (No Looping!)", module);
                return "Cannot render content being rendered! (No Looping!)";
            }
            try {
                return ContentWorker.renderContentAsText(dispatcher, delegator, contentId, renderCtx, locale, mimeType, cache);
            } catch (GeneralException e) {
                Debug.logError(e, module);
                return e.toString();
            } catch (IOException e) {
                Debug.logError(e, module);
                return e.toString();
            }
        }

        return null;
    }

    abstract class AbstractInfo implements Map {

        public int size() {
            return 0;
        }

        public boolean isEmpty() {
            return false;
        }

        public boolean containsKey(Object object) {
            return false;
        }

        public boolean containsValue(Object object) {
            return false;
        }

        public abstract Object get(Object object);


        public Object put(Object name, Object value) {
            Debug.logWarning("This [put()] method is not implemented in ContentMapFacade.AbstractInfo", module);
            return null;
        }

        public Object remove(Object object) {
            Debug.logWarning("This [remove()] method is not implemented in ContentMapFacade.AbstractInfo", module);
            return null;
        }

        public void putAll(Map map) {
            Debug.logWarning("This method [putAll()] is not implemented in ContentMapFacade.AbstractInfo", module);
        }

        public void clear() {
            Debug.logWarning("This method [clear()] is not implemented in ContentMapFacade.AbstractInfo", module);
        }

        public Set keySet() {
            Debug.logWarning("This method [keySet()] is not implemented in ContentMapFacade.AbstractInfo", module);
            return null;
        }

        public Collection values() {
            Debug.logWarning("This method [values()] is not implemented in ContentMapFacade.AbstractInfo", module);
            return null;
        }

        public Set entrySet() {
            Debug.logWarning("This method [entrySet()] is not implemented in ContentMapFacade.AbstractInfo", module);
            return null;
        }

    }

    class Content extends AbstractInfo {
        public Object get(Object key) {
            if (!(key instanceof String)) {
                Debug.logWarning("Key parameters must be a string", module);
                return null;
            }
            String name = (String) key;
            if (name.toLowerCase().startsWith("id_")) {
                name = name.substring(3);
            }
            
            // look up the content ID (of name)
            GenericValue content = null;
            try {
                content = delegator.findByPrimaryKeyCache("Content", UtilMisc.toMap("contentId", name));
            } catch (GenericEntityException e) {
                Debug.logError(e, module);
            }
            if (content != null) {
                return new ContentMapFacade(dispatcher, delegator, content.getString("contentId"), context, locale, mimeType, cache);
            }

            return null;
        }
    }

    class SubContent extends AbstractInfo {
        public Object get(Object key) {
            if (!(key instanceof String)) {
                Debug.logWarning("Key parameters must be a string", module);
                return null;
            }
            String name = (String) key;            
            if (name.toLowerCase().startsWith("id_")) {
                name = name.substring(3);
            }

            // key is the mapKey
            List subs = null;
            try {
                subs = delegator.findByAnd("ContentAssoc", UtilMisc.toMap("contentId", contentId, "mapKey", name), UtilMisc.toList("-fromDate"));
            } catch (GenericEntityException e) {
                Debug.logError(e, module);
            }
            if (subs != null) {
                subs = EntityUtil.filterByDate(subs);
                GenericValue v = EntityUtil.getFirst(subs);
                if (v != null) {
                    return new ContentMapFacade(dispatcher, delegator, v.getString("contentIdTo"), context, locale, mimeType, cache);
                }
            }

            return null;
        }
    }

    class MetaData extends AbstractInfo {
        public Object get(Object key) {
            if (!(key instanceof String)) {
                Debug.logWarning("Key parameters must be a string", module);
                return null;
            }
            String name = (String) key;
            List metaData = null;
            try {
                metaData = delegator.findByAnd("ContentMetaData", UtilMisc.toMap("contentId", contentId, "metaDataPredicateId", name));
            } catch (GenericEntityException e) {
                Debug.logError(e, module);
            }
            return metaData;
        }
    }

    class DataResource extends AbstractInfo {
        public Object get(Object key) {
            if (!(key instanceof String)) {
                Debug.logWarning("Key parameters must be a string", module);
                return null;
            }
            String name = (String) key;

            // get the data resource value object
            if ("fields".equals(name)) {
                GenericValue dr = null;
                try {
                    dr = value.getRelatedOne("DataResource");
                } catch (GenericEntityException e) {
                    Debug.logError(e, module);
                }
                return dr;
            }

            // render just the dataresource
            if ("render".equals(name)) {
                try {
                    return DataResourceWorker.renderDataResourceAsText(delegator, value.getString("dataResourceId"), context, locale, mimeType, cache);
                } catch (GeneralException e) {
                    Debug.logError(e, module);
                    return e.toString();
                } catch (IOException e) {
                    Debug.logError(e, module);
                    return e.toString();
                }
            }

            return null;
        }
    }
}

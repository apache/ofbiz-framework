/*
k Licensed to the Apache Software Foundation (ASF) under one
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

package org.apache.ofbiz.content.content;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.GeneralException;
import org.apache.ofbiz.base.util.UtilGenerics;
import org.apache.ofbiz.content.data.DataResourceWorker;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.webapp.control.RequestHandler;
import org.apache.ofbiz.webapp.website.WebSiteWorker;

/**
 * ContentMapFacade
 */
public class ContentMapFacade implements Map<Object, Object> {

    public static final String module = ContentMapFacade.class.getName();

    private static final Set<String> mapKeySet = new HashSet<>();
    static {
        mapKeySet.add("fields");
        mapKeySet.add("link");
        mapKeySet.add("data");
        mapKeySet.add("dataresource");
        mapKeySet.add("subcontent");
        mapKeySet.add("subcontent_all");
        mapKeySet.add("metadata");
        mapKeySet.add("content");
        mapKeySet.add("render");
    }

    protected final LocalDispatcher dispatcher;
    protected final Delegator delegator;
    protected final String contentId;
    protected final GenericValue value;
    protected final Map<String, Object> context;
    protected final Locale locale;
    protected final String mimeType;
    protected final boolean cache;
    protected boolean allowRender = true;
    protected boolean isDecorated = false;
    protected ContentMapFacade decoratedContent = null;

    // internal objects
    private String sortOrder="-fromDate";
    private String mapKeyFilter="";
    private String statusFilter="";
    private DataResource dataResource;
    private SubContent subContent;
    private MetaData metaData;
    private Content content;
    private GenericValue fields = null;

    public ContentMapFacade(LocalDispatcher dispatcher, GenericValue content, Map<String, Object> context, Locale locale, String mimeTypeId, boolean cache) {
        this.dispatcher = dispatcher;
        this.value = content;
        this.context = context;
        this.locale = locale;
        this.mimeType = mimeTypeId;
        this.cache = cache;
        this.contentId = content.getString("contentId");
        this.delegator = content.getDelegator();
        this.allowRender = false;
        init();
    }

    private ContentMapFacade(LocalDispatcher dispatcher, String contentId, Map<String, Object> context, Locale locale, String mimeTypeId, boolean cache) {
        this.dispatcher = dispatcher;
        this.delegator = dispatcher.getDelegator();
        this.contentId = contentId;
        this.context = context;
        this.locale = locale;
        this.mimeType = mimeTypeId;
        this.cache = cache;
        try {
            if (cache) {
                this.value = EntityQuery.use(this.delegator).from("Content").where("contentId", contentId).cache().queryOne();
            } else {
                this.value = EntityQuery.use(this.delegator).from("Content").where("contentId", contentId).queryOne();
            }
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

    public void setRenderFlag(boolean render) {
        this.allowRender = render;
    }

    public void setIsDecorated(boolean isDecorated) {
        this.isDecorated = isDecorated;
    }

    // interface methods
    public int size() {
        return 1;
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

    public void putAll(Map<?, ?> map) {
        Debug.logWarning("This method [putAll()] is not implemented in ContentMapFacade", module);
    }

    public void clear() {
        Debug.logWarning("This method [clear()] is not implemented in ContentMapFacade", module);
    }

    public Set<Object> keySet() {
        return UtilGenerics.checkSet(mapKeySet);
    }

    public Collection<Object> values() {
        Debug.logWarning("This method [values()] is not implemented in ContentMapFacade", module);
        return null;
    }

    public Set<Map.Entry<Object, Object>> entrySet() {
        Debug.logWarning("This method [entrySet()] is not implemented in ContentMapFacade", module);
        return null;
    }

    public void setSortOrder(Object obj) {
        if (!(obj instanceof String)) {
            Debug.logWarning("sortOrder parameters must be a string", module);
            return;
        }
        this.sortOrder=(String) obj;
        this.subContent.setSortOrder(obj);
    }

    public void setMapKeyFilter(Object obj) {
        if (!(obj instanceof String)) {
            Debug.logWarning("mapKeyFilter parameters must be a string", module);
            return;
        }
        this.mapKeyFilter=(String) obj;
    }

    public void setStatusFilter(Object obj) {
        if (!(obj instanceof String)) {
            Debug.logWarning("statusFilter parameters must be a string", module);
            return;
        }
        this.statusFilter=(String) obj;
        this.subContent.setStatusFilter(obj);
    }

    public void setDecoratedContent(ContentMapFacade decoratedContent) {
        this.decoratedContent = decoratedContent;
    }

    // implemented get method
    public Object get(Object obj) {
        if (!(obj instanceof String)) {
            Debug.logWarning("Key parameters must be a string", module);
            return null;
        }
        String name = (String) obj;

        if ("fields".equalsIgnoreCase(name)) {
            // fields key, returns value object
            if (this.fields != null) {
                return fields;
            }
            try {
                EntityQuery contentQuery = EntityQuery.use(delegator).from("Content").where("contentId", contentId);
                if (cache) {
                    contentQuery.cache();
                }
                this.fields = contentQuery.queryOne();
            } catch (GenericEntityException e) {
                Debug.logError(e, module);
            }
            return this.fields;

        } else if ("link".equalsIgnoreCase(name)) {
            // link to this content

            RequestHandler rh = (RequestHandler) this.context.get("_REQUEST_HANDLER_");
            HttpServletRequest request = (HttpServletRequest) this.context.get("request");
            HttpServletResponse response = (HttpServletResponse) this.context.get("response");

            if (rh != null && request != null && response != null) {
                String webSiteId = WebSiteWorker.getWebSiteId(request);
                Delegator delegator = (Delegator) request.getAttribute("delegator");
                
                String contentUri = this.contentId;
                // Try and find a WebSitePathAlias record to use, it isn't very feasible to find an alias by (parent)contentId/mapKey
                // so we're only looking for a direct alias using contentId
                if (webSiteId != null && delegator != null) {
                    try {
                        GenericValue webSitePathAlias = EntityQuery.use(delegator).from("WebSitePathAlias")
                                .where("mapKey", null, "webSiteId", webSiteId,"contentId", this.contentId)
                                .orderBy("-fromDate")
                                .cache()
                                .filterByDate()
                                .queryFirst();
                        if (webSitePathAlias != null) {
                            contentUri = webSitePathAlias.getString("pathAlias");
                        }
                    } catch (GenericEntityException e) {
                        Debug.logError(e, module);
                    }
                }
                String contextLink = rh.makeLink(request, response, contentUri, true, false, true);
                return contextLink;
            } else {
                return this.contentId;
            }
        } else if ("data".equalsIgnoreCase(name) || "dataresource".equalsIgnoreCase(name)) {
            // data (resource) object
            return dataResource;
        } else if ("subcontent_all".equalsIgnoreCase(name)) {
            // subcontent list of ordered subcontent
            List<ContentMapFacade> subContent = new LinkedList<>();
            List<GenericValue> subs = null;
            try {
                Map<String, Object> expressions = new HashMap<>();
                expressions.put("contentIdStart", contentId);
                if(!this.mapKeyFilter.equals("")) {
                    expressions.put("caMapKey", this.mapKeyFilter);
                }
                if(!this.statusFilter.equals("")) {
                    expressions.put("statusId", this.statusFilter);
                }

                subs = EntityQuery.use(delegator).from("ContentAssocViewTo")
                        .where(expressions)
                        .orderBy(this.sortOrder)
                        .filterByDate()
                        .cache(cache).queryList();
            } catch (GenericEntityException e) {
                Debug.logError(e, module);
            }
            if (subs != null) {
                for (GenericValue v: subs) {
                    subContent.add(new ContentMapFacade(dispatcher, v.getString("contentId"), context, locale, mimeType, cache));
                }
            }
            return subContent;
        } else if ("subcontent".equalsIgnoreCase(name)) {
            // return the subcontent object
            return this.subContent;
        } else if ("metadata".equalsIgnoreCase(name)) {
            // return list of metaData by predicate ID
            return this.metaData;
        } else if ("content".equalsIgnoreCase(name)) {
            // content; returns object from contentId
            return content;
        } else if ("render".equalsIgnoreCase(name)) {
            // render this content
            return this.renderThis();
        }

        return null;
    }

    protected String renderThis() {
        if (!this.allowRender && !this.isDecorated) {
            String errorMsg = "WARNING: Cannot render content being rendered! (Infinite Recursion NOT allowed!)";
            Debug.logWarning(errorMsg, module);
            return "=========> " + errorMsg + " <=========";
        }
        // TODO: change to use the MapStack instead of a cloned Map
        Map<String, Object> renderCtx = new HashMap<>();
        renderCtx.putAll(context);
        if (this.decoratedContent != null) {
            renderCtx.put("decoratedContent", decoratedContent);
        }

        if (this.isDecorated) {
            renderCtx.put("_IS_DECORATED_", Boolean.TRUE);
        }

        try {
            return ContentWorker.renderContentAsText(dispatcher, contentId, renderCtx, locale, mimeType, cache);
        } catch (GeneralException e) {
            Debug.logError(e, module);
            return e.toString();
        } catch (IOException e) {
            Debug.logError(e, module);
            return e.toString();
        }
    }

    @Override
    public String toString() {
        return this.renderThis();
    }

    abstract class AbstractInfo implements Map<Object, Object> {
        public int size() {
            return 1;
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
            Debug.logWarning("This [put()] method is not implemented in ContentMapFacade.AbstractInfo", module);
            return null;
        }

        public Object remove(Object object) {
            Debug.logWarning("This [remove()] method is not implemented in ContentMapFacade.AbstractInfo", module);
            return null;
        }

        public void putAll(Map<?, ?> map) {
            Debug.logWarning("This method [putAll()] is not implemented in ContentMapFacade.AbstractInfo", module);
        }

        public void clear() {
            Debug.logWarning("This method [clear()] is not implemented in ContentMapFacade.AbstractInfo", module);
        }

        public Set<Object> keySet() {
            Debug.logWarning("This method [keySet()] is not implemented in ContentMapFacade.AbstractInfo", module);
            return null;
        }

        public Collection<Object> values() {
            Debug.logWarning("This method [values()] is not implemented in ContentMapFacade.AbstractInfo", module);
            return null;
        }

        public Set<Map.Entry<Object, Object>> entrySet() {
            Debug.logWarning("This method [entrySet()] is not implemented in ContentMapFacade.AbstractInfo", module);
            return null;
        }
    }

    class Content extends AbstractInfo {
        @Override
        public Object get(Object key) {
            if (!(key instanceof String)) {
                Debug.logWarning("Key parameters must be a string", module);
                return null;
            }
            String name = (String) key;
            if (name.toLowerCase(Locale.getDefault()).startsWith("id_")) {
                name = name.substring(3);
            }

            // look up the content ID (of name)
            GenericValue content = null;
            try {
                if (cache) {
                    content = EntityQuery.use(delegator).from("Content").where("contentId", name).cache().queryOne();
                } else {
                    content = EntityQuery.use(delegator).from("Content").where("contentId", name).queryOne();
                }
            } catch (GenericEntityException e) {
                Debug.logError(e, module);
            }
            if (content != null) {
                return new ContentMapFacade(dispatcher, content.getString("contentId"), context, locale, mimeType, cache);
            }

            return null;
        }
    }

    class SubContent extends AbstractInfo {
        private String sortOrder="-fromDate";
        private String statusFilter="";
        @Override
        public Object get(Object key) {
            if (!(key instanceof String)) {
                Debug.logWarning("Key parameters must be a string", module);
                return null;
            }
            String name = (String) key;
            if (name.toLowerCase(Locale.getDefault()).startsWith("id_")) {
                name = name.substring(3);
            }

            // key is the mapKey
            GenericValue sub = null;
            try {
                Map<String, Object> expressions = new HashMap<>();
                expressions.put("contentIdStart", contentId);
                expressions.put("caMapKey", name);
                if(!this.statusFilter.equals("")) {
                    expressions.put("statusId", this.statusFilter);
                }
                sub = EntityQuery.use(delegator).from("ContentAssocViewTo")
                        .where(expressions)
                        .orderBy(this.sortOrder)
                        .cache(cache)
                        .filterByDate().queryFirst();
            } catch (GenericEntityException e) {
                Debug.logError(e, module);
            }
            if (sub != null) {
                return new ContentMapFacade(dispatcher, sub.getString("contentId"), context, locale, mimeType, cache);
            }

            return null;
        }
        public void setSortOrder(Object obj) {
            if (!(obj instanceof String)) {
                Debug.logWarning("sortOrder parameters must be a string", module);
                return;
            }
            this.sortOrder=(String) obj;
        }
        public void setStatusFilter(Object obj) {
            if (!(obj instanceof String)) {
                Debug.logWarning("statusFilter parameters must be a string", module);
                return;
            }
            this.statusFilter=(String) obj;
        }
    }

    class MetaData extends AbstractInfo {
        @Override
        public Object get(Object key) {
            if (!(key instanceof String)) {
                Debug.logWarning("Key parameters must be a string", module);
                return null;
            }
            String name = (String) key;
            List<GenericValue> metaData = null;
            try {
                metaData = EntityQuery.use(delegator).from("ContentMetaData")
                        .where("contentId", contentId, "metaDataPredicateId", name)
                        .cache(cache).queryList();
            } catch (GenericEntityException e) {
                Debug.logError(e, module);
            }
            return metaData;
        }
    }

    class DataResource extends AbstractInfo {
        @Override
        public Object get(Object key) {
            if (!(key instanceof String)) {
                Debug.logWarning("Key parameters must be a string", module);
                return null;
            }
            String name = (String) key;

            if ("fields".equalsIgnoreCase(name)) {
                // get the data resource value object
                GenericValue dr = null;
                try {
                    dr = value.getRelatedOne("DataResource", cache);
                } catch (GenericEntityException e) {
                    Debug.logError(e, module);
                }
                return dr;
            } else if ("render".equalsIgnoreCase(name)) {
                // render just the dataresource
                try {
                    return DataResourceWorker.renderDataResourceAsText(dispatcher, delegator, value.getString("dataResourceId"), context, locale, mimeType, cache);
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

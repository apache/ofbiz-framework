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

import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.util.EntityUtil;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.webapp.control.RequestHandler;
import org.ofbiz.webapp.website.WebSiteWorker;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.GeneralException;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.content.data.DataResourceWorker;

import java.util.*;
import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import javolution.util.FastList;
import javolution.util.FastMap;
import javolution.util.FastSet;

/**
 * ContentMapFacade
 */
public class ContentMapFacade implements Map {

    public static final String module = ContentMapFacade.class.getName();

    protected static final Set<String> mapKeySet = FastSet.newInstance();
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
    protected final Map context;
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

    public ContentMapFacade(LocalDispatcher dispatcher, GenericValue content, Map context, Locale locale, String mimeTypeId, boolean cache) {
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

    private ContentMapFacade(LocalDispatcher dispatcher, Delegator delegator, String contentId, Map context, Locale locale, String mimeTypeId, boolean cache) {
        this.dispatcher = dispatcher;
        this.delegator = delegator;
        this.contentId = contentId;
        this.context = context;
        this.locale = locale;
        this.mimeType = mimeTypeId;
        this.cache = cache;
        try {
            if (cache) {
                this.value = delegator.findByPrimaryKeyCache("Content", UtilMisc.toMap("contentId", contentId));
            } else {
                this.value = delegator.findByPrimaryKey("Content", UtilMisc.toMap("contentId", contentId));
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

    public void putAll(Map map) {
        Debug.logWarning("This method [putAll()] is not implemented in ContentMapFacade", module);
    }

    public void clear() {
        Debug.logWarning("This method [clear()] is not implemented in ContentMapFacade", module);
    }

    public Set<String> keySet() {
        // Debug.logWarning("This method [keySet()] is not completely implemented in ContentMapFacade", module);
        return mapKeySet;
    }

    public Collection values() {
        Debug.logWarning("This method [values()] is not implemented in ContentMapFacade", module);
        return null;
    }

    public Set entrySet() {
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
                if (cache) {
                    this.fields = delegator.findByPrimaryKeyCache("Content", UtilMisc.toMap("contentId", contentId));
                } else {
                    this.fields = delegator.findByPrimaryKey("Content", UtilMisc.toMap("contentId", contentId));
                }
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
                    EntityCondition condition = EntityCondition.makeCondition(
                            UtilMisc.toMap(
                                    "mapKey", null,
                                    "webSiteId", webSiteId,
                                    "contentId", this.contentId
                            )
                    );
                    try {
                        List<GenericValue> webSitePathAliases = delegator.findList("WebSitePathAlias", condition, null, null, null, true);
                        GenericValue webSitePathAlias = EntityUtil.getFirst(webSitePathAliases);
                        if (webSitePathAlias != null) {
                            contentUri = webSitePathAlias.getString("pathAlias");
                        }
                    } catch (GenericEntityException e) {
                        Debug.logError(e, module);
                    }
                }
                String contextLink = rh.makeLink(request, response, contentUri, true, false, true);
                // Debug.logInfo("Made link to content with ID [" + this.contentId + "]: " + contextLink, module);
                return contextLink;
            } else {
                return this.contentId;
            }
        } else if ("data".equalsIgnoreCase(name) || "dataresource".equalsIgnoreCase(name)) {
            // data (resource) object
            return dataResource;
        } else if ("subcontent_all".equalsIgnoreCase(name)) {
            // subcontent list of ordered subcontent
            List<ContentMapFacade> subContent = FastList.newInstance();
            List<GenericValue> subs = null;
            try {
                Map expressions = FastMap.newInstance();
                expressions.put("contentIdStart", contentId);
                if(!this.mapKeyFilter.equals("")) {
                    expressions.put("caMapKey", this.mapKeyFilter);
                }
                if(!this.statusFilter.equals("")) {
                    expressions.put("statusId", this.statusFilter);
                }

                if (cache) {
                    subs = delegator.findByAndCache("ContentAssocViewTo", expressions, UtilMisc.toList(this.sortOrder));
                } else {
                    subs = delegator.findByAnd("ContentAssocViewTo", expressions, UtilMisc.toList(this.sortOrder));
                }
            } catch (GenericEntityException e) {
                Debug.logError(e, module);
            }
            if (subs != null) {
                subs = EntityUtil.filterByDate(subs);

                for (GenericValue v: subs) {
                    subContent.add(new ContentMapFacade(dispatcher, delegator, v.getString("contentId"), context, locale, mimeType, cache));
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
        Map renderCtx = FastMap.newInstance();
        renderCtx.putAll(context);
        if (this.decoratedContent != null) {
            renderCtx.put("decoratedContent", decoratedContent);
        }

        if (this.isDecorated) {
            renderCtx.put("_IS_DECORATED_", Boolean.TRUE);
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

    @Override
    public String toString() {
        return this.renderThis();
    }

    abstract class AbstractInfo implements Map {
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
        @Override
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
                if (cache) {
                    content = delegator.findByPrimaryKeyCache("Content", UtilMisc.toMap("contentId", name));
                } else {
                    content = delegator.findByPrimaryKey("Content", UtilMisc.toMap("contentId", name));
                }
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
        private String sortOrder="-fromDate";
        private String statusFilter="";
        @Override
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
                Map expressions = FastMap.newInstance();
                expressions.put("contentIdStart", contentId);
                expressions.put("caMapKey", name);
                if(!this.statusFilter.equals("")) {
                    expressions.put("statusId", this.statusFilter);
                }
                if (cache) {
                    subs = delegator.findByAndCache("ContentAssocViewTo", expressions, UtilMisc.toList(this.sortOrder));
                } else {
                    subs = delegator.findByAnd("ContentAssocViewTo", expressions, UtilMisc.toList(this.sortOrder));
                }
            } catch (GenericEntityException e) {
                Debug.logError(e, module);
            }
            if (subs != null) {
                subs = EntityUtil.filterByDate(subs);
                GenericValue v = EntityUtil.getFirst(subs);
                if (v != null) {
                    return new ContentMapFacade(dispatcher, delegator, v.getString("contentId"), context, locale, mimeType, cache);
                }
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
            List metaData = null;
            try {
                if (cache) {
                    metaData = delegator.findByAndCache("ContentMetaData", UtilMisc.toMap("contentId", contentId, "metaDataPredicateId", name));
                } else {
                    metaData = delegator.findByAnd("ContentMetaData", UtilMisc.toMap("contentId", contentId, "metaDataPredicateId", name));
                }
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
                    if (cache) {
                        dr = value.getRelatedOneCache("DataResource");
                    } else {
                        dr = value.getRelatedOne("DataResource");
                    }
                } catch (GenericEntityException e) {
                    Debug.logError(e, module);
                }
                return dr;
            } else if ("render".equalsIgnoreCase(name)) {
                // render just the dataresource
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

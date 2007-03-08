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
package org.ofbiz.workeffort.content;

import java.util.*;
import java.io.Writer;
import java.io.IOException;
import java.io.StringWriter;
import java.sql.Timestamp;
import javax.servlet.http.HttpServletRequest;

import org.ofbiz.content.content.ContentWrapper;
import org.ofbiz.content.content.ContentWorker;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.util.EntityUtil;
import org.ofbiz.entity.model.ModelUtil;
import org.ofbiz.entity.model.ModelEntity;
import org.ofbiz.base.util.UtilHttp;
import org.ofbiz.base.util.GeneralException;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.GeneralRuntimeException;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.cache.UtilCache;
import org.ofbiz.service.LocalDispatcher;
import javolution.util.FastMap;
import javolution.util.FastList;

/**
 * WorkEffortContentWrapper; gets work effort content for display
 */
public class WorkEffortContentWrapper implements ContentWrapper {

    public static final String module = WorkEffortContentWrapper.class.getName();
    public static final String CACHE_KEY_SEPARATOR = "::";
    
    public static UtilCache workEffortContentCache = new UtilCache("workeffort.content.rendered", true);

    protected LocalDispatcher dispatcher;
    protected GenericValue workEffort;
    protected Locale locale;
    protected String mimeTypeId;

    public WorkEffortContentWrapper(LocalDispatcher dispatcher, GenericValue workEffort, Locale locale, String mimeTypeId) {
        this.dispatcher = dispatcher;
        this.workEffort = workEffort;
        this.locale = locale;
        this.mimeTypeId = mimeTypeId;
    }

    public WorkEffortContentWrapper(GenericValue workEffort, HttpServletRequest request) {
        this.workEffort = workEffort;
        this.locale = UtilHttp.getLocale(request);
        this.mimeTypeId = "text/html";
    }

    // interface implementation(s)
    public String get(String workEffortContentId, boolean useCache) {
        return getWorkEffortContentAsText(workEffort, workEffortContentId, locale, mimeTypeId, workEffort.getDelegator(), dispatcher, useCache);
    }

    /**
     * Get the most current content data by the defined type
     * @param contentTypeId Type of content to return
     * @return String containing the content data
     */
    public String get(String contentTypeId) {
        return get(contentTypeId, true);
    }

    /**
     * Get the ID from the most current content data by the defined type
     * @param contentTypeId Type of content to return
     * @return String containing the contentId
     */
    public String getContentId(String contentTypeId) {
        GenericValue workEffortContent = getFirstWorkEffortContentByType(null, workEffort, contentTypeId, workEffort.getDelegator());
        if (workEffortContent != null) {
            return workEffortContent.getString("contentId");
        } else {
            return null;
        }
    }

    /**
     * Get the name of the most current content data by the defined type
     * @param contentTypeId Type of content to return
     * @return String containing the name of the content record
     */
    public String getContentName(String contentTypeId) {
        GenericValue workEffortContent = getFirstWorkEffortContentByType(null, workEffort, contentTypeId, workEffort.getDelegator());
        if (workEffortContent != null) {
            GenericValue content;
            try {
                content = workEffortContent.getRelatedOne("Content");
            } catch (GeneralException e) {
                Debug.logError(e, module);
                return null;
            }

            if (content != null) {
                return content.getString("contentName");
            }
        }

        return null;
    }

    /**
     * Get the fromDate from teh most current content data by the defined type
     * @param contentTypeId Type of content to return
     * @return Timestamp of the fromDate field for this content type
     */
    public Timestamp getFromDate(String contentTypeId) {
        GenericValue workEffortContent = getFirstWorkEffortContentByType(null, workEffort, contentTypeId, workEffort.getDelegator());
        if (workEffortContent != null) {
            return workEffortContent.getTimestamp("fromDate");
        } else {
            return null;
        }
    }

    public String getDataResourceId(String contentTypeId) {
        GenericValue workEffortContent = getFirstWorkEffortContentByType(null, workEffort, contentTypeId, workEffort.getDelegator());
        if (workEffortContent != null) {
            GenericValue content;
            try {
                content = workEffortContent.getRelatedOne("Content");
            } catch (GeneralException e) {
                Debug.logError(e, module);
                return null;
            }
            if (content != null) {
                GenericValue dataResource;
                try {
                    dataResource = content.getRelatedOne("DataResource");
                } catch (GeneralException e) {
                    Debug.logError(e, module);
                    return null;
                }
                if (dataResource != null) {
                    return dataResource.getString("dataResourceId");
                }
            }
        }

        return null;
    }
    
    public List getList(String contentTypeId) {
        try {
            return getWorkEffortContentTextList(workEffort, contentTypeId, locale, mimeTypeId, workEffort.getDelegator(), dispatcher);
        } catch (Exception e) {
            Debug.logError(e, module);
            return null;
        }
    }

    public String getTypeDescription(String contentTypeId) {
        GenericDelegator delegator = null;
        if (workEffort != null) {
            delegator = workEffort.getDelegator();
        }

        if (delegator != null) {
            GenericValue contentType = null;
            try {
                contentType = delegator.findByPrimaryKeyCache("WorkEffortContentType", UtilMisc.toMap("workEffortContentTypeId", contentTypeId));
            } catch (GeneralException e) {
                Debug.logError(e, module);
            }

            if (contentType != null) {
                return contentType.getString("description");
            }
        }

        return null;        
    }

    public String getContent(String contentId, boolean useCache) {
        return getWorkEffortContentAsText(workEffort, contentId, null, locale, mimeTypeId, workEffort.getDelegator(), dispatcher, useCache);
    }

    public String getContent(String contentId) {
        return getContent(contentId, true);
    }

    // static method helpers
     public static String getWorkEffortContentAsText(GenericValue workEffort, String workEffortContentTypeId, HttpServletRequest request) {
        LocalDispatcher dispatcher = (LocalDispatcher) request.getAttribute("dispatcher");
        return getWorkEffortContentAsText(workEffort, workEffortContentTypeId, UtilHttp.getLocale(request), "text/html", workEffort.getDelegator(), dispatcher, true);
    }

    public static String getWorkEffortContentAsText(GenericValue workEffort, String workEffortContentTypeId, Locale locale, LocalDispatcher dispatcher) {
        return getWorkEffortContentAsText(workEffort, workEffortContentTypeId, locale, null, null, dispatcher, true);
    }

    public static String getWorkEffortContentAsText(GenericValue workEffort, String workEffortContentTypeId,
            Locale locale, String mimeTypeId, GenericDelegator delegator, LocalDispatcher dispatcher, boolean useCache) {
        return getWorkEffortContentAsText(workEffort, null, workEffortContentTypeId, locale, mimeTypeId, delegator, dispatcher, useCache);
    }

    public static String getWorkEffortContentAsText(GenericValue workEffort, String contentId, String workEffortContentTypeId,
            Locale locale, String mimeTypeId, GenericDelegator delegator, LocalDispatcher dispatcher, boolean useCache) {
        if (workEffort == null) {
            return null;
        }

        String candidateFieldName = ModelUtil.dbNameToVarName(workEffortContentTypeId);
        String cacheKey;
        if (contentId != null) {
            cacheKey = contentId + CACHE_KEY_SEPARATOR + locale + CACHE_KEY_SEPARATOR + mimeTypeId +
                    CACHE_KEY_SEPARATOR + workEffort.get("workEffortId");
        } else {
            cacheKey = workEffortContentTypeId + CACHE_KEY_SEPARATOR + locale + CACHE_KEY_SEPARATOR + mimeTypeId +
                    CACHE_KEY_SEPARATOR + workEffort.get("workEffortId");
        }

        try {
            if (useCache && workEffortContentCache.get(cacheKey) != null) {
                return (String) workEffortContentCache.get(cacheKey);
            }

            Writer outWriter = new StringWriter();
            getWorkEffortContentAsText(contentId, null, workEffort, workEffortContentTypeId, locale, mimeTypeId, delegator, dispatcher, outWriter);
            String outString = outWriter.toString();
            if (outString.length() > 0) {
                if (workEffortContentCache != null) {
                    workEffortContentCache.put(cacheKey, outString);
                }
                return outString;
            } else {
                String candidateOut = workEffort.getModelEntity().isField(candidateFieldName) ? workEffort.getString(candidateFieldName): "";
                return candidateOut == null? "" : candidateOut;
            }
        } catch (GeneralException e) {
            Debug.logError(e, "Error rendering WorkEffortContent, inserting empty String", module);
            String candidateOut = workEffort.getModelEntity().isField(candidateFieldName) ? workEffort.getString(candidateFieldName): "";
            return candidateOut == null? "" : candidateOut;
        } catch (IOException e) {
            Debug.logError(e, "Error rendering WorkEffortContent, inserting empty String", module);
            String candidateOut = workEffort.getModelEntity().isField(candidateFieldName) ? workEffort.getString(candidateFieldName): "";
            return candidateOut == null? "" : candidateOut;
        }               
    }

    public static void getWorkEffortContentAsText(String contentId, String workEffortId, GenericValue workEffort, String workEffortContentTypeId, Locale locale, String mimeTypeId, GenericDelegator delegator, LocalDispatcher dispatcher, Writer outWriter) throws GeneralException, IOException {
        if (workEffortId == null && workEffort != null) {
            workEffortId = workEffort.getString("workEffortId");
        }

        if (delegator == null && workEffort != null) {
            delegator = workEffort.getDelegator();
        }

        if (UtilValidate.isEmpty(mimeTypeId)) {
            mimeTypeId = "text/html";
        }

        if (delegator == null) {
            throw new GeneralRuntimeException("Unable to find a delegator to use!");
        }

        // check for workeffort field
        String candidateFieldName = ModelUtil.dbNameToVarName(workEffortContentTypeId);
        ModelEntity workEffortModel = delegator.getModelEntity("WorkEffort");
        if (workEffortModel != null && workEffortModel.isField(candidateFieldName)) {
            if (workEffort == null) {
                workEffort = delegator.findByPrimaryKeyCache("WorkEffort", UtilMisc.toMap("workEffortId", workEffortId));
            }
            if (workEffort != null) {
                String candidateValue = workEffort.getString(candidateFieldName);
                if (UtilValidate.isNotEmpty(candidateValue)) {
                    outWriter.write(candidateValue);
                    return;
                }
            }
        }

        // otherwise check content record
        GenericValue workEffortContent;
        if (contentId != null) {
            workEffortContent = delegator.findByPrimaryKeyCache("WorkEffortContent", UtilMisc.toMap("workEffortId", workEffortId, "contentId", contentId));
        } else {
            workEffortContent = getFirstWorkEffortContentByType(workEffortId, workEffort, workEffortContentTypeId, delegator);
        }
        if (workEffortContent != null) {
            // when rendering the product content, always include the Product and ProductContent records that this comes from
            Map inContext = FastMap.newInstance();
            inContext.put("workEffort", workEffort);
            inContext.put("workEffortContent", workEffortContent);
            ContentWorker.renderContentAsText(dispatcher, delegator, workEffortContent.getString("contentId"), outWriter, inContext, locale, mimeTypeId, false);
        }
    }

    public static List getWorkEffortContentTextList(GenericValue workEffort, String workEffortContentTypeId, Locale locale, String mimeTypeId, GenericDelegator delegator, LocalDispatcher dispatcher) throws GeneralException, IOException {
        List partyContentList = delegator.findByAndCache("WorkEffortContent", UtilMisc.toMap("workEffortId", workEffort.getString("partyId"), "workEffortContentTypeId", workEffortContentTypeId), UtilMisc.toList("-fromDate"));
        partyContentList = EntityUtil.filterByDate(partyContentList);

        List contentList = FastList.newInstance();
        if (partyContentList != null) {
            Iterator i = partyContentList.iterator();
            while (i.hasNext()) {
                GenericValue workEffortContent = (GenericValue) i.next();
                StringWriter outWriter = new StringWriter();
                Map inContext = FastMap.newInstance();
                inContext.put("workEffort", workEffort);
                inContext.put("workEffortContent", workEffortContent);
                ContentWorker.renderContentAsText(dispatcher, delegator, workEffortContent.getString("contentId"), outWriter, inContext, locale, mimeTypeId, false);
                contentList.add(outWriter.toString());
            }
        }

        return contentList;
    }

    public static GenericValue getFirstWorkEffortContentByType(String workEffortId, GenericValue workEffort, String workEffortContentTypeId, GenericDelegator delegator) {
        if (workEffortId == null && workEffort != null) {
            workEffortId = workEffort.getString("workEffortId");
        }

        if (delegator == null && workEffort != null) {
            delegator = workEffort.getDelegator();
        }

        if (delegator == null) {
            throw new IllegalArgumentException("GenericDelegator missing");
        }
        
        List workEffortContentList = null;
        try {
                workEffortContentList = delegator.findByAndCache("WorkEffortContent", UtilMisc.toMap("workEffortId", workEffortId, "workEffortContentTypeId", workEffortContentTypeId), UtilMisc.toList("-fromDate"));
        } catch (GeneralException e) {
            Debug.logError(e, module);
        }

        if (workEffortContentList != null) {
            workEffortContentList = EntityUtil.filterByDate(workEffortContentList);
            return EntityUtil.getFirst(workEffortContentList);
        } else {
            return null;
        }
    }

    public static WorkEffortContentWrapper makeWorkEffortContentWrapper(GenericValue workEffort, HttpServletRequest request) {
        return new WorkEffortContentWrapper(workEffort, request);
    }
}

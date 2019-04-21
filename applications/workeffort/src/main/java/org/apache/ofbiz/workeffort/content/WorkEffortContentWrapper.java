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
package org.apache.ofbiz.workeffort.content;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.GeneralException;
import org.apache.ofbiz.base.util.GeneralRuntimeException;
import org.apache.ofbiz.base.util.StringUtil;
import org.apache.ofbiz.base.util.UtilCodec;
import org.apache.ofbiz.base.util.UtilHttp;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.base.util.cache.UtilCache;
import org.apache.ofbiz.content.content.ContentWorker;
import org.apache.ofbiz.content.content.ContentWrapper;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.model.ModelEntity;
import org.apache.ofbiz.entity.model.ModelUtil;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.entity.util.EntityUtilProperties;
import org.apache.ofbiz.service.LocalDispatcher;

/**
 * WorkEffortContentWrapper; gets work effort content for display
 */
public class WorkEffortContentWrapper implements ContentWrapper {

    public static final String module = WorkEffortContentWrapper.class.getName();
    public static final String CACHE_KEY_SEPARATOR = "::";

    private static final UtilCache<String, String> workEffortContentCache = UtilCache.createUtilCache("workeffort.content.rendered", true);

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
        this.mimeTypeId = EntityUtilProperties.getPropertyValue("content", "defaultMimeType", "text/html; charset=utf-8", (Delegator) request.getAttribute("delegator"));
    }

    // interface implementation(s)
    public String get(String workEffortContentId, boolean useCache, String encoderType) {
        return getWorkEffortContentAsText(workEffort, workEffortContentId, locale, mimeTypeId, workEffort.getDelegator(), dispatcher, useCache, encoderType);
    }

    /**
     * Get the most current content data by the defined type
     * @param contentTypeId Type of content to return
     * @return String containing the content data
     */
    public StringUtil.StringWrapper get(String contentTypeId, String encoderType) {
        return StringUtil.makeStringWrapper(get(contentTypeId, true, encoderType));
    }

    /**
     * Get the ID from the most current content data by the defined type
     * @param contentTypeId Type of content to return
     * @return String containing the contentId
     */
    public String getContentId(String contentTypeId) {
        GenericValue workEffortContent = getFirstWorkEffortContentByType(null, workEffort, contentTypeId, workEffort.getDelegator(), true);
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
        GenericValue workEffortContent = getFirstWorkEffortContentByType(null, workEffort, contentTypeId, workEffort.getDelegator(), true);
        if (workEffortContent != null) {
            GenericValue content;
            try {
                content = workEffortContent.getRelatedOne("Content", false);
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
        GenericValue workEffortContent = getFirstWorkEffortContentByType(null, workEffort, contentTypeId, workEffort.getDelegator(), true);
        if (workEffortContent != null) {
            return workEffortContent.getTimestamp("fromDate");
        } else {
            return null;
        }
    }

    public String getDataResourceId(String contentTypeId) {
        GenericValue workEffortContent = getFirstWorkEffortContentByType(null, workEffort, contentTypeId, workEffort.getDelegator(), true);
        if (workEffortContent != null) {
            GenericValue content;
            try {
                content = workEffortContent.getRelatedOne("Content", false);
            } catch (GeneralException e) {
                Debug.logError(e, module);
                return null;
            }
            if (content != null) {
                GenericValue dataResource;
                try {
                    dataResource = content.getRelatedOne("DataResource", false);
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

    public List<String> getList(String contentTypeId) {
        try {
            return getWorkEffortContentTextList(workEffort, contentTypeId, locale, mimeTypeId, workEffort.getDelegator(), dispatcher);
        } catch (Exception e) {
            Debug.logError(e, module);
            return null;
        }
    }

    public String getTypeDescription(String contentTypeId) {
        Delegator delegator = null;
        if (workEffort != null) {
            delegator = workEffort.getDelegator();
        }

        if (delegator != null) {
            GenericValue contentType = null;
            try {
                contentType = EntityQuery.use(delegator).from("WorkEffortContentType").where("workEffortContentTypeId", contentTypeId).cache().queryOne();
            } catch (GeneralException e) {
                Debug.logError(e, module);
            }

            if (contentType != null) {
                return contentType.getString("description");
            }
        }

        return null;
    }

    public String getContent(String contentId, boolean useCache, String encoderType) {
        return getWorkEffortContentAsText(workEffort, contentId, null, locale, mimeTypeId, workEffort.getDelegator(), dispatcher, useCache, encoderType);
    }

    public String getContent(String contentId, String encoderType) {
        return getContent(contentId, true, encoderType);
    }

    // static method helpers
     public static String getWorkEffortContentAsText(GenericValue workEffort, String workEffortContentTypeId, HttpServletRequest request, String encoderType) {
        LocalDispatcher dispatcher = (LocalDispatcher) request.getAttribute("dispatcher");
        String mimeTypeId = EntityUtilProperties.getPropertyValue("content", "defaultMimeType", "text/html; charset=utf-8", workEffort.getDelegator());
        return getWorkEffortContentAsText(workEffort, workEffortContentTypeId, UtilHttp.getLocale(request), mimeTypeId, workEffort.getDelegator(), dispatcher, true, encoderType);
    }

    public static String getWorkEffortContentAsText(GenericValue workEffort, String workEffortContentTypeId, Locale locale, LocalDispatcher dispatcher, String encoderType) {
        return getWorkEffortContentAsText(workEffort, workEffortContentTypeId, locale, null, null, dispatcher, true, encoderType);
    }

    public static String getWorkEffortContentAsText(GenericValue workEffort, String workEffortContentTypeId,
            Locale locale, String mimeTypeId, Delegator delegator, LocalDispatcher dispatcher, boolean useCache, String encoderType) {
        return getWorkEffortContentAsText(workEffort, null, workEffortContentTypeId, locale, mimeTypeId, delegator, dispatcher, useCache, encoderType);
    }

    public static String getWorkEffortContentAsText(GenericValue workEffort, String contentId, String workEffortContentTypeId,
            Locale locale, String mimeTypeId, Delegator delegator, LocalDispatcher dispatcher, boolean useCache, String encoderType) {
        if (workEffort == null) {
            return null;
        }

        UtilCodec.SimpleEncoder encoder = UtilCodec.getEncoder(encoderType);
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
            if (useCache) {
                String cachedValue = workEffortContentCache.get(cacheKey);
                if (cachedValue != null) {
                    return cachedValue;
                }
            }

            Writer outWriter = new StringWriter();
            getWorkEffortContentAsText(contentId, null, workEffort, workEffortContentTypeId, locale, mimeTypeId, delegator, dispatcher, outWriter, false);
            String outString = outWriter.toString();
            if (UtilValidate.isEmpty(outString)) {
                outString = workEffort.getModelEntity().isField(candidateFieldName) ? workEffort.getString(candidateFieldName): "";
                outString = outString == null? "" : outString;
            }
            outString = encoder.sanitize(outString, null);
            if (workEffortContentCache != null) {
                workEffortContentCache.put(cacheKey, outString);
            }
            return outString;
        } catch (GeneralException e) {
            Debug.logError(e, "Error rendering WorkEffortContent, inserting empty String", module);
            String candidateOut = workEffort.getModelEntity().isField(candidateFieldName) ? workEffort.getString(candidateFieldName): "";
            return candidateOut == null? "" : encoder.sanitize(candidateOut, null);
        } catch (IOException e) {
            Debug.logError(e, "Error rendering WorkEffortContent, inserting empty String", module);
            String candidateOut = workEffort.getModelEntity().isField(candidateFieldName) ? workEffort.getString(candidateFieldName): "";
            return candidateOut == null? "" : encoder.sanitize(candidateOut, null);
        }
    }

    public static void getWorkEffortContentAsText(String contentId, String workEffortId, GenericValue workEffort, String workEffortContentTypeId, Locale locale, String mimeTypeId, Delegator delegator, LocalDispatcher dispatcher, Writer outWriter) throws GeneralException, IOException {
        getWorkEffortContentAsText(contentId, null, workEffort, workEffortContentTypeId, locale, mimeTypeId, delegator, dispatcher, outWriter, true);
    }

    public static void getWorkEffortContentAsText(String contentId, String workEffortId, GenericValue workEffort, String workEffortContentTypeId, Locale locale, String mimeTypeId, Delegator delegator, LocalDispatcher dispatcher, Writer outWriter, boolean cache) throws GeneralException, IOException {
        if (workEffortId == null && workEffort != null) {
            workEffortId = workEffort.getString("workEffortId");
        }

        if (delegator == null && workEffort != null) {
            delegator = workEffort.getDelegator();
        }

        if (UtilValidate.isEmpty(mimeTypeId)) {
            mimeTypeId = EntityUtilProperties.getPropertyValue("content", "defaultMimeType", "text/html; charset=utf-8", delegator);
        }

        if (delegator == null) {
            throw new GeneralRuntimeException("Unable to find a delegator to use!");
        }

        // Honor work effort content over WorkEffort entity fields.
        GenericValue workEffortContent;
        if (contentId != null) {
            workEffortContent = EntityQuery.use(delegator).from("WorkEffortContent").where("workEffortId", workEffortId, "contentId", contentId).cache(cache).queryOne();
        } else {
            workEffortContent = getFirstWorkEffortContentByType(workEffortId, workEffort, workEffortContentTypeId, delegator, cache);
        }
        if (workEffortContent != null) {
            // when rendering the product content, always include the Product and ProductContent records that this comes from
            Map<String, Object> inContext = new HashMap<>();
            inContext.put("workEffort", workEffort);
            inContext.put("workEffortContent", workEffortContent);
            ContentWorker.renderContentAsText(dispatcher, workEffortContent.getString("contentId"), outWriter, inContext, locale, mimeTypeId, null, null, false);
            return;
        }
        
        // check for workeffort field
        String candidateFieldName = ModelUtil.dbNameToVarName(workEffortContentTypeId);
        ModelEntity workEffortModel = delegator.getModelEntity("WorkEffort");
        if (workEffortModel != null && workEffortModel.isField(candidateFieldName)) {
            if (workEffort == null) {
                workEffort = EntityQuery.use(delegator).from("WorkEffort").where("workEffortId", workEffortId).cache().queryOne();
            }
            if (workEffort != null) {
                String candidateValue = workEffort.getString(candidateFieldName);
                if (UtilValidate.isNotEmpty(candidateValue)) {
                    outWriter.write(candidateValue);
                    return;
                }
            }
        }
    }

    public static List<String> getWorkEffortContentTextList(GenericValue workEffort, String workEffortContentTypeId, Locale locale, String mimeTypeId, Delegator delegator, LocalDispatcher dispatcher) throws GeneralException, IOException {
        List<GenericValue> partyContentList = EntityQuery.use(delegator).from("WorkEffortContent")
                .where("workEffortId", workEffort.getString("partyId"), "workEffortContentTypeId", workEffortContentTypeId)
                .orderBy("-fromDate")
                .cache(true)
                .filterByDate()
                .queryList();

        List<String> contentList = new LinkedList<>();
        if (partyContentList != null) {
            for (GenericValue workEffortContent: partyContentList) {
                StringWriter outWriter = new StringWriter();
                Map<String, Object> inContext = new HashMap<>();
                inContext.put("workEffort", workEffort);
                inContext.put("workEffortContent", workEffortContent);
                ContentWorker.renderContentAsText(dispatcher, workEffortContent.getString("contentId"), outWriter, inContext, locale, mimeTypeId, null, null, false);
                contentList.add(outWriter.toString());
            }
        }

        return contentList;
    }

    public static GenericValue getFirstWorkEffortContentByType(String workEffortId, GenericValue workEffort, String workEffortContentTypeId, Delegator delegator, boolean cache) {
        if (workEffortId == null && workEffort != null) {
            workEffortId = workEffort.getString("workEffortId");
        }

        if (delegator == null && workEffort != null) {
            delegator = workEffort.getDelegator();
        }

        if (delegator == null) {
            throw new IllegalArgumentException("Delegator missing");
        }

        GenericValue workEffortContent = null;
        try {
            workEffortContent = EntityQuery.use(delegator).from("WorkEffortContent")
                                    .where("workEffortId", workEffortId, "workEffortContentTypeId", workEffortContentTypeId)
                                    .orderBy("-fromDate")
                                    .filterByDate()
                                    .cache(cache)
                                    .queryFirst();
        } catch (GeneralException e) {
            Debug.logError(e, module);
        }
        return workEffortContent;
    }

    public static WorkEffortContentWrapper makeWorkEffortContentWrapper(GenericValue workEffort, HttpServletRequest request) {
        return new WorkEffortContentWrapper(workEffort, request);
    }
}

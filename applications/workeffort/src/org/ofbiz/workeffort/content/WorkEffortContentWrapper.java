/*
 *
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
package org.ofbiz.workeffort.content;

import java.util.Locale;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.io.Writer;
import java.io.IOException;
import java.io.StringWriter;
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

/**
 * WorkEffortContentWrapper; gets work effort content for display
 */
public class WorkEffortContentWrapper implements ContentWrapper {

    public static final String module = WorkEffortContentWrapper.class.getName();
    public static final String CACHE_KEY_SEPARATOR = "::";
    
    public static UtilCache workEffortContentCache = new UtilCache("workeffort.content.rendered", true);

    protected GenericValue workEffort;
    protected Locale locale;
    protected String mimeTypeId;

    public WorkEffortContentWrapper(GenericValue workEffort, Locale locale, String mimeTypeId) {
        this.workEffort = workEffort;
        this.locale = locale;
        this.mimeTypeId = mimeTypeId;
    }

    public WorkEffortContentWrapper(GenericValue workEffort, HttpServletRequest request) {
        this.workEffort = workEffort;
        this.locale = UtilHttp.getLocale(request);
        this.mimeTypeId = "text/html";
    }

    // interface implementation
    public String get(String workEffortContentId) {
        return getWorkEffortContentAsText(workEffort, workEffortContentId, locale, mimeTypeId, workEffort.getDelegator());
    }

    // static method helpers
    public static String getWorkEffortContentAsText(GenericValue workEffort, String workEffortContentId, HttpServletRequest request) {
        return getWorkEffortContentAsText(workEffort, workEffortContentId, UtilHttp.getLocale(request), "text/html", workEffort.getDelegator());
    }

    public static String getWorkEffortContentAsText(GenericValue workEffort, String workEffortContentId, Locale locale) {
        return getWorkEffortContentAsText(workEffort, workEffortContentId, locale, null, null);
    }

    public static String getWorkEffortContentAsText(GenericValue workEffort, String workEffortContentTypeId,
            Locale locale, String mimeTypeId, GenericDelegator delegator) {
        if (workEffort == null) {
            return null;
        }

        String candidateFieldName = ModelUtil.dbNameToVarName(workEffortContentTypeId);
        String cacheKey = workEffortContentTypeId + CACHE_KEY_SEPARATOR + locale + CACHE_KEY_SEPARATOR + mimeTypeId +
                CACHE_KEY_SEPARATOR + workEffort.get("workEffortId");

        try {
            if (workEffortContentCache.get(cacheKey) != null) {
                return (String) workEffortContentCache.get(cacheKey);
            }

            Writer outWriter = new StringWriter();
            getWorkEffortContentAsText(null, workEffort, workEffortContentTypeId, locale, mimeTypeId, delegator, outWriter);
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

    public static void getWorkEffortContentAsText(String workEffortId, GenericValue workEffort, String workEffortContentTypeId, Locale locale, String mimeTypeId, GenericDelegator delegator, Writer outWriter) throws GeneralException, IOException {
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

        List workEffortContentList = delegator.findByAndCache("WorkEffortContent", UtilMisc.toMap("workEffortId", workEffortId, "workEffortContentTypeId", workEffortContentTypeId), UtilMisc.toList("-fromDate"));
        workEffortContentList = EntityUtil.filterByDate(workEffortContentList);
        GenericValue workEffortContent = EntityUtil.getFirst(workEffortContentList);
        if (workEffortContent != null) {
            // when rendering the product content, always include the Product and ProductContent records that this comes from
            Map inContext = new HashMap();
            inContext.put("workEffort", workEffort);
            inContext.put("workEffortContent", workEffortContent);
            ContentWorker.renderContentAsText(delegator, workEffortContent.getString("contentId"), outWriter, inContext, null, locale, mimeTypeId);
        }
    }

    public static WorkEffortContentWrapper makeWorkEffortContentWrapper(GenericValue workEffort, HttpServletRequest request) {
        return new WorkEffortContentWrapper(workEffort, request);
    }
}

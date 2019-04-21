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

package org.apache.ofbiz.party.content;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
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
import org.apache.ofbiz.entity.util.EntityUtil;
import org.apache.ofbiz.entity.util.EntityUtilProperties;
import org.apache.ofbiz.service.LocalDispatcher;

/**
 * WorkEffortContentWrapper; gets work effort content for display
 */
public class PartyContentWrapper implements ContentWrapper {

    public static final String module = PartyContentWrapper.class.getName();
    public static final String CACHE_KEY_SEPARATOR = "::";

    private static final UtilCache<String, String> partyContentCache = UtilCache.createUtilCache("party.content.rendered", true);

    protected LocalDispatcher dispatcher;
    protected GenericValue party;
    protected Locale locale;
    protected String mimeTypeId;

    public PartyContentWrapper(LocalDispatcher dispatcher, GenericValue party, Locale locale, String mimeTypeId) {
        this.dispatcher = dispatcher;
        this.party = party;
        this.locale = locale;
        this.mimeTypeId = mimeTypeId;
    }

    public PartyContentWrapper(GenericValue party, HttpServletRequest request) {
        this.dispatcher = (LocalDispatcher) request.getAttribute("dispatcher");
        this.party = party;
        this.locale = UtilHttp.getLocale(request);
        this.mimeTypeId = EntityUtilProperties.getPropertyValue("content", "defaultMimeType", "text/html; charset=utf-8", (Delegator) request.getAttribute("delegator"));
    }

    // interface implementation
    public String get(String contentTypeId, boolean useCache, String encoderType) {
        return getPartyContentAsText(party, contentTypeId, locale, mimeTypeId, party.getDelegator(), dispatcher, useCache, encoderType);
    }

    public StringUtil.StringWrapper get(String contentTypeId, String encoderType) {
        return StringUtil.makeStringWrapper(get(contentTypeId, true, encoderType));
    }

    public String getId(String contentTypeId) {
        GenericValue partyContent = getFirstPartyContentByType(null, party, contentTypeId, party.getDelegator());
        if (partyContent != null) {
            return partyContent.getString("contentId");
        } else {
            return null;
        }
    }

    public List<String> getList(String contentTypeId) {
        try {
            return getPartyContentTextList(party, contentTypeId, locale, mimeTypeId, party.getDelegator(), dispatcher);
        } catch (GeneralException ge) {
            Debug.logError(ge, module);
            return null;
        } catch (IOException ioe) {
            Debug.logError(ioe, module);
            return null;
        } catch (Exception e) {
            Debug.logError(e, module);
            return null;
        }
    }

    public String getContent(String contentId, boolean useCache, String encoderType) {
        return getPartyContentAsText(party, contentId, null, locale, mimeTypeId, party.getDelegator(), dispatcher, useCache, encoderType);
    }

    public String getContent(String contentId, String encoderType) {
        return getContent(contentId, true, encoderType);
    }

    // static methods
    public static String getPartyContentAsText(GenericValue party, String partyContentId, HttpServletRequest request, String encoderType) {
        LocalDispatcher dispatcher = (LocalDispatcher) request.getAttribute("dispatcher");
        String mimeTypeId = EntityUtilProperties.getPropertyValue("content", "defaultMimeType", "text/html; charset=utf-8", party.getDelegator());
        return getPartyContentAsText(party, partyContentId, null, UtilHttp.getLocale(request), mimeTypeId, party.getDelegator(), dispatcher, true,encoderType);
    }

    public static String getPartyContentAsText(GenericValue party, String partyContentId, Locale locale, LocalDispatcher dispatcher, String encoderType) {
        return getPartyContentAsText(party, partyContentId, null, locale, null, null, dispatcher, true, encoderType);
    }

    public static String getPartyContentAsText(GenericValue party, String partyContentTypeId,
            Locale locale, String mimeTypeId, Delegator delegator, LocalDispatcher dispatcher, boolean useCache, String encoderType) {
        return getPartyContentAsText(party, null, partyContentTypeId, locale, mimeTypeId, delegator, dispatcher, useCache, encoderType);
    }

    public static String getPartyContentAsText(GenericValue party, String contentId, String partyContentTypeId,
            Locale locale, String mimeTypeId, Delegator delegator, LocalDispatcher dispatcher, boolean useCache, String encoderType) {
        if (party == null) {
            return null;
        }
        
        UtilCodec.SimpleEncoder encoder = UtilCodec.getEncoder(encoderType);
        String candidateFieldName = ModelUtil.dbNameToVarName(partyContentTypeId);
        String cacheKey;
        if (contentId != null) {
            cacheKey = contentId + CACHE_KEY_SEPARATOR + locale + CACHE_KEY_SEPARATOR + mimeTypeId +
                    CACHE_KEY_SEPARATOR + party.get("partyId");
        } else {
            cacheKey = partyContentTypeId + CACHE_KEY_SEPARATOR + locale + CACHE_KEY_SEPARATOR + mimeTypeId +
                    CACHE_KEY_SEPARATOR + party.get("partyId");
        }

        try {
            if (useCache) {
                String cachedValue = partyContentCache.get(cacheKey);
                if (cachedValue != null) {
                    return cachedValue;
                }
            }

            Writer outWriter = new StringWriter();
            getPartyContentAsText(contentId, party.getString("partyId"), party, partyContentTypeId, locale, mimeTypeId, delegator, dispatcher, outWriter, false);

            String outString = outWriter.toString();
            if (UtilValidate.isEmpty(outString)) {
                outString = party.getModelEntity().isField(candidateFieldName) ? party.getString(candidateFieldName): "";
                outString = outString == null? "" : outString;
            }
            outString = encoder.sanitize(outString, null);
            if (partyContentCache != null) {
                partyContentCache.put(cacheKey, outString);
            }
            return outString;
        } catch (GeneralException e) {
            Debug.logError(e, "Error rendering PartyContent, inserting empty String", module);
            String candidateOut = party.getModelEntity().isField(candidateFieldName) ? party.getString(candidateFieldName): "";
            return candidateOut == null? "" : encoder.sanitize(candidateOut, null);
        } catch (IOException e) {
            Debug.logError(e, "Error rendering PartyContent, inserting empty String", module);
            String candidateOut = party.getModelEntity().isField(candidateFieldName) ? party.getString(candidateFieldName): "";
            return candidateOut == null? "" : encoder.sanitize(candidateOut, null);
        }
    }

    public static void getPartyContentAsText(String contentId, String partyId, GenericValue party, String partyContentTypeId, Locale locale, String mimeTypeId, Delegator delegator, LocalDispatcher dispatcher, Writer outWriter) throws GeneralException, IOException {
        getPartyContentAsText(contentId, partyId, party, partyContentTypeId, locale, mimeTypeId, delegator, dispatcher, outWriter, true);
    }

    public static void getPartyContentAsText(String contentId, String partyId, GenericValue party, String partyContentTypeId, Locale locale, String mimeTypeId, Delegator delegator, LocalDispatcher dispatcher, Writer outWriter, boolean cache) throws GeneralException, IOException {
        if (partyId == null && party != null) {
            partyId = party.getString("partyId");
        }

        if (delegator == null && party != null) {
            delegator = party.getDelegator();
        }

        if (UtilValidate.isEmpty(mimeTypeId)) {
            mimeTypeId = EntityUtilProperties.getPropertyValue("content", "defaultMimeType", "text/html; charset=utf-8", delegator);
        }

        if (delegator == null) {
            throw new GeneralRuntimeException("Unable to find a delegator to use!");
        }

        // Honor party content over Party entity fields.
        GenericValue partyContent;
        if (contentId != null) {
            partyContent = EntityQuery.use(delegator).from("PartyContent").where("partyId", partyId, "contentId", contentId).cache(cache).queryOne();
        } else {
            partyContent = getFirstPartyContentByType(partyId, party, partyContentTypeId, delegator);
        }
        if (partyContent != null) {
            // when rendering the product content, always include the Product and ProductContent records that this comes from
            Map<String, Object> inContext = new HashMap<>();
            inContext.put("party", party);
            inContext.put("partyContent", partyContent);
            ContentWorker.renderContentAsText(dispatcher, partyContent.getString("contentId"), outWriter, inContext, locale, mimeTypeId, null, null, cache);
            return;
        }
        
        if (partyContentTypeId != null) {
            String candidateFieldName = ModelUtil.dbNameToVarName(partyContentTypeId);

            // first check for a person field
            ModelEntity partyPersonModel = delegator.getModelEntity("PartyAndPerson");
            if (partyPersonModel != null && partyPersonModel.isField(candidateFieldName)) {
                if (party == null) {
                    party = EntityQuery.use(delegator).from("PartyAndPerson").where("partyId", partyId).cache().queryOne();
                }
                if (party != null) {
                    String candidateValue = party.getString(candidateFieldName);
                    if (UtilValidate.isNotEmpty(candidateValue)) {
                        outWriter.write(candidateValue);
                        return;
                    }
                }
            }

            // next check for group field
            ModelEntity partyGroupModel = delegator.getModelEntity("PartyAndGroup");
            if (partyGroupModel != null && partyGroupModel.isField(candidateFieldName)) {
                if (party == null) {
                    party = EntityQuery.use(delegator).from("PartyAndGroup").where("partyId", partyId).cache().queryOne();
                }
                if (party != null) {
                    String candidateValue = party.getString(candidateFieldName);
                    if (UtilValidate.isNotEmpty(candidateValue)) {
                        outWriter.write(candidateValue);
                        return;
                    }
                }
            }
        }
    }

    public static List<String> getPartyContentTextList(GenericValue party, String partyContentTypeId, Locale locale, String mimeTypeId, Delegator delegator, LocalDispatcher dispatcher) throws GeneralException, IOException {
        List<GenericValue> partyContentList = EntityQuery.use(delegator).from("PartyContent")
                .where("partyId", party.getString("partyId"), "partyContentTypeId", partyContentTypeId)
                .orderBy("-fromDate")
                .cache(true)
                .filterByDate()
                .queryList();

        List<String> contentList = new LinkedList<>();
        if (partyContentList != null) {
            for (GenericValue partyContent: partyContentList) {
                StringWriter outWriter = new StringWriter();
                Map<String, Object> inContext = new HashMap<>();
                inContext.put("party", party);
                inContext.put("partyContent", partyContent);
                ContentWorker.renderContentAsText(dispatcher, partyContent.getString("contentId"), outWriter, inContext, locale, mimeTypeId, null, null, false);
                contentList.add(outWriter.toString());
            }
        }

        return contentList;
    }

    public static GenericValue getFirstPartyContentByType(String partyId, GenericValue party, String partyContentTypeId, Delegator delegator) {
        if (partyId == null && party != null) {
            partyId = party.getString("partyId");
        }

        if (delegator == null && party != null) {
            delegator = party.getDelegator();
        }

        if (delegator == null) {
            throw new IllegalArgumentException("Delegator missing");
        }

        List<GenericValue> partyContentList = null;
        try {
            partyContentList = EntityQuery.use(delegator).from("PartyContent")
                    .where("partyId", partyId, "partyContentTypeId", partyContentTypeId)
                    .orderBy("-fromDate")
                    .cache(true)
                    .queryList();
        } catch (GeneralException e) {
            Debug.logError(e, module);
        }

        if (partyContentList != null) {
            partyContentList = EntityUtil.filterByDate(partyContentList);
            return EntityUtil.getFirst(partyContentList);
        } else {
            return null;
        }
    }

    public static PartyContentWrapper makePartyContentWrapper(GenericValue party, HttpServletRequest request) {
        return new PartyContentWrapper(party, request);
    }
}

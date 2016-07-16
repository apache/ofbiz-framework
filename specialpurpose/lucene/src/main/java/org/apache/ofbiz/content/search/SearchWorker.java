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
package org.apache.ofbiz.content.search;

import java.util.ArrayList;
import java.util.List;

import org.apache.lucene.util.Version;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilDateTime;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilProperties;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.content.content.ContentWorker;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.service.LocalDispatcher;

/**
 * SearchWorker Class
 */
public final class SearchWorker {

    public static final String module = SearchWorker.class.getName();

    private static final Version LUCENE_VERSION = Version.LUCENE_5_3_1;

    private SearchWorker() {}

    public static void indexContentTree(LocalDispatcher dispatcher, Delegator delegator, String siteId) throws Exception {
        GenericValue content = delegator.makeValue("Content", UtilMisc.toMap("contentId", siteId));
        List<GenericValue> siteList = ContentWorker.getAssociatedContent(content, "To", UtilMisc.toList("SUBSITE", "PUBLISH_LINK", "SUB_CONTENT"), null, UtilDateTime.nowTimestamp().toString(), null);

        if (siteList != null) {
            for (GenericValue siteContent : siteList) {
                String siteContentId = siteContent.getString("contentId");
                List<GenericValue> subContentList = ContentWorker.getAssociatedContent(siteContent, "To", UtilMisc.toList("SUBSITE", "PUBLISH_LINK", "SUB_CONTENT"), null, UtilDateTime.nowTimestamp().toString(), null);

                if (subContentList != null) {
                    List<String> contentIdList = new ArrayList<String>();
                    for (GenericValue subContent : subContentList) {
                        contentIdList.add(subContent.getString("contentId"));
                    }
                    indexContentList(dispatcher, delegator, contentIdList);
                    indexContentTree(dispatcher, delegator, siteContentId);
                }
            }
        }
    }

    public static String getIndexPath(String path) {
        String basePath = UtilProperties.getPropertyValue("lucene", "defaultIndex", "index");
        return (UtilValidate.isNotEmpty(path)? basePath + "/" + path: basePath);
    }

    public static void indexContentList(LocalDispatcher dispatcher, Delegator delegator, List<String> idList) throws Exception {
        DocumentIndexer indexer = DocumentIndexer.getInstance(delegator, "content");
        List<GenericValue> contentList = new ArrayList<GenericValue>();
        for (String id : idList) {
            try {
                GenericValue content = EntityQuery.use(delegator).from("Content").where("contentId", id).cache(true).queryOne();
                if (content != null) {
                    contentList.add(content);
                }
            } catch (GenericEntityException e) {
                Debug.logError(e, module);
                return;
            }
        }
        for (GenericValue content : contentList) {
            indexer.queue(new ContentDocument(content, dispatcher));
        }
    }

    public static Version getLuceneVersion() {
        return LUCENE_VERSION;
    }
}

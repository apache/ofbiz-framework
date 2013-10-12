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
package org.ofbiz.content.search;

import java.io.File;
import java.io.IOException;
import java.lang.String;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilDateTime;
import org.ofbiz.base.util.UtilGenerics;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilProperties;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.content.content.ContentWorker;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.service.LocalDispatcher;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.LockObtainFailedException;
import org.apache.lucene.util.Version;

/**
 * SearchWorker Class
 */
public class SearchWorker {

    public static final String module = SearchWorker.class.getName();

    public static final Version LUCENE_VERSION = Version.LUCENE_45;

    public static Map<String, Object> indexTree(LocalDispatcher dispatcher, Delegator delegator, String siteId, Map<String, Object> context) throws Exception {
        GenericValue content = delegator.makeValue("Content", UtilMisc.toMap("contentId", siteId));
        if (Debug.infoOn()) Debug.logInfo("in indexTree, siteId:" + siteId + " content:" + content, module);
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
                    indexContentList(dispatcher, delegator, context, contentIdList);
                    indexTree(dispatcher, delegator, siteContentId, context);
                } else {
                    List<String> badIndexList = UtilGenerics.checkList(context.get("badIndexList"));
                    badIndexList.add(siteContentId + " had no sub-entities.");
                }
            }
        } else {
            List<String> badIndexList = UtilGenerics.checkList(context.get("badIndexList"));
            badIndexList.add(siteId + " had no sub-entities.");
        }

        return UtilMisc.toMap("badIndexList", context.get("badIndexList"), "goodIndexCount", context.get("goodIndexCount"));
    }

    public static String getIndexPath(String path) {
        String basePath = UtilProperties.getPropertyValue("search", "defaultIndex", "index");
        return (UtilValidate.isNotEmpty(path)? basePath + "/" + path: basePath);
    }

    private static IndexWriter getDefaultIndexWriter(Directory directory) {
        IndexWriter writer = null;
        long savedWriteLockTimeout = IndexWriterConfig.getDefaultWriteLockTimeout();
        Analyzer analyzer = new StandardAnalyzer(LUCENE_VERSION);
        IndexWriterConfig conf = new IndexWriterConfig(LUCENE_VERSION, analyzer);
        IndexWriterConfig.setDefaultWriteLockTimeout(2000);
        try {
            writer  = new IndexWriter(directory, conf);
        } catch (CorruptIndexException e) {
            Debug.logError("Corrupted lucene index: "  + e.getMessage(), module);
        } catch (LockObtainFailedException e) {
            Debug.logError("Could not obtain Lock on lucene index "  + e.getMessage(), module);
        } catch (IOException e) {
            Debug.logError(e.getMessage(), module);
        } finally {
            IndexWriterConfig.setDefaultWriteLockTimeout(savedWriteLockTimeout);
        }
        return writer;
    }

    public static void indexContentList(LocalDispatcher dispatcher, Delegator delegator, Map<String, Object> context,List<String> idList) throws Exception {
        Directory directory = FSDirectory.open(new File(getIndexPath("content")));
        if (Debug.infoOn()) Debug.logInfo("in indexContentList, indexAllPath: " + directory.toString(), module);
        // Delete existing documents
        IndexWriter writer = getDefaultIndexWriter(directory);
        List<GenericValue> contentList = new ArrayList<GenericValue>();
        for (String id : idList) {
            if (Debug.infoOn()) Debug.logInfo("in indexContentList, id:" + id, module);
            try {
                GenericValue content = delegator.findOne("Content", UtilMisc .toMap("contentId", id), true);
                if (content != null) {
                    if (writer != null) {
                        deleteContentDocuments(content, writer);
                    }
                    contentList.add(content);
                }
            } catch (GenericEntityException e) {
                Debug.logError(e, module);
                return;
            }
        }
        for (GenericValue gv : contentList) {
            indexContent(dispatcher, context, gv, writer);
        }
        try {
            writer.forceMerge(1);
        } catch (NullPointerException e) {
            Debug.logError(e, module);
        }
        writer.close();
    }

    private static void deleteContentDocuments(GenericValue content, IndexWriter writer) throws Exception {
        String contentId = content.getString("contentId");
        Term term = new Term("contentId", contentId);
        deleteDocumentsByTerm(term, writer);
        String dataResourceId = content.getString("dataResourceId");
        if (dataResourceId != null) {
            term = new Term("dataResourceId", dataResourceId);
            deleteDocumentsByTerm(term, writer);
        }
    }

    private static void deleteDocumentsByTerm(Term term, IndexWriter writer) throws Exception {
        DirectoryReader reader = DirectoryReader.open(writer, false);
        int qtyBefore = reader.docFreq(term);

        //deletes documents, all the rest is for logging
        writer.deleteDocuments(term);

        int qtyAfter = reader.docFreq(term);
        reader.close();

        if (Debug.infoOn()) Debug.logInfo("For term " + term.toString() + ", documents deleted: " + qtyBefore + ", remaining: " + qtyAfter, module);
    }

    private static void indexContent(LocalDispatcher dispatcher, Map<String, Object> context, GenericValue content, IndexWriter writer) throws Exception {
        Document doc = ContentDocument.Document(content, context, dispatcher);

        if (doc != null) {
            writer.addDocument(doc);
            Integer goodIndexCount = (Integer)context.get("goodIndexCount");
            Integer newIndexCount = goodIndexCount + 1;
            context.put("goodIndexCount", newIndexCount);
        }
    }

}
